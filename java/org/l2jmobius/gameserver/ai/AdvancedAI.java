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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

import org.l2jmobius.gameserver.managers.AutobotManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Autobot;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.commons.util.Rnd;

/**
 * Advanced AI coordination system for managing interactions between autobots,
 * group formation, shared objectives, and intelligent communication.
 * @author YourName
 */
public class AdvancedAI
{
	private static final Logger LOGGER = Logger.getLogger(AdvancedAI.class.getName());
	
	// Group formation types
	public enum FormationType
	{
		SOLO,        // Individual operation
		PARTY,       // Small group 2-5 bots
		RAID,        // Large group 6+ bots
		DEFENSIVE,   // Protective formation
		OFFENSIVE    // Aggressive formation
	}
	
	// Communication types between bots
	public enum CommunicationType
	{
		TARGET_SHARING,
		ASSISTANCE_REQUEST,
		AREA_WARNING,
		GROUP_FORMATION,
		RESOURCE_SHARING,
		SOCIAL_INTERACTION
	}
	
	// Shared objectives
	public enum ObjectiveType
	{
		HUNT_TOGETHER,
		DEFEND_AREA,
		EXPLORE_REGION,
		SOCIAL_GATHERING,
		RESOURCE_FARMING,
		PVP_ENGAGEMENT
	}
	
	// Bot coordination data
	private static class BotCoordination
	{
		Autobot leader;
		List<Autobot> members;
		FormationType formation;
		ObjectiveType objective;
		Location targetArea;
		long createdTime;
		
		BotCoordination()
		{
			members = new ArrayList<>();
			createdTime = System.currentTimeMillis();
		}
	}
	
	// Static coordination data
	private static final Map<String, BotCoordination> _activeGroups = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> _lastCommunication = new ConcurrentHashMap<>();
	private static final Map<String, List<String>> _botRelationships = new ConcurrentHashMap<>();
	
	/**
	 * Process advanced AI coordination for all autobots
	 */
	public static void processCoordination()
	{
		// Group formation and communication disabled
		// Autobots operate independently without forming groups
		return;
	}
	
	/**
	 * Update active group formations
	 */
	private static void updateActiveGroups()
	{
		List<String> groupsToRemove = new ArrayList<>();
		
		for (Map.Entry<String, BotCoordination> entry : _activeGroups.entrySet())
		{
			BotCoordination group = entry.getValue();
			
			// Remove offline members
			group.members.removeIf(bot -> !bot.isOnline());
			
			// Remove groups that are too old or empty
			long age = System.currentTimeMillis() - group.createdTime;
			if (group.members.size() < 2 || age > 300000) // 5 minutes
			{
				groupsToRemove.add(entry.getKey());
			}
		}
		
		groupsToRemove.forEach(_activeGroups::remove);
	}
	
	/**
	 * Form new bot groups based on proximity and compatibility
	 */
	private static void formNewGroups(Collection<Autobot> autobots)
	{
		List<Autobot> ungroupedBots = new ArrayList<>();
		
		// Find bots not in any group
		for (Autobot bot : autobots)
		{
			if (bot.isOnline() && !isInGroup(bot))
			{
				ungroupedBots.add(bot);
			}
		}
		
		// Try to form groups
		while (ungroupedBots.size() >= 2)
		{
			Autobot leader = ungroupedBots.remove(0);
			List<Autobot> potentialMembers = findCompatibleBots(leader, ungroupedBots);
			
			if (!potentialMembers.isEmpty())
			{
				createGroup(leader, potentialMembers);
				ungroupedBots.removeAll(potentialMembers);
			}
		}
	}
	
	/**
	 * Find bots compatible for grouping
	 */
	private static List<Autobot> findCompatibleBots(Autobot leader, List<Autobot> candidates)
	{
		List<Autobot> compatible = new ArrayList<>();
		Player leaderPlayer = leader.getPlayer();
		
		for (Autobot candidate : candidates)
		{
			if (areCompatible(leader, candidate))
			{
				double distance = leaderPlayer.calculateDistance2D(candidate.getPlayer());
				if (distance <= 1000) // Within 1000 units
				{
					compatible.add(candidate);
				}
			}
			
			// Limit group size
			if (compatible.size() >= 4)
			{
				break;
			}
		}
		
		return compatible;
	}
	
	/**
	 * Check if two bots are compatible for grouping
	 */
	private static boolean areCompatible(Autobot bot1, Autobot bot2)
	{
		// Level compatibility
		int levelDiff = Math.abs(bot1.getLevel() - bot2.getLevel());
		if (levelDiff > 10)
		{
			return false;
		}
		
		// Personality compatibility
		int aggressionDiff = Math.abs(bot1.getAggressionLevel() - bot2.getAggressionLevel());
		if (aggressionDiff > 40)
		{
			return false;
		}
		
		// Check existing relationships
		List<String> relationships = _botRelationships.get(bot1.getName());
		if (relationships != null && relationships.contains(bot2.getName()))
		{
			return true; // Known relationship
		}
		
		// Random compatibility factor
		return Rnd.get(100) < 60; // 60% chance of compatibility
	}
	
	/**
	 * Create a new bot group
	 */
	private static void createGroup(Autobot leader, List<Autobot> members)
	{
		String groupId = "Group_" + leader.getName() + "_" + System.currentTimeMillis();
		BotCoordination group = new BotCoordination();
		
		group.leader = leader;
		group.members.add(leader);
		group.members.addAll(members);
		group.formation = determineFormationType(group.members);
		group.objective = determineObjective(group.members);
		group.targetArea = calculateCentralLocation(group.members);
		
		_activeGroups.put(groupId, group);
		
		// Announce group formation
		announceGroupFormation(group);
		
		LOGGER.info("Formed new bot group: " + groupId + " with " + group.members.size() + " members");
	}
	
	/**
	 * Determine formation type based on group composition
	 */
	private static FormationType determineFormationType(List<Autobot> members)
	{
		if (members.size() <= 1)
		{
			return FormationType.SOLO;
		}
		else if (members.size() <= 5)
		{
			// Check average aggression
			double avgAggression = members.stream()
				.mapToInt(Autobot::getAggressionLevel)
				.average()
				.orElse(50);
			
			return avgAggression > 60 ? FormationType.OFFENSIVE : FormationType.DEFENSIVE;
		}
		else
		{
			return FormationType.RAID;
		}
	}
	
	/**
	 * Determine group objective
	 */
	private static ObjectiveType determineObjective(List<Autobot> members)
	{
		// Base on average personality
		double avgAggression = members.stream()
			.mapToInt(Autobot::getAggressionLevel)
			.average()
			.orElse(50);
		
		double avgSocial = members.stream()
			.mapToInt(Autobot::getSocialLevel)
			.average()
			.orElse(30);
		
		if (avgSocial > 60)
		{
			return ObjectiveType.SOCIAL_GATHERING;
		}
		else if (avgAggression > 70)
		{
			return ObjectiveType.PVP_ENGAGEMENT;
		}
		else if (avgAggression > 40)
		{
			return ObjectiveType.HUNT_TOGETHER;
		}
		else
		{
			return ObjectiveType.RESOURCE_FARMING;
		}
	}
	
	/**
	 * Process group objectives
	 */
	private static void processGroupObjectives()
	{
		for (BotCoordination group : _activeGroups.values())
		{
			switch (group.objective)
			{
				case HUNT_TOGETHER:
					processGroupHunting(group);
					break;
				case DEFEND_AREA:
					processAreaDefense(group);
					break;
				case EXPLORE_REGION:
					processGroupExploration(group);
					break;
				case SOCIAL_GATHERING:
					processSocialGathering(group);
					break;
				case RESOURCE_FARMING:
					processResourceFarming(group);
					break;
				case PVP_ENGAGEMENT:
					processPvPEngagement(group);
					break;
			}
		}
	}
	
	/**
	 * Process group hunting coordination
	 */
	private static void processGroupHunting(BotCoordination group)
	{
		if (group.leader == null || !group.leader.isOnline())
		{
			return;
		}
		
		// Leader finds target, others assist
		Player leaderPlayer = group.leader.getPlayer();
		if (leaderPlayer.getTarget() instanceof Creature)
		{
			Creature target = (Creature) leaderPlayer.getTarget();
			
			// Coordinate other members to assist
			for (Autobot member : group.members)
			{
				if (member != group.leader && member.isOnline())
				{
					Player memberPlayer = member.getPlayer();
					double distance = memberPlayer.calculateDistance2D(target);
					
					if (distance <= 800) // Within assist range
					{
						memberPlayer.setTarget(target);
					}
				}
			}
		}
	}
	
	/**
	 * Process area defense coordination
	 */
	private static void processAreaDefense(BotCoordination group)
	{
		if (group.targetArea == null)
		{
			return;
		}
		
		// Position members around the area
		double angleStep = (2 * Math.PI) / group.members.size();
		int radius = 200;
		
		for (int i = 0; i < group.members.size(); i++)
		{
			Autobot member = group.members.get(i);
			if (!member.isOnline()) continue;
			
			double angle = i * angleStep;
			int x = group.targetArea.getX() + (int)(Math.cos(angle) * radius);
			int y = group.targetArea.getY() + (int)(Math.sin(angle) * radius);
			
			Location guardPosition = new Location(x, y, group.targetArea.getZ());
			
			// Move to guard position if far away
			if (member.getPlayer().calculateDistance2D(guardPosition) > 150)
			{
				// AI will handle movement through pathfinding
				member.getPlayer().getAI().setIntention(
					Intention.MOVE_TO, guardPosition);
			}
		}
	}
	
	/**
	 * Process group exploration
	 */
	private static void processGroupExploration(BotCoordination group)
	{
		if (group.leader == null || !group.leader.isOnline())
		{
			return;
		}
		
		// Generate exploration path using DynamicPathfinder
		Location center = group.leader.getPlayer().getLocation();
		List<Location> explorationPath = DynamicPathfinder.generatePatternPath(
			center, DynamicPathfinder.MovementPattern.PATROL, 500, 4);
		
		// Assign different waypoints to different members
		for (int i = 0; i < group.members.size() && i < explorationPath.size(); i++)
		{
			Autobot member = group.members.get(i);
			if (member.isOnline())
			{
				Location waypoint = explorationPath.get(i);
				member.getPlayer().getAI().setIntention(
					Intention.MOVE_TO, waypoint);
			}
		}
	}
	
	/**
	 * Process social gathering
	 */
	private static void processSocialGathering(BotCoordination group)
	{
		if (group.targetArea == null)
		{
			group.targetArea = calculateCentralLocation(group.members);
		}
		
		// Members gather at central location
		for (Autobot member : group.members)
		{
			if (member.isOnline())
			{
				double distance = member.getPlayer().calculateDistance2D(group.targetArea);
				if (distance > 100)
				{
					member.getPlayer().getAI().setIntention(
						Intention.MOVE_TO, group.targetArea);
				}
				else
				{
					// Occasional social emotes or chat
					if (Rnd.get(100) < 2) // 2% chance
					{
						sendSocialMessage(member, group);
					}
				}
			}
		}
	}
	
	/**
	 * Process resource farming coordination
	 */
	private static void processResourceFarming(BotCoordination group)
	{
		// Spread members across farming area to avoid competition
		if (group.targetArea == null)
		{
			group.targetArea = calculateCentralLocation(group.members);
		}
		
		int spreadRadius = 300;
		for (int i = 0; i < group.members.size(); i++)
		{
			Autobot member = group.members.get(i);
			if (!member.isOnline()) continue;
			
			// Calculate spread position
			double angle = (2 * Math.PI * i) / group.members.size();
			int x = group.targetArea.getX() + (int)(Math.cos(angle) * spreadRadius);
			int y = group.targetArea.getY() + (int)(Math.sin(angle) * spreadRadius);
			
			Location farmPosition = new Location(x, y, group.targetArea.getZ());
			
			// Set farming area for this member
			double distance = member.getPlayer().calculateDistance2D(farmPosition);
			if (distance > 200)
			{
				member.getPlayer().getAI().setIntention(
					Intention.MOVE_TO, farmPosition);
			}
		}
	}
	
	/**
	 * Process PvP engagement coordination
	 */
	private static void processPvPEngagement(BotCoordination group)
	{
		// Find player targets and coordinate attacks
		Player leader = group.leader.getPlayer();
		if (leader.getTarget() instanceof Player)
		{
			Player target = (Player) leader.getTarget();
			
			// Focus fire on the same target
			for (Autobot member : group.members)
			{
				if (member.isOnline() && member != group.leader)
				{
					Player memberPlayer = member.getPlayer();
					if (memberPlayer.calculateDistance2D(target) <= 600)
					{
						memberPlayer.setTarget(target);
					}
				}
			}
		}
	}
	
	/**
	 * Process inter-bot communication
	 */
	private static void processInterbotCommunication(Collection<Autobot> autobots)
	{
		for (Autobot bot : autobots)
		{
			if (!bot.isOnline()) continue;
			
			// Check if should communicate with nearby bots
			if (shouldCommunicate(bot))
			{
				List<Autobot> nearbyBots = findNearbyBots(bot, 300);
				if (!nearbyBots.isEmpty())
				{
					Autobot target = nearbyBots.get(Rnd.get(nearbyBots.size()));
					sendCommunication(bot, target, determineMessageType(bot, target));
				}
			}
		}
	}
	
	/**
	 * Send communication between bots
	 */
	private static void sendCommunication(Autobot sender, Autobot receiver, CommunicationType type)
	{
		String message = generateCommunicationMessage(type, sender, receiver);
		if (message != null)
		{
			sender.getPlayer().broadcastPacket(new CreatureSay(
				sender.getPlayer(), ChatType.GENERAL, sender.getPlayer().getName(), message));
			
			_lastCommunication.put(sender.getObjectId(), System.currentTimeMillis());
		}
	}
	
	/**
	 * Generate appropriate communication message
	 */
	private static String generateCommunicationMessage(CommunicationType type, Autobot sender, Autobot receiver)
	{
		switch (type)
		{
			case TARGET_SHARING:
				return "Found good hunting spot, " + receiver.getName() + "!";
			case ASSISTANCE_REQUEST:
				return "Could use some help here, " + receiver.getName() + ".";
			case AREA_WARNING:
				return "Careful " + receiver.getName() + ", strong enemies nearby.";
			case GROUP_FORMATION:
				return "Want to team up, " + receiver.getName() + "?";
			case RESOURCE_SHARING:
				return "Good resources over here, " + receiver.getName() + "!";
			case SOCIAL_INTERACTION:
				String[] socialMessages = {
					"How's the hunting going, " + receiver.getName() + "?",
					"Nice weather today, isn't it " + receiver.getName() + "?",
					"Have you been here long, " + receiver.getName() + "?",
					"Good to see you again, " + receiver.getName() + "!"
				};
				return socialMessages[Rnd.get(socialMessages.length)];
		}
		return null;
	}
	
	// Helper methods
	private static boolean isInGroup(Autobot bot)
	{
		return _activeGroups.values().stream()
			.anyMatch(group -> group.members.contains(bot));
	}
	
	private static Location calculateCentralLocation(List<Autobot> members)
	{
		if (members.isEmpty()) return null;
		
		int avgX = members.stream().mapToInt(Autobot::getX).sum() / members.size();
		int avgY = members.stream().mapToInt(Autobot::getY).sum() / members.size();
		int avgZ = members.stream().mapToInt(Autobot::getZ).sum() / members.size();
		
		return new Location(avgX, avgY, avgZ);
	}
	
	private static void announceGroupFormation(BotCoordination group)
	{
		if (group.leader != null)
		{
			String message = "Forming group for " + group.objective.toString().toLowerCase() + "!";
			group.leader.getPlayer().broadcastPacket(new CreatureSay(
				group.leader.getPlayer(), ChatType.GENERAL, group.leader.getPlayer().getName(), message));
		}
	}
	
	private static void sendSocialMessage(Autobot member, BotCoordination group)
	{
		String[] messages = {
			"Good to be with the team!",
			"This is a nice spot to gather.",
			"How is everyone doing?",
			"Nice group we have here!"
		};
		
		String message = messages[Rnd.get(messages.length)];
		member.getPlayer().broadcastPacket(new CreatureSay(
			member.getPlayer(), ChatType.GENERAL, member.getPlayer().getName(), message));
	}
	
	private static boolean shouldCommunicate(Autobot bot)
	{
		Long lastComm = _lastCommunication.get(bot.getObjectId());
		if (lastComm != null && (System.currentTimeMillis() - lastComm) < 30000)
		{
			return false; // 30 second cooldown
		}
		
		return bot.getSocialLevel() > 30 && Rnd.get(100) < 5; // 5% chance
	}
	
	private static List<Autobot> findNearbyBots(Autobot bot, int range)
	{
		return World.getInstance().getVisibleObjectsInRange(bot.getPlayer(), Player.class, range)
			.stream()
			.filter(p -> AutobotManager.getInstance().getAutobot(p.getName()) != null)  // Check if player is an autobot
			.map(p -> AutobotManager.getInstance().getAutobot(p.getName()))
			.filter(autobot -> autobot != null)
			.collect(toList());
	}
	
	private static CommunicationType determineMessageType(Autobot sender, Autobot receiver)
	{
		if (sender.getPlayer().isInCombat())
		{
			return CommunicationType.ASSISTANCE_REQUEST;
		}
		else if (sender.getSocialLevel() > 50)
		{
			return CommunicationType.SOCIAL_INTERACTION;
		}
		else
		{
			return CommunicationType.TARGET_SHARING;
		}
	}
	
	private static void updateBotRelationships(Collection<Autobot> autobots)
	{
		// Build relationships based on proximity and interactions
		for (Autobot bot : autobots)
		{
			if (!bot.isOnline()) continue;
			
			List<Autobot> nearbyBots = findNearbyBots(bot, 500);
			List<String> relationships = _botRelationships.computeIfAbsent(bot.getName(), k -> new ArrayList<>());
			
			for (Autobot nearby : nearbyBots)
			{
				if (!relationships.contains(nearby.getName()))
				{
					relationships.add(nearby.getName());
					
					// Limit relationship list size
					if (relationships.size() > 10)
					{
						relationships.remove(0);
					}
				}
			}
		}
	}
}