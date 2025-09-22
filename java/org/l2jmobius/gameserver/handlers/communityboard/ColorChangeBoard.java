/*
 * Copyright (C) 2004-2025 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.handlers.communityboard;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IWriteBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.commons.threads.ThreadPool;

/**
 * Color Change Board handler for Community Board.
 * Handles hex color validation and character name/title color changes.
 * 
 * @author L2Hardcore
 */
public class ColorChangeBoard implements IWriteBoardHandler
{
	// Hex color pattern for validation (6 digits, case insensitive)
	private static final int COLOR_CHANGE_PRICE = 1000000; // 1,000,000 Adena
	private static final int COLOR_CHANGE_ITEM = 57; // Adena
	
	// Commands handled by this parser
	private static final String[] COMMANDS =
	{
		"_bbscolor"
	};
	
	// Preset colors
	private static final int[] COLORS =
	{
		0xFF69B4, // Pink
		0xFF1493, // Rose Pink
		0xFFFF00, // Lemon Yellow
		0xDDA0DD, // Lilac
		0x9932CC, // Cobalt Violet
		0x98FB98, // Mint Green
		0x00CED1, // Peacock Green
		0xDAA520, // Yellow Ochre
		0xD2691E, // Chocolate
		0xC0C0C0, // Silver
		0x000000, // Black
		0xFF0000  // Red
	};
	
	// Additional preset colors
	private static final int[] ADDITIONAL_COLORS =
	{
		0xFF1493, // Hot Pink
		0xDA70D6, // Orchid
		0x8A2BE2, // Violet
		0xFF7F50, // Coral
		0xDC143C, // Crimson
		0x008B8B, // Dark Cyan
		0x006400, // Dark Green
		0x2F4F4F, // Dark Slate
		0x7CFC00, // Lawn Green
		0xFFD700, // Gold
		0x9ACD32, // Yellow Green
		0x8A2BE2, // Blue Violet
		0x5F9EA0, // Cadet Blue
		0x7FFF00, // Chartreuse
		0xD2691E, // Cocoa
		0x6495ED, // Cornflower
		0xDC143C, // Red Crimson
		0x00FFFF  // Cyan
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