# Admin Time Control Commands

This document describes the admin commands for controlling game time in the L2Hardcore server.

## Overview

The admin time control system allows Game Masters to manipulate the in-game time, which directly affects the day/night cycle and related game mechanics such as the [Midnight Mod](MIDNIGHT_MOD_GUIDE.md).

## Commands

### `//settime [hour]`

**Description**: Sets the game time to a specific hour (0-23)

**Syntax**: `//settime <hour>`

**Parameters**:
- `hour` - Integer value between 0 and 23 representing the hour to set

**Examples**:
```
//settime 0    - Sets time to midnight (00:00)
//settime 6    - Sets time to dawn (06:00)
//settime 12   - Sets time to noon (12:00)
//settime 18   - Sets time to evening (18:00)
//settime 23   - Sets time to late night (23:00)
```

**Effects**:
- Immediately changes the game time to the specified hour
- Triggers day/night change events if crossing the 6 AM threshold
- If Midnight Mod is enabled, may activate/deactivate night bonuses based on new time

**Error Messages**:
- `"Usage: //settime [hour] (0-23)"` - Wrong number of parameters
- `"Hour must be between 0 and 23"` - Invalid hour value
- `"Invalid hour format. Use: //settime [hour] (0-23)"` - Non-numeric input

---

### `//night`

**Description**: Forces night mode by setting the time to midnight

**Syntax**: `//night`

**Parameters**: None

**Effects**:
- Sets game time to 00:00 (midnight)
- Immediately activates night mode
- If Midnight Mod is enabled, instantly applies all night bonuses:
  - Monster HP increased by 2x
  - Monster attack/defense increased by 1.5x
  - Double experience and SP rewards
  - Double item drop rates and amounts
  - Double adena drops
  - Server announcement about Midnight activation

**Success Message**: `"Game time set to midnight (night mode activated)"`

---

### `//day`

**Description**: Forces day mode by setting the time to noon

**Syntax**: `//day`

**Parameters**: None

**Effects**:
- Sets game time to 12:00 (noon)
- Immediately activates day mode
- If Midnight Mod was active, instantly removes all night bonuses:
  - Monster stats return to normal values
  - Experience and drop rates return to configured values
  - Server announcement about Midnight deactivation

**Success Message**: `"Game time set to noon (day mode activated)"`

## Technical Details

### Implementation

The commands are implemented in the following files:
- **Handler**: `java/org/l2jmobius/gameserver/handler/admincommandhandlers/AdminTime.java`
- **Time Manager**: `java/org/l2jmobius/gameserver/taskmanagers/GameTimeTaskManager.java`
- **Registration**: `java/org/l2jmobius/gameserver/GameServer.java`

### Day/Night Cycle

- **Day Time**: 06:00 - 05:59 (6 AM to 5:59 AM)
- **Night Time**: 00:00 - 05:59 (Midnight to 5:59 AM)
- **Midnight Threshold**: Game hour < 6 is considered night

### Event System Integration

The time control commands integrate with the server's event system:
1. Time changes trigger `OnDayNightChange` events
2. [MidnightManager](java/org/l2jmobius/gameserver/managers/MidnightManager.java) listens for these events
3. Monster stat multipliers and announcements are applied automatically

### Time Offset System

The implementation uses a time offset mechanism:
- Base time calculation: `(System.currentTimeMillis() - referenceTime) / MILLIS_IN_TICK`
- Offset applied: `(System.currentTimeMillis() - referenceTime + timeOffset) / MILLIS_IN_TICK`
- This allows persistent time manipulation without affecting the core time system

## Usage Examples

### Scenario 1: Testing Midnight Mod
```bash
# Activate night mode to test Midnight bonuses
//night

# Check if monsters have enhanced stats and drops
# Hunt some monsters to verify double rewards

# Return to day mode
//day
```

### Scenario 2: Event Management
```bash
# Set time for a specific event at 8 PM
//settime 20

# Later, force night mode for a special midnight event
//night
```

### Scenario 3: Server Debugging
```bash
# Test day/night transitions at specific times
//settime 5    # Just before dawn
//settime 6    # Dawn transition
//settime 0    # Midnight transition
```

## Access Requirements

- Commands require GM access level permissions
- Standard admin command access control applies
- Commands respect the server's admin rights configuration
- **Access Level 100**: All time control commands are configured for access level 100

### Configuration

The admin time control commands are properly configured in `dist/game/config/AdminCommands.xml`:

```xml
<!-- ADMIN TIME CONTROL -->
<admin command="admin_settime" description="Set game time to specific hour (0-23)." accessLevel="100" />
<admin command="admin_night" description="Force night mode (set time to midnight)." accessLevel="100" />
<admin command="admin_day" description="Force day mode (set time to noon)." accessLevel="100" />
```

**Note**: After adding these entries to the configuration file, restart the GameServer for the changes to take effect and eliminate console warnings about undefined admin commands.

## Integration with Midnight Mod

When the [Midnight Mod](MIDNIGHT_MOD_GUIDE.md) is enabled (`MIDNIGHT_MOD_ENABLED = true`), these time commands directly control the mod's activation:

### Night Mode Effects (//night or //settime 0-5):
- Monster HP multiplied by `MIDNIGHT_MOD_HP_MULTIPLIER` (default: 2.0)
- Monster Attack/Defense multiplied by `MIDNIGHT_MOD_ATTACK_MULTIPLIER` and `MIDNIGHT_MOD_DEFENSE_MULTIPLIER` (default: 1.5)
- Experience rewards multiplied by `MIDNIGHT_MOD_EXP_MULTIPLIER` (default: 2.0)
- SP rewards multiplied by `MIDNIGHT_MOD_SP_MULTIPLIER` (default: 2.0)
- Drop rates and amounts multiplied by `MIDNIGHT_MOD_DROP_MULTIPLIER` (default: 2.0)
- Adena drops multiplied by `MIDNIGHT_MOD_ADENA_MULTIPLIER` (default: 2.0)

### Day Mode Effects (//day or //settime 6-23):
- All monster bonuses removed
- Normal game mechanics restored

## Troubleshooting

### Common Issues

1. **Command not recognized**: Ensure you have GM privileges and the AdminTime handler is properly registered
2. **No effect on Midnight Mod**: Check that `MIDNIGHT_MOD_ENABLED = true` in configuration
3. **Events not triggering**: Verify that the GameTimeTaskManager is running correctly

### Debug Information

The system provides feedback through:
- Admin command response messages
- Server announcements (when Midnight Mod is active)
- Console logs for event triggers
- System messages for validation errors

## See Also

- [Midnight Mod Guide](MIDNIGHT_MOD_GUIDE.md) - Complete documentation for the Midnight enhancement system
- [Admin Commands Reference](docs/admin_commands.md) - Other available admin commands
- [Configuration Guide](docs/configuration.md) - Server configuration options