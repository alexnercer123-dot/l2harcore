# Caravan Coordinate Collection System

## Overview
The `//caravan` admin command has been implemented to help collect accurate coordinates manually for creating caravan routes. This command captures the current player position and logs it in both human-readable and XML formats for easy route creation.

## Usage

### Basic Command
```
//caravan
```

### What it does:
1. **Captures current coordinates**: X, Y, Z position and heading of the admin player
2. **Logs to file**: Saves coordinates to `log/caravan_coordinates.log`
3. **Provides XML format**: Ready-to-use XML entries for Routes.xml
4. **Shows confirmation**: Displays captured coordinates in chat

### Output Example

When you use `//caravan`, it creates log entries like this:

```
[2024-01-15 14:30:25] Caravan Waypoint: X="15664" Y="142979" Z="-2707" heading="0" - Player: AdminName
<!-- Captured: 2024-01-15 14:30:25 by AdminName -->
<point X="15664" Y="142979" Z="-2707" delay="5000" run="true" />
```

## Coordinate Collection Workflow

### Step 1: Plan Your Route
1. Start at your intended caravan starting point (e.g., Dion center)
2. Use `//caravan` to capture the first waypoint
3. Walk/teleport to the next logical waypoint
4. Use `//caravan` again
5. Repeat until you reach the destination

### Step 2: Collect Strategic Points
For a Dion → Floran route, capture coordinates at:
- **Start**: Dion Castle Town center
- **Exit**: Dion town exit toward northeast
- **Bridge/Road**: Major landmarks or path changes
- **Midway**: Rest stops or significant terrain changes
- **Approach**: Near destination entrance
- **End**: Floran Village center

### Step 3: Review and Edit
1. Check `log/caravan_coordinates.log` for all captured points
2. Copy the XML `<point>` entries to your Routes.xml
3. Adjust `delay` and `run` attributes as needed
4. Add chat messages with `string` attribute if desired

## Log File Location
```
log/caravan_coordinates.log
```

## XML Integration

The generated XML points can be directly copied into Routes.xml:

```xml
<route name="dion_floran_caravan" repeat="true" repeatStyle="back">
    <target id="900009" spawnX="15664" spawnY="142979" spawnZ="-2707" />
    
    <!-- Copy generated points here -->
    <point X="15664" Y="142979" Z="-2707" delay="30000" run="false" string="Starting from Dion!" />
    <point X="16200" Y="144500" Z="-2800" delay="5000" run="true" />
    <!-- ... more points ... -->
    
</route>
```

## Command Details

- **Access Level**: 30 (GM and above)
- **Parameters**: None
- **Permissions**: Requires admin access
- **File Output**: Appends to log file (creates if doesn't exist)

## Advantages of Manual Collection

1. **Accuracy**: Real in-game coordinates ensure proper pathfinding
2. **Terrain Awareness**: You can avoid obstacles and choose optimal paths
3. **Visual Verification**: See exactly where the NPC will walk
4. **Custom Spacing**: Add waypoints where needed for smooth movement
5. **Landmark Integration**: Include significant locations and rest stops

## Tips for Best Results

1. **Use safe paths**: Avoid areas with aggressive monsters
2. **Check accessibility**: Ensure NPCs can reach all waypoints
3. **Add buffer zones**: Don't place waypoints too close to walls
4. **Test distances**: Reasonable spacing between waypoints (not too far apart)
5. **Consider elevation**: Check Z-coordinates for proper vertical positioning

## Troubleshooting

### Command not working:
- Verify you have GM access level 30 or higher
- Check that the command is registered in AdminCommands.xml
- Restart server if recently added

### Log file not created:
- Ensure `log/` directory exists
- Check file permissions
- Verify server has write access to log directory

### Coordinates seem wrong:
- Use `/loc` command to verify your position
- Check if you're in the correct area
- Ensure you're not in an instance or special zone

## Example Usage Session

```
1. Go to Dion center: /loc shows X=15664 Y=142979 Z=-2707
2. Use: //caravan
   → "Caravan coordinates captured and logged: 15664, 142979, -2707"

3. Walk to Dion exit
4. Use: //caravan  
   → Coordinates logged

5. Continue to next waypoint...
6. Repeat until route complete
```

The system makes it easy to create accurate, tested caravan routes by walking the path yourself and capturing waypoints along the way!