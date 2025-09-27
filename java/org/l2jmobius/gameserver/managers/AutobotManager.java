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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Autobot;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.taskmanagers.AutobotTaskManager;

/**
 * Manager for autobot system
 */
public class AutobotManager
{
	private static final Logger LOGGER = Logger.getLogger(AutobotManager.class.getName());
	private static final Logger FAKE_PLAYER_LOGGER = Logger.getLogger("FakePlayer");
	
	private final ConcurrentHashMap<String, Autobot> _autobots = new ConcurrentHashMap<>();
	private final AtomicInteger _autobotCount = new AtomicInteger(0);
	
	private static final String LOAD_AUTOBOTS = "SELECT * FROM autobots";
	private static final String INSERT_AUTOBOT = "INSERT INTO autobots (name, account_name, class_id, level, x, y, z, heading) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String DELETE_AUTOBOT = "DELETE FROM autobots WHERE name = ?";
	private static final String DELETE_ALL_AUTOBOTS = "DELETE FROM autobots";
	
	protected AutobotManager()
	{
		setupFakePlayerLogger();
		load();
		// Start AI processing
		AutobotTaskManager.getInstance().startAIProcessing();
		LOGGER.info("AutobotManager initialized - AI processing started");
	}
	
	/**
	 * Setup dedicated logger for fake players
	 */
	private void setupFakePlayerLogger()
	{
		try
		{
			// Create timestamp for log file name
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
			String timestamp = dateFormat.format(new Date());
			String logFileName = "log/fake-player-" + timestamp + ".log";
			
			// Create file handler
			FileHandler fileHandler = new FileHandler(logFileName, true);
			fileHandler.setFormatter(new SimpleFormatter());
			
			// Configure the fake player logger
			FAKE_PLAYER_LOGGER.setUseParentHandlers(false); // Don't use parent console handler
			FAKE_PLAYER_LOGGER.addHandler(fileHandler);
			FAKE_PLAYER_LOGGER.setLevel(Level.INFO);
			
			FAKE_PLAYER_LOGGER.info("=== Fake Player System Started at " + new Date() + " ===");
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, "Failed to setup fake player logger", e);
		}
	}
	
	/**
	 * Get the dedicated fake player logger
	 */
	public static Logger getFakePlayerLogger()
	{
		return FAKE_PLAYER_LOGGER;
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
			
			FAKE_PLAYER_LOGGER.info("Created and spawned autobot: " + name + " with class " + playerClass + " at level " + level + " at location " + location);
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
	 * Register an existing autobot with the manager
	 */
	public void registerAutobot(Autobot autobot)
	{
		if (autobot != null && !_autobots.containsKey(autobot.getName()))
		{
			_autobots.put(autobot.getName(), autobot);
			_autobotCount.incrementAndGet();
			FAKE_PLAYER_LOGGER.info("Registered autobot: " + autobot.getName());
			LOGGER.info("Registered autobot: " + autobot.getName());
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
			FAKE_PLAYER_LOGGER.info("Despawned autobot: " + name);
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
			autobot.handleChatMessage(message, sender, chatType);
		}
	}
	
	/**
	 * Get autobot statistics
	 */
	public String getAutobotStatistics()
	{
		StringBuilder stats = new StringBuilder();
		stats.append("=== Autobot Statistics ===").append("\n");
		stats.append("Total Autobots: ").append(_autobotCount.get()).append("\n");
		stats.append("AI Processing: ").append(AutobotTaskManager.getInstance().isActive() ? "Active" : "Inactive").append("\n");
		
		if (!_autobots.isEmpty())
		{
			stats.append("\n=== Individual Autobots ===").append("\n");
			for (Autobot autobot : _autobots.values())
			{
				stats.append("Name: ").append(autobot.getName())
					.append(", Level: ").append(autobot.getLevel())
					.append(", Class: ").append(autobot.getClassName())
					.append(", State: ").append(autobot.getAIState())
					.append(", Online: ").append(autobot.isOnline())
					.append("\n");
			}
		}
		
		return stats.toString();
	}
	
	/**
	 * Shutdown manager
	 */
	public void shutdown()
	{
		LOGGER.info("Shutting down AutobotManager...");
		
		// Stop AI processing
		AutobotTaskManager.getInstance().stopAIProcessing();
		
		// Despawn all autobots
		despawnAllAutobots();
		
		LOGGER.info("AutobotManager shutdown complete.");
	}
	

	
	/**
	 * Save autobot to database
	 */
	private void saveAutobotToDatabase(String name, String accountName, int classId, int level, Location location)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Disable autocommit to handle transaction manually
			con.setAutoCommit(false);
			
			try
			{
				// First, delete any existing entry with this name to prevent primary key conflicts
				try (PreparedStatement deletePs = con.prepareStatement(DELETE_AUTOBOT))
				{
					deletePs.setString(1, name);
					int deletedRows = deletePs.executeUpdate();
				if (deletedRows > 0)
				{
					FAKE_PLAYER_LOGGER.info("Deleted existing autobot entry for: " + name);
					LOGGER.info("Deleted existing autobot entry for: " + name);
				}
				}
				
				// Now insert the new entry
				try (PreparedStatement insertPs = con.prepareStatement(INSERT_AUTOBOT))
				{
					insertPs.setString(1, name);
					insertPs.setString(2, accountName);
					insertPs.setInt(3, classId);
					insertPs.setInt(4, level);
					insertPs.setInt(5, location.getX());
					insertPs.setInt(6, location.getY());
					insertPs.setInt(7, location.getZ());
					insertPs.setInt(8, location.getHeading());
					insertPs.executeUpdate();
				}
				
				// Commit the transaction
				con.commit();
				FAKE_PLAYER_LOGGER.info("Successfully saved autobot to database: " + name);
				LOGGER.info("Successfully saved autobot to database: " + name);
			}
			catch (SQLException e)
			{
				// Rollback on error
				try
				{
					con.rollback();
				}
				catch (SQLException rollbackEx)
				{
					LOGGER.log(Level.WARNING, "Error rolling back transaction for: " + name, rollbackEx);
				}
				throw e; // Re-throw to be caught by outer catch
			}
			finally
			{
				// Restore autocommit
				try
				{
					con.setAutoCommit(true);
				}
				catch (SQLException e)
				{
					LOGGER.log(Level.WARNING, "Error restoring autocommit for: " + name, e);
				}
			}
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
			int deletedRows = ps.executeUpdate();
			if (deletedRows > 0)
			{
				LOGGER.info("Deleted autobot from database: " + name);
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error deleting autobot from database: " + name, e);
		}
	}
	
	/**
	 * Clear specific autobots from database by name pattern
	 */
	public void clearAutobotsFromDatabase(String namePattern)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM autobots WHERE name LIKE ?"))
		{
			ps.setString(1, namePattern);
			int deletedRows = ps.executeUpdate();
			LOGGER.info("Cleared " + deletedRows + " autobots from database with pattern: " + namePattern);
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error clearing autobots from database with pattern: " + namePattern, e);
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