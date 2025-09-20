# Permanent Death System Documentation

## Overview

The Permanent Death System is a hardcore gameplay feature implemented for L2Hardcore that introduces permanent character death when players die from PvE sources (monsters, raid bosses, NPCs, epic bosses). This system enhances the risk-reward mechanics and creates a more immersive, high-stakes gaming experience.

## Core Features

### 1. Death Type Differentiation
- **PvE Deaths**: Monsters, Raid Bosses, Grand Bosses, NPCs → **Permanent Death**
- **PvP Deaths**: Player vs Player combat → **No Permanent Death**

### 2. Persistent Storage
The system uses the PlayerVariables framework to store permanent death status:
- `PERMANENT_DEATH_STATUS`: Boolean flag indicating if player is permanently dead
- `PERMANENT_DEATH_KILLER`: String storing the name of the killer
- `PERMANENT_DEATH_TIME`: Long timestamp of when the death occurred

### 3. Action Restrictions
Permanently dead players cannot:
- Trade with other players
- Drop items from inventory
- Use any teleportation options (To Town, To Clan Hall, To Castle, To Fortress, To Siege HQ)
- Be resurrected by any means

### 4. Global Notifications
When a permanent death occurs, all online players receive a broadcast message:
```
[HARDCORE] PlayerName has died permanently to [killer type] [killer name]!
```

### 5. Login Validation
The system validates permanent death status on every login and corrects any inconsistencies.

## Technical Implementation

### Modified Files

#### 1. PlayerVariables.java
Added three new constants for permanent death tracking:
```java
public static final String PERMANENT_DEATH_STATUS = "PERMANENT_DEATH_STATUS";
public static final String PERMANENT_DEATH_KILLER = "PERMANENT_DEATH_KILLER";
public static final String PERMANENT_DEATH_TIME = "PERMANENT_DEATH_TIME";
```

#### 2. Player.java
**Core Methods Added:**
- `handlePermanentDeath(Creature killer)`: Main logic for processing deaths
- `setPermanentDeath(boolean isDead, String killerName)`: Sets/clears permanent death status
- `isPermanentlyDead()`: Checks if player is permanently dead
- `getPermanentDeathKiller()`: Returns killer name
- `getPermanentDeathTime()`: Returns death timestamp
- `broadcastPermanentDeathAnnouncement(Creature killer)`: Broadcasts death to all players

**Modified Methods:**
- `doDie()`: Now calls `handlePermanentDeath()` to process deaths
- `canRevive()`: Blocks resurrection for permanently dead players
- `reviveRequest()`: Blocks resurrection attempts by other players on permanently dead characters
- `doRevive()` and `doRevive(double)`: Prevent actual revival of permanently dead players

#### 3. TradeRequest.java
Added permanent death checks to prevent trading:
```java
// Block if player trying to trade is permanently dead
if (player.isPermanentlyDead()) {
    player.sendMessage("You cannot trade while permanently dead.");
    player.sendPacket(ActionFailed.STATIC_PACKET);
    return;
}

// Block if target player is permanently dead
if (partner.isPermanentlyDead()) {
    player.sendMessage("[HARDCORE] You cannot trade with permanently dead players.");
    player.sendPacket(ActionFailed.STATIC_PACKET);
    return;
}
```

#### 4. RequestDropItem.java
Added permanent death check to prevent item dropping:
```java
if (player.isPermanentlyDead()) {
    player.sendMessage("You cannot drop items while permanently dead.");
    return;
}
```

#### 5. RequestRestartPoint.java
Added permanent death check to prevent all teleportation types:
```java
if (player.isPermanentlyDead()) {
    player.sendMessage("[HARDCORE] You cannot use any teleportation while permanently dead.");
    return;
}
```

#### 6. EnterWorld.java
Added login validation for permanent death status:
```java
if (player.isPermanentlyDead()) {
    PacketLogger.info("[HARDCORE] Player " + player.getName() + " logged in with permanent death status...");
    player.sendMessage("[HARDCORE] You are permanently dead. Your character cannot be resurrected.");
    
    if (player.canRevive()) {
        PacketLogger.warning("[HARDCORE] Correcting canRevive status for permanently dead player...");
        player.setCanRevive(false);
    }
}
```

## Debug Logging

The system includes comprehensive debug logging for troubleshooting:

### Death Events
- PvE deaths: Logs permanent death assignment with killer details
- PvP deaths: Logs that no permanent death occurred
- Killer type detection (Monster/Raid Boss/Grand Boss/NPC)

### Revival Attempts
- Logs when permanently dead players attempt resurrection
- Records blocked revival attempts

### Action Blocking
- Logs blocked trading attempts
- Records prevented item dropping
- Tracks blocked teleportation attempts

### Login Validation
- Validates permanent death status consistency
- Logs any corrections made to revival permissions
- Records permanent death details (killer, timestamp)

## Usage Examples

### Example 1: Monster Death
```
[INFO] [HARDCORE] Player TestPlayer (12345) died permanently from Cave Ant (ID: 2001)
[BROADCAST] [HARDCORE] TestPlayer has died permanently to monster Cave Ant!
```

### Example 2: Raid Boss Death
```
[INFO] [HARDCORE] Player WarriorX (67890) died permanently from Queen Ant (ID: 29001)
[BROADCAST] [HARDCORE] WarriorX has died permanently to Raid Boss Queen Ant!
```

### Example 3: PvP Death (No Permanent Death)
```
[INFO] [HARDCORE] Player FighterY died in PvP from PlayerZ - no permanent death
```

### Example 4: Login Validation
```
[INFO] [HARDCORE] Player DeadPlayer (11111) logged in with permanent death status. Killer: Orfen, Time: Mon Dec 18 15:30:45 2023
```

## Configuration

No additional configuration files are required. The system is enabled by default and works automatically with the existing L2J Mobius framework.

## Database Impact

The permanent death status is stored using the existing PlayerVariables system, which uses the `character_variables` table in the database. No additional database schema changes are required.

## Testing Recommendations

1. **PvE Death Testing**: Kill characters with different monster types to verify permanent death
2. **PvP Death Testing**: Verify that player kills do NOT trigger permanent death
3. **Action Blocking**: Test that permanently dead players cannot trade, drop items, or teleport
4. **Login Validation**: Log in with permanently dead characters to verify status persistence
5. **Broadcast Testing**: Verify that death announcements reach all online players

## Troubleshooting

### Common Issues

1. **Player can still revive after permanent death**
   - Check logs for permanent death status assignment
   - Verify `canRevive()` method is returning false
   - Check login validation logs for corrections

2. **PvP deaths causing permanent death**
   - Review `handlePermanentDeath()` killer type detection
   - Check that `killer.isPlayable()` is working correctly

3. **Actions not blocked for permanently dead players**
   - Verify permanent death checks in packet handlers
   - Check `isPermanentlyDead()` method return value

4. **Disconnect when logging in with permanently dead character (Fixed in v1.1)**
   - Issue was caused by improper handling of `_canRevive` state during login
   - Fixed by setting `canRevive = false` early in login process
   - Ensured proper dead state handling without causing client-server sync issues

5. **Players can still be resurrected despite permanent death (Fixed in v1.2)**
   - Issue was that resurrection scrolls, spells, and player resurrection attempts could still work
   - Fixed by adding permanent death checks in `reviveRequest()`, `doRevive()`, and `doRevive(double)`
   - Added proper error messages for resurrection attempts

6. **Trading with permanently dead players allowed (Fixed in v1.2)**
   - Issue was that players could initiate trades with permanently dead targets
   - Fixed by adding permanent death check for trade target in `TradeRequest.java`
   - Added appropriate error message when attempting to trade with permanently dead players

### Log Analysis

Search logs for these patterns:
- `[HARDCORE]` - All permanent death system messages
- `permanently dead` - Status checks and validations
- `died permanently from` - Actual death events
- `died in PvP` - Non-permanent PvP deaths

## Future Enhancements

Potential improvements for the system:
1. Configurable permanent death grace period
2. Clan-based resurrection abilities
3. Special items that can prevent permanent death
4. Statistics tracking for permanent deaths
5. Web interface for viewing death statistics

## Version History

- **v1.0** (Initial Implementation): Complete permanent death system with PvE/PvP differentiation, action blocking, global notifications, and login validation with comprehensive debug logging.
- **v1.1** (Bug Fix): Fixed disconnect issue when logging in with permanently dead characters by improving login validation logic and ensuring proper dead state handling.
- **v1.2** (Security Enhancement): Fixed critical gaps in permanent death system:
  - Blocked resurrection attempts by other players on permanently dead characters
  - Prevented direct doRevive() calls from working on permanently dead players
  - Added protection against trading with permanently dead players (both initiator and target)
  - Enhanced resurrection blocking with proper error messages
- **v1.3** (Teleportation Security): Enhanced teleportation blocking for permanently dead players:
  - Extended teleportation restrictions beyond "To Town" to include all teleportation types
  - Now blocks: To Clan Hall, To Castle, To Fortress, To Siege HQ, and Fixed/Festival resurrection
  - Moved permanent death check to `portPlayer()` method for comprehensive coverage