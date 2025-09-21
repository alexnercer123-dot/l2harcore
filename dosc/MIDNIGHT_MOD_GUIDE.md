# Midnight Mod - Implementation Guide

## Overview
The Midnight mod enhances the Lineage 2 Classic server by making monsters stronger and more rewarding during nighttime hours.

## Features
- **Enhanced Monster Stats**: 2x HP, 1.5x Attack/Defense during night
- **Double Rewards**: 2x Experience, 2x Drop rates, 2x Adena
- **Server Announcements**: Notify all players when Midnight begins/ends
- **Compatible**: Works with existing NPC stat multipliers and systems

## Configuration

### Enable the Mod
Edit `dist/game/config/Custom/MidnightMod.ini`:
```ini
EnableMidnightMod = true
```

### Customize Settings
```ini
# Monster enhancement multipliers
MonsterHpMultiplier = 2.0
MonsterPatkMultiplier = 1.5
MonsterMatkMultiplier = 1.5  
MonsterPdefMultiplier = 1.5
MonsterMdefMultiplier = 1.5

# Reward multipliers
ExpMultiplier = 2.0
DropChanceMultiplier = 2.0
DropAmountMultiplier = 2.0
AdenaMultiplier = 2.0

# Announcements
AnnouncesEnabled = true
StartMessage = "Midnight has fallen! Monsters are now incredibly strong!"
EndMessage = "Dawn has broken! Monsters return to normal strength."
```

## Testing

### 1. Basic Functionality Test
1. Start the server with `EnableMidnightMod = true`
2. Wait for nighttime (game time 00:00-06:00)
3. Verify announcement appears: "Midnight has fallen!"
4. Check monster stats are enhanced
5. Wait for dawn (game time 06:00)
6. Verify announcement: "Dawn has broken!"
7. Check monster stats return to normal

### 2. Stat Enhancement Test
During Midnight:
- Monster HP should be 2x base value (including NPC multipliers)
- Monster attack/defense should be 1.5x base value
- Can verify via admin commands or combat testing

### 3. Reward Enhancement Test
During Midnight:
- Experience gained from monsters should be 2x normal
- Item drop chances should be doubled
- Item drop amounts should be doubled  
- Adena drops should be doubled

### 4. Compatibility Test
- Verify existing NPC stat multipliers still work
- Test with Champion monsters (should stack bonuses)
- Test with Premium/VIP systems (should stack bonuses)
- Test in party situations (experience sharing works)

## Technical Notes

### How It Works
- Uses `GameTimeTaskManager` to detect day/night changes
- `MidnightManager` listens for `OnDayNightChange` events  
- Dynamically applies multipliers via overridden stat calculation methods
- Compatible with all existing rate systems

### Performance Impact
- Minimal: Only adds condition checks during stat/drop calculations
- No continuous background tasks
- Event-driven activation/deactivation

### Troubleshooting
1. **Mod not activating**: Check `EnableMidnightMod = true` in config
2. **No announcements**: Check `AnnouncesEnabled = true`
3. **Stats not changing**: Verify game time is during night (00:00-06:00)
4. **Compilation errors**: Ensure all imports are correct

## Implementation Details

### Files Modified
- `Config.java` - Configuration loading
- `MidnightMod.ini` - Configuration file
- `MidnightManager.java` - Core functionality  
- `GameServer.java` - Manager initialization
- `NpcTemplate.java` - Stat and drop multipliers
- `Npc.java` - Experience/SP multipliers

### Key Methods
- `MidnightManager.isMidnightActive()` - Check if Midnight is active
- `NpcTemplate.getBaseValue()` - Apply stat multipliers  
- `Npc.getExpReward()` - Apply experience multipliers
- `NpcTemplate.calculateDrops()` - Apply drop multipliers

## Future Enhancements
- Configurable time periods (not just night)
- Different multipliers for different monster types
- Visual effects during Midnight
- Integration with custom quest systems