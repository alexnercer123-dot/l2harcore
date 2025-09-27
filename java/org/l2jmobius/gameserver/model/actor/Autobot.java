/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.l2jmobius.gameserver.model.actor;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.ai.AutobotAI;

/**
 * Autobot class - wrapper around Player to create visible AI-controlled players
 * This implementation uses composition to manage a Player instance
 */
public class Autobot
{
	private static final Logger LOGGER = Logger.getLogger(Autobot.class.getName());
	
	private final Player _player;
	private Location _homeLocation;
	private int _autoFarmRadius = 1000; // Limited radius to 1000
	private boolean _autoFarmEnabled = false;
	private boolean _isOnline = false;
	private final PlayerClass _playerClass;
	private final int _level;
	private AutobotAI _autobotAI;
	private long _lastChatTime = 0;
	private int _chatCooldown = 5000; // 5 seconds between chats
	
	/**
	 * Create an autobot using the Player.create factory method
	 */
	public static Autobot createAutobot(String name, PlayerClass playerClass, int level, Location spawnLocation)
	{
		try
		{
			// Get the player template for the class
			final PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(playerClass);
			if (template == null)
			{
				LOGGER.warning("No template found for player class: " + playerClass);
				return null;
			}
			
			// Create appearance
			final PlayerAppearance appearance = createRandomAppearance(playerClass.getRace());
			
			// Create the player using the static factory method
			final Player player = Player.create(template, "AutobotAccount", name, appearance);
			if (player == null)
			{
				LOGGER.warning("Failed to create player for autobot: " + name);
				return null;
			}
			
			// Create autobot wrapper
			final Autobot autobot = new Autobot(player, playerClass, level, spawnLocation);
			
			LOGGER.info("Created autobot: " + name + " (" + playerClass + ", Level " + level + ")");
			return autobot;
		}
		catch (Exception e)
		{
			LOGGER.warning("Error creating autobot " + name + ": " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Private constructor that wraps an existing player
	 */
	private Autobot(Player player, PlayerClass playerClass, int level, Location spawnLocation)
	{
		_player = player;
		_playerClass = playerClass;
		_level = level;
		_homeLocation = spawnLocation;
		_isOnline = true;
		_autoFarmEnabled = true;
		
		// Set the player's location
		_player.setXYZ(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
		_player.setHeading(spawnLocation.getHeading());
		
		// Set some basic stats based on level
		initializeStats(level);
		
		// Initialize AI
		_autobotAI = new AutobotAI(this);
		_player.setAI(_autobotAI);
		
		LOGGER.info("Autobot AI initialized for: " + player.getName() + ", AI class: " + _autobotAI.getClass().getSimpleName());
	}
	
	/**
	 * Initialize autobot stats based on level
	 */
	private void initializeStats(int level)
	{
		try
		{
			// Set experience for the level
			long exp = 0;
			for (int i = 1; i < level; i++)
			{
				exp += (i * 1000); // Simple exp formula
			}
			_player.getStat().setExp(exp);
			
			// Set full HP/MP/CP
			_player.setCurrentHp(_player.getMaxHp());
			_player.setCurrentMp(_player.getMaxMp());
			_player.setCurrentCp(_player.getMaxCp());
			
			// Give infinite mana to non-Orc mages for constant magic usage
			if (_playerClass.isMage() && _playerClass.getRace() != org.l2jmobius.gameserver.model.actor.enums.creature.Race.ORC)
			{
				// Add a permanent stat modifier for infinite mana
				_player.getStat().addFixedValue(org.l2jmobius.gameserver.model.stats.Stat.MAX_MP, 999999.0);
				_player.setCurrentMp(999999);
				LOGGER.info("Given infinite mana to non-Orc mage autobot: " + _player.getName());
			}
			
			// Add Wind Strike skill for non-Orc mages only
			if (_playerClass.isMage() && _playerClass.getRace() != org.l2jmobius.gameserver.model.actor.enums.creature.Race.ORC)
			{
				addWindStrikeSkill();
			}
					
			// Equip starting items based on race and class
			equipStartingItems();
		}
		catch (Exception e)
		{
			LOGGER.warning("Error initializing autobot stats: " + e.getMessage());
		}
	}
	
	/**
	 * Add Wind Strike skill to mage autobots
	 */
	private void addWindStrikeSkill()
	{
		try
		{
			// Get Wind Strike skill (ID 1177, Level 1)
			org.l2jmobius.gameserver.model.skill.Skill windStrike = 
				org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1177, 1);
			
			if (windStrike != null)
			{
				_player.addSkill(windStrike, true);
				LOGGER.info("Added Wind Strike skill to mage autobot: " + _player.getName());
			}
			else
			{
				LOGGER.warning("Wind Strike skill (1177) not found for autobot: " + _player.getName());
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Error adding Wind Strike skill: " + e.getMessage());
		}
	}
	
	/**
	 * Equip starting items based on race and class type
	 */
	private void equipStartingItems()
	{
		try
		{
			org.l2jmobius.gameserver.model.actor.enums.creature.Race race = _playerClass.getRace();
			boolean isMage = _playerClass.isMage();
			
			// Equipment IDs based on race and class
			int weaponId;
			int chestId;
			int legsId;
			
			if (isMage)
			{
				// Mage equipment
				switch (race)
				{
					case HUMAN:
					case ELF:
					case DARK_ELF:
						weaponId = 6; // Apprentice's Wand
						chestId = 425; // Apprentice's Tunic
						legsId = 461; // Apprentice's Stockings
						break;
					case ORC:
						weaponId = 2368; // Training Gloves
						chestId = 425; // Apprentice's Tunic
						legsId = 461; // Apprentice's Stockings
						break;
					default:
						weaponId = 6; // Default: Apprentice's Wand
						chestId = 425; // Apprentice's Tunic
						legsId = 461; // Apprentice's Stockings
						break;
				}
			}
			else
			{
				// Fighter equipment
				switch (race)
				{
					case HUMAN:
					case ELF:
					case DARK_ELF:
						weaponId = 2369; // Squire's Sword
						chestId = 1146; // Squire's Shirt
						legsId = 1147; // Squire's Pants
						break;
					case DWARF:
						weaponId = 2370; // Guild Member's Club
						chestId = 1146; // Squire's Shirt
						legsId = 1147; // Squire's Pants
						break;
					case ORC:
						weaponId = 2368; // Training Gloves
						chestId = 1146; // Squire's Shirt
						legsId = 1147; // Squire's Pants
						break;
					default:
						weaponId = 2369; // Default: Squire's Sword
						chestId = 1146; // Squire's Shirt
						legsId = 1147; // Squire's Pants
						break;
				}
			}
			
			// Create and equip items
			equipItem(weaponId, "weapon");
			equipItem(chestId, "chest");
			equipItem(legsId, "legs");
			
			LOGGER.info("Equipped starting items for " + _player.getName() + " (" + race + ", " + 
				(isMage ? "Mage" : "Fighter") + "): Weapon " + weaponId + ", Chest " + chestId + ", Legs " + legsId);
		}
		catch (Exception e)
		{
			LOGGER.warning("Error equipping starting items for autobot " + _player.getName() + ": " + e.getMessage());
		}
	}
	
	/**
	 * Helper method to create and equip an item
	 */
	private void equipItem(int itemId, String slotName)
	{
		try
		{
			// Get item template
			org.l2jmobius.gameserver.model.item.ItemTemplate template = 
				org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(itemId);
			
			if (template != null)
			{
				// Create item instance using ItemManager
				org.l2jmobius.gameserver.model.item.instance.Item item = 
					org.l2jmobius.gameserver.managers.ItemManager.createItem(
						org.l2jmobius.gameserver.model.item.enums.ItemProcessType.QUEST, itemId, 1, _player, null);
				
				if (item != null)
				{
					// Add to inventory using correct process type
					_player.getInventory().addItem(
						org.l2jmobius.gameserver.model.item.enums.ItemProcessType.QUEST, 
						itemId, 1, _player, null);
					
					// Equip the item
					_player.getInventory().equipItem(item);
					
					LOGGER.fine("Equipped " + slotName + " item " + itemId + " (" + template.getName() + ") for " + _player.getName());
				}
				else
				{
					LOGGER.warning("Failed to create item instance for " + slotName + " (ID: " + itemId + ") for " + _player.getName());
				}
			}
			else
			{
				LOGGER.warning("Item template not found for " + slotName + " (ID: " + itemId + ") for " + _player.getName());
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Error equipping " + slotName + " item " + itemId + " for " + _player.getName() + ": " + e.getMessage());
		}
	}
	
	/**
	 * Create random appearance for race
	 */
	private static PlayerAppearance createRandomAppearance(Race race)
	{
		final boolean isFemale = Math.random() < 0.5;
		final byte face = (byte) (Math.random() * 3); // 0-2
		final byte hairColor = (byte) (Math.random() * 4); // 0-3
		final byte hairStyle = (byte) (Math.random() * 5); // 0-4
		
		return new PlayerAppearance(face, hairColor, hairStyle, isFemale);
	}
	
	/**
	 * Get the underlying player instance
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Get autobot name
	 */
	public String getName()
	{
		return _player.getName();
	}
	
	/**
	 * Get object ID
	 */
	public int getObjectId()
	{
		return _player.getObjectId();
	}
	
	/**
	 * Get level
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Get class name
	 */
	public String getClassName()
	{
		return _playerClass.name();
	}
	
	/**
	 * Get class ID
	 */
	public PlayerClass getClassId()
	{
		return _playerClass;
	}
	
	/**
	 * Get X coordinate
	 */
	public int getX()
	{
		return _player.getX();
	}
	
	/**
	 * Get Y coordinate
	 */
	public int getY()
	{
		return _player.getY();
	}
	
	/**
	 * Get Z coordinate
	 */
	public int getZ()
	{
		return _player.getZ();
	}
	
	/**
	 * Handle chat message to this autobot - DISABLED
	 */
	public void handleChatMessage(String message, Player sender, ChatType chatType)
	{
		// Chat interactions disabled to prevent unwanted social behavior
		// Autobots will focus solely on combat and farming
		return;
	}
	
	/**
	 * Handle party invitation - always decline to prevent party formation
	 */
	public void handlePartyInvitation(Player inviter)
	{
		if (inviter != null)
		{
			LOGGER.info("Autobot " + getName() + " declining party invitation from " + inviter.getName());
			// Send decline response
			_player.broadcastPacket(new org.l2jmobius.gameserver.network.serverpackets.CreatureSay(
				_player, ChatType.GENERAL, _player.getName(), "Sorry, I prefer to hunt alone!"));
		}
	}
	
	/**
	 * Set home location for autobot
	 */
	public void setHomeLocation(Location location)
	{
		_homeLocation = location;
	}
	
	/**
	 * Get home location
	 */
	public Location getHomeLocation()
	{
		return _homeLocation;
	}
	
	/**
	 * Set auto farm radius
	 */
	public void setAutoFarmRadius(int radius)
	{
		_autoFarmRadius = radius;
	}
	
	/**
	 * Get auto farm radius
	 */
	public int getAutoFarmRadius()
	{
		return _autoFarmRadius;
	}
	
	/**
	 * Enable/disable auto farming
	 */
	public void setAutoFarmEnabled(boolean enabled)
	{
		_autoFarmEnabled = enabled;
		LOGGER.info("Auto farming " + (enabled ? "enabled" : "disabled") + " for autobot: " + getName());
	}
	
	/**
	 * Check if auto farming is enabled
	 */
	public boolean isAutoFarmEnabled()
	{
		return _autoFarmEnabled;
	}
	
	/**
	 * Check if autobot is online
	 */
	public boolean isOnline()
	{
		return _isOnline && (_player != null);
	}
	
	/**
	 * Get current AI state for debugging
	 */
	public String getAIState()
	{
		if (_autobotAI != null)
		{
			return _autobotAI.getCurrentState().toString();
		}
		return "NO_AI";
	}
	
	/**
	 * Set online status
	 */
	public void setOnline(boolean online)
	{
		_isOnline = online;
	}
	
	/**
	 * Spawn the autobot in the world and make it visible
	 */
	public void spawnInWorld()
	{
		if (_homeLocation != null && _player != null)
		{
			// Spawn the autobot at its home location
			_player.spawnMe(_homeLocation.getX(), _homeLocation.getY(), _homeLocation.getZ());
			
			// Make sure it's visible to other players
			_player.broadcastUserInfo();
			
			// Ensure AI is properly initialized and connected
			if (_autobotAI != null)
			{
				_player.setAI(_autobotAI);
				// Start AI thinking process and set to farming mode
				_player.getAI().setIntention(org.l2jmobius.gameserver.ai.Intention.ACTIVE);
				_autoFarmEnabled = true; // Force enable auto farming
				
				// Prevent party formation - ensure no party
				_player.setParty(null);
				
				LOGGER.info("AutobotAI initialized and activated for: " + getName());
			}
			
			LOGGER.info("Autobot " + getName() + " spawned at " + _homeLocation);
		}
	}
	
	/**
	 * Despawn the autobot from the world
	 */
	public void despawnFromWorld()
	{
		if (_player != null)
		{
			// Remove from world
			_player.decayMe();
			_isOnline = false;
			
			LOGGER.info("Autobot " + getName() + " despawned from world");
		}
	}
	
	/**
	 * Delete the autobot
	 */
	public void deleteMe()
	{
		LOGGER.info("Deleting autobot: " + getName());
		_isOnline = false;
		if (_player != null)
		{
			_player.deleteMe();
		}
	}
	
	/**
	 * Check if autobot died and handle despawning
	 */
	public void checkDeathStatus()
	{
		if (_player != null && _player.isDead() && _isOnline)
		{
			LOGGER.info("Autobot " + getName() + " has died and will be despawned permanently");
			
			// Mark as offline to prevent further processing
			_isOnline = false;
			
			// Schedule despawn with a short delay to allow death animation
			org.l2jmobius.commons.threads.ThreadPool.schedule(() -> {
				// Remove from AutobotManager
				org.l2jmobius.gameserver.managers.AutobotManager.getInstance().despawnAutobot(getName());
			}, 3000); // 3 second delay
		}
	}
	
	/**
	 * Get aggression level
	 */
	public int getAggressionLevel()
	{
		if (_autobotAI != null)
		{
			return _autobotAI.getAggressionLevel();
		}
		return 50;
	}
	
	/**
	 * Get social level
	 */
	public int getSocialLevel()
	{
		if (_autobotAI != null)
		{
			return _autobotAI.getSocialLevel();
		}
		return 30;
	}
}