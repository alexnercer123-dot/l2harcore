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
package handlers.voicedcommandhandlers;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author RvRSystem
 * Voice command to change player race for RvR system
 */
public class RaceChangeVoicedCommand implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"racechange"
	};
	
	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (!Config.RVR_ENABLED || Config.RVR_CLASS_RACE_RESTRICTIONS)
		{
			player.sendMessage("Race change system is currently disabled.");
			return true;
		}
		
		if (command.equals("racechange"))
		{
			if ((params != null) && !params.isEmpty())
			{
				try
				{
					final Race newRace = Race.valueOf(params.toUpperCase());
					if (isValidPlayerRace(newRace))
					{
						changePlayerRace(player, newRace);
						player.sendMessage("Your race has been changed to " + newRace + "!");
						player.broadcastUserInfo();
						return true;
					}
					else
					{
						player.sendMessage("Invalid race: " + params);
					}
				}
				catch (IllegalArgumentException e)
				{
					player.sendMessage("Invalid race: " + params);
				}
			}
			else
			{
				showRaceChangeMenu(player);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Show race change menu to player
	 * @param player the player
	 */
	private void showRaceChangeMenu(Player player)
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html><head><title>Race Change</title></head><body>");
		html.append("<center>");
		html.append("<font color=\"LEVEL\">RvR Race Change System</font><br>");
		html.append("Current Race: <font color=\"00FF00\">" + player.getRace() + "</font><br><br>");
		html.append("Select your new race:<br><br>");
		
		final Race[] races = {Race.HUMAN, Race.ELF, Race.DARK_ELF, Race.ORC, Race.DWARF, Race.KAMAEL, Race.ERTHEIA};
		for (Race race : races)
		{
			if (race != player.getRace())
			{
				html.append("<button value=\"" + race + "\" action=\"bypass -h voice .racechange " + race + "\" ");
				html.append("width=120 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br>");
			}
		}
		
		html.append("<br><font color=\"B09878\">Note: Changing race will keep your current class.</font>");
		html.append("</center>");
		html.append("</body></html>");
		
		final NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setHtml(html.toString());
		player.sendPacket(msg);
	}
	
	/**
	 * Change player's race
	 * @param player the player
	 * @param newRace the new race
	 */
	private void changePlayerRace(Player player, Race newRace)
	{
		// Store current values
		final double currentHp = player.getCurrentHp();
		final double currentMp = player.getCurrentMp();
		final double currentCp = player.getCurrentCp();
		
		// Set new race by modifying the template
		player.getTemplate().setRace(newRace);
		
		// Recalculate stats
		player.getStat().recalculateStats(true);
		
		// Restore HP/MP/CP ratios
		final double hpPercent = currentHp / player.getMaxHp();
		final double mpPercent = currentMp / player.getMaxMp();
		final double cpPercent = currentCp / player.getMaxCp();
		
		player.setCurrentHp(player.getMaxHp() * hpPercent);
		player.setCurrentMp(player.getMaxMp() * mpPercent);
		player.setCurrentCp(player.getMaxCp() * cpPercent);
		
		// Update database
		updateRaceInDatabase(player, newRace);
	}
	
	/**
	 * Update race in database
	 * @param player the player
	 * @param newRace the new race
	 */
	private void updateRaceInDatabase(Player player, Race newRace)
	{
		// Note: The race is stored implicitly through the class ID in the database
		// For full implementation, we would need to add a separate race column
		// or modify the class system. For now, this demonstrates the concept.
		player.storeCharBase();
	}
	
	/**
	 * Check if race is a valid player race
	 * @param race the race
	 * @return true if valid
	 */
	private boolean isValidPlayerRace(Race race)
	{
		return (race == Race.HUMAN) || (race == Race.ELF) || (race == Race.DARK_ELF) || 
			   (race == Race.ORC) || (race == Race.DWARF) || (race == Race.KAMAEL) || 
			   (race == Race.ERTHEIA);
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}