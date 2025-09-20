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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Autobot;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Manager for autobot system
 */
public class AutobotManager
{
	private static final Logger LOGGER = Logger.getLogger(AutobotManager.class.getName());
	
	private final ConcurrentHashMap<String, Autobot> _autobots = new ConcurrentHashMap<>();
	private final AtomicInteger _autobotCount = new AtomicInteger(0);
	
	private static final String LOAD_AUTOBOTS = "SELECT * FROM autobots";
	private static final String INSERT_AUTOBOT = "INSERT INTO autobots (name, account_name, class_id, level, x, y, z, heading) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String DELETE_AUTOBOT = "DELETE FROM autobots WHERE name = ?";
	private static final String DELETE_ALL_AUTOBOTS = "DELETE FROM autobots";
	
	protected AutobotManager()
	{
		load();
	}
	
	/**
	 * Load autobots from database
	 */
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_AUTOBOTS);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final String name = rs.getString("name");
				final String accountName = rs.getString("account_name");
				final int classId = rs.getInt("class_id");
				final int level = rs.getInt("level");
				final int x = rs.getInt("x");
				final int y = rs.getInt("y");
				final int z = rs.getInt("z");
				final int heading = rs.getInt("heading");
				
				// For now, just count loaded autobots. We'll implement proper loading later.
				_autobotCount.incrementAndGet();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error loading autobots from database", e);
		}
		
		LOGGER.info("AutobotManager: Loaded " + _autobotCount.get() + " autobots from database.");
	}
	
	/**
	 * Create and spawn a new autobot
	 */
	public boolean createAutobot(String name, PlayerClass playerClass, int level, Location location, Player creator)
	{
		if (_autobots.containsKey(name))
		{
			return false;
		}
		
		try
		{
			// Create the autobot using the new factory method
			final Autobot autobot = Autobot.createAutobot(name, playerClass, level, location);
			if (autobot == null)
			{
				LOGGER.warning("Failed to create autobot: " + name);
				return false;
			}
			
			// Store the autobot
			_autobots.put(name, autobot);
			
			// Spawn the autobot in the world to make it visible
			autobot.spawnInWorld();
			
			LOGGER.info("Created and spawned autobot: " + name + " with class " + playerClass + " at level " + level);
			
			// Save to database
			saveAutobotToDatabase(name, "AutobotAccount", playerClass.getId(), level, location);
			
			_autobotCount.incrementAndGet();
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error creating autobot: " + name, e);
			return false;
		}
	}
	
	/**
	 * Despawn autobot by name
	 */
	public boolean despawnAutobot(String name)
	{
		final Autobot autobot = _autobots.remove(name);
		if (autobot != null)
		{
			// Despawn from world
			autobot.despawnFromWorld();
			
			// Remove from database
			deleteAutobotFromDatabase(name);
			
			_autobotCount.decrementAndGet();
			return true;
		}
		return false;
	}
	
	/**
	 * Despawn all autobots
	 */
	public int despawnAllAutobots()
	{
		final int count = _autobots.size();
		
		// Despawn all from world
		for (Autobot autobot : _autobots.values())
		{
			autobot.despawnFromWorld();
		}
		
		_autobots.clear();
		_autobotCount.set(0);
		
		// Clear database
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_ALL_AUTOBOTS))
		{
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error clearing autobots from database", e);
		}
		
		return count;
	}
	
	/**
	 * Get active autobots count
	 */
	public int getActiveAutobotsCount()
	{
		return _autobotCount.get();
	}
	
	/**
	 * Get all active autobots
	 */
	public Collection<Autobot> getActiveAutobots()
	{
		return _autobots.values();
	}
	
	/**
	 * Get specific autobot by name
	 */
	public Autobot getAutobot(String name)
	{
		return _autobots.get(name);
	}
	
	/**
	 * Handle chat message to autobot
	 */
	public void handleChatToAutobot(Player sender, String targetName, String message, ChatType chatType)
	{
		final Autobot autobot = _autobots.get(targetName);
		if (autobot != null)
		{
			// For now, just log the message. We can implement chat handling later.
			LOGGER.info("Chat to autobot " + targetName + " from " + sender.getName() + ": " + message);
		}
	}
	

	
	/**
	 * Save autobot to database
	 */
	private void saveAutobotToDatabase(String name, String accountName, int classId, int level, Location location)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_AUTOBOT))
		{
			ps.setString(1, name);
			ps.setString(2, accountName);
			ps.setInt(3, classId);
			ps.setInt(4, level);
			ps.setInt(5, location.getX());
			ps.setInt(6, location.getY());
			ps.setInt(7, location.getZ());
			ps.setInt(8, location.getHeading());
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error saving autobot to database: " + name, e);
		}
	}
	
	/**
	 * Delete autobot from database
	 */
	private void deleteAutobotFromDatabase(String name)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_AUTOBOT))
		{
			ps.setString(1, name);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error deleting autobot from database: " + name, e);
		}
	}
	
	/**
	 * Get singleton instance
	 */
	public static AutobotManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AutobotManager INSTANCE = new AutobotManager();
	}
}