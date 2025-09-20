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

import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Home Community Board Manager
 * @author L2jMobius
 */
public class HomeBBSManager extends BaseBBSManager
{
	@Override
	public void parsecmd(String command, Player player)
	{
		if (command.equals("_bbshome"))
		{
			showHome(player);
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>The command: " + command + " is not implemented yet.</center><br><br></body></html>", player);
		}
	}
	
	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
		// No write functionality needed for Home
	}
	
	/**
	 * Shows the main home page
	 * @param player the player viewing the page
	 */
	private void showHome(Player player)
	{
		CommunityBoardHandler.separateAndSend("<html><body><br><br><center>Welcome to our Community Board.<br><br>This is currently under development.<br><br>Please come back soon!</center><br><br></body></html>", player);
	}
	
	public static HomeBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HomeBBSManager INSTANCE = new HomeBBSManager();
	}
}