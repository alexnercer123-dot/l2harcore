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
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.l2jmobius.gameserver.handler.admincommandhandlers;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Admin command handler for caravan route coordinate collection.
 * The //caravan command captures current player coordinates and logs them for caravan route creation.
 * @author L2Hardcore Team
 */
public class AdminCaravan implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminCaravan.class.getName());
	private static final String CARAVAN_LOG_FILE = "log/caravan_coordinates.log";
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_caravan"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_caravan"))
		{
			// Get current player coordinates
			final int x = activeChar.getX();
			final int y = activeChar.getY();
			final int z = activeChar.getZ();
			final int heading = activeChar.getHeading();
			
			// Format timestamp
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			final String timestamp = dateFormat.format(new Date());
			
			// Create log entry
			final String logEntry = String.format("[%s] Caravan Waypoint: X=\"%d\" Y=\"%d\" Z=\"%d\" heading=\"%d\" - Player: %s%n",
				timestamp, x, y, z, heading, activeChar.getName());
			
			// Create XML format entry  
			final String xmlEntry = String.format("<!-- Captured: %s by %s -->%n<point X=\"%d\" Y=\"%d\" Z=\"%d\" delay=\"5000\" run=\"true\" />%n",
				timestamp, activeChar.getName(), x, y, z);
			
			// Write to log file
			try (FileWriter writer = new FileWriter(CARAVAN_LOG_FILE, true))
			{
				writer.write(logEntry);
				writer.write(xmlEntry);
				writer.write(System.lineSeparator());
				writer.flush();
				
				// Notify admin
				activeChar.sendMessage("Caravan coordinates captured and logged:");
				activeChar.sendMessage("Location: " + x + ", " + y + ", " + z + " (heading: " + heading + ")");
				activeChar.sendMessage("Saved to: " + CARAVAN_LOG_FILE);
				
				LOGGER.info("Caravan coordinates captured by " + activeChar.getName() + ": " + x + ", " + y + ", " + z);
			}
			catch (IOException e)
			{
				activeChar.sendMessage("Error writing to caravan log file: " + e.getMessage());
				LOGGER.log(Level.WARNING, "Failed to write caravan coordinates to log file", e);
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}