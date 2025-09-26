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
package ai.bosses;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

import ai.AbstractNpcAI;

/**
 * Caravan Boss AI - Defensive raid boss with custom drop system
 * @author L2J Mobius
 */
public class CaravanBoss extends AbstractNpcAI
{
	// NPC ID
	private static final int CARAVAN_BOSS = 900009;
	
	// Mercy pleas - limited to once per 10 seconds
	private long _lastMercyTime = 0;
	private static final long MERCY_COOLDOWN = 10000; // 10 seconds
	
	// Damage tracking for reward system
	private final Map<Integer, Integer> _playerDamage = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _partyDamage = new ConcurrentHashMap<>();
	
	// Mercy messages
	private static final String[] MERCY_MESSAGES = {
		"Please, have mercy! I'm just a simple merchant!",
		"I beg you, spare my life! I have a family!",
		"Stop! I surrender! Take what you want but don't kill me!",
		"Have pity on an old trader! I mean no harm!",
		"Please, I'll give you anything you want! Just don't hurt me!"
	};
	
	// Drop items for all participants
	private static final ItemHolder[] ALL_PARTICIPANT_REWARDS = {
		new ItemHolder(29008, 2), // XP/SP Scroll x2
		new ItemHolder(29011, 3), // 1st Class Buff Scroll x3
		new ItemHolder(1419, 1)   // Proof of Blood x1
	};
	
	// Drop items for killer
	private static final ItemHolder[] KILLER_REWARDS = {
		new ItemHolder(29009, 5),  // XP/SP Scroll - Normal x5
		new ItemHolder(29011, 10)  // 1st Class Buff Scroll x10
	};
	
	// Bonus item for killer (10% chance)
	private static final ItemHolder KILLER_BONUS = new ItemHolder(1419, 4); // Proof of Blood x4
	
	// Expensive NG grade equipment for top damage group (10% chance each)
	private static final ItemHolder[] NG_GRADE_DROPS = {
		// Weapons
		new ItemHolder(2, 1),   // Long Sword
		new ItemHolder(9, 1),   // Cedar Staff  
		new ItemHolder(73, 1),  // Falchion
		new ItemHolder(149, 1), // Apprentice's Spear
		new ItemHolder(174, 1), // Bow of Peril
		// Armor
		new ItemHolder(350, 1), // Chain Mail Shirt
		new ItemHolder(394, 1), // Reinforced Leather Shirt
		new ItemHolder(425, 1), // Apprentice's Tunic
		new ItemHolder(472, 1), // Leather Pants
		new ItemHolder(500, 1), // Great Helmet
		// Jewelry
		new ItemHolder(881, 1), // Ring of Knowledge
		new ItemHolder(890, 1), // Necklace of Knowledge
		new ItemHolder(906, 1)  // Earring of Knowledge
	};
	
	public CaravanBoss()
	{
		addAttackId(CARAVAN_BOSS);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		// Track damage for reward system
		final int attackerId = attacker.getObjectId();
		_playerDamage.merge(attackerId, damage, Integer::sum);
		
		// Track party damage if player is in party
		final Party party = attacker.getParty();
		if (party != null)
		{
			final int partyId = party.getLeaderObjectId();
			_partyDamage.merge(partyId, damage, Integer::sum);
		}
		
		// Plea for mercy (max once per 10 seconds)
		final long currentTime = System.currentTimeMillis();
		if ((currentTime - _lastMercyTime) > MERCY_COOLDOWN)
		{
			_lastMercyTime = currentTime;
			final String mercyMessage = MERCY_MESSAGES[ThreadLocalRandom.current().nextInt(MERCY_MESSAGES.length)];
			npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, npc.getName(), mercyMessage));
		}
		
		// DO NOT call super.onAttack to prevent the NPC from fighting back
		return null;
	}
	
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.NPC)
	public void onCreatureDeath(OnCreatureDeath event)
	{
		final Npc npc = (Npc) event.getTarget();
		if (npc.getId() != CARAVAN_BOSS)
		{
			return;
		}
		
		final Player killer = event.getAttacker() instanceof Player ? (Player) event.getAttacker() : null;
		if (killer == null)
		{
			return;
		}
		
		// Distribute rewards
		distributeRewards(npc, killer);
		
		// Clear damage tracking
		_playerDamage.clear();
		_partyDamage.clear();
	}
	
	private void distributeRewards(Npc npc, Player killer)
	{
		final Location npcLoc = npc.getLocation();
		
		// 1. Reward all participants who dealt damage and are nearby
		for (Map.Entry<Integer, Integer> entry : _playerDamage.entrySet())
		{
			final Player player = World.getInstance().getPlayer(entry.getKey());
			if ((player != null) && (player.calculateDistance3D(npcLoc) <= 1500)) // Within reasonable range
			{
				// Give base rewards to all participants
				for (ItemHolder reward : ALL_PARTICIPANT_REWARDS)
				{
					addItem(player, reward.getId(), reward.getCount());
				}
				
				player.sendMessage("You have been rewarded for participating in the caravan raid!");
			}
		}
		
		// 2. Reward the killer
		if (killer != null)
		{
			// Give killer rewards
			for (ItemHolder reward : KILLER_REWARDS)
			{
				addItem(killer, reward.getId(), reward.getCount());
			}
			
			// 10% chance for bonus item
			if (ThreadLocalRandom.current().nextInt(100) < 10)
			{
				addItem(killer, KILLER_BONUS.getId(), KILLER_BONUS.getCount());
				killer.sendMessage("You received a bonus reward for delivering the final blow!");
			}
			
			killer.sendMessage("You have been rewarded as the one who defeated the caravan boss!");
		}
		
		// 3. Reward the party that dealt the most damage
		if (!_partyDamage.isEmpty())
		{
			int maxDamage = 0;
			int topPartyId = 0;
			
			for (Map.Entry<Integer, Integer> entry : _partyDamage.entrySet())
			{
				if (entry.getValue() > maxDamage)
				{
					maxDamage = entry.getValue();
					topPartyId = entry.getKey();
				}
			}
			
			// Find the top damage party leader and distribute NG grade items to party
			final Player partyLeader = World.getInstance().getPlayer(topPartyId);
			if ((partyLeader != null) && (partyLeader.getParty() != null))
			{
				final Party topParty = partyLeader.getParty();
				
				// Each NG grade item has 10% chance to drop
				for (ItemHolder ngItem : NG_GRADE_DROPS)
				{
					if (ThreadLocalRandom.current().nextInt(100) < 10) // 10% chance
					{
						// Give to a random party member who is online and nearby
						final Player randomMember = getRandomNearbyPartyMember(topParty, npcLoc);
						if (randomMember != null)
						{
							addItem(randomMember, ngItem.getId(), ngItem.getCount());
							randomMember.sendMessage("Your party dealt the most damage! You received an expensive NG grade item!");
						}
					}
				}
			}
		}
	}
	
	private Player getRandomNearbyPartyMember(Party party, Location npcLoc)
	{
		final java.util.List<Player> nearbyMembers = new java.util.ArrayList<>();
		
		for (Player member : party.getMembers())
		{
			if ((member != null) && member.isOnline() && (member.calculateDistance3D(npcLoc) <= 1500))
			{
				nearbyMembers.add(member);
			}
		}
		
		if (!nearbyMembers.isEmpty())
		{
			return nearbyMembers.get(ThreadLocalRandom.current().nextInt(nearbyMembers.size()));
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new CaravanBoss();
	}
}