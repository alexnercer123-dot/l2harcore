# L2J Mobius Fake Player System - Code Snippets & Modifications

## Essential Code Modifications for Existing Classes

### 1. Config.java Modifications

Add these constants at the top of Config.java (around line 200):
```java
public static final String CUSTOM_FAKE_PLAYERS_CONFIG_FILE = "./config/custom/FakePlayers.properties";
```

Add these variables in the Custom section (around line 3534):
```java
// --------------------------------------------------
// Custom - Fake Players
// --------------------------------------------------
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
```

Add configuration loading (around line 3540):
```java
// --------------------------------------------------
// Custom - Fake Players
// --------------------------------------------------
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
```

### 2. GameServer.java Modifications

Add manager initializations in the managers section:
```java
print("AutobotManager", AutobotManager.getInstance());
print("FakePlayerSpawnManager", FakePlayerSpawnManager.getInstance());
print("FakePlayerChatManager", FakePlayerChatManager.getInstance());
```

### 3. AdminCommandHandlers.xml Registration

Add these entries to your AdminCommandHandlers.xml:
```xml
<admincommand name="admin_autobot" handler="AdminAutobots" />
<admincommand name="admin_autobot_spawn" handler="AdminAutobots" />
<admincommand name="admin_autobot_despawn" handler="AdminAutobots" />
<admincommand name="admin_autobot_list" handler="AdminAutobots" />
<admincommand name="admin_autobot_info" handler="AdminAutobots" />
<admincommand name="admin_autobot_reload" handler="AdminAutobots" />
<admincommand name="admin_autobot_stats" handler="AdminAutobots" />
<admincommand name="admin_autobot_ai_toggle" handler="AdminAutobots" />
<admincommand name="admin_spawn_fake" handler="AdminAutobots" />
<admincommand name="admin_despawn_fake" handler="AdminAutobots" />
<admincommand name="admin_fake_info" handler="AdminAutobots" />
<admincommand name="admin_fake_groups" handler="AdminAutobots" />
<admincommand name="admin_fake_help" handler="AdminAutobots" />
```

### 4. Database Schema

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

### 5. Key AI Configuration Values

Critical settings for optimal performance:

**Movement Intervals (AutobotAI.java line ~140)**:
```java
if (_autobot.getPlayer().isMoving())
{
    thinkingInterval = 5000; // 5 seconds when moving (smooth movement)
}
else if (_autobot.getPlayer().isInCombat() || _autobot.getPlayer().isCastingNow() || _autobot.getPlayer().isAttackingNow())
{
    thinkingInterval = 800; // 0.8 seconds in combat (fast response)
}
else
{
    thinkingInterval = 2500; // 2.5 seconds when idle
}
```

**Combat System Settings (CombatAI.java line ~35-40)**:
```java
private static final int SEARCH_RADIUS = 1000; // Limited to 1000 for performance
private static final int MAX_ATTACKERS_PER_TARGET = 3; // Max 3 players per mob
private static final long TARGET_REVISIT_DELAY = 45000; // 45 seconds
```

**Spawn Limit (AdminAutobots.java line ~225)**:
```java
if (count < 1 || count > 500)
{
    activeChar.sendMessage("Count must be between 1 and 500.");
    return false;
}
```

### 6. Critical Method Implementations

**Movement Control (AutobotAI.java)**:
```java
private void moveTo(Location destination)
{
    if (destination != null)
    {
        Player player = _autobot.getPlayer();
        
        // Only issue movement command if we're not already moving or destination is far
        if (!player.isMoving() || player.calculateDistance2D(destination) > 200)
        {
            player.getAI().setIntention(Intention.MOVE_TO, destination);
            FAKE_PLAYER_LOGGER.fine(player.getName() + " moving to " + destination);
        }
    }
}
```

**Mage Combat System (CombatAI.java)**:
```java
// CRITICAL: Check if Wind Strike is on cooldown
if (player.isSkillDisabled(windStrike))
{
    LOGGER.fine("Non-Orc mage " + player.getName() + " Wind Strike is on cooldown - waiting");
    return true; // Wait for cooldown, DO NOT use physical attack
}

// Use Wind Strike skill - set target and cast
player.setTarget(target);
player.useMagic(windStrike, null, false, false);
```

**Sequential Spawning (FakePlayerSpawnManager.java)**:
```java
// Schedule the next bot spawn after 2 seconds delay
org.l2jmobius.commons.threads.ThreadPool.schedule(() -> {
    spawnBotsSequentially(groupId, group, totalCount, spawnLoc, spawnedBots, currentIndex + 1);
}, 2000); // 2 second delay between each bot
```

### 7. Essential Imports

Add these imports where needed:

**For AutobotAI.java**:
```java
import org.l2jmobius.gameserver.managers.AutobotManager;
import org.l2jmobius.gameserver.model.actor.Autobot;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.commons.util.Rnd;
```

**For CombatAI.java**:
```java
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
```

**For AdminAutobots.java**:
```java
import org.l2jmobius.gameserver.managers.AutobotManager;
import org.l2jmobius.gameserver.managers.FakePlayerSpawnManager;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
```

### 8. Configuration File Template

Create `config/custom/FakePlayers.properties`:
```properties
# =================================================================
# L2J Mobius Fake Player System Configuration
# =================================================================

# Enable the fake player system
EnableFakePlayers = true

# Chat and social features (disabled for performance)
FakePlayerChat = false

# Combat features
FakePlayerUseShots = false
FakePlayerKillsRewardPvP = false
FakePlayerUnflaggedKillsKarma = false
FakePlayerAutoAttackable = false

# Targeting behavior
FakePlayerAggroMonsters = true
FakePlayerAggroPlayers = false
FakePlayerAggroFPC = false

# Item interaction (disabled for performance)
FakePlayerCanDropItems = false
FakePlayerCanPickup = false
```

### 9. Performance Tuning

**AutobotTaskManager.java - AI Processing Frequency**:
```java
private static final long AI_TASK_DELAY = 1000; // 1 second AI processing
```

**AutobotAI.java - Target Search Optimization**:
```java
// Clear checked targets every 45 seconds to allow revisiting
if (currentTime - _lastTargetSwitchTime > 45000)
{
    _checkedTargets.clear();
    _lastTargetSwitchTime = currentTime;
}
```

### 10. Debug and Logging

**Enable Debug Logging**:
```java
// In AutobotManager.java - setupFakePlayerLogger()
String logFileName = "log/fake-player-" + timestamp + ".log";
FileHandler fileHandler = new FileHandler(logFileName, true);
fileHandler.setFormatter(new SimpleFormatter());
FAKE_PLAYER_LOGGER.addHandler(fileHandler);
FAKE_PLAYER_LOGGER.setLevel(Level.INFO); // Change to FINE for debug
```

## Integration Checklist

- [ ] Database table created
- [ ] Config.java modified
- [ ] FakePlayers.properties created
- [ ] All Java files added
- [ ] AdminCommandHandlers.xml updated
- [ ] GameServer.java managers initialized
- [ ] Compilation successful
- [ ] Test spawn commands work
- [ ] Movement is smooth
- [ ] Combat is responsive
- [ ] Mages use only Wind Strike
- [ ] Only target Gremlins
- [ ] Sequential spawning works
- [ ] Death despawning works

## Common Integration Errors

1. **ClassNotFoundException**: Missing AutobotManager import
2. **SQLException**: Database table not created
3. **Config not found**: FakePlayers.properties missing
4. **Command not working**: AdminCommandHandlers.xml not updated
5. **Manager not initialized**: Missing GameServer.java initialization

---
**Integration Difficulty**: Beginner  
**Time Required**: 30-60 minutes  
**Files Modified**: 3 existing files + new files  
**Dependencies**: L2J Mobius Classic 1.5+