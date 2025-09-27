# L2J Mobius Fake Player System - Complete Integration Guide

## Overview
This guide provides a complete step-by-step integration of the Fake Player system into any L2J Mobius server build. The system creates AI-controlled fake players that behave like real players with smooth movement, intelligent combat, and proper targeting.

## ✅ Features
- **Smooth Movement**: No jerky movement, bots move naturally
- **Intelligent Combat**: Fast combat response (0.8s), mages use Wind Strike only
- **Smart Targeting**: Only target Gremlins, max 3 players per mob, 1000 search radius
- **Sequential Spawning**: Bots spawn one by one with 2-second delays
- **Player Protection**: Never attack players, only monsters
- **Death Management**: Permanent death with no respawning
- **Spawn Limit**: Up to 500 bots per group
- **Level Progression**: Auto-level from 1 to 6 by fighting appropriate monsters

## 📁 Required Files

### 1. Core AI Files
- `java/org/l2jmobius/gameserver/ai/AutobotAI.java` - Main AI logic
- `java/org/l2jmobius/gameserver/ai/CombatAI.java` - Combat system
- `java/org/l2jmobius/gameserver/ai/AdvancedAI.java` - Advanced coordination
- `java/org/l2jmobius/gameserver/ai/DynamicPathfinder.java` - Movement pathfinding

### 2. Management Files
- `java/org/l2jmobius/gameserver/managers/AutobotManager.java` - Main manager
- `java/org/l2jmobius/gameserver/managers/FakePlayerSpawnManager.java` - Spawn management
- `java/org/l2jmobius/gameserver/managers/FakePlayerChatManager.java` - Chat system

### 3. Model Files
- `java/org/l2jmobius/gameserver/model/actor/Autobot.java` - Autobot class
- `java/org/l2jmobius/gameserver/data/holders/FakePlayerChatHolder.java` - Chat data

### 4. Task Management
- `java/org/l2jmobius/gameserver/taskmanagers/AutobotTaskManager.java` - AI processing

### 5. Admin Commands
- `java/org/l2jmobius/gameserver/handlers/admincommandhandlers/AdminAutobots.java` - Admin interface

### 6. Database
- `sql/game/autobots.sql` - Database structure

## 🚀 Installation Steps

### Step 1: Create Database Tables
Execute this SQL in your game database:

```sql
CREATE TABLE IF NOT EXISTS `autobots` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `account_name` varchar(45) NOT NULL DEFAULT 'AutobotSystem',
  `name` varchar(35) NOT NULL,
  `class_id` int(11) NOT NULL DEFAULT 0,
  `level` int(11) NOT NULL DEFAULT 1,
  `exp` bigint(20) NOT NULL DEFAULT 0,
  `sp` bigint(20) NOT NULL DEFAULT 0,
  `karma` int(11) NOT NULL DEFAULT 0,
  `fame` int(11) NOT NULL DEFAULT 0,
  `pk_kills` int(11) NOT NULL DEFAULT 0,
  `pvp_kills` int(11) NOT NULL DEFAULT 0,
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `race` int(11) NOT NULL DEFAULT 0,
  `sex` int(11) NOT NULL DEFAULT 0,
  `face` int(11) NOT NULL DEFAULT 0,
  `hair_style` int(11) NOT NULL DEFAULT 0,
  `hair_color` int(11) NOT NULL DEFAULT 0,
  `heading` int(11) NOT NULL DEFAULT 0,
  `x` int(11) NOT NULL DEFAULT -71424,
  `y` int(11) NOT NULL DEFAULT 258336,
  `z` int(11) NOT NULL DEFAULT -3109,
  `hp` decimal(8,0) NOT NULL DEFAULT 1,
  `mp` decimal(8,0) NOT NULL DEFAULT 1,
  `cp` decimal(8,0) NOT NULL DEFAULT 0,
  `hp_regen` decimal(8,4) NOT NULL DEFAULT 0,
  `mp_regen` decimal(8,4) NOT NULL DEFAULT 0,
  `cp_regen` decimal(8,4) NOT NULL DEFAULT 0,
  `max_hp` decimal(8,0) NOT NULL DEFAULT 1,
  `max_mp` decimal(8,0) NOT NULL DEFAULT 1,
  `max_cp` decimal(8,0) NOT NULL DEFAULT 1,
  `access_level` int(11) NOT NULL DEFAULT 0,
  `online_status` int(11) NOT NULL DEFAULT 0,
  `char_slot` int(11) NOT NULL DEFAULT 0,
  `newbie` int(11) NOT NULL DEFAULT 1,
  `last_access` bigint(20) NOT NULL DEFAULT 0,
  `clan_privs` int(11) NOT NULL DEFAULT 0,
  `want_peace` int(11) NOT NULL DEFAULT 0,
  `base_class` int(11) NOT NULL DEFAULT 0,
  `nobless` int(11) NOT NULL DEFAULT 0,
  `power_grade` int(11) NOT NULL DEFAULT 0,
  `hero` int(11) NOT NULL DEFAULT 0,
  `subpledge` int(11) NOT NULL DEFAULT 0,
  `lvl_joined_academy` int(11) NOT NULL DEFAULT 0,
  `apprentice` int(11) NOT NULL DEFAULT 0,
  `sponsor` int(11) NOT NULL DEFAULT 0,
  `clan_join_expiry_time` bigint(20) NOT NULL DEFAULT 0,
  `clan_create_expiry_time` bigint(20) NOT NULL DEFAULT 0,
  `death_penalty_level` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
```

### Step 2: Modify Config.java
Add these lines to your `Config.java` file in the appropriate sections:

```java
// Around line 3534 in Custom - Fake Players section
public static boolean FAKE_PLAYERS_ENABLED;
public static boolean FAKE_PLAYER_CHAT;
public static boolean FAKE_PLAYER_USE_SHOTS;
public static boolean FAKE_PLAYER_KILL_PVP;
public static boolean FAKE_PLAYER_KILL_KARMA;
public static boolean FAKE_PLAYER_AUTO_ATTACKABLE;
public static boolean FAKE_PLAYER_AGGRO_MONSTERS;
public static boolean FAKE_PLAYER_AGGRO_PLAYERS;
public static boolean FAKE_PLAYER_AGGRO_FPC;
public static boolean FAKE_PLAYER_CAN_DROP_ITEMS;
public static boolean FAKE_PLAYER_CAN_PICKUP;

// In the config loading section around line 3540
final ConfigReader fakePlayerConfig = new ConfigReader(CUSTOM_FAKE_PLAYERS_CONFIG_FILE);
FAKE_PLAYERS_ENABLED = fakePlayerConfig.getBoolean("EnableFakePlayers", true);
FAKE_PLAYER_CHAT = fakePlayerConfig.getBoolean("FakePlayerChat", false);
FAKE_PLAYER_USE_SHOTS = fakePlayerConfig.getBoolean("FakePlayerUseShots", false);
FAKE_PLAYER_KILL_PVP = fakePlayerConfig.getBoolean("FakePlayerKillsRewardPvP", false);
FAKE_PLAYER_KILL_KARMA = fakePlayerConfig.getBoolean("FakePlayerUnflaggedKillsKarma", false);
FAKE_PLAYER_AUTO_ATTACKABLE = fakePlayerConfig.getBoolean("FakePlayerAutoAttackable", false);
FAKE_PLAYER_AGGRO_MONSTERS = fakePlayerConfig.getBoolean("FakePlayerAggroMonsters", true);
FAKE_PLAYER_AGGRO_PLAYERS = fakePlayerConfig.getBoolean("FakePlayerAggroPlayers", false);
FAKE_PLAYER_AGGRO_FPC = fakePlayerConfig.getBoolean("FakePlayerAggroFPC", false);
FAKE_PLAYER_CAN_DROP_ITEMS = fakePlayerConfig.getBoolean("FakePlayerCanDropItems", false);
FAKE_PLAYER_CAN_PICKUP = fakePlayerConfig.getBoolean("FakePlayerCanPickup", false);

// Also add the config file constant at the top
public static final String CUSTOM_FAKE_PLAYERS_CONFIG_FILE = "./config/custom/FakePlayers.properties";
```

### Step 3: Create Configuration File
Create `config/custom/FakePlayers.properties`:

```properties
# Enable fake players system
EnableFakePlayers = true

# Fake player behavior settings
FakePlayerChat = false
FakePlayerUseShots = false
FakePlayerKillsRewardPvP = false
FakePlayerUnflaggedKillsKarma = false
FakePlayerAutoAttackable = false
FakePlayerAggroMonsters = true
FakePlayerAggroPlayers = false
FakePlayerAggroFPC = false
FakePlayerCanDropItems = false
FakePlayerCanPickup = false
```

### Step 4: Add All Java Files
Copy all the Java files from the required files list above into your project structure. Maintain the exact package structure.

### Step 5: Register Admin Commands
Add to your `AdminCommandHandlers.xml` file:

```xml
<admincommand name="admin_autobot" handler="AdminAutobots" />
<admincommand name="admin_spawn_fake" handler="AdminAutobots" />
<admincommand name="admin_despawn_fake" handler="AdminAutobots" />
<admincommand name="admin_fake_info" handler="AdminAutobots" />
<admincommand name="admin_fake_groups" handler="AdminAutobots" />
<admincommand name="admin_fake_help" handler="AdminAutobots" />
```

### Step 6: Initialize Managers
Add these initializations to your `GameServer.java` in the appropriate sections:

```java
// In the managers initialization section
print("AutobotManager", AutobotManager.getInstance());
print("FakePlayerSpawnManager", FakePlayerSpawnManager.getInstance());
print("FakePlayerChatManager", FakePlayerChatManager.getInstance());
```

## 🎮 Usage Commands

### Admin Commands
```
//spawn_fake <group_id> <count>  - Spawn fake players (max 500)
//despawn_fake <group_id|all>    - Despawn fake players
//fake_info                      - Show active spawn information
//fake_groups                    - List available spawn groups
//fake_help                      - Show help information
```

### Available Groups
1. Human Fighter (Talking Island)
2. Human Mage (Talking Island) 
3. Elf Fighter (Elven Village)
4. Elf Mage (Elven Village)
5. Dark Elf Fighter (Dark Elf Village)
6. Dark Elf Mage (Dark Elf Village)
7. Dwarf Fighter (Dwarf Village)
8. Orc Fighter (Orc Village)
9. Orc Mage (Orc Village)

### Usage Examples
```
//spawn_fake 1 50     # Spawn 50 Human Fighters
//spawn_fake 2 100    # Spawn 100 Human Mages
//despawn_fake 1      # Despawn Human Fighter group
//despawn_fake all    # Despawn all groups
```

## ⚙️ Technical Specifications

### AI Behavior Settings
- **Movement Thinking**: 5 seconds (smooth movement)
- **Combat Thinking**: 0.8 seconds (fast response)
- **Idle Thinking**: 2.5 seconds (balanced)
- **Search Radius**: 1000 units (focused targeting)
- **Max Attackers**: 3 per mob (reduced competition)
- **Target Revisit**: 45 seconds (avoid spam)

### Combat Features
- **Mage Behavior**: Only Wind Strike spell (ID 1177), infinite mana
- **Fighter Behavior**: Weapon auto-attacks with persistence
- **Cooldown Respect**: Wait for spell cooldowns instead of physical fallback
- **Player Protection**: Multiple safety layers prevent attacking players

### Spawning System
- **Sequential Mode**: One bot every 2 seconds
- **Positioning**: Circular formation around spawn point
- **Naming**: Race-specific prefixes (HumanWarrior01, ElfMage02, etc.)
- **Database Cleanup**: Automatic orphaned bot removal

## 🔧 Performance Optimization

### Recommended Settings
- **Max Concurrent Bots**: 200-300 (depending on hardware)
- **Think Interval**: Current optimized values
- **Logging Level**: INFO for production, FINE for debugging

### Memory Usage
- **Per Bot**: ~5-10MB RAM
- **Database**: Optimized queries with prepared statements
- **CPU**: Minimal impact with current AI intervals

## 🐛 Troubleshooting

### Common Issues
1. **Jerky Movement**: Check AI thinking intervals in AutobotAI.java
2. **Combat Delays**: Verify combat thinking interval (should be 800ms)
3. **Database Errors**: Ensure autobots table exists with correct structure
4. **Spawn Failures**: Check for name conflicts and database cleanup

### Debug Mode
Enable detailed logging by setting log level to FINE in the fake player logger.

## 📈 Future Enhancements

### Planned Features
- Dynamic difficulty scaling
- Advanced pathfinding improvements
- Multi-zone support
- Guild/clan integration
- PvP behavior modes

## 📝 License
This system is compatible with L2J Mobius licensing. Maintain all original copyright notices.

## 🆘 Support
For technical support, ensure all files are properly integrated and database structure matches the provided SQL exactly.

---
**Version**: 2.0  
**Compatibility**: L2J Mobius Classic 1.5+  
**Last Updated**: December 2024  
**Author**: L2J Community