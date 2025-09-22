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
package org.l2jmobius.gameserver.handler.voicedcommandhandlers;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.managers.RvRManager;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author YourName
 * Voice command to display current RvR points for player's race
 */
public class RaceVoicedCommand implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"race"
	};
	
	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (!Config.RVR_ENABLED)
		{
			player.sendMessage("RvR System is currently disabled.");
			return true;
		}
		
		if (command.equals("race"))
		{
			final int racePoints = RvRManager.getInstance().getPoints(player.getRace());
			final String raceName = player.getRace().toString();
			
			// Send message to player about their race points
			final String message = "Your race (" + raceName + ") currently has " + racePoints + " RvR points.";
			player.sendMessage(message);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}