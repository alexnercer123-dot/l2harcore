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
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.enums.MailType;

/**
 * RvR (Race vs Race) Points Manager
 * @author Mobius
 */
public class RvRManager
{
	private static final Logger LOGGER = Logger.getLogger(RvRManager.class.getName());
	
	private static final String SELECT_RVR_POINTS = "SELECT race, points FROM rvr_points";
	private static final String UPDATE_RVR_POINTS = "UPDATE rvr_points SET points = ? WHERE race = ?";
	private static final String INSERT_RVR_POINTS = "INSERT INTO rvr_points (race, points) VALUES (?, ?) ON DUPLICATE KEY UPDATE points = VALUES(points)";
	
	private final Map<String, Integer> _racePoints = new ConcurrentHashMap<>();
	private final Map<String, Integer> _lastRewardPoints = new ConcurrentHashMap<>();
	
	protected RvRManager()
	{
		load();
	}
	
	private void load()
	{
		if (!Config.RVR_ENABLED)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_RVR_POINTS);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final String race = rs.getString("race");
				final int points = rs.getInt("points");
				_racePoints.put(race, points);
				_lastRewardPoints.put(race, 0);
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error loading RvR points from database: " + e.getMessage(), e);
		}
		
		LOGGER.info("RvRManager: Loaded " + _racePoints.size() + " race points.");
	}
	
	/**
	 * Add points for a race when a player kills another race player
	 * @param killerRace the race of the killer
	 * @param points the points to add
	 */
	public void addPoints(Race killerRace, int points)
	{
		if (!Config.RVR_ENABLED || (killerRace == null))
		{
			return;
		}
		
		final String raceName = killerRace.name();
		final int currentPoints = _racePoints.getOrDefault(raceName, 0);
		final int newPoints = currentPoints + points;
		
		_racePoints.put(raceName, newPoints);
		updateDatabase(raceName, newPoints);
		
		// Check for rewards
		checkAndGiveRewards(raceName, currentPoints, newPoints);
		
		LOGGER.info("RvRManager: Added " + points + " points to race " + raceName + ". Total: " + newPoints);
	}
	
	/**
	 * Get points for a race
	 * @param race the race
	 * @return the points
	 */
	public int getPoints(Race race)
	{
		if (race == null)
		{
			return 0;
		}
		return _racePoints.getOrDefault(race.name(), 0);
	}
	
	/**
	 * Update points in database
	 * @param race the race name
	 * @param points the points
	 */
	private void updateDatabase(String race, int points)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_RVR_POINTS))
		{
			ps.setInt(1, points);
			ps.setString(2, race);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error updating RvR points in database: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Check and give rewards if race reached threshold
	 * @param raceName the race name
	 * @param oldPoints the old points
	 * @param newPoints the new points
	 */
	private void checkAndGiveRewards(String raceName, int oldPoints, int newPoints)
	{
		final Map<Integer, ItemHolder> raceRewards = Config.RVR_RACE_REWARDS.get(raceName);
		if (raceRewards == null)
		{
			return;
		}
		
		for (Map.Entry<Integer, ItemHolder> entry : raceRewards.entrySet())
		{
			final int threshold = entry.getKey();
			final ItemHolder reward = entry.getValue();
			
			// Check if we crossed this threshold
			if ((oldPoints < threshold) && (newPoints >= threshold))
			{
				giveRewardToRace(raceName, reward, threshold);
			}
		}
	}
	
	/**
	 * Give reward to all players of a race
	 * @param raceName the race name
	 * @param reward the reward item
	 * @param threshold the threshold reached
	 */
	private void giveRewardToRace(String raceName, ItemHolder reward, int threshold)
	{
		final Race race;
		try
		{
			race = Race.valueOf(raceName);
		}
		catch (IllegalArgumentException e)
		{
			LOGGER.warning("RvRManager: Invalid race name: " + raceName);
			return;
		}
		
		int rewardCount = 0;
		for (Player player : World.getInstance().getPlayers())
		{
			if ((player != null) && (player.getRace() == race))
			{
				// Create mail message with reward attachment
				final String subject = "RvR Race Reward";
				final String content = "Congratulations! Your race (" + raceName + ") has reached " + threshold + " points. Please enjoy this reward.";
				final Message msg = new Message(player.getObjectId(), subject, content, MailType.NPC);
				
				// Create attachments and add the reward item
				final Mail attachments = msg.createAttachments();
				if (attachments != null)
				{
					// Create the item instance
					final Item rewardItem = ItemManager.createItem(ItemProcessType.REFUND, reward.getId(), reward.getCount(), player);
					if (rewardItem != null)
					{
						attachments.addItem(ItemProcessType.REFUND, rewardItem, null, null);
						
						// Send the mail
						MailManager.getInstance().sendMessage(msg);
						rewardCount++;
					}
				}
			}
		}
		
		LOGGER.info("RvRManager: Sent rewards to " + rewardCount + " players of race " + raceName + " for reaching " + threshold + " points.");
	}
	
	/**
	 * Check if two races are different
	 * @param race1 the first race
	 * @param race2 the second race
	 * @return true if races are different
	 */
	public static boolean isDifferentRace(Race race1, Race race2)
	{
		if ((race1 == null) || (race2 == null))
		{
			return false;
		}
		
		// Only count player races
		return isPlayerRace(race1) && isPlayerRace(race2) && (race1 != race2);
	}
	
	/**
	 * Check if race is a player race
	 * @param race the race
	 * @return true if it's a player race
	 */
	private static boolean isPlayerRace(Race race)
	{
		return (race == Race.HUMAN) || (race == Race.ELF) || (race == Race.DARK_ELF) || 
			   (race == Race.ORC) || (race == Race.DWARF) || (race == Race.KAMAEL) || 
			   (race == Race.ERTHEIA);
	}
	
	/**
	 * Get the singleton instance
	 * @return the RvRManager instance
	 */
	public static RvRManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RvRManager INSTANCE = new RvRManager();
	}
}