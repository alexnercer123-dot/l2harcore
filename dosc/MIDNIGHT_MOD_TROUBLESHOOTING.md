# Midnight Mod Troubleshooting Guide

## Problem: Midnight Mod Not Activating

### Issues Found and Solutions:

### 1. **Configuration Issue - FIXED**
**Problem**: `EnableMidnightMod = false` in config file
**Solution**: Changed to `EnableMidnightMod = true` in `dist/game/config/Custom/MidnightMod.ini`

### 2. **Time Understanding Issue**
**Important**: The Midnight mod activates during **NIGHT TIME**, not specifically at midnight!

**Night Time Definition**:
- **Night**: Game hours 0-5 (00:00 - 05:59)
- **Day**: Game hours 6-23 (06:00 - 23:59)

**Real Midnight**: Game hour 0 (00:00) is when midnight effects should be strongest, but they're active throughout hours 0-5.

### 3. **Testing Instructions**

#### Method 1: Wait for Natural Night
- Game time cycles automatically
- Wait for game time to reach hours 0-5
- You should see the announcement: "Midnight has fallen! Monsters become incredibly strong, but rewards are doubled!"

#### Method 2: Force Night Mode (Recommended for Testing)
Use admin commands to instantly activate night:

```bash
# Force midnight (hour 0) - will activate Midnight mod
//night

# Or manually set to any night hour (0-5)
//settime 0   # Midnight
//settime 1   # 1 AM  
//settime 2   # 2 AM
//settime 3   # 3 AM
//settime 4   # 4 AM
//settime 5   # 5 AM

# Force day mode to test deactivation
//day

# Or set to any day hour (6-23)
//settime 6   # 6 AM - should deactivate Midnight mod
//settime 12  # Noon
//settime 18  # 6 PM
```

### 4. **What to Expect When Active**

#### Server Announcements:
- **Activation**: "Midnight has fallen! Monsters become incredibly strong, but rewards are doubled!"
- **Deactivation**: "Dawn breaks! Monsters return to their normal strength."

#### Monster Changes:
- **HP**: Doubled (2x)
- **Attack/Defense**: 1.5x stronger
- **Experience**: Doubled rewards
- **Item Drops**: Doubled chance and amounts
- **Adena**: Doubled drops

### 5. **Verification Steps**

1. **Check Current Game Time**:
   ```bash
   # Use this to see current game hour
   //settime 0  # This will show you current time before changing
   ```

2. **Force Activation Test**:
   ```bash
   //night  # Should trigger announcement if mod is working
   ```

3. **Check Monster Stats**:
   - Find a monster
   - Note its HP before Midnight activation
   - Activate night mode with `//night`
   - Check same monster type - HP should be doubled

4. **Force Deactivation Test**:
   ```bash
   //day   # Should trigger dawn announcement
   ```

### 6. **Server Restart Required**

**IMPORTANT**: After changing the config file, you must restart the GameServer for changes to take effect!

```bash
# Stop the server
# Then restart with:
java -jar dist/libs/GameServer.jar
```

### 7. **Debug Console Messages**

After restart, when the mod activates/deactivates, you should see:
- No more "auto setting accesslevel" warnings (these are fixed)
- Day/night change events in server logs
- Successful activation/deactivation messages

### 8. **Common Mistakes**

❌ **Wrong**: Expecting activation only at game hour 12 (noon)
✅ **Correct**: Activation during game hours 0-5 (night)

❌ **Wrong**: Not restarting server after config changes
✅ **Correct**: Always restart server after config file modifications

❌ **Wrong**: Using real-world time instead of game time
✅ **Correct**: Game time cycles independently of real time

### 9. **Quick Test Sequence**

1. Ensure server is restarted with new config
2. Use `//night` command
3. Look for announcement message
4. Test monster stats/drops
5. Use `//day` command
6. Confirm deactivation announcement

### 10. **Config File Location Reminder**

✅ **Correct**: `dist/game/config/Custom/MidnightMod.ini`
❌ **Wrong**: `config/Custom/MidnightMod.ini`

The file must be in the `dist/game/config/Custom/` directory!