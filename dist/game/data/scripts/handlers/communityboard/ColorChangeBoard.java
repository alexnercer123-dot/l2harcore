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
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.communityboard;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.handler.IWriteBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.l2jmobius.commons.threads.ThreadPool;

/**
 * Color Change board.
 * @author Qoder
 */
public class ColorChangeBoard implements IWriteBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbscolor"
	};
	
	private static final int COLOR_CHANGE_PRICE = 1000000; // 1,000,000 Adena
	private static final int COLOR_CHANGE_ITEM = 57; // Adena
	
	// Preset colors
	private static final int[] COLORS =
	{
		0x9393FF, // Pink
		0x7C49FC, // Rose Pink
		0x97F8FC, // Lemon Yellow
		0xFA9AEE, // Lilac
		0xFF5D93, // Cobalt Violet
		0x00FCA0, // Mint Green
		0xA0A601, // Peacock Green
		0x7898AF, // Yellow Ochre
		0x486295, // Chocolate
		0x999999, // Silver
		0x000000, // Black
		0xFF0000, // Red
		0x00FF00, // Green
		0x0000FF, // Blue
		0xFFFF00, // Yellow
		0xFF00FF, // Magenta
		0x00FFFF, // Cyan
		0xFFA500, // Orange
		0xFFC0CB, // Light Pink
		0x800080, // Purple
	};
	
	// Additional preset colors
	private static final int[] ADDITIONAL_COLORS =
	{
		0xFF69B4, // Hot Pink
		0xDA70D6, // Orchid
		0xEE82EE, // Violet
		0xD2691E, // Chocolate
		0xFF7F50, // Coral
		0xDC143C, // Crimson
		0x008B8B, // Dark Cyan
		0x006400, // Dark Green
		0x483D8B, // Dark Slate Blue
		0x2F4F4F, // Dark Slate Gray
		0x7CFC00, // Lawn Green
		0xFFD700, // Gold
		0x9ACD32, // Yellow Green
		0x8A2BE2, // Blue Violet
		0x5F9EA0, // Cadet Blue
		0x7FFF00, // Chartreuse
		0xD2691E, // Cocoa Brown
		0x6495ED, // Cornflower Blue
		0xDC143C, // Red Crimson
		0x00FFFF, // Cyan/Aqua
	};
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		if (command.startsWith("_bbscolor;"))
		{
			final String[] params = command.split(";");
			if (params.length < 2)
			{
				return false;
			}
			
			final String action = params[1];
			
			// Handle preset colors
			if (action.equals("nick_preset") || action.equals("title_preset"))
			{
				if (params.length < 3)
				{
					return false;
				}
				
				final int colorIndex = Integer.parseInt(params[2]);
				
				// Validate color index
				if ((colorIndex < 0) || (colorIndex >= COLORS.length))
				{
					player.sendMessage("Invalid color selection.");
					return false;
				}
				
				// Check if player has enough adena
				if (player.getInventory().getInventoryItemCount(COLOR_CHANGE_ITEM, -1) < COLOR_CHANGE_PRICE)
				{
					player.sendMessage("You don't have enough Adena for this service.");
					return false;
				}
				
				// Deduct the price
				if (!player.destroyItemByItemId(ItemProcessType.FEE, COLOR_CHANGE_ITEM, COLOR_CHANGE_PRICE, player, true))
				{
					player.sendMessage("Failed to deduct the service fee.");
					return false;
				}
				
				// Apply the color
				if (action.equals("nick_preset"))
				{
					// Change the nickname color
					player.getAppearance().setNameColor(COLORS[colorIndex]);
					player.broadcastUserInfo();
					player.sendMessage("Your nickname color has been changed!");
				}
				else if (action.equals("title_preset"))
				{
					// Change the title color
					player.getAppearance().setTitleColor(COLORS[colorIndex]);
					player.broadcastUserInfo();
					player.sendMessage("Your title color has been changed!");
				}
				
				// Refresh the page to show the change
				String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/color_change/main.html");
				if (html != null)
				{
					CommunityBoardHandler.separateAndSend(html, player);
				}
				return true;
			}
			// Handle additional preset colors
			else if (action.equals("nick_preset_add") || action.equals("title_preset_add"))
			{
				if (params.length < 3)
				{
					return false;
				}
				
				final int colorIndex = Integer.parseInt(params[2]);
				
				// Validate color index
				if ((colorIndex < 0) || (colorIndex >= ADDITIONAL_COLORS.length))
				{
					player.sendMessage("Invalid color selection.");
					return false;
				}
				
				// Check if player has enough adena
				if (player.getInventory().getInventoryItemCount(COLOR_CHANGE_ITEM, -1) < COLOR_CHANGE_PRICE)
				{
					player.sendMessage("You don't have enough Adena for this service.");
					return false;
				}
				
				// Deduct the price
				if (!player.destroyItemByItemId(ItemProcessType.FEE, COLOR_CHANGE_ITEM, COLOR_CHANGE_PRICE, player, true))
				{
					player.sendMessage("Failed to deduct the service fee.");
					return false;
				}
				
				// Apply the color
				if (action.equals("nick_preset_add"))
				{
					// Change the nickname color
					player.getAppearance().setNameColor(ADDITIONAL_COLORS[colorIndex]);
					player.broadcastUserInfo();
					player.sendMessage("Your nickname color has been changed!");
				}
				else if (action.equals("title_preset_add"))
				{
					// Change the title color
					player.getAppearance().setTitleColor(ADDITIONAL_COLORS[colorIndex]);
					player.broadcastUserInfo();
					player.sendMessage("Your title color has been changed!");
				}
				
				// Refresh the page to show the change
				String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/color_change/more_colors.html");
				if (html != null)
				{
					CommunityBoardHandler.separateAndSend(html, player);
				}
				return true;
			}
			// Handle custom hex colors - these will be handled in parsewrite
			else if (action.equals("nick_preview") || action.equals("title_preview") || action.equals("nick_apply") || action.equals("title_apply"))
			{
				// These actions will be handled in writeCommunityBoardCommand method with form variables
				return false;
			}
			else if (action.equals("more_colors"))
			{
				// Show additional colors page
				String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/color_change/more_colors.html");
				if (html != null)
				{
					CommunityBoardHandler.separateAndSend(html, player);
					return true;
				}
			}
		}
		else if (command.equals("_bbscolor") || command.startsWith("_bbscolor "))
		{
			// Show the color selection page
			String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/color_change/main.html");
			if (html != null)
			{
				CommunityBoardHandler.separateAndSend(html, player);
				return true;
			}
		}
	
		return false;
	}
	
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		// Handle form variable actions
		if (arg1 != null && (arg1.equals("nick_preview") || arg1.equals("title_preview") || arg1.equals("nick_apply") || arg1.equals("title_apply")))
		{
			// For preview/apply actions, arg2 contains the hex color value
			String hexColor = arg2;
			
			// Debug message to see what hex color is received
			player.sendMessage("Received hex color: " + hexColor);
			
			// Validate hex color format
			if (!isValidHexColor(hexColor))
			{
				player.sendMessage("Invalid HEX color format. Please use format: RRGGBB (e.g., FF0000 for red)");
				player.sendMessage("Received: " + hexColor + " Length: " + (hexColor != null ? hexColor.length() : "null"));
				return false;
			}
			
			// Clean the hex color (remove # if present)
			String cleanHexColor = hexColor.trim();
			if (cleanHexColor.startsWith("#") && cleanHexColor.length() == 7)
			{
				cleanHexColor = cleanHexColor.substring(1);
			}
			
			final int color = Integer.parseInt(cleanHexColor, 16);
			
			// For apply actions, check price and deduct adena
			if (arg1.equals("nick_apply") || arg1.equals("title_apply"))
			{
				// Check if player has enough adena
				if (player.getInventory().getInventoryItemCount(COLOR_CHANGE_ITEM, -1) < COLOR_CHANGE_PRICE)
				{
					player.sendMessage("You don't have enough Adena for this service.");
					return false;
				}
				
				// Deduct the price
				if (!player.destroyItemByItemId(ItemProcessType.FEE, COLOR_CHANGE_ITEM, COLOR_CHANGE_PRICE, player, true))
				{
					player.sendMessage("Failed to deduct the service fee.");
					return false;
				}
			}
			
			// Handle the action
			if (arg1.equals("nick_preview"))
			{
				// Preview nickname color for 10 seconds
				player.sendMessage("Previewing nickname color: " + cleanHexColor);
				previewNameColor(player, color);
				player.sendMessage("Nickname color preview for 10 seconds!");
			}
			else if (arg1.equals("title_preview"))
			{
				// Preview title color for 10 seconds
				player.sendMessage("Previewing title color: " + cleanHexColor);
				previewTitleColor(player, color);
				player.sendMessage("Title color preview for 10 seconds!");
			}
			else if (arg1.equals("nick_apply"))
			{
				// Apply nickname color permanently
				player.sendMessage("Applying nickname color: " + cleanHexColor);
				player.getAppearance().setNameColor(color);
				player.broadcastUserInfo();
				player.sendMessage("Your nickname color has been changed permanently!");
			}
			else if (arg1.equals("title_apply"))
			{
				// Apply title color permanently
				player.sendMessage("Applying title color: " + cleanHexColor);
				player.getAppearance().setTitleColor(color);
				player.broadcastUserInfo();
				player.sendMessage("Your title color has been changed permanently!");
			}
			
			// Refresh the page to show the change
			String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/color_change/main.html");
			if (html != null)
			{
				CommunityBoardHandler.separateAndSend(html, player);
			}
			return true;
		}
		
		return false;
	}
	
	/**
	 * Preview nickname color for 10 seconds then revert to original
	 */
	private void previewNameColor(Player player, int newColor)
	{
		final int originalColor = player.getAppearance().getNameColor();
		
		// Set new color
		player.getAppearance().setNameColor(newColor);
		player.broadcastUserInfo();
		
		// Schedule revert after 10 seconds
		ThreadPool.schedule(() -> {
			player.getAppearance().setNameColor(originalColor);
			player.broadcastUserInfo();
			player.sendMessage("Nickname color preview ended.");
		}, 10000); // 10 seconds
	}
	
	/**
	 * Preview title color for 10 seconds then revert to original
	 */
	private void previewTitleColor(Player player, int newColor)
	{
		final int originalColor = player.getAppearance().getTitleColor();
		
		// Set new color
		player.getAppearance().setTitleColor(newColor);
		player.broadcastUserInfo();
		
		// Schedule revert after 10 seconds
		ThreadPool.schedule(() -> {
			player.getAppearance().setTitleColor(originalColor);
			player.broadcastUserInfo();
			player.sendMessage("Title color preview ended.");
		}, 10000); // 10 seconds
	}
	
	/**
	 * Validate HEX color format (6 characters, valid hex digits)
	 */
	private boolean isValidHexColor(String hexColor)
	{
		// Handle null or empty
		if (hexColor == null || hexColor.isEmpty())
		{
			return false;
		}
		
		// Trim whitespace
		hexColor = hexColor.trim();
		
		// Remove # if present at the beginning
		if (hexColor.startsWith("#") && hexColor.length() == 7)
		{
			hexColor = hexColor.substring(1);
		}
		
		// Must be exactly 6 characters now
		if (hexColor.length() != 6)
		{
			return false;
		}
		
		// Check if all characters are valid hex digits
		for (int i = 0; i < hexColor.length(); i++)
		{
			char c = hexColor.charAt(i);
			if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')))
			{
				return false;
			}
		}
		
		return true;
	}
}