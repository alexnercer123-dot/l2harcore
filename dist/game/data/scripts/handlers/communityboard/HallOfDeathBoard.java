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
package handlers.communityboard;

import java.text.SimpleDateFormat;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.HallOfDeathTable;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Hall of Death Community Board Handler
 * @author L2jMobius
 */
public class HallOfDeathBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbshallofdeath"
	};
	
	private static final String NAVIGATION_PATH = "data/html/CommunityBoard/Custom/navigation.html";
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		String returnHtml = null;
		String navigation = null;
		
		if (Config.CUSTOM_CB_ENABLED)
		{
			navigation = HtmCache.getInstance().getHtm(player, NAVIGATION_PATH);
		}
		
		if (command.equals("_bbshallofdeath"))
		{
			// Show main Hall of Death page
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/hallofdeath/main.html");
			
			// Get recent deaths for display
			final List<HallOfDeathTable.DeathRecord> recentDeaths = HallOfDeathTable.getLastDeaths(5);
			final StringBuilder recentDeathsHtml = new StringBuilder();
			
			for (HallOfDeathTable.DeathRecord death : recentDeaths)
			{
				final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				final String levelColor = death.getPlayerLevel() >= 70 ? "FF6666" : death.getPlayerLevel() >= 40 ? "FFFF66" : "FFFFFF";
				final String killerColor = death.getKillerType().contains("Boss") ? "FF0000" : "AAAAAA";
				
				recentDeathsHtml.append("<font color=\"").append(levelColor).append("\">")
					.append(death.getPlayerName()).append(" (Lv.").append(death.getPlayerLevel()).append(")")
					.append("</font> killed by <font color=\"").append(killerColor).append("\">")
					.append(death.getKillerName()).append("</font><br>");
			}
			
			if (returnHtml != null)
			{
				returnHtml = returnHtml.replace("%recent_deaths%", recentDeathsHtml.toString());
				returnHtml = returnHtml.replace("%total_deaths%", String.valueOf(HallOfDeathTable.getTotalDeathCount()));
			}
		}
		else if (command.startsWith("_bbshallofdeath;"))
		{
			final String subCommand = command.replace("_bbshallofdeath;", "");
			final String[] parts = subCommand.split(";");
			final String viewType = parts[0];
			int page = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
			
			if ("last".equals(viewType))
			{
				returnHtml = generateLastDeathsPage(page);
			}
			else if ("toplevel".equals(viewType))
			{
				returnHtml = generateTopLevelDeathsPage(page);
			}
			else if ("all".equals(viewType))
			{
				returnHtml = generateAllDeathsPage(page);
			}
		}
		
		// Replace navigation if custom CB is enabled
		if ((returnHtml != null) && (navigation != null))
		{
			returnHtml = returnHtml.replace("%navigation%", navigation);
		}
		
		if (returnHtml != null)
		{
			CommunityBoardHandler.separateAndSend(returnHtml, player);
		}
		
		return true;
	}
	
	private String generateLastDeathsPage(int page)
	{
		final int limit = 20;
		final List<HallOfDeathTable.DeathRecord> deaths = HallOfDeathTable.getLastDeaths(limit);
		final StringBuilder deathsHtml = new StringBuilder();
		
		deathsHtml.append("<table border=0 cellpadding=2 cellspacing=1 width=520>");
		deathsHtml.append("<tr><td align=center><font color=\"CDB67F\">Last Deaths</font></td></tr>");
		deathsHtml.append("<tr><td height=10></td></tr>");
		
		for (HallOfDeathTable.DeathRecord death : deaths)
		{
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			final String levelColor = death.getPlayerLevel() >= 70 ? "FF6666" : death.getPlayerLevel() >= 40 ? "FFFF66" : "FFFFFF";
			final String killerColor = death.getKillerType().contains("Boss") ? "FF0000" : "AAAAAA";
			
			deathsHtml.append("<tr><td><font color=\"").append(levelColor).append("\">")
				.append(death.getPlayerName()).append(" (Lv.").append(death.getPlayerLevel()).append(") ")
				.append(death.getPlayerClass()).append("</font> killed by <font color=\"").append(killerColor).append("\">")
				.append(death.getKillerName()).append(" (").append(death.getKillerType()).append(")")
				.append("</font> at ").append(dateFormat.format(death.getDeathTime()))
				.append("</td></tr>");
		}
		
		deathsHtml.append("</table>");
		
		return createPageWrapper("Last Deaths", deathsHtml.toString());
	}
	
	private String generateTopLevelDeathsPage(int page)
	{
		final int limit = 20;
		final List<HallOfDeathTable.DeathRecord> deaths = HallOfDeathTable.getTopLevelDeaths(limit);
		final StringBuilder deathsHtml = new StringBuilder();
		
		deathsHtml.append("<table border=0 cellpadding=2 cellspacing=1 width=520>");
		deathsHtml.append("<tr><td align=center><font color=\"CDB67F\">Top Level Deaths</font></td></tr>");
		deathsHtml.append("<tr><td height=10></td></tr>");
		
		for (HallOfDeathTable.DeathRecord death : deaths)
		{
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			final String levelColor = death.getPlayerLevel() >= 70 ? "FF6666" : death.getPlayerLevel() >= 40 ? "FFFF66" : "FFFFFF";
			final String killerColor = death.getKillerType().contains("Boss") ? "FF0000" : "AAAAAA";
			
			deathsHtml.append("<tr><td><font color=\"").append(levelColor).append("\">")
				.append(death.getPlayerName()).append(" (Lv.").append(death.getPlayerLevel()).append(") ")
				.append(death.getPlayerClass()).append("</font> killed by <font color=\"").append(killerColor).append("\">")
				.append(death.getKillerName()).append(" (").append(death.getKillerType()).append(")")
				.append("</font> at ").append(dateFormat.format(death.getDeathTime()))
				.append("</td></tr>");
		}
		
		deathsHtml.append("</table>");
		
		return createPageWrapper("Top Level Deaths", deathsHtml.toString());
	}
	
	private String generateAllDeathsPage(int page)
	{
		final int limit = 20;
		final int offset = (page - 1) * limit;
		final List<HallOfDeathTable.DeathRecord> deaths = HallOfDeathTable.getAllDeaths(limit, offset);
		final int totalDeaths = HallOfDeathTable.getTotalDeathCount();
		final int totalPages = (totalDeaths + limit - 1) / limit;
		final StringBuilder deathsHtml = new StringBuilder();
		
		deathsHtml.append("<table border=0 cellpadding=2 cellspacing=1 width=520>");
		deathsHtml.append("<tr><td align=center><font color=\"CDB67F\">All Deaths (Page ").append(page).append(" of ").append(totalPages).append(")</font></td></tr>");
		deathsHtml.append("<tr><td height=10></td></tr>");
		
		for (HallOfDeathTable.DeathRecord death : deaths)
		{
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			final String levelColor = death.getPlayerLevel() >= 70 ? "FF6666" : death.getPlayerLevel() >= 40 ? "FFFF66" : "FFFFFF";
			final String killerColor = death.getKillerType().contains("Boss") ? "FF0000" : "AAAAAA";
			
			deathsHtml.append("<tr><td><font color=\"").append(levelColor).append("\">")
				.append(death.getPlayerName()).append(" (Lv.").append(death.getPlayerLevel()).append(") ")
				.append(death.getPlayerClass()).append("</font> killed by <font color=\"").append(killerColor).append("\">")
				.append(death.getKillerName()).append(" (").append(death.getKillerType()).append(")")
				.append("</font> at ").append(dateFormat.format(death.getDeathTime()))
				.append("</td></tr>");
		}
		
		// Pagination controls
		if (totalPages > 1)
		{
			deathsHtml.append("<tr><td height=10></td></tr>");
			deathsHtml.append("<tr><td align=center>");
			
			if (page > 1)
			{
				deathsHtml.append("<button value=\"Previous\" action=\"bypass _bbshallofdeath;all;").append(page - 1).append("\" width=80 height=20>");
			}
			
			deathsHtml.append(" Page ").append(page).append(" of ").append(totalPages).append(" ");
			
			if (page < totalPages)
			{
				deathsHtml.append("<button value=\"Next\" action=\"bypass _bbshallofdeath;all;").append(page + 1).append("\" width=80 height=20>");
			}
			
			deathsHtml.append("</td></tr>");
		}
		
		deathsHtml.append("</table>");
		
		return createPageWrapper("All Deaths", deathsHtml.toString());
	}
	
	private String createPageWrapper(String title, String content)
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html noscrollbar><body>");
		html.append("<table width=700><tr><td height=10></td></tr></table>");
		html.append("<table width=20><tr><td>%navigation%</td><td><center>");
		html.append("<table border=0 cellpadding=0 cellspacing=0 width=555 height=30 background=\"L2UI_CT1.Windows_DF_TooltipBG\">");
		html.append("<tr><td align=center><font name=\"hs12\" color=\"CDB67F\">").append(title).append("</font></td></tr></table>");
		html.append("<table border=0 cellpadding=0 cellspacing=0 width=555><tr><td height=10></td></tr></table>");
		html.append("<table border=0 cellpadding=0 cellspacing=0 width=555 background=\"L2UI_CT1.Windows_DF_TooltipBG\">");
		html.append("<tr><td height=10></td></tr><tr><td align=center valign=top>");
		html.append(content);
		html.append("</td></tr><tr><td height=10></td></tr></table>");
		html.append("<table border=0 cellpadding=0 cellspacing=0 width=555><tr><td height=10></td></tr></table>");
		html.append("<table border=0 cellpadding=0 cellspacing=0 width=555 background=\"L2UI_CT1.Windows_DF_TooltipBG\">");
		html.append("<tr><td height=10></td></tr><tr><td align=center>");
		html.append("<button value=\"Back to Hall of Death\" action=\"bypass _bbshallofdeath\" width=160 height=25>");
		html.append("</td></tr><tr><td height=10></td></tr></table>");
		html.append("</center></td></tr></table></body></html>");
		return html.toString();
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}