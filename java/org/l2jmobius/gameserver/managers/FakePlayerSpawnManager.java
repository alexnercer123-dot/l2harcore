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
package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Autobot;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;

/**
 * Manager for spawning groups of fake players at racial starting locations
 * with automatic leveling from 1 to 6 by fighting appropriate monsters.
 * @author YourName
 */
public class FakePlayerSpawnManager
{
	private static final Logger LOGGER = Logger.getLogger(FakePlayerSpawnManager.class.getName());
	private static final Logger FAKE_PLAYER_LOGGER = AutobotManager.getFakePlayerLogger();
	
	// Spawn group definitions
	public enum SpawnGroup
	{
		HUMAN_FIGHTER(1, "Human Fighter", PlayerClass.FIGHTER, new Location(-71424, 258336, -3109)),
		HUMAN_MAGE(2, "Human Mage", PlayerClass.MAGE, new Location(-91036, 248044, -3568)),
		ELF_FIGHTER(3, "Elf Fighter", PlayerClass.ELVEN_FIGHTER, new Location(46112, 41200, -3504)),
		ELF_MAGE(4, "Elf Mage", PlayerClass.ELVEN_MAGE, new Location(46112, 41200, -3504)),
		DARK_ELF_FIGHTER(5, "Dark Elf Fighter", PlayerClass.DARK_FIGHTER, new Location(28384, 11056, -4233)),
		DARK_ELF_MAGE(6, "Dark Elf Mage", PlayerClass.DARK_MAGE, new Location(28384, 11056, -4233)),
		DWARF_FIGHTER(7, "Dwarf Fighter", PlayerClass.DWARVEN_FIGHTER, new Location(108567, -173994, -406)),
		ORC_FIGHTER(8, "Orc Fighter", PlayerClass.ORC_FIGHTER, new Location(-56682, -113730, -690)),
		ORC_MAGE(9, "Orc Mage", PlayerClass.ORC_MAGE, new Location(-56682, -113730, -690));
		
		private final int id;
		private final String description;
		private final PlayerClass playerClass;
		private final Location spawnLocation;
		
		SpawnGroup(int id, String description, PlayerClass playerClass, Location spawnLocation)
		{
			this.id = id;
			this.description = description;
			this.playerClass = playerClass;
			this.spawnLocation = spawnLocation;
		}
		
		public int getId() { return id; }
		public String getDescription() { return description; }
		public PlayerClass getPlayerClass() { return playerClass; }
		public Location getSpawnLocation() { return spawnLocation; }
		
		public static SpawnGroup getById(int id)
		{
			for (SpawnGroup group : values())
			{
				if (group.getId() == id)
				{
					return group;
				}
			}
			return null;
		}
	}
	
	// Active spawn groups
	private final Map<Integer, List<Autobot>> _activeSpawns = new ConcurrentHashMap<>();
	private final Map<Integer, String> _spawnGroupNames = new HashMap<>();
	
	protected FakePlayerSpawnManager()
	{
		LOGGER.info("FakePlayerSpawnManager initialized with " + SpawnGroup.values().length + " spawn groups.");
	}
	
	/**
	 * Spawn a group of fake players - now spawns sequentially with delays
	 */
	public boolean spawnGroup(int groupId, int count)
	{
		SpawnGroup group = SpawnGroup.getById(groupId);
		if (group == null)
		{
			LOGGER.warning("Invalid spawn group ID: " + groupId);
			return false;
		}
		
		// Check if group is already spawned - if so, despawn it first
		if (_activeSpawns.containsKey(groupId))
		{
			LOGGER.info("Spawn group " + groupId + " is already active. Auto-despawning first...");
			despawnGroup(groupId);
		}
		
		List<Autobot> spawnedBots = new ArrayList<>();
		Location spawnLoc = group.getSpawnLocation();
		
		LOGGER.info("Starting sequential spawn of " + count + " fake players for group: " + group.getDescription());
		FAKE_PLAYER_LOGGER.info("=== SEQUENTIAL SPAWNING GROUP " + groupId + " ===");
		FAKE_PLAYER_LOGGER.info("Group: " + group.getDescription() + ", Count: " + count + ", Location: " + spawnLoc);
		
		// Clear any existing database entries for this group to prevent conflicts
		String namePattern = getNamePatternForGroup(group);
		AutobotManager.getInstance().clearAutobotsFromDatabase(namePattern + "%");
		
		// Also clean up any orphaned bots with similar names
		cleanupOrphanedBots(group);
		
		// Initialize the active spawn list immediately
		_activeSpawns.put(groupId, spawnedBots);
		_spawnGroupNames.put(groupId, group.getDescription());
		
		// Start sequential spawning with delays (spawn one bot every 2 seconds)
		spawnBotsSequentially(groupId, group, count, spawnLoc, spawnedBots, 0);
		
		LOGGER.info("Sequential spawning initiated for " + count + " bots in group: " + group.getDescription());
		return true;
	}
	
	/**
	 * Spawn bots one by one with delays
	 */
	private void spawnBotsSequentially(int groupId, SpawnGroup group, int totalCount, Location spawnLoc, List<Autobot> spawnedBots, int currentIndex)
	{
		if (currentIndex >= totalCount)
		{
			LOGGER.info("Sequential spawning completed: " + spawnedBots.size() + "/" + totalCount + " bots spawned for group: " + group.getDescription());
			FAKE_PLAYER_LOGGER.info("Sequential spawning completed: " + spawnedBots.size() + "/" + totalCount + " bots spawned for group: " + group.getDescription());
			return;
		}
		
		// Generate unique name for this bot
		String botName = generateBotName(group, currentIndex);
		
		// Calculate spawn position with slight randomization
		Location individualSpawn = calculateSpawnPosition(spawnLoc, currentIndex, totalCount);
		
		// Create the autobot using AutobotManager
		boolean success = AutobotManager.getInstance().createAutobot(botName, group.getPlayerClass(), 1, individualSpawn, null);
		if (success)
		{
			// Get the created autobot for configuration
			Autobot autobot = AutobotManager.getInstance().getAutobot(botName);
			if (autobot != null)
			{
				// Configure for automatic leveling
				configureLevelingBot(autobot, group);
				spawnedBots.add(autobot);
				LOGGER.fine("Successfully spawned bot " + (currentIndex + 1) + "/" + totalCount + ": " + botName);
				FAKE_PLAYER_LOGGER.info("Spawned bot " + (currentIndex + 1) + "/" + totalCount + ": " + botName + " at " + individualSpawn);
			}
		}
		else
		{
			LOGGER.warning("Failed to create autobot " + (currentIndex + 1) + "/" + totalCount + ": " + botName);
		}
		
		// Schedule the next bot spawn after 2 seconds delay
		org.l2jmobius.commons.threads.ThreadPool.schedule(() -> {
			spawnBotsSequentially(groupId, group, totalCount, spawnLoc, spawnedBots, currentIndex + 1);
		}, 2000); // 2 second delay between each bot
	}
	
	/**
	 * Despawn a group of fake players
	 */
	public boolean despawnGroup(int groupId)
	{
		List<Autobot> bots = _activeSpawns.remove(groupId);
		String groupName = _spawnGroupNames.remove(groupId);
		
		if (bots == null || bots.isEmpty())
		{
			LOGGER.warning("No active spawn group found with ID: " + groupId);
			return false;
		}
		
		int despawnedCount = 0;
		for (Autobot bot : bots)
		{
			if (AutobotManager.getInstance().despawnAutobot(bot.getName()))
			{
				despawnedCount++;
			}
		}
		
		LOGGER.info("Despawned " + despawnedCount + " bots from group: " + groupName);
		return true;
	}
	
	/**
	 * Despawn all active groups
	 */
	public int despawnAllGroups()
	{
		int totalDespawned = 0;
		
		for (Integer groupId : new ArrayList<>(_activeSpawns.keySet()))
		{
			if (despawnGroup(groupId))
			{
				totalDespawned++;
			}
		}
		
		LOGGER.info("Despawned " + totalDespawned + " spawn groups.");
		return totalDespawned;
	}
	
	/**
	 * Get information about active spawns
	 */
	public String getActiveSpawnsInfo()
	{
		StringBuilder info = new StringBuilder();
		info.append("=== Active Fake Player Spawns ===\n");
		
		if (_activeSpawns.isEmpty())
		{
			info.append("No active spawns.\n");
		}
		else
		{
			for (Map.Entry<Integer, List<Autobot>> entry : _activeSpawns.entrySet())
			{
				int groupId = entry.getKey();
				List<Autobot> bots = entry.getValue();
				String groupName = _spawnGroupNames.get(groupId);
				
				long onlineCount = bots.stream().filter(Autobot::isOnline).count();
				double avgLevel = bots.stream().mapToInt(Autobot::getLevel).average().orElse(0);
				
				info.append("Group ").append(groupId).append(" (").append(groupName).append("): ")
					.append(onlineCount).append("/").append(bots.size()).append(" online, ")
					.append("Avg Level: ").append(String.format("%.1f", avgLevel)).append("\n");
			}
		}
		
		return info.toString();
	}
	
	/**
	 * Generate unique bot name for the group
	 */
	private String generateBotName(SpawnGroup group, int index)
	{
		String prefix = "";
		switch (group.getPlayerClass())
		{
			case FIGHTER:
				prefix = "HumanWarrior";
				break;
			case MAGE:
				prefix = "HumanMage";
				break;
			case ELVEN_FIGHTER:
				prefix = "ElfWarrior";
				break;
			case ELVEN_MAGE:
				prefix = "ElfMage";
				break;
			case DARK_FIGHTER:
				prefix = "DarkWarrior";
				break;
			case DARK_MAGE:
				prefix = "DarkMage";
				break;
			case DWARVEN_FIGHTER:
				prefix = "DwarfWarrior";
				break;
			case ORC_FIGHTER:
				prefix = "OrcWarrior";
				break;
			case ORC_MAGE:
				prefix = "OrcShaman";
				break;
			default:
				prefix = "Bot"; // Changed from "FakePlayer" to "Bot"
		}
		
		// Generate unique name by checking existing autobots
		String baseName = prefix + String.format("%02d", index + 1);
		String uniqueName = baseName;
		int suffix = 0;
		
		// Keep trying until we find a unique name
		while (AutobotManager.getInstance().getAutobot(uniqueName) != null)
		{
			suffix++;
			uniqueName = baseName + "_" + suffix;
			
			// Failsafe to prevent infinite loop
			if (suffix > 999)
			{
				// Use timestamp as last resort
				uniqueName = prefix + "_" + System.currentTimeMillis() % 100000;
				break;
			}
		}
		
		return uniqueName;
	}
	
	/**
	 * Get name pattern for the group
	 */
	private String getNamePatternForGroup(SpawnGroup group)
	{
		switch (group.getPlayerClass())
		{
			case FIGHTER:
				return "HumanWarrior";
			case MAGE:
				return "HumanMage";
			case ELVEN_FIGHTER:
				return "ElfWarrior";
			case ELVEN_MAGE:
				return "ElfMage";
			case DARK_FIGHTER:
				return "DarkWarrior";
			case DARK_MAGE:
				return "DarkMage";
			case DWARVEN_FIGHTER:
				return "DwarfWarrior";
			case ORC_FIGHTER:
				return "OrcWarrior";
			case ORC_MAGE:
				return "OrcShaman";
			default:
				return "Bot"; // Changed from "FakePlayer" to "Bot"
		}
	}
	
	/**
	 * Calculate individual spawn position within the group area
	 */
	private Location calculateSpawnPosition(Location baseLocation, int index, int totalCount)
	{
		// Arrange bots in a circle around the spawn point
		double angle = (2 * Math.PI * index) / totalCount;
		int radius = 50 + (index / 5) * 30; // Expand radius for larger groups
		
		int x = baseLocation.getX() + (int)(Math.cos(angle) * radius);
		int y = baseLocation.getY() + (int)(Math.sin(angle) * radius);
		int z = baseLocation.getZ();
		
		return new Location(x, y, z);
	}
	
	/**
	 * Configure bot for automatic leveling behavior
	 */
	private void configureLevelingBot(Autobot autobot, SpawnGroup group)
	{
		// Set auto farming enabled
		autobot.setAutoFarmEnabled(true);
		
		// Set appropriate farming radius for starting areas
		autobot.setAutoFarmRadius(800);
		
		// Configure AI for leveling behavior (will be handled by AutobotAI)
		// The bot will automatically target appropriate monsters for its level
		
		LOGGER.fine("Configured leveling bot: " + autobot.getName() + " for group: " + group.getDescription());
	}
	
	/**
	 * Check if a group is active
	 */
	public boolean isGroupActive(int groupId)
	{
		return _activeSpawns.containsKey(groupId);
	}
	
	/**
	 * Get active group count
	 */
	public int getActiveGroupCount()
	{
		return _activeSpawns.size();
	}
	
	/**
	 * Get total active bot count
	 */
	public int getTotalActiveBotCount()
	{
		return _activeSpawns.values().stream()
			.mapToInt(List::size)
			.sum();
	}
	
	/**
	 * Get list of all available spawn groups
	 */
	public String getAvailableGroups()
	{
		StringBuilder groups = new StringBuilder();
		groups.append("=== Available Spawn Groups ===\n");
		
		for (SpawnGroup group : SpawnGroup.values())
		{
			groups.append("ID ").append(group.getId()).append(": ").append(group.getDescription())
				.append(" (").append(group.getPlayerClass()).append(")")
				.append(" at ").append(group.getSpawnLocation()).append("\n");
		}
		
		return groups.toString();
	}
	
	/**
	 * Clean up orphaned bots that might exist in database but not in memory
	 */
	private void cleanupOrphanedBots(SpawnGroup group)
	{
		String prefix = getNamePatternForGroup(group);
		
		// Remove any existing bots with names that start with this prefix
		// This helps prevent database primary key conflicts
		for (int i = 1; i <= 50; i++) // Clean up potential conflicts for up to 50 bots
		{
			String baseName = prefix + String.format("%02d", i);
			
			// Check base name
			Autobot existingBot = AutobotManager.getInstance().getAutobot(baseName);
			if (existingBot != null)
			{
				LOGGER.info("Cleaning up orphaned bot: " + baseName);
				AutobotManager.getInstance().despawnAutobot(baseName);
			}
			
			// Also check variants with suffixes
			for (int suffix = 1; suffix <= 10; suffix++)
			{
				String variantName = baseName + "_" + suffix;
				Autobot variantBot = AutobotManager.getInstance().getAutobot(variantName);
				if (variantBot != null)
				{
					LOGGER.info("Cleaning up orphaned bot variant: " + variantName);
					AutobotManager.getInstance().despawnAutobot(variantName);
				}
			}
		}
	}
	
	/**
	 * Singleton pattern
	 */
	public static FakePlayerSpawnManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePlayerSpawnManager INSTANCE = new FakePlayerSpawnManager();
	}
}