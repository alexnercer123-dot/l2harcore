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

/**
 * Autobot class - wrapper around Player to create visible AI-controlled players
 * This implementation uses composition to manage a Player instance
 */
public class Autobot
{
	private static final Logger LOGGER = Logger.getLogger(Autobot.class.getName());
	
	private final Player _player;
	private Location _homeLocation;
	private int _autoFarmRadius = 1000;
	private boolean _autoFarmEnabled = false;
	private boolean _isOnline = false;
	private final PlayerClass _playerClass;
	private final int _level;
	
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
		}
		catch (Exception e)
		{
			LOGGER.warning("Error initializing autobot stats: " + e.getMessage());
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
	 * Handle chat message to this autobot
	 */
	public void handleChatMessage(String message, Player sender, ChatType chatType)
	{
		LOGGER.info("Autobot " + getName() + " received message from " + sender.getName() + ": " + message);
		
		// Simple auto-response
		if (message.toLowerCase().contains("hello"))
		{
			_player.sendMessage("Hello " + sender.getName() + "!");
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
}