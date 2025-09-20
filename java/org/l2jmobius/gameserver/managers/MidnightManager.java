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
package org.l2jmobius.gameserver.managers;

import java.util.function.Consumer;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.OnDayNightChange;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Manager for the Midnight mod that enhances monsters during nighttime.
 * When night falls, monsters become stronger but provide better rewards.
 * @author Generated for L2J Mobius
 */
public class MidnightManager
{
	private static boolean _midnightActive = false;
	
	// Event listener for day/night changes
	private final Consumer<OnDayNightChange> _dayNightListener = event ->
	{
		if (!Config.MIDNIGHT_MOD_ENABLED)
		{
			return;
		}
		
		final boolean isNight = event.isNight();
		if (isNight && !_midnightActive)
		{
			// Night has fallen - activate Midnight effects
			activateMidnight();
		}
		else if (!isNight && _midnightActive)
		{
			// Dawn has broken - deactivate Midnight effects
			deactivateMidnight();
		}
	};
	
	protected MidnightManager()
	{
		// Register the day/night change listener if the mod is enabled
		if (Config.MIDNIGHT_MOD_ENABLED)
		{
			Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_DAY_NIGHT_CHANGE, _dayNightListener, this));
			
			// Set initial state based on current game time
			_midnightActive = GameTimeTaskManager.getInstance().isNight();
		}
	}
	
	/**
	 * Activates Midnight effects when night falls.
	 */
	private void activateMidnight()
	{
		_midnightActive = true;
		
		// Refresh all monster stats and scale HP proportionally
		refreshAllMonsterStats(true);
		
		// Send announcement to all players if enabled
		if (Config.MIDNIGHT_ANNOUNCES_ENABLED)
		{
			Broadcast.toAllOnlinePlayers(Config.MIDNIGHT_START_MESSAGE);
		}
	}
	
	/**
	 * Deactivates Midnight effects when dawn breaks.
	 */
	private void deactivateMidnight()
	{
		_midnightActive = false;
		
		// Refresh all monster stats and scale HP proportionally
		refreshAllMonsterStats(false);
		
		// Send announcement to all players if enabled
		if (Config.MIDNIGHT_ANNOUNCES_ENABLED)
		{
			Broadcast.toAllOnlinePlayers(Config.MIDNIGHT_END_MESSAGE);
		}
	}
	
	/**
	 * Refreshes stats for all monsters in the world and scales their current HP proportionally.
	 * This ensures that when Midnight activates/deactivates, monsters maintain their HP percentage.
	 * @param midnightActivating true if midnight is activating, false if deactivating
	 */
	private void refreshAllMonsterStats(boolean midnightActivating)
	{
		// Iterate through all visible objects in the world to find monsters
		for (WorldObject obj : World.getInstance().getVisibleObjects())
		{
				if (obj.isNpc())
				{
					final Npc npc = obj.asNpc();
					// Only affect regular monsters that are not raid bosses, minions, or raid minions
					if (npc.isMonster() && !npc.isRaid() && !npc.isMinion() && !npc.isRaidMinion())
				{
					// Store current HP percentage before stat change
					final double currentHpPercent = npc.getCurrentHp() / npc.getMaxHp();
					
					// Force stat recalculation
					npc.getStat().recalculateStats(true);
					
					// Scale current HP to maintain the same percentage with new max HP
					final double newCurrentHp = npc.getMaxHp() * currentHpPercent;
					npc.setCurrentHp(newCurrentHp, true);
				}
			}
		}
	}
	
	/**
	 * Checks if Midnight mode is currently active.
	 * @return true if it's nighttime and Midnight mod is active
	 */
	public static boolean isMidnightActive()
	{
		return Config.MIDNIGHT_MOD_ENABLED && _midnightActive;
	}
	
	/**
	 * Gets the monster HP multiplier for Midnight mode.
	 * @return the HP multiplier (includes base NPC multipliers if enabled)
	 */
	public static double getMidnightHpMultiplier()
	{
		return isMidnightActive() ? Config.MIDNIGHT_MONSTER_HP_MULTIPLIER : 1.0;
	}
	
	/**
	 * Gets the monster physical attack multiplier for Midnight mode.
	 * @return the physical attack multiplier
	 */
	public static double getMidnightPAtkMultiplier()
	{
		return isMidnightActive() ? Config.MIDNIGHT_MONSTER_PATK_MULTIPLIER : 1.0;
	}
	
	/**
	 * Gets the monster magical attack multiplier for Midnight mode.
	 * @return the magical attack multiplier
	 */
	public static double getMidnightMAtkMultiplier()
	{
		return isMidnightActive() ? Config.MIDNIGHT_MONSTER_MATK_MULTIPLIER : 1.0;
	}
	
	/**
	 * Gets the monster physical defense multiplier for Midnight mode.
	 * @return the physical defense multiplier
	 */
	public static double getMidnightPDefMultiplier()
	{
		return isMidnightActive() ? Config.MIDNIGHT_MONSTER_PDEF_MULTIPLIER : 1.0;
	}
	
	/**
	 * Gets the monster magical defense multiplier for Midnight mode.
	 * @return the magical defense multiplier
	 */
	public static double getMidnightMDefMultiplier()
	{
		return isMidnightActive() ? Config.MIDNIGHT_MONSTER_MDEF_MULTIPLIER : 1.0;
	}
	
	/**
	 * Gets the experience multiplier for Midnight mode.
	 * @return the experience multiplier
	 */
	public static double getMidnightExpMultiplier()
	{
		return isMidnightActive() ? Config.MIDNIGHT_EXP_MULTIPLIER : 1.0;
	}
	
	/**
	 * Gets the drop chance multiplier for Midnight mode.
	 * @return the drop chance multiplier
	 */
	public static double getMidnightDropChanceMultiplier()
	{
		return isMidnightActive() ? Config.MIDNIGHT_DROP_CHANCE_MULTIPLIER : 1.0;
	}
	
	/**
	 * Gets the drop amount multiplier for Midnight mode.
	 * @return the drop amount multiplier
	 */
	public static double getMidnightDropAmountMultiplier()
	{
		return isMidnightActive() ? Config.MIDNIGHT_DROP_AMOUNT_MULTIPLIER : 1.0;
	}
	
	/**
	 * Gets the adena multiplier for Midnight mode.
	 * @return the adena multiplier
	 */
	public static double getMidnightAdenaMultiplier()
	{
		return isMidnightActive() ? Config.MIDNIGHT_ADENA_MULTIPLIER : 1.0;
	}
	
	/**
	 * Gets the singleton instance of MidnightManager.
	 * @return MidnightManager instance
	 */
	public static MidnightManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MidnightManager INSTANCE = new MidnightManager();
	}
}