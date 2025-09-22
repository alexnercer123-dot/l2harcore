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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IWriteBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;

/**
 * Simple and effective Color Change System for Community Board.
 * Allows players to preview colors for free and buy them with Adena.
 * Colors are stored in the database (characters table: name_color, title_color).
 * 
 * @author L2Hardcore
 */
public class ColorSystem implements IWriteBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(ColorSystem.class.getName());
	
	static {
		LOGGER.info("ColorSystem handler initialized - IWriteBoardHandler interface");
	}
	
	private static final String[] COMMANDS =
	{
		"_bbscolorsystem"
	};
	
	private static final int COLOR_PRICE = 1000000; // 1,000,000 Adena
	private static final int ADENA_ID = 57; // Adena item ID
	private static final int PREVIEW_DURATION = 10000; // 10 seconds in milliseconds
	
	@Override
	public String[] getCommandList()
	{
		LOGGER.info("ColorSystem getCommandList() called - returning: " + java.util.Arrays.toString(COMMANDS));
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		LOGGER.info("ColorSystem onCommand called with: " + command + " by player: " + player.getName());
		
		if (command.equals("_bbscolorsystem"))
		{
			LOGGER.info("ColorSystem: Showing color system page");
			showColorSystemPage(player);
			return true;
		}
		else if (command.startsWith("_bbscolorsystem;"))
		{
			// Handle bypass commands like _bbscolorsystem;preview_nick;FF0000
			String[] parts = command.split(";");
			LOGGER.info("ColorSystem: Processing bypass command parts: " + java.util.Arrays.toString(parts));
			
			if (parts.length >= 2)
			{
				String action = parts[1];
				String hexColor = parts.length > 2 ? parts[2] : null;
				
				LOGGER.info("ColorSystem: Action: " + action + ", HEX: " + hexColor);
				
				switch (action)
				{
					case "test_action":
						player.sendMessage("Test bypass action successful!");
						return true;
					case "preview_nick":
						if (hexColor != null && !hexColor.trim().isEmpty())
						{
							return previewNickColor(player, hexColor.trim());
						}
						else
						{
							player.sendMessage("Please enter a color code!");
							return false;
						}
					case "preview_title":
						if (hexColor != null && !hexColor.trim().isEmpty())
						{
							return previewTitleColor(player, hexColor.trim());
						}
						else
						{
							player.sendMessage("Please enter a color code!");
							return false;
						}
					case "buy_nick":
						if (hexColor != null && !hexColor.trim().isEmpty())
						{
							return buyNickColor(player, hexColor.trim());
						}
						else
						{
							player.sendMessage("Please enter a color code!");
							return false;
						}
					case "buy_title":
						if (hexColor != null && !hexColor.trim().isEmpty())
						{
							return buyTitleColor(player, hexColor.trim());
						}
						else
						{
							player.sendMessage("Please enter a color code!");
							return false;
						}
					default:
						LOGGER.warning("ColorSystem: Unknown action: " + action);
						player.sendMessage("Unknown action: " + action);
						return false;
				}
			}
			else
			{
				LOGGER.warning("ColorSystem: Invalid bypass command format: " + command);
				return false;
			}
		}
		
		LOGGER.warning("ColorSystem command not recognized: " + command);
		return false;
	}
	
	/**
	 * This method should be called when forms with Write commands are submitted
	 */
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		LOGGER.info("=== ColorSystem writeCommunityBoardCommand CALLED ===");
		LOGGER.info("Player: " + player.getName());
		LOGGER.info("arg1: " + arg1);
		LOGGER.info("arg2: " + arg2);
		LOGGER.info("arg3: " + arg3);
		LOGGER.info("arg4: " + arg4);
		LOGGER.info("arg5: " + arg5);
		
		if (arg1 == null)
		{
			LOGGER.warning("ColorSystem writeCommunityBoardCommand: arg1 is null");
			return false;
		}
		
		try
		{
			switch (arg1)
			{
				case "test_write":
					LOGGER.info("ColorSystem: Test write command received! arg2: " + arg2);
					player.sendMessage("Test write successful! Color received: " + arg2);
					return true;
				case "preview_nick":
					LOGGER.info("ColorSystem: Processing preview_nick with color: " + arg2);
					if (arg2 == null || arg2.trim().isEmpty())
					{
						player.sendMessage("Please enter a color code!");
						return false;
					}
					return previewNickColor(player, arg2.trim());
				case "preview_title":
					LOGGER.info("ColorSystem: Processing preview_title with color: " + arg2);
					if (arg2 == null || arg2.trim().isEmpty())
					{
						player.sendMessage("Please enter a color code!");
						return false;
					}
					return previewTitleColor(player, arg2.trim());
				case "buy_nick":
					LOGGER.info("ColorSystem: Processing buy_nick with color: " + arg2);
					if (arg2 == null || arg2.trim().isEmpty())
					{
						player.sendMessage("Please enter a color code!");
						return false;
					}
					return buyNickColor(player, arg2.trim());
				case "buy_title":
					LOGGER.info("ColorSystem: Processing buy_title with color: " + arg2);
					if (arg2 == null || arg2.trim().isEmpty())
					{
						player.sendMessage("Please enter a color code!");
						return false;
					}
					return buyTitleColor(player, arg2.trim());
				default:
					LOGGER.warning("ColorSystem: Unknown command: " + arg1);
					player.sendMessage("Unknown command: " + arg1);
					return false;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "ColorSystem ERROR - Player: " + player.getName() + ", Command: " + arg1 + ", Error: " + e.getMessage(), e);
			player.sendMessage("System error occurred. Please try again.");
			return false;
		}
	}
	
	/**
	 * Shows the main color system page
	 */
	private void showColorSystemPage(Player player)
	{
		LOGGER.info("ColorSystem showColorSystemPage called for player: " + player.getName());
		final String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/color_change/color_system.html");
		if (html != null)
		{
			LOGGER.info("HTML file found, sending to player");
			CommunityBoardHandler.separateAndSend(html, player);
		}
		else
		{
			LOGGER.warning("HTML file not found: data/html/CommunityBoard/Custom/color_change/color_system.html");
			player.sendMessage("Color system page not found.");
		}
	}
	
	/**
	 * Preview nickname color for free (10 seconds)
	 */
	private boolean previewNickColor(Player player, String hexColor)
	{
		LOGGER.info("ColorSystem previewNickColor - Player: " + player.getName() + ", HEX: " + hexColor);
		
		// Check for empty color
		if (hexColor == null || hexColor.trim().isEmpty() || hexColor.equals("$nick_color"))
		{
			LOGGER.warning("ColorSystem previewNickColor - Empty or invalid HEX color: " + hexColor);
			player.sendMessage("Please enter a valid HEX color code! Example: FF0000, 00FF00, 0000FF");
			return false;
		}
		
		// Clean the hex color
		hexColor = hexColor.trim().toUpperCase();
		if (hexColor.startsWith("#"))
		{
			hexColor = hexColor.substring(1);
		}
		
		if (!isValidHexColor(hexColor))
		{
			LOGGER.warning("ColorSystem previewNickColor - Invalid HEX color: " + hexColor);
			player.sendMessage("Invalid HEX color! Use 6-digit format: FF0000 (red), 00FF00 (green), 0000FF (blue)");
			return false;
		}
		
		try
		{
			final int colorValue = Integer.parseInt(hexColor, 16);
			final int originalColor = player.getAppearance().getNameColor();
			
			LOGGER.info("ColorSystem previewNickColor - Original color: " + originalColor + ", New color: " + colorValue);
			
			// Apply preview color
			player.getAppearance().setNameColor(colorValue);
			player.broadcastUserInfo();
			player.sendMessage("Previewing nickname color #" + hexColor + " for 10 seconds...");
			
			LOGGER.info("ColorSystem previewNickColor - Color applied, scheduling revert");
			
			// Schedule revert to original color
			ThreadPool.schedule(() -> {
				LOGGER.info("ColorSystem previewNickColor - Reverting color for player: " + player.getName());
				player.getAppearance().setNameColor(originalColor);
				player.broadcastUserInfo();
				player.sendMessage("Nickname color preview ended.");
			}, PREVIEW_DURATION);
			
			return true;
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("ColorSystem previewNickColor - NumberFormatException for HEX: " + hexColor + ", Error: " + e.getMessage());
			player.sendMessage("Invalid HEX color format!");
			return false;
		}
	}
	
	/**
	 * Preview title color for free (10 seconds)
	 */
	private boolean previewTitleColor(Player player, String hexColor)
	{
		LOGGER.info("ColorSystem previewTitleColor - Player: " + player.getName() + ", HEX: " + hexColor);
		
		// Check for empty color
		if (hexColor == null || hexColor.trim().isEmpty() || hexColor.equals("$title_color"))
		{
			LOGGER.warning("ColorSystem previewTitleColor - Empty or invalid HEX color: " + hexColor);
			player.sendMessage("Please enter a valid HEX color code! Example: FF0000, 00FF00, 0000FF");
			return false;
		}
		
		// Clean the hex color
		hexColor = hexColor.trim().toUpperCase();
		if (hexColor.startsWith("#"))
		{
			hexColor = hexColor.substring(1);
		}
		
		if (!isValidHexColor(hexColor))
		{
			player.sendMessage("Invalid HEX color! Use 6-digit format: FF0000 (red), 00FF00 (green), 0000FF (blue)");
			return false;
		}
		
		try
		{
			final int colorValue = Integer.parseInt(hexColor, 16);
			final int originalColor = player.getAppearance().getTitleColor();
			
			// Apply preview color
			player.getAppearance().setTitleColor(colorValue);
			player.broadcastUserInfo();
			player.sendMessage("Previewing title color #" + hexColor + " for 10 seconds...");
			
			// Schedule revert to original color
			ThreadPool.schedule(() -> {
				player.getAppearance().setTitleColor(originalColor);
				player.broadcastUserInfo();
				player.sendMessage("Title color preview ended.");
			}, PREVIEW_DURATION);
			
			return true;
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("Invalid HEX color format!");
			return false;
		}
	}
	
	/**
	 * Buy and permanently apply nickname color
	 */
	private boolean buyNickColor(Player player, String hexColor)
	{
		LOGGER.info("ColorSystem buyNickColor - Player: " + player.getName() + ", HEX: " + hexColor);
		
		// Check for empty color
		if (hexColor == null || hexColor.trim().isEmpty() || hexColor.equals("$nick_color"))
		{
			LOGGER.warning("ColorSystem buyNickColor - Empty or invalid HEX color: " + hexColor);
			player.sendMessage("Please enter a valid HEX color code! Example: FF0000, 00FF00, 0000FF");
			return false;
		}
		
		// Clean the hex color
		hexColor = hexColor.trim().toUpperCase();
		if (hexColor.startsWith("#"))
		{
			hexColor = hexColor.substring(1);
		}
		
		if (!isValidHexColor(hexColor))
		{
			LOGGER.warning("ColorSystem buyNickColor - Invalid HEX color: " + hexColor);
			player.sendMessage("Invalid HEX color! Use 6-digit format: FF0000 (red), 00FF00 (green), 0000FF (blue)");
			return false;
		}
		
		// Check if player has enough Adena
		long adenaCount = player.getInventory().getInventoryItemCount(ADENA_ID, -1);
		LOGGER.info("ColorSystem buyNickColor - Player has " + adenaCount + " Adena, requires " + COLOR_PRICE);
		
		if (adenaCount < COLOR_PRICE)
		{
			LOGGER.warning("ColorSystem buyNickColor - Insufficient Adena. Player has: " + adenaCount + ", needs: " + COLOR_PRICE);
			player.sendMessage("You need " + COLOR_PRICE + " Adena to buy this color!");
			return false;
		}
		
		try
		{
			final int colorValue = Integer.parseInt(hexColor, 16);
			LOGGER.info("ColorSystem buyNickColor - Parsed color value: " + colorValue);
			
			// Take Adena from player
			LOGGER.info("ColorSystem buyNickColor - Attempting to destroy " + COLOR_PRICE + " Adena");
			if (!player.destroyItemByItemId(ItemProcessType.FEE, ADENA_ID, COLOR_PRICE, player, true))
			{
				LOGGER.warning("ColorSystem buyNickColor - Failed to destroy Adena items");
				player.sendMessage("Failed to take payment. Transaction cancelled.");
				return false;
			}
			
			LOGGER.info("ColorSystem buyNickColor - Adena destroyed successfully, applying color");
			
			// Apply color permanently
			player.getAppearance().setNameColor(colorValue);
			player.broadcastUserInfo();
			
			// Save to database
			LOGGER.info("ColorSystem buyNickColor - Saving to database");
			if (saveNameColorToDatabase(player.getObjectId(), colorValue))
			{
				LOGGER.info("ColorSystem buyNickColor - Database save successful");
				player.sendMessage("Nickname color changed permanently to #" + hexColor.toUpperCase() + "!");
				player.sendMessage("Color has been saved to database.");
			}
			else
			{
				LOGGER.warning("ColorSystem buyNickColor - Database save failed");
				player.sendMessage("Warning: Color applied but database save failed. Contact administrator.");
			}
			
			// Refresh the page
			showColorSystemPage(player);
			return true;
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("ColorSystem buyNickColor - NumberFormatException for HEX: " + hexColor + ", Error: " + e.getMessage());
			player.sendMessage("Invalid HEX color format!");
			return false;
		}
	}
	
	/**
	 * Buy and permanently apply title color
	 */
	private boolean buyTitleColor(Player player, String hexColor)
	{
		LOGGER.info("ColorSystem buyTitleColor - Player: " + player.getName() + ", HEX: " + hexColor);
		
		// Check for empty color
		if (hexColor == null || hexColor.trim().isEmpty() || hexColor.equals("$title_color"))
		{
			LOGGER.warning("ColorSystem buyTitleColor - Empty or invalid HEX color: " + hexColor);
			player.sendMessage("Please enter a valid HEX color code! Example: FF0000, 00FF00, 0000FF");
			return false;
		}
		
		// Clean the hex color
		hexColor = hexColor.trim().toUpperCase();
		if (hexColor.startsWith("#"))
		{
			hexColor = hexColor.substring(1);
		}
		
		if (!isValidHexColor(hexColor))
		{
			player.sendMessage("Invalid HEX color! Use 6-digit format: FF0000 (red), 00FF00 (green), 0000FF (blue)");
			return false;
		}
		
		// Check if player has enough Adena
		if (player.getInventory().getInventoryItemCount(ADENA_ID, -1) < COLOR_PRICE)
		{
			player.sendMessage("You need " + COLOR_PRICE + " Adena to buy this color!");
			return false;
		}
		
		try
		{
			final int colorValue = Integer.parseInt(hexColor, 16);
			
			// Take Adena from player
			if (!player.destroyItemByItemId(ItemProcessType.FEE, ADENA_ID, COLOR_PRICE, player, true))
			{
				player.sendMessage("Failed to take payment. Transaction cancelled.");
				return false;
			}
			
			// Apply color permanently
			player.getAppearance().setTitleColor(colorValue);
			player.broadcastUserInfo();
			
			// Save to database
			if (saveTitleColorToDatabase(player.getObjectId(), colorValue))
			{
				player.sendMessage("Title color changed permanently to #" + hexColor.toUpperCase() + "!");
				player.sendMessage("Color has been saved to database.");
			}
			else
			{
				player.sendMessage("Warning: Color applied but database save failed. Contact administrator.");
			}
			
			// Refresh the page
			showColorSystemPage(player);
			return true;
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("Invalid HEX color format!");
			return false;
		}
	}
	
	/**
	 * Validate HEX color format (6 characters, valid hex digits)
	 */
	private boolean isValidHexColor(String hexColor)
	{
		if (hexColor == null || hexColor.trim().isEmpty())
		{
			LOGGER.warning("ColorSystem isValidHexColor: hexColor is null or empty");
			return false;
		}
		
		// Remove spaces and convert to uppercase
		hexColor = hexColor.trim().toUpperCase();
		LOGGER.info("ColorSystem isValidHexColor: Processing color: " + hexColor);
		
		// Remove # if present
		if (hexColor.startsWith("#"))
		{
			hexColor = hexColor.substring(1);
			LOGGER.info("ColorSystem isValidHexColor: Removed # symbol, new color: " + hexColor);
		}
		
		// Must be exactly 6 characters
		if (hexColor.length() != 6)
		{
			LOGGER.warning("ColorSystem isValidHexColor: Invalid length: " + hexColor.length() + ", expected 6");
			return false;
		}
		
		// Check if all characters are valid hex digits
		for (int i = 0; i < hexColor.length(); i++)
		{
			final char c = hexColor.charAt(i);
			if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')))
			{
				LOGGER.warning("ColorSystem isValidHexColor: Invalid character '" + c + "' at position " + i);
				return false;
			}
		}
		
		LOGGER.info("ColorSystem isValidHexColor: Valid color: " + hexColor);
		return true;
	}
	
	/**
	 * Save nickname color to database
	 */
	private boolean saveNameColorToDatabase(int charId, int colorValue)
	{
		LOGGER.info("ColorSystem saveNameColorToDatabase - CharId: " + charId + ", Color: " + colorValue);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET name_color = ? WHERE charId = ?"))
		{
			ps.setInt(1, colorValue);
			ps.setInt(2, charId);
			int rowsUpdated = ps.executeUpdate();
			LOGGER.info("ColorSystem saveNameColorToDatabase - Rows updated: " + rowsUpdated);
			return rowsUpdated > 0;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "ColorSystem saveNameColorToDatabase - Failed to save name color for charId: " + charId + ", Error: " + e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * Save title color to database
	 */
	private boolean saveTitleColorToDatabase(int charId, int colorValue)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET title_color = ? WHERE charId = ?"))
		{
			ps.setInt(1, colorValue);
			ps.setInt(2, charId);
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Failed to save title color to database for charId: " + charId, e);
			return false;
		}
	}
}