/*
 * Copyright (C) 2004-2022 L2J Mobius
 * 
 * This file is part of L2J Mobius.
 * 
 * L2J Mobius is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Mobius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.ai;

import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Autobot;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/**
 * Enhanced combat AI for autobots with tactical positioning,
 * skill rotation, and intelligent combat decisions.
 * @author YourName
 */
public class CombatAI
{
	private static final Logger LOGGER = Logger.getLogger(CombatAI.class.getName());
	
	public enum CombatTactic
	{
		AGGRESSIVE,    // Close combat, high damage
		DEFENSIVE,     // Defensive positioning, survival focus
		BALANCED,      // Mix of offense and defense
		SUPPORT,       // Focus on healing/buffing
		KITING         // Hit and run tactics
	}
	
	/**
	 * Execute advanced combat behavior with target persistence
	 */
	public static boolean executeCombat(Autobot autobot, Creature target)
	{
		Player player = autobot.getPlayer();
		if (player == null || target == null || target.isDead())
		{
			return false;
		}
		
		// CRITICAL SAFETY CHECK: Never attack players - only monsters
		if (target instanceof Player)
		{
			LOGGER.warning("SAFETY: Autobot " + player.getName() + " attempted to attack player " + target.getName() + ", blocking attack!");
			player.setTarget(null);
			return false;
		}
		
		// Enhanced spam prevention - check if already performing any action
		if (player.isAttackingNow() || player.isCastingNow())
		{
			return true; // Already doing something, don't spam commands
		}
		
		// Don't interrupt movement to target
		if (player.isMoving() && player.getAI().getIntention() == Intention.MOVE_TO)
		{
			// Check if we're moving towards our target
			Location targetLoc = target.getLocation();
			
			double distanceToTarget = player.calculateDistance2D(target);
			double attackRange = isMage(player) ? 600 : player.getPhysicalAttackRange();
			
			// If we're close enough to attack, stop moving and attack
			if (distanceToTarget <= attackRange)
			{
				player.getAI().setIntention(Intention.IDLE);
			}
			else
			{
				return true; // Continue moving
			}
		}
		
		// Check if target is in range
		double distance = player.calculateDistance2D(target);
		double attackRange = isMage(player) ? 600 : player.getPhysicalAttackRange();
		
		if (distance > attackRange)
		{
			// Move closer to target
			Location moveToLoc = calculateOptimalPosition(player, target, (int)(attackRange * 0.8));
			if (moveToLoc != null)
			{
				player.getAI().setIntention(Intention.MOVE_TO, moveToLoc);
				return true;
			}
		}
		else
		{
			// In attack range - perform attack based on class and race
			if (isMage(player))
			{
				// Check if Orc mage - they always use physical attacks
				if (player.getTemplate().getPlayerClass().getRace() == org.l2jmobius.gameserver.model.actor.enums.creature.Race.ORC)
				{
					return executeFighterAttack(player, target);
				}
				else
				{
					// Non-Orc mages always use magic
					return executeMageAttack(player, target);
				}
			}
			else
			{
				return executeFighterAttack(player, target);
			}
		}
		
		return false;
	}
	
	/**
	 * Execute mage attack using Wind Strike skill - STRICTLY MAGIC ONLY for non-Orc mages
	 */
	private static boolean executeMageAttack(Player player, Creature target)
	{
		// Safety check: Never attack players
		if (target instanceof Player)
		{
			LOGGER.warning("SAFETY: Preventing mage attack on player " + target.getName());
			return false;
		}
		
		// Non-Orc mages have infinite mana and should ONLY use Wind Strike - NO PHYSICAL ATTACKS EVER
		org.l2jmobius.gameserver.model.actor.enums.creature.Race race = player.getTemplate().getPlayerClass().getRace();
		if (race != org.l2jmobius.gameserver.model.actor.enums.creature.Race.ORC)
		{
			// CRITICAL: Non-Orc mages must NEVER use physical attacks
			// Check if already casting - don't interrupt and don't try physical attack
			if (player.isCastingNow())
			{
				LOGGER.fine("Non-Orc mage " + player.getName() + " is already casting - waiting");
				return true; // Already casting, wait for completion
			}
			
			// Ensure mage has infinite mana
			if (player.getCurrentMp() < 50)
			{
				player.setCurrentMp(999999); // Restore infinite mana
				LOGGER.fine("Restored infinite mana to non-Orc mage " + player.getName());
			}
			
			// Get Wind Strike skill (ID 1177)
			Skill windStrike = player.getKnownSkill(1177);
			if (windStrike != null)
			{
				// CRITICAL: Check if Wind Strike is on cooldown
				if (player.isSkillDisabled(windStrike))
				{
					LOGGER.fine("Non-Orc mage " + player.getName() + " Wind Strike is on cooldown - waiting");
					return true; // Wait for cooldown, DO NOT use physical attack
				}
				
				// Use Wind Strike skill - set target and cast
				player.setTarget(target);
				player.useMagic(windStrike, null, false, false);
				LOGGER.fine("Non-Orc mage " + player.getName() + " casting Wind Strike on " + target.getName());
				return true;
			}
			else
			{
				// Critical: Mage without Wind Strike - try to add it immediately
				LOGGER.warning("CRITICAL: Non-Orc mage " + player.getName() + " missing Wind Strike skill - adding it now");
				
				// Get and add Wind Strike skill from SkillData
				org.l2jmobius.gameserver.model.skill.Skill windStrikeSkill = 
					org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1177, 1);
				
				if (windStrikeSkill != null)
				{
					player.addSkill(windStrikeSkill, true);
					LOGGER.info("Successfully added Wind Strike skill to mage " + player.getName());
					
					// Immediately use the skill
					player.setTarget(target);
					player.useMagic(windStrikeSkill, null, false, false);
					return true;
				}
				else
				{
					LOGGER.severe("FATAL: Cannot find Wind Strike skill (ID 1177) in SkillData - non-Orc mage CANNOT attack");
					// ABSOLUTELY NO PHYSICAL ATTACKS FOR NON-ORC MAGES - return false to skip this combat round
					return false;
				}
			}
		}
		else
		{
			// Orc mages should use physical attacks (this shouldn't be called for them)
			LOGGER.warning("Orc mage " + player.getName() + " incorrectly routed to magic attack, using physical attack");
			player.doAutoAttack(target);
			return true;
		}
	}
	
	/**
	 * Execute fighter attack using weapon with enhanced persistence
	 */
	private static boolean executeFighterAttack(Player player, Creature target)
	{
		// Safety check: Never attack players
		if (target instanceof Player)
		{
			LOGGER.warning("SAFETY: Preventing fighter attack on player " + target.getName());
			return false;
		}
		
		// Ensure we're targeting the right creature
		if (player.getTarget() != target)
		{
			player.setTarget(target);
		}
		
		// Use doAutoAttack for more persistent combat
		player.doAutoAttack(target);
		return true;
	}
	
	/**
	 * Check if player is a mage class
	 */
	private static boolean isMage(Player player)
	{
		return player.getTemplate().getPlayerClass().isMage();
	}
	
	private static CombatTactic determineCombatTactic(Autobot autobot, Creature target)
	{
		double hpPercent = (autobot.getPlayer().getCurrentHp() / autobot.getPlayer().getMaxHp()) * 100;
		
		if (hpPercent < 30)
		{
			return CombatTactic.DEFENSIVE;
		}
		
		if (autobot.getAggressionLevel() > 70)
		{
			return CombatTactic.AGGRESSIVE;
		}
		
		if (target.getLevel() > autobot.getLevel() + 5)
		{
			return CombatTactic.KITING;
		}
		
		return CombatTactic.BALANCED;
	}
	
	private static boolean executeAggressiveCombat(Player player, Creature target)
	{
		// Move to optimal attack range
		if (!player.isInsideRadius2D(target, player.getPhysicalAttackRange()))
		{
			Location targetLoc = calculateOptimalPosition(player, target, 40);
			if (targetLoc != null)
			{
				player.getAI().setIntention(Intention.MOVE_TO, targetLoc);
			}
		}
		else
		{
			// Attack with skills or regular attack - but respect mage rules
			if (shouldUseSkill(player, target))
			{
				Skill skill = selectBestSkill(player, target);
				if (skill != null)
				{
					player.useMagic(skill, null, false, false);
					return true;
				}
			}
			
			// CRITICAL: Use class-appropriate attack method
			if (isMage(player))
			{
				// Check if Orc mage - they use physical attacks
				if (player.getTemplate().getPlayerClass().getRace() == org.l2jmobius.gameserver.model.actor.enums.creature.Race.ORC)
				{
					player.doAutoAttack(target);
				}
				else
				{
					// Non-Orc mages MUST use magic only
					return executeMageAttack(player, target);
				}
			}
			else
			{
				player.doAutoAttack(target);
			}
		}
		
		return true;
	}
	
	private static boolean executeDefensiveCombat(Player player, Creature target)
	{
		// Try to create distance
		Location safePosition = calculateSafePosition(player, target);
		if (safePosition != null)
		{
			player.getAI().setIntention(Intention.MOVE_TO, safePosition);
		}
		
		// Use defensive skills
		if (player.getCurrentHp() < player.getMaxHp() * 0.5)
		{
			// Try to use healing skill
			Skill healSkill = findHealingSkill(player);
			if (healSkill != null)
			{
				player.useMagic(healSkill, null, false, false);
				return true;
			}
		}
		
		// Continue attacking if safe - but respect mage rules
		if (player.calculateDistance2D(target) <= player.getPhysicalAttackRange())
		{
			// CRITICAL: Use class-appropriate attack method
			if (isMage(player))
			{
				// Check if Orc mage - they use physical attacks
				if (player.getTemplate().getPlayerClass().getRace() == org.l2jmobius.gameserver.model.actor.enums.creature.Race.ORC)
				{
					player.doAutoAttack(target);
				}
				else
				{
					// Non-Orc mages MUST use magic only
					return executeMageAttack(player, target);
				}
			}
			else
			{
				player.doAutoAttack(target);
			}
		}
		
		return true;
	}
	
	private static boolean executeBalancedCombat(Player player, Creature target)
	{
		// Balance between offense and defense
		double distance = player.calculateDistance2D(target);
		double optimalRange = player.getPhysicalAttackRange() * 0.8;
		
		if (distance > optimalRange)
		{
			// Move closer
			Location closerPos = calculateOptimalPosition(player, target, (int)optimalRange);
			if (closerPos != null)
			{
				player.getAI().setIntention(Intention.MOVE_TO, closerPos);
			}
		}
		else if (distance < optimalRange * 0.5)
		{
			// Too close, back away slightly
			Location backPos = calculateSafePosition(player, target);
			if (backPos != null)
			{
				player.getAI().setIntention(Intention.MOVE_TO, backPos);
			}
		}
		else
		{
			// Good range, attack - but respect mage rules
			// CRITICAL: Use class-appropriate attack method
			if (isMage(player))
			{
				// Check if Orc mage - they use physical attacks
				if (player.getTemplate().getPlayerClass().getRace() == org.l2jmobius.gameserver.model.actor.enums.creature.Race.ORC)
				{
					player.doAutoAttack(target);
				}
				else
				{
					// Non-Orc mages MUST use magic only
					return executeMageAttack(player, target);
				}
			}
			else
			{
				player.doAutoAttack(target);
			}
		}
		
		return true;
	}
	
	private static boolean executeSupportCombat(Player player, Creature target)
	{
		// Focus on support actions - simplified for now
		return executeBalancedCombat(player, target);
	}
	
	private static boolean executeKitingCombat(Player player, Creature target)
	{
		// Hit and run tactics
		if (player.isAttackingNow() || player.isCastingNow())
		{
			return true; // Currently attacking, wait
		}
		
		double distance = player.calculateDistance2D(target);
		if (distance <= player.getPhysicalAttackRange())
		{
			// Attack then move - but respect mage rules
			// CRITICAL: Use class-appropriate attack method
			if (isMage(player))
			{
				// Check if Orc mage - they use physical attacks
				if (player.getTemplate().getPlayerClass().getRace() == org.l2jmobius.gameserver.model.actor.enums.creature.Race.ORC)
				{
					player.doAutoAttack(target);
				}
				else
				{
					// Non-Orc mages MUST use magic only
					executeMageAttack(player, target);
				}
			}
			else
			{
				player.doAutoAttack(target);
			}
			
			// Plan next move position
			Location retreatPos = calculateRetreatPosition(player, target);
			if (retreatPos != null)
			{
				player.getAI().setIntention(Intention.MOVE_TO, retreatPos);
			}
		}
		else
		{
			// Move to attack range
			Location attackPos = calculateOptimalPosition(player, target, player.getPhysicalAttackRange() - 20);
			if (attackPos != null)
			{
				player.getAI().setIntention(Intention.MOVE_TO, attackPos);
			}
		}
		
		return true;
	}
	
	// Helper methods
	private static Location calculateOptimalPosition(Player player, Creature target, int range)
	{
		Location targetLoc = target.getLocation();
		Location playerLoc = player.getLocation();
		
		double angle = Math.atan2(targetLoc.getY() - playerLoc.getY(), targetLoc.getX() - playerLoc.getX());
		
		int x = targetLoc.getX() - (int)(Math.cos(angle) * range);
		int y = targetLoc.getY() - (int)(Math.sin(angle) * range);
		int z = GeoEngine.getInstance().getHeight(x, y, targetLoc.getZ());
		
		Location newPos = new Location(x, y, z);
		
		return GeoEngine.getInstance().canMoveToTarget(playerLoc, newPos) ? newPos : null;
	}
	
	private static Location calculateSafePosition(Player player, Creature target)
	{
		Location playerLoc = player.getLocation();
		Location targetLoc = target.getLocation();
		
		// Move away from target
		double angle = Math.atan2(playerLoc.getY() - targetLoc.getY(), playerLoc.getX() - targetLoc.getX());
		
		int distance = 150;
		int x = playerLoc.getX() + (int)(Math.cos(angle) * distance);
		int y = playerLoc.getY() + (int)(Math.sin(angle) * distance);
		int z = GeoEngine.getInstance().getHeight(x, y, playerLoc.getZ());
		
		Location safePos = new Location(x, y, z);
		
		return GeoEngine.getInstance().canMoveToTarget(playerLoc, safePos) ? safePos : null;
	}
	
	private static Location calculateRetreatPosition(Player player, Creature target)
	{
		// Similar to safe position but shorter distance for kiting
		Location playerLoc = player.getLocation();
		Location targetLoc = target.getLocation();
		
		double angle = Math.atan2(playerLoc.getY() - targetLoc.getY(), playerLoc.getX() - targetLoc.getX());
		
		int distance = 80;
		int x = playerLoc.getX() + (int)(Math.cos(angle) * distance);
		int y = playerLoc.getY() + (int)(Math.sin(angle) * distance);
		int z = GeoEngine.getInstance().getHeight(x, y, playerLoc.getZ());
		
		Location retreatPos = new Location(x, y, z);
		
		return GeoEngine.getInstance().canMoveToTarget(playerLoc, retreatPos) ? retreatPos : null;
	}
	
	private static boolean shouldUseSkill(Player player, Creature target)
	{
		// Simple skill usage logic - can be expanded
		return player.getCurrentMp() > player.getMaxMp() * 0.3 &&
			   !player.isCastingNow() && 
			   Math.random() < 0.3; // 30% chance to use skill
	}
	
	private static Skill selectBestSkill(Player player, Creature target)
	{
		// Simple skill selection - return first available offensive skill
		return player.getSkills().values().stream()
			.filter(skill -> skill.isMagic() && !player.isSkillDisabled(skill))
			.findFirst()
			.orElse(null);
	}
	
	private static Skill findHealingSkill(Player player)
	{
		// Find first available healing skill
		return player.getSkills().values().stream()
			.filter(skill -> skill.hasEffectType(org.l2jmobius.gameserver.model.effects.EffectType.HEAL))
			.findFirst()
			.orElse(null);
	}
}