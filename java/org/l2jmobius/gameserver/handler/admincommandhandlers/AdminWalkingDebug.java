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

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.model.WalkInfo;
import org.l2jmobius.gameserver.model.WalkRoute;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.WorldObject;

/**
 * Debug admin command for walking system
 * @author Assistant
 */
public class AdminWalkingDebug implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminWalkingDebug.class.getName());
	private static final String WALKING_DEBUG_LOG = "log/walking_debug.log";
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_walkdebug"
	};
	
	/**
	 * Log debug information to both console and file
	 */
	private void logDebug(Player admin, String message)
	{
		// Send to admin
		admin.sendMessage(message);
		
		// Log to server console
		LOGGER.info("[WalkingDebug] " + admin.getName() + ": " + message);
		
		// Log to file
		try (FileWriter writer = new FileWriter(WALKING_DEBUG_LOG, true))
		{
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			final String timestamp = dateFormat.format(new Date());
			writer.write(String.format("[%s] %s: %s%n", timestamp, admin.getName(), message));
			writer.flush();
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, "Failed to write walking debug log", e);
		}
	}
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		if (actualCommand.equalsIgnoreCase("admin_walkdebug"))
		{
			final WorldObject target = activeChar.getTarget();
			if ((target == null) || !target.isNpc())
			{
				logDebug(activeChar, "Please target an NPC first.");
				return false;
			}
			
			final Npc npc = (Npc) target;
			final WalkingManager walkingManager = WalkingManager.getInstance();
			
			// Debug information
			logDebug(activeChar, "=== Walking Debug Information ===");
			logDebug(activeChar, "NPC ID: " + npc.getId());
			logDebug(activeChar, "NPC Name: " + npc.getName());
			logDebug(activeChar, "NPC Position: " + npc.getX() + ", " + npc.getY() + ", " + npc.getZ());
			
			if (npc.getSpawn() != null)
			{
				logDebug(activeChar, "Spawn Position: " + npc.getSpawn().getX() + ", " + npc.getSpawn().getY() + ", " + npc.getSpawn().getZ());
			}
			else
			{
				logDebug(activeChar, "Spawn: null");
			}
			
			logDebug(activeChar, "Is Targeted: " + walkingManager.isTargeted(npc));
			logDebug(activeChar, "Is On Walk: " + walkingManager.isOnWalk(npc));
			logDebug(activeChar, "Current Route: " + walkingManager.getRouteName(npc));
			
			// Get WalkInfo details
			if (walkingManager.isOnWalk(npc))
			{
				final WalkInfo walkInfo = WalkingManager.getInstance().getWalkInfo(npc);
				if (walkInfo != null)
				{
					logDebug(activeChar, "Current Node Index: " + walkInfo.getCurrentNodeId());
					logDebug(activeChar, "Is Blocked: " + walkInfo.isBlocked());
					logDebug(activeChar, "Is Suspended: " + walkInfo.isSuspended());
					logDebug(activeChar, "Is Stopped By Attack: " + walkInfo.isStoppedByAttack());
					logDebug(activeChar, "Last Action Time: " + walkInfo.getLastAction());
				}
			}
			
			// Check if caravan route exists
			final WalkRoute caravanRoute = walkingManager.getRoute("dion_floran_caravan");
			if (caravanRoute != null)
			{
				logDebug(activeChar, "Caravan route found with " + caravanRoute.getNodesCount() + " nodes");
				logDebug(activeChar, "Route repeat: " + caravanRoute.repeatWalk());
				logDebug(activeChar, "Route repeat type: " + caravanRoute.getRepeatType());
			}
			else
			{
				logDebug(activeChar, "Caravan route NOT found!");
			}
			
			// Parse command arguments properly
			String action = null;
			if (st.hasMoreTokens())
			{
				action = st.nextToken().toLowerCase();
			}
			
			// Try to manually start walking
			if ("start".equals(action))
			{
				logDebug(activeChar, "Attempting to start walking...");
				walkingManager.startMoving(npc, "dion_floran_caravan");
				logDebug(activeChar, "Start walking command sent.");
			}
			// Force continue to next waypoint
			else if ("continue".equals(action))
			{
				logDebug(activeChar, "Attempting to force continue to next waypoint...");
				walkingManager.onArrived(npc);
				logDebug(activeChar, "Force continue command sent.");
			}
			// Reset walking system for this NPC
			else if ("reset".equals(action))
			{
				logDebug(activeChar, "Attempting to reset walking system...");
				walkingManager.cancelMoving(npc);
				try { Thread.sleep(1000); } catch (InterruptedException e) { /* ignore */ }
				walkingManager.onSpawn(npc);
				logDebug(activeChar, "Reset walking system completed.");
			}
			// Force next waypoint - manually advance node index
			else if ("forcenext".equals(action))
			{
				logDebug(activeChar, "Attempting to force next waypoint...");
				final WalkInfo walkInfo = walkingManager.getWalkInfo(npc);
				if (walkInfo != null)
				{
					logDebug(activeChar, "Current node before force: " + walkInfo.getCurrentNodeId());
					// Manually calculate next node
					walkInfo.calculateNextNode(npc);
					logDebug(activeChar, "Current node after force: " + walkInfo.getCurrentNodeId());
					// Force movement to next waypoint
					walkingManager.startMoving(npc, "dion_floran_caravan");
					logDebug(activeChar, "Force next waypoint completed.");
				}
				else
				{
					logDebug(activeChar, "No WalkInfo found for NPC!");
				}
			}
			// Reload walking manager routes
			else if ("reload".equals(action))
			{
				logDebug(activeChar, "Reloading WalkingManager routes...");
				WalkingManager.getInstance().load();
				logDebug(activeChar, "WalkingManager routes reloaded.");
			}
			// Force unblock walking system
			else if ("unblock".equals(action))
			{
				logDebug(activeChar, "Attempting to force unblock walking system...");
				final WalkInfo walkInfo = walkingManager.getWalkInfo(npc);
				if (walkInfo != null)
				{
					logDebug(activeChar, "Blocked before: " + walkInfo.isBlocked());
					walkInfo.setBlocked(false);
					walkInfo.setSuspended(false);
					walkInfo.setStoppedByAttack(false);
					logDebug(activeChar, "Blocked after: " + walkInfo.isBlocked());
					// Try to start movement immediately
					walkingManager.startMoving(npc, "dion_floran_caravan");
					logDebug(activeChar, "Force unblock completed.");
				}
				else
				{
					logDebug(activeChar, "No WalkInfo found for NPC!");
				}
			}
			// Simulate ArrivedTask execution
			else if ("arrivedtask".equals(action))
			{
				logDebug(activeChar, "Simulating ArrivedTask execution...");
				final WalkInfo walkInfo = walkingManager.getWalkInfo(npc);
				if (walkInfo != null)
				{
					// Simulate what ArrivedTask.run() does
					npc.broadcastInfo();
					walkInfo.setBlocked(false);
					walkingManager.startMoving(npc, walkInfo.getRoute().getName());
					logDebug(activeChar, "ArrivedTask simulation completed.");
				}
				else
				{
					logDebug(activeChar, "No WalkInfo found for NPC!");
				}
			}
			// Check ArrivedTask status
			else if ("taskstatus".equals(action))
			{
				logDebug(activeChar, "Checking ArrivedTask status...");
				// We need to access the _arriveTasks map from WalkingManager
				// This would require adding a public method to WalkingManager
				logDebug(activeChar, "ArrivedTask status check completed (implementation needed).");
			}
			// Clear stuck ArrivedTask
			else if ("cleartask".equals(action))
			{
				logDebug(activeChar, "Clearing stuck ArrivedTask...");
				// Force cancel the NPC completely, then restart
				walkingManager.cancelMoving(npc);
				try { Thread.sleep(500); } catch (InterruptedException e) { /* ignore */ }
				walkingManager.onSpawn(npc);
				logDebug(activeChar, "ArrivedTask cleared and walking restarted.");
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