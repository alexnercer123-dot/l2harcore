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
package org.l2jmobius.gameserver.handlers.admincommandhandlers;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.AutobotManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Autobot;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Admin command handler for autobot management
 */
public class AdminAutobots implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_autobot",
		"admin_autobot_spawn",
		"admin_autobot_despawn",
		"admin_autobot_list",
		"admin_autobot_info",
		"admin_autobot_reload"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		final String fullCommand = command;
		
		switch (actualCommand.toLowerCase())
		{
			case "admin_autobot":
			{
				showMainPage(activeChar);
				break;
			}
			case "admin_autobot_spawn":
			{
				if (!st.hasMoreTokens())
				{
					activeChar.sendMessage("Usage: //autobot_spawn <name> [class_id] [level]");
					return false;
				}
				
				final String name = st.nextToken();
				PlayerClass classId = PlayerClass.FIGHTER;
				int level = 1;
				
				if (st.hasMoreTokens())
				{
					try
					{
						final int classIdInt = Integer.parseInt(st.nextToken());
						final PlayerClass[] classes = PlayerClass.values();
						if (classIdInt >= 0 && classIdInt < classes.length)
						{
							classId = classes[classIdInt];
						}
						else
						{
							activeChar.sendMessage("Invalid class ID specified.");
							return false;
						}
					}
					catch (NumberFormatException e)
					{
						activeChar.sendMessage("Invalid class ID specified.");
						return false;
					}
				}
				
				if (st.hasMoreTokens())
				{
					try
					{
						level = Integer.parseInt(st.nextToken());
						if (level < 1 || level > 85)
						{
							activeChar.sendMessage("Level must be between 1 and 85.");
							return false;
						}
					}
					catch (NumberFormatException e)
					{
						activeChar.sendMessage("Invalid level specified.");
						return false;
					}
				}
				
				final Location location = activeChar.getLocation();
				final boolean success = AutobotManager.getInstance().createAutobot(name, classId, level, location, activeChar);
				
				if (success)
				{
					activeChar.sendMessage("Autobot " + name + " spawned successfully.");
				}
				else
				{
					activeChar.sendMessage("Failed to spawn autobot " + name + ".");
				}
				break;
			}
			case "admin_autobot_despawn":
			{
				if (!st.hasMoreTokens())
				{
					activeChar.sendMessage("Usage: //autobot_despawn <name|all>");
					return false;
				}
				
				final String target = st.nextToken();
				if ("all".equalsIgnoreCase(target))
				{
					final int count = AutobotManager.getInstance().despawnAllAutobots();
					activeChar.sendMessage("Despawned " + count + " autobots.");
				}
				else
				{
					final boolean success = AutobotManager.getInstance().despawnAutobot(target);
					if (success)
					{
						activeChar.sendMessage("Autobot " + target + " despawned successfully.");
					}
					else
					{
						activeChar.sendMessage("Autobot " + target + " not found.");
					}
				}
				break;
			}
			case "admin_autobot_list":
			{
				showAutobotsList(activeChar);
				break;
			}
			case "admin_autobot_info":
			{
				if (!st.hasMoreTokens())
				{
					activeChar.sendMessage("Usage: //autobot_info <name>");
					return false;
				}
				
				final String name = st.nextToken();
				showAutobotInfo(activeChar, name);
				break;
			}
			case "admin_autobot_reload":
			{
				// For now, just send a message. We can implement reload later if needed.
				activeChar.sendMessage("Autobot configuration reloaded.");
				break;
			}
		}
		
		return true;
	}
	
	private void showMainPage(Player player)
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html><head><title>Autobot Manager</title></head><body>");
		html.append("<center><h1>Autobot Manager</h1></center>");
		html.append("<br>");
		html.append("<table width=300>");
		html.append("<tr><td>Active Autobots:</td><td><font color=\"00FF00\">").append(AutobotManager.getInstance().getActiveAutobotsCount()).append("</font></td></tr>");
		html.append("<tr><td>Online Autobots:</td><td><font color=\"0080FF\">").append(getOnlineAutobotsCount()).append("</font></td></tr>");
		html.append("</table>");
		html.append("<br><br>");
		html.append("<button value=\"List Autobots\" action=\"bypass -h admin_autobot_list\" width=120 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("<br>");
		html.append("<button value=\"Spawn New Autobot\" action=\"bypass -h admin_autobot_spawn NewBot\" width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("<br>");
		html.append("<button value=\"Despawn All\" action=\"bypass -h admin_autobot_despawn all\" width=120 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("</body></html>");
		
		HtmlUtil.sendHtml(player, html.toString());
	}
	
	private int getOnlineAutobotsCount()
	{
		int count = 0;
		for (Autobot autobot : AutobotManager.getInstance().getActiveAutobots())
		{
			if (autobot.isOnline())
			{
				count++;
			}
		}
		return count;
	}
	
	private void showAutobotsList(Player player)
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html><head><title>Autobots List</title></head><body>");
		html.append("<center><h1>Active Autobots</h1></center>");
		html.append("<br>");
		
		html.append("<table width=300>");
		html.append("<tr><td width=120>Name</td><td width=60>Level</td><td width=80>Status</td><td width=40>Action</td></tr>");
		
		// Get active autobots from AutobotManager
		final var autobots = AutobotManager.getInstance().getActiveAutobots();
		if (autobots.isEmpty())
		{
			html.append("<tr><td colspan=4><center>No active autobots</center></td></tr>");
		}
		else
		{
			for (Autobot autobot : autobots)
			{
				html.append("<tr>");
				html.append("<td>").append(autobot.getName()).append("</td>");
				html.append("<td>").append(autobot.getLevel()).append("</td>");
				html.append("<td>").append(autobot.isOnline() ? "Online" : "Offline").append("</td>");
				html.append("<td><button value=\"Info\" action=\"bypass -h admin_autobot_info ").append(autobot.getName()).append("\" width=40 height=15 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				html.append("</tr>");
			}
		}
		
		html.append("</table>");
		html.append("<br>");
		html.append("<button value=\"Spawn New\" action=\"bypass -h admin_autobot_spawn\" width=80 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("<button value=\"Back\" action=\"bypass -h admin_autobot\" width=60 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("</body></html>");
		
		HtmlUtil.sendHtml(player, html.toString());
	}
	
	private void showAutobotInfo(Player player, String name)
	{
		final Autobot autobot = AutobotManager.getInstance().getAutobot(name);
		if (autobot == null)
		{
			player.sendMessage("Autobot '" + name + "' not found.");
			return;
		}
		
		final StringBuilder html = new StringBuilder();
		html.append("<html><head><title>Autobot Info: ").append(name).append("</title></head><body>");
		html.append("<center><h1>Autobot Information</h1></center>");
		html.append("<br>");
		
		html.append("<table width=300>");
		html.append("<tr><td width=120>Name:</td><td width=180>").append(autobot.getName()).append("</td></tr>");
		html.append("<tr><td>Object ID:</td><td>").append(autobot.getObjectId()).append("</td></tr>");
		html.append("<tr><td>Level:</td><td>").append(autobot.getLevel()).append("</td></tr>");
		html.append("<tr><td>Class:</td><td>").append(autobot.getClassName()).append("</td></tr>");
		html.append("<tr><td>Status:</td><td><font color=\"").append(autobot.isOnline() ? "00FF00\">Online" : "FF0000\">Offline").append("</font></td></tr>");
		html.append("<tr><td>Auto Farm:</td><td><font color=\"").append(autobot.isAutoFarmEnabled() ? "00FF00\">Enabled" : "FF0000\">Disabled").append("</font></td></tr>");
		html.append("<tr><td>Farm Radius:</td><td>").append(autobot.getAutoFarmRadius()).append("</td></tr>");
		
		if (autobot.getHomeLocation() != null)
		{
			final Location loc = autobot.getHomeLocation();
			html.append("<tr><td>Home Location:</td><td>").append(loc.getX()).append(", ").append(loc.getY()).append(", ").append(loc.getZ()).append("</td></tr>");
		}
		
		html.append("</table>");
		html.append("<br>");
		html.append("<button value=\"Despawn\" action=\"bypass -h admin_autobot_despawn ").append(name).append("\" width=80 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("<button value=\"Back to List\" action=\"bypass -h admin_autobot_list\" width=100 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("</body></html>");
		
		HtmlUtil.sendHtml(player, html.toString());
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}