/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Hall of Death manager for tracking permanent deaths
 * @author L2jMobius
 */
public class HallOfDeathTable
{
	private static final Logger LOGGER = Logger.getLogger(HallOfDeathTable.class.getName());
	
	private static final String INSERT_DEATH = "INSERT INTO hall_of_death (player_name, player_id, killer_name, killer_type, player_level, player_class, death_time, death_location) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String SELECT_LAST_DEATHS = "SELECT * FROM hall_of_death ORDER BY death_time DESC LIMIT ?";
	private static final String SELECT_TOP_LEVEL_DEATHS = "SELECT * FROM hall_of_death ORDER BY player_level DESC, death_time DESC LIMIT ?";
	private static final String SELECT_ALL_DEATHS = "SELECT * FROM hall_of_death ORDER BY death_time DESC LIMIT ? OFFSET ?";
	private static final String COUNT_ALL_DEATHS = "SELECT COUNT(*) FROM hall_of_death";
	
	/**
	 * Death record holder class
	 */
	public static class DeathRecord
	{
		private final int id;
		private final String playerName;
		private final int playerId;
		private final String killerName;
		private final String killerType;
		private final int playerLevel;
		private final String playerClass;
		private final long deathTime;
		private final String deathLocation;
		
		public DeathRecord(int id, String playerName, int playerId, String killerName, String killerType, int playerLevel, String playerClass, long deathTime, String deathLocation)
		{
			this.id = id;
			this.playerName = playerName;
			this.playerId = playerId;
			this.killerName = killerName;
			this.killerType = killerType;
			this.playerLevel = playerLevel;
			this.playerClass = playerClass;
			this.deathTime = deathTime;
			this.deathLocation = deathLocation;
		}
		
		public int getId()
		{
			return id;
		}
		
		public String getPlayerName()
		{
			return playerName;
		}
		
		public int getPlayerId()
		{
			return playerId;
		}
		
		public String getKillerName()
		{
			return killerName;
		}
		
		public String getKillerType()
		{
			return killerType;
		}
		
		public int getPlayerLevel()
		{
			return playerLevel;
		}
		
		public String getPlayerClass()
		{
			return playerClass;
		}
		
		public long getDeathTime()
		{
			return deathTime;
		}
		
		public String getDeathLocation()
		{
			return deathLocation;
		}
	}
	
	/**
	 * Records a permanent death in the database
	 * @param player the player who died
	 * @param killerName name of the killer
	 * @param killerType type of killer (Monster, Raid Boss, etc.)
	 * @param deathLocation location where death occurred
	 */
	public static void recordDeath(Player player, String killerName, String killerType, String deathLocation)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_DEATH))
		{
			ps.setString(1, player.getName());
			ps.setInt(2, player.getObjectId());
			ps.setString(3, killerName);
			ps.setString(4, killerType);
			ps.setInt(5, player.getLevel());
			ps.setString(6, ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName());
			ps.setLong(7, System.currentTimeMillis());
			ps.setString(8, deathLocation);
			
			ps.executeUpdate();
			
			LOGGER.info("[HALL OF DEATH] Recorded death of " + player.getName() + " (Level " + player.getLevel() + " " + ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName() + ") killed by " + killerType + " " + killerName + " at " + deathLocation);
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to record death for player " + player.getName(), e);
		}
	}
	
	/**
	 * Gets the most recent deaths
	 * @param limit number of records to return
	 * @return list of death records
	 */
	public static List<DeathRecord> getLastDeaths(int limit)
	{
		final List<DeathRecord> deaths = new ArrayList<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_LAST_DEATHS))
		{
			ps.setInt(1, limit);
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					deaths.add(new DeathRecord(
						rs.getInt("id"),
						rs.getString("player_name"),
						rs.getInt("player_id"),
						rs.getString("killer_name"),
						rs.getString("killer_type"),
						rs.getInt("player_level"),
						rs.getString("player_class"),
						rs.getLong("death_time"),
						rs.getString("death_location")
					));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to retrieve last deaths", e);
		}
		
		return deaths;
	}
	
	/**
	 * Gets deaths sorted by highest level first
	 * @param limit number of records to return
	 * @return list of death records
	 */
	public static List<DeathRecord> getTopLevelDeaths(int limit)
	{
		final List<DeathRecord> deaths = new ArrayList<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_TOP_LEVEL_DEATHS))
		{
			ps.setInt(1, limit);
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					deaths.add(new DeathRecord(
						rs.getInt("id"),
						rs.getString("player_name"),
						rs.getInt("player_id"),
						rs.getString("killer_name"),
						rs.getString("killer_type"),
						rs.getInt("player_level"),
						rs.getString("player_class"),
						rs.getLong("death_time"),
						rs.getString("death_location")
					));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to retrieve top level deaths", e);
		}
		
		return deaths;
	}
	
	/**
	 * Gets all deaths with pagination
	 * @param limit number of records to return
	 * @param offset offset for pagination
	 * @return list of death records
	 */
	public static List<DeathRecord> getAllDeaths(int limit, int offset)
	{
		final List<DeathRecord> deaths = new ArrayList<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_ALL_DEATHS))
		{
			ps.setInt(1, limit);
			ps.setInt(2, offset);
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					deaths.add(new DeathRecord(
						rs.getInt("id"),
						rs.getString("player_name"),
						rs.getInt("player_id"),
						rs.getString("killer_name"),
						rs.getString("killer_type"),
						rs.getInt("player_level"),
						rs.getString("player_class"),
						rs.getLong("death_time"),
						rs.getString("death_location")
					));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to retrieve all deaths", e);
		}
		
		return deaths;
	}
	
	/**
	 * Gets total count of deaths in database
	 * @return total death count
	 */
	public static int getTotalDeathCount()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(COUNT_ALL_DEATHS);
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				return rs.getInt(1);
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to get total death count", e);
		}
		
		return 0;
	}
}