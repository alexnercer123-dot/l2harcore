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
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Color Change board.
 * @author Qoder
 */
public class ColorChangeBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbscolor"
	};
	
	private static final int COLOR_CHANGE_PRICE = 1000000; // 1,000,000 Adena
	private static final int COLOR_CHANGE_ITEM = 57; // Adena
	
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
			if (params.length < 3)
			{
				return false;
			}
			
			final String type = params[1]; // "nick" or "title"
			final int colorIndex = Integer.parseInt(params[2]);
			
			// Validate color index
			final int[] COLORS =
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
			};
			
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
			
			// For nickname color, we change it directly
			if (type.equals("nick"))
			{
				// Change the nickname color
				player.getAppearance().setNameColor(COLORS[colorIndex]);
				player.broadcastUserInfo();
				player.sendMessage("Your nickname color has been changed!");
				return true;
			}
			// For title color, we change it directly
			else if (type.equals("title"))
			{
				// Change the title color
				player.getAppearance().setTitleColor(COLORS[colorIndex]);
				player.broadcastUserInfo();
				player.sendMessage("Your title color has been changed!");
				return true;
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
}