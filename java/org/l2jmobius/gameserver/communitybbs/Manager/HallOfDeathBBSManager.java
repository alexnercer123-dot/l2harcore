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
package org.l2jmobius.gameserver.communitybbs.Manager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.l2jmobius.gameserver.data.sql.HallOfDeathTable;
import org.l2jmobius.gameserver.data.sql.HallOfDeathTable.DeathRecord;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Hall of Death Community Board Manager
 * @author L2jMobius
 */
public class HallOfDeathBBSManager extends BaseBBSManager
{
	private static final int DEATHS_PER_PAGE = 20;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm");
	
	@Override
	public void parsecmd(String command, Player player)
	{
		if (command.equals("_bbshallofdeath"))
		{
			showHallOfDeath(player, "last", 1);
		}
		else if (command.startsWith("_bbshallofdeath;"))
		{
			final String[] params = command.split(";");
			if (params.length >= 3)
			{
				final String category = params[1];
				final int page = Integer.parseInt(params[2]);
				showHallOfDeath(player, category, page);
			}
			else
			{
				showHallOfDeath(player, "last", 1);
			}
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>The command: " + command + " is not implemented yet.</center><br><br></body></html>", player);
		}
	}
	
	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
		// No write functionality needed for Hall of Death
	}
	
	/**
	 * Shows the Hall of Death page
	 * @param player the player viewing the page
	 * @param category the category to display (last, toplevel, all)
	 * @param page the page number
	 */
	private void showHallOfDeath(Player player, String category, int page)
	{
		final StringBuilder html = new StringBuilder(4000);
		
		// Header
		html.append("<html><body>");
		html.append("<br><br>");
		html.append("<table border=0 width=610>");
		html.append("<tr><td width=10></td>");
		html.append("<td width=600 align=left>");
		html.append("<a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;");
		html.append("<font color=\"LEVEL\">Hall of Death</font>");
		html.append("</td></tr>");
		html.append("</table>");
		html.append("<img src=\"L2UI.squareblank\" width=\"1\" height=\"10\">");
		
		// Title
		html.append("<center>");
		html.append("<table border=0 cellspacing=0 cellpadding=2 bgcolor=888888 width=610>");
		html.append("<tr>");
		html.append("<td FIXWIDTH=610 align=center>");
		html.append("<font color=\"FFFFFF\"><font size=3><b>HALL OF DEATH</b></font></font>");
		html.append("</td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("<br>");
		
		// Category buttons
		html.append("<table border=0 cellspacing=5 cellpadding=0 width=610>");
		html.append("<tr>");
		html.append("<td align=center>");
		
		// Last Deaths button
		if ("last".equals(category))
		{
			html.append("<button value=\"Last Deaths\" action=\"bypass _bbshallofdeath;last;1\" back=\"l2ui_ch3.smallbutton2_over\" fore=\"l2ui_ch3.smallbutton2\" width=120 height=20>");
		}
		else
		{
			html.append("<button value=\"Last Deaths\" action=\"bypass _bbshallofdeath;last;1\" back=\"l2ui_ch3.smallbutton2_down\" fore=\"l2ui_ch3.smallbutton2\" width=120 height=20>");
		}
		
		html.append("</td><td align=center>");
		
		// Top Level button
		if ("toplevel".equals(category))
		{
			html.append("<button value=\"Top Level\" action=\"bypass _bbshallofdeath;toplevel;1\" back=\"l2ui_ch3.smallbutton2_over\" fore=\"l2ui_ch3.smallbutton2\" width=120 height=20>");
		}
		else
		{
			html.append("<button value=\"Top Level\" action=\"bypass _bbshallofdeath;toplevel;1\" back=\"l2ui_ch3.smallbutton2_down\" fore=\"l2ui_ch3.smallbutton2\" width=120 height=20>");
		}
		
		html.append("</td><td align=center>");
		
		// All Deaths button
		if ("all".equals(category))
		{
			html.append("<button value=\"All Deaths\" action=\"bypass _bbshallofdeath;all;1\" back=\"l2ui_ch3.smallbutton2_over\" fore=\"l2ui_ch3.smallbutton2\" width=120 height=20>");
		}
		else
		{
			html.append("<button value=\"All Deaths\" action=\"bypass _bbshallofdeath;all;1\" back=\"l2ui_ch3.smallbutton2_down\" fore=\"l2ui_ch3.smallbutton2\" width=120 height=20>");
		}
		
		html.append("</td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("<br>");
		
		// Death records table header
		html.append("<table border=0 cellspacing=0 cellpadding=2 bgcolor=888888 width=610>");
		html.append("<tr>");
		html.append("<td FIXWIDTH=150 align=center><font color=\"FFFFFF\">Player</font></td>");
		html.append("<td FIXWIDTH=60 align=center><font color=\"FFFFFF\">Level</font></td>");
		html.append("<td FIXWIDTH=100 align=center><font color=\"FFFFFF\">Class</font></td>");
		html.append("<td FIXWIDTH=150 align=center><font color=\"FFFFFF\">Killed By</font></td>");
		html.append("<td FIXWIDTH=150 align=center><font color=\"FFFFFF\">Date</font></td>");
		html.append("</tr>");
		html.append("</table>");
		
		// Get death records based on category
		List<DeathRecord> deaths;
		int totalDeaths = 0;
		
		switch (category)
		{
			case "last":
				deaths = HallOfDeathTable.getLastDeaths(DEATHS_PER_PAGE);
				break;
			case "toplevel":
				deaths = HallOfDeathTable.getTopLevelDeaths(DEATHS_PER_PAGE);
				break;
			case "all":
				totalDeaths = HallOfDeathTable.getTotalDeathCount();
				final int offset = (page - 1) * DEATHS_PER_PAGE;
				deaths = HallOfDeathTable.getAllDeaths(DEATHS_PER_PAGE, offset);
				break;
			default:
				deaths = HallOfDeathTable.getLastDeaths(DEATHS_PER_PAGE);
				break;
		}
		
		// Death records
		if (deaths.isEmpty())
		{
			html.append("<table border=0 cellspacing=0 cellpadding=5 width=610>");
			html.append("<tr>");
			html.append("<td align=center>");
			html.append("<font color=\"999999\">No deaths recorded yet.</font>");
			html.append("</td>");
			html.append("</tr>");
			html.append("</table>");
		}
		else
		{
			for (DeathRecord death : deaths)
			{
				html.append("<table border=0 cellspacing=0 cellpadding=5 width=610>");
				html.append("<tr>");
				
				// Player name with color based on level
				String playerColor = getPlayerLevelColor(death.getPlayerLevel());
				html.append("<td FIXWIDTH=150 align=center>");
				html.append("<font color=\"").append(playerColor).append("\">").append(death.getPlayerName()).append("</font>");
				html.append("</td>");
				
				// Level
				html.append("<td FIXWIDTH=60 align=center>").append(death.getPlayerLevel()).append("</td>");
				
				// Class
				html.append("<td FIXWIDTH=100 align=center>");
				html.append("<font size=1>").append(death.getPlayerClass()).append("</font>");
				html.append("</td>");
				
				// Killer with color based on type
				String killerColor = getKillerTypeColor(death.getKillerType());
				html.append("<td FIXWIDTH=150 align=center>");
				html.append("<font color=\"").append(killerColor).append("\">").append(death.getKillerName()).append("</font>");
				html.append("</td>");
				
				// Date
				html.append("<td FIXWIDTH=150 align=center>");
				html.append("<font size=1>").append(DATE_FORMAT.format(new Date(death.getDeathTime()))).append("</font>");
				html.append("</td>");
				
				html.append("</tr>");
				html.append("</table>");
				html.append("<img src=\"L2UI.Squaregray\" width=\"610\" height=\"1\">");
			}
		}
		
		// Pagination for "all" category
		if ("all".equals(category) && totalDeaths > DEATHS_PER_PAGE)
		{
			html.append("<br>");
			html.append("<table border=0 cellspacing=0 cellpadding=0 width=610>");
			html.append("<tr>");
			html.append("<td align=center>");
			
			final int totalPages = (totalDeaths + DEATHS_PER_PAGE - 1) / DEATHS_PER_PAGE;
			
			// Previous button
			if (page > 1)
			{
				html.append("<button value=\"Previous\" action=\"bypass _bbshallofdeath;all;").append(page - 1).append("\" back=\"l2ui_ch3.smallbutton2_down\" fore=\"l2ui_ch3.smallbutton2\" width=80 height=20>");
			}
			else
			{
				html.append("<button value=\"Previous\" action=\"\" back=\"l2ui_ch3.smallbutton2_down\" fore=\"l2ui_ch3.smallbutton2\" width=80 height=20>");
			}
			
			html.append("&nbsp;&nbsp;");
			html.append("Page ").append(page).append(" of ").append(totalPages);
			html.append("&nbsp;&nbsp;");
			
			// Next button
			if (page < totalPages)
			{
				html.append("<button value=\"Next\" action=\"bypass _bbshallofdeath;all;").append(page + 1).append("\" back=\"l2ui_ch3.smallbutton2_down\" fore=\"l2ui_ch3.smallbutton2\" width=80 height=20>");
			}
			else
			{
				html.append("<button value=\"Next\" action=\"\" back=\"l2ui_ch3.smallbutton2_down\" fore=\"l2ui_ch3.smallbutton2\" width=80 height=20>");
			}
			
			html.append("</td>");
			html.append("</tr>");
			html.append("</table>");
		}
		
		html.append("<br><br>");
		html.append("</center>");
		html.append("</body></html>");
		
		CommunityBoardHandler.separateAndSend(html.toString(), player);
	}
	
	/**
	 * Gets color for player name based on level
	 * @param level player level
	 * @return color code
	 */
	private String getPlayerLevelColor(int level)
	{
		if (level >= 80)
		{
			return "FF6B00"; // Orange for very high level
		}
		else if (level >= 70)
		{
			return "FFFF00"; // Yellow for high level
		}
		else if (level >= 50)
		{
			return "00FF00"; // Green for mid level
		}
		else if (level >= 30)
		{
			return "FFFFFF"; // White for low-mid level
		}
		else
		{
			return "999999"; // Gray for low level
		}
	}
	
	/**
	 * Gets color for killer name based on type
	 * @param killerType type of killer
	 * @return color code
	 */
	private String getKillerTypeColor(String killerType)
	{
		switch (killerType)
		{
			case "Grand Boss":
				return "FF0000"; // Red for Grand Bosses
			case "Raid Boss":
				return "FF6600"; // Orange for Raid Bosses
			case "Monster":
				return "CCCCCC"; // Light gray for regular monsters
			case "NPC":
				return "00CCFF"; // Light blue for NPCs
			default:
				return "FFFFFF"; // White for unknown
		}
	}
	
	public static HallOfDeathBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HallOfDeathBBSManager INSTANCE = new HallOfDeathBBSManager();
	}
}