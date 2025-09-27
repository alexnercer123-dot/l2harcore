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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.AutobotManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Autobot;
import org.l2jmobius.gameserver.model.zone.ZoneId;

/**
 * Smart target selection system for autobots with threat assessment,
 * priority calculation, and tactical decision making.
 * @author YourName
 */
public class SmartTargetSelector
{
	private static final Logger LOGGER = Logger.getLogger(SmartTargetSelector.class.getName());
	
	// Target priority weights
	private static final double DISTANCE_WEIGHT = 0.25;
	private static final double HEALTH_WEIGHT = 0.20;
	private static final double LEVEL_WEIGHT = 0.15;
	private static final double THREAT_WEIGHT = 0.30;
	private static final double REWARD_WEIGHT = 0.10;
	
	// Target categories
	public enum TargetCategory
	{
		MONSTER,
		PLAYER,
		AUTOBOT,
		BOSS,
		RESOURCE,
		UNKNOWN
	}
	
	// Target threat levels
	public enum ThreatLevel
	{
		NONE,
		LOW,
		MEDIUM,
		HIGH,
		EXTREME
	}
	
	/**
	 * Find the best target for an autobot based on multiple criteria
	 */
	public static Creature findBestTarget(Autobot autobot, int searchRadius)
	{
		Player player = autobot.getPlayer();
		if (player == null)
		{
			return null;
		}
		
		// Get all potential targets in range
		List<Creature> candidates = World.getInstance().getVisibleObjectsInRange(player, Creature.class, searchRadius);
		
		Creature bestTarget = null;
		double bestScore = 0;
		
		for (Creature candidate : candidates)
		{
			if (isValidTarget(candidate, autobot))
			{
				double score = calculateTargetScore(candidate, autobot);
				if (score > bestScore)
				{
					bestScore = score;
					bestTarget = candidate;
				}
			}
		}
		
		return bestTarget;
	}
	
	/**
	 * Calculate comprehensive target score
	 */
	private static double calculateTargetScore(Creature target, Autobot autobot)
	{
		double score = 0;
		Player player = autobot.getPlayer();
		
		// Distance factor (closer = better)
		double distance = player.calculateDistance2D(target);
		double distanceScore = Math.max(0, 100 - (distance / 20));
		score += distanceScore * DISTANCE_WEIGHT;
		
		// Health factor (lower HP = easier kill, but also consider if it's worth it)
		double healthPercent = (target.getCurrentHp() / target.getMaxHp()) * 100;
		double healthScore = calculateHealthScore(healthPercent, target);
		score += healthScore * HEALTH_WEIGHT;
		
		// Level factor
		double levelScore = calculateLevelScore(target, player);
		score += levelScore * LEVEL_WEIGHT;
		
		// Threat assessment
		double threatScore = calculateThreatScore(target, autobot);
		score += threatScore * THREAT_WEIGHT;
		
		// Reward potential
		double rewardScore = calculateRewardScore(target, autobot);
		score += rewardScore * REWARD_WEIGHT;
		
		// Apply personality modifiers
		score = applyPersonalityModifiers(score, target, autobot);
		
		return score;
	}
	
	/**
	 * Calculate health-based score
	 */
	private static double calculateHealthScore(double healthPercent, Creature target)
	{
		if (healthPercent < 25)
		{
			return 90; // Very low HP - easy kill
		}
		else if (healthPercent < 50)
		{
			return 70; // Medium HP - good target
		}
		else if (healthPercent < 75)
		{
			return 50; // High HP - moderate target
		}
		else
		{
			// Full HP targets get bonus if they're valuable
			if (target instanceof Attackable && ((Attackable) target).isRaid())
			{
				return 60; // Raid boss with full HP still valuable
			}
			return 30; // Full HP regular target
		}
	}
	
	/**
	 * Calculate level-based score
	 */
	private static double calculateLevelScore(Creature target, Player player)
	{
		int levelDiff = target.getLevel() - player.getLevel();
		
		if (levelDiff <= -5)
		{
			return 40; // Much lower level - not much reward
		}
		else if (levelDiff <= 0)
		{
			return 80; // Same or slightly lower level - good target
		}
		else if (levelDiff <= 3)
		{
			return 90; // Slightly higher level - great target
		}
		else if (levelDiff <= 7)
		{
			return 60; // Moderately higher level - challenging but doable
		}
		else
		{
			return 20; // Much higher level - dangerous
		}
	}
	
	/**
	 * Calculate threat assessment score
	 */
	private static double calculateThreatScore(Creature target, Autobot autobot)
	{
		ThreatLevel threat = assessThreatLevel(target, autobot);
		Player player = autobot.getPlayer();
		
		switch (threat)
		{
			case NONE:
				return 100; // No threat - safe target
			case LOW:
				return 80; // Low threat - good target
			case MEDIUM:
				return autobot.getAggressionLevel() > 50 ? 60 : 40; // Depends on aggression
			case HIGH:
				return autobot.getAggressionLevel() > 70 ? 40 : 10; // Only if very aggressive
			case EXTREME:
				return autobot.getAggressionLevel() > 90 ? 20 : 0; // Almost never
			default:
				return 50;
		}
	}
	
	/**
	 * Assess threat level of a target
	 */
	public static ThreatLevel assessThreatLevel(Creature target, Autobot autobot)
	{
		Player player = autobot.getPlayer();
		
		// Check if target is already attacking someone
		if (target.getTarget() != null && target.isAttackingNow())
		{
			if (target.getTarget() == player)
			{
				return ThreatLevel.HIGH; // Already targeting us
			}
			else if (target.getTarget() instanceof Player)
			{
				return ThreatLevel.MEDIUM; // Busy with another player
			}
		}
		
		// Level-based threat
		int levelDiff = target.getLevel() - player.getLevel();
		if (levelDiff > 10)
		{
			return ThreatLevel.EXTREME;
		}
		else if (levelDiff > 5)
		{
			return ThreatLevel.HIGH;
		}
		else if (levelDiff > 0)
		{
			return ThreatLevel.MEDIUM;
		}
		
		// Special creature types
		if (target instanceof Attackable)
		{
			Attackable mob = (Attackable) target;
			if (mob.isRaid())
			{
				return ThreatLevel.EXTREME;
			}
			if (mob.isChampion())
			{
				return ThreatLevel.HIGH;
			}
		}
		
		// Player threat assessment
		if (target instanceof Player)
		{
			Player targetPlayer = (Player) target;
			
			// Check if player is in combat with others
			if (targetPlayer.getPvpFlag() > 0)
			{
				return ThreatLevel.HIGH;
			}
			
			// Check player's current HP
			double hpPercent = (targetPlayer.getCurrentHp() / targetPlayer.getMaxHp()) * 100;
			if (hpPercent < 50)
			{
				return ThreatLevel.LOW; // Wounded player
			}
			
			return ThreatLevel.MEDIUM;
		}
		
		return ThreatLevel.LOW;
	}
	
	/**
	 * Calculate potential reward score
	 */
	private static double calculateRewardScore(Creature target, Autobot autobot)
	{
		double score = 50; // Base score
		
		if (target instanceof Attackable)
		{
			Attackable mob = (Attackable) target;
			
			// Raid bosses have high reward potential
			if (mob.isRaid())
			{
				score += 40;
			}
			
			// Champions have good rewards
			if (mob.isChampion())
			{
				score += 25;
			}
			
			// Level-appropriate mobs give good exp
			int levelDiff = Math.abs(mob.getLevel() - autobot.getLevel());
			if (levelDiff <= 3)
			{
				score += 20;
			}
		}
		
		// Player kills can give honor points (in PvP zones)
		if (target instanceof Player && autobot.getPlayer().isInsideZone(ZoneId.PVP))
		{
			score += 30;
		}
		
		return Math.min(100, score);
	}
	
	/**
	 * Apply personality-based modifiers to score
	 */
	private static double applyPersonalityModifiers(double baseScore, Creature target, Autobot autobot)
	{
		double modifiedScore = baseScore;
		
		// Aggression modifier
		int aggression = autobot.getAggressionLevel();
		if (target instanceof Player)
		{
			// High aggression autobots prefer player targets
			if (aggression > 70)
			{
				modifiedScore *= 1.3;
			}
			else if (aggression < 30)
			{
				modifiedScore *= 0.7;
			}
		}
		
		// Social modifier (affects target selection in social zones)
		int socialLevel = autobot.getSocialLevel();
		if (autobot.getPlayer().isInsideZone(ZoneId.PEACE))
		{
			if (socialLevel > 60)
			{
				modifiedScore *= 0.5; // Less likely to attack in peace zones
			}
		}
		
		// Random factor for unpredictability
		double randomFactor = 0.9 + (ThreadLocalRandom.current().nextDouble() * 0.2); // 0.9 to 1.1
		modifiedScore *= randomFactor;
		
		return modifiedScore;
	}
	
	/**
	 * Check if target is valid for attacking
	 */
	public static boolean isValidTarget(Creature target, Autobot autobot)
	{
		Player player = autobot.getPlayer();
		
		if (target == null || target.isDead() || target == player)
		{
			return false;
		}
		
		// Check if target is attackable
		if (!target.isAttackable())
		{
			return false;
		}
		
		// Check line of sight
		if (!GeoEngine.getInstance().canSeeTarget(player, target))
		{
			return false;
		}
		
		// Don't attack other autobots unless in PvP zone
		if (target instanceof Player)
		{
			Player targetPlayer = (Player) target;
			if (AutobotManager.getInstance().getAutobot(targetPlayer.getName()) != null)
			{
				return player.isInsideZone(ZoneId.PVP) || player.isInsideZone(ZoneId.SIEGE);
			}
		}
		
		// Check if target is in safe zone
		if (target.isInsideZone(ZoneId.PEACE) && !player.isInsideZone(ZoneId.PVP))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Get target category for strategic decisions
	 */
	public static TargetCategory getTargetCategory(Creature target)
	{
		if (target instanceof Player)
		{
			if (target instanceof Player && AutobotManager.getInstance().getAutobot(((Player)target).getName()) != null)
			{
				return TargetCategory.AUTOBOT;
			}
			return TargetCategory.PLAYER;
		}
		else if (target instanceof Attackable)
		{
			Attackable mob = (Attackable) target;
			if (mob.isRaid())
			{
				return TargetCategory.BOSS;
			}
			return TargetCategory.MONSTER;
		}
		
		return TargetCategory.UNKNOWN;
	}
	
	/**
	 * Find nearby allies for group tactics
	 */
	public static List<Autobot> findNearbyAllies(Autobot autobot, int range)
	{
		Player player = autobot.getPlayer();
		
		return World.getInstance().getVisibleObjectsInRange(player, Player.class, range)
			.stream()
			.filter(p -> AutobotManager.getInstance().getAutobot(p.getName()) != null)
			.map(p -> AutobotManager.getInstance().getAutobot(p.getName()))
			.filter(bot -> bot != autobot)
			.filter(bot -> bot != null)
			.collect(Collectors.toList());
	}
	
	/**
	 * Check if should engage in group combat
	 */
	public static boolean shouldEngageInGroupCombat(Autobot autobot, Creature target)
	{
		List<Autobot> allies = findNearbyAllies(autobot, 500);
		
		if (allies.isEmpty())
		{
			return true; // No allies, individual decision
		}
		
		// Count allies already fighting
		long alliesInCombat = allies.stream()
			.filter(ally -> ally.getPlayer().isInCombat())
			.count();
		
		// If many allies are fighting, join the fight
		if (alliesInCombat >= 2)
		{
			return true;
		}
		
		// Consider threat level
		ThreatLevel threat = assessThreatLevel(target, autobot);
		return threat != ThreatLevel.EXTREME || alliesInCombat > 0;
	}
}