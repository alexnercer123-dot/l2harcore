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
package org.l2jmobius.gameserver.model.actor.stat;

import org.l2jmobius.gameserver.managers.MidnightManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.stats.Stat;

public class NpcStat extends CreatureStat
{
	public NpcStat(Npc activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public byte getLevel()
	{
		return getActiveChar().getTemplate().getLevel();
	}
	
	@Override
	public Npc getActiveChar()
	{
		return super.getActiveChar().asNpc();
	}
	
	@Override
	public int getPhysicalAttackAngle()
	{
		return getActiveChar().getTemplate().getBaseAttackAngle();
	}
	
	/**
	 * Override getValue to apply Midnight mod multipliers dynamically while excluding minions.
	 * This ensures that Midnight multipliers work with existing NPC stat multipliers but
	 * don't affect raid boss minions or regular minions.
	 * @param stat the stat to get value for
	 * @return the stat value with potential Midnight multipliers applied
	 */
	@Override
	public double getValue(Stat stat)
	{
		double value = super.getValue(stat);
		
		// Apply Midnight multipliers only for regular monsters when Midnight is active
		// Exclude raid bosses, minions, and raid minions from Midnight effects
		if (MidnightManager.isMidnightActive() && getActiveChar().isMonster() && !getActiveChar().isRaid() && !getActiveChar().isMinion() && !getActiveChar().isRaidMinion())
		{
			switch (stat)
			{
				case MAX_HP:
					value *= MidnightManager.getMidnightHpMultiplier();
					break;
				case PHYSICAL_ATTACK:
					value *= MidnightManager.getMidnightPAtkMultiplier();
					break;
				case MAGIC_ATTACK:
					value *= MidnightManager.getMidnightMAtkMultiplier();
					break;
				case PHYSICAL_DEFENCE:
					value *= MidnightManager.getMidnightPDefMultiplier();
					break;
				case MAGICAL_DEFENCE:
					value *= MidnightManager.getMidnightMDefMultiplier();
					break;
				default:
					// Other stats are not affected by Midnight mod
					break;
			}
		}
		
		return value;
	}
}
