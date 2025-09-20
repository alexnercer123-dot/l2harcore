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
package org.l2jmobius.gameserver.handler.admincommandhandlers;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Admin command handler for time control commands.
 * Supports //settime [hour], //night, and //day commands.
 */
public class AdminTime implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_settime",
		"admin_night", 
		"admin_day"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_settime"))
		{
			try
			{
				final String[] tokens = command.split(" ");
				if (tokens.length != 2)
				{
					activeChar.sendSysMessage("Usage: //settime [hour] (0-23)");
					return false;
				}
				
				final int hour = Integer.parseInt(tokens[1]);
				if ((hour < 0) || (hour > 23))
				{
					activeChar.sendSysMessage("Hour must be between 0 and 23");
					return false;
				}
				
				GameTimeTaskManager.getInstance().setGameTime(hour);
				activeChar.sendSysMessage("Game time set to " + hour + ":00");
			}
			catch (NumberFormatException e)
			{
				activeChar.sendSysMessage("Invalid hour format. Use: //settime [hour] (0-23)");
				return false;
			}
		}
		else if (command.equals("admin_night"))
		{
			GameTimeTaskManager.getInstance().setGameTime(0); // Midnight
			activeChar.sendSysMessage("Game time set to midnight (night mode activated)");
		}
		else if (command.equals("admin_day"))
		{
			GameTimeTaskManager.getInstance().setGameTime(12); // Noon
			activeChar.sendSysMessage("Game time set to noon (day mode activated)");
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}