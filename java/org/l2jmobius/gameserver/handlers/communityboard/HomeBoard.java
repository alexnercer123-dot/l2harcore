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
package org.l2jmobius.gameserver.handlers.communityboard;

import org.l2jmobius.gameserver.communitybbs.Manager.HomeBBSManager;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Home Community Board Handler
 * @author L2jMobius
 */
public class HomeBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbshome"
	};
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		HomeBBSManager.getInstance().parsecmd(command, player);
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}