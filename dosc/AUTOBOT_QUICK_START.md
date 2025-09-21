# L2J Autobot System - Quick Start Guide

## Overview
The L2J Autobot system has been successfully ported to L2J Mobius Hardcore. This guide shows you how to use the admin commands to manage autobots.

## Prerequisites

### 1. Server Setup
- ✅ Compilation completed successfully (0 errors)
- ✅ Admin commands registered in GameServer.java
- ✅ AdminCommands.xml updated with autobot permissions
- ✅ AutobotManager and AdminAutobots classes created

### 2. Database Setup (Required)
Before using autobots, you need to create the database table:

```sql
CREATE TABLE IF NOT EXISTS autobots (
    name VARCHAR(35) NOT NULL PRIMARY KEY,
    account_name VARCHAR(45) NOT NULL,
    class_id INT NOT NULL,
    level INT NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    heading INT NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Available Admin Commands

### Basic Commands
- `//autobot` - Opens the main autobot management interface
- `//autobot_spawn <name> [class_id] [level]` - Spawn a new autobot
- `//autobot_despawn <name|all>` - Despawn autobot(s)  
- `//autobot_list` - Display list of active autobots
- `//autobot_info <name>` - Show autobot information
- `//autobot_reload` - Reload autobot configuration

### Usage Examples

#### Spawn Autobots
```
//autobot_spawn TestBot1
//autobot_spawn TestBot2 0 20  // Fighter class, level 20
//autobot_spawn TestMage 10 30 // Mage class, level 30
```

#### Manage Autobots  
```
//autobot_list                // List all active autobots
//autobot_info TestBot1       // Show info for TestBot1
//autobot_despawn TestBot1    // Remove TestBot1
//autobot_despawn all         // Remove all autobots
```

## Access Levels
- **Required GM Level**: 30+ (General GM access)
- All autobot commands are set to access level 30
- Commands will work for any GM with level 30 or higher

## Current Implementation Status

### ✅ Working Features
- Admin command registration and execution
- Database persistence for autobot data
- Basic autobot creation and management
- Access level control through AdminCommands.xml
- Clean compilation with zero errors

### 🔧 Basic Implementation
- Simplified Autobot class (non-Player extending)
- Basic AutobotManager with core functionality
- Admin interface with HTML panels
- Database operations (create, read, delete)

### 🚧 Future Enhancements Needed
- Full Player class integration
- AI behavior implementation  
- Chat response system
- Auto-farming capabilities
- Party coordination
- Visual representation in game world

## Testing Steps

1. **Start the server** with the compiled code
2. **Login as GM** (access level 30+)
3. **Execute database setup** if not done already
4. **Test basic commands**:
   ```
   //autobot
   //autobot_spawn TestBot
   //autobot_list
   //autobot_despawn TestBot
   ```

## Troubleshooting

### "Command does not exist"
- Verify you have GM access level 30+
- Check server logs for registration messages
- Ensure AdminCommands.xml contains autobot entries

### Database Errors
- Run the database creation script above
- Check database connection configuration
- Verify table exists: `SHOW TABLES LIKE 'autobots';`

### Compilation Issues
- All compilation errors have been resolved
- If issues occur, check imports in Java files
- Verify L2J Mobius API compatibility

## Next Development Steps

1. **Enhance Autobot Class**: Integrate with L2J Mobius Player system
2. **Add AI Behaviors**: Implement class-specific behaviors
3. **World Integration**: Make autobots visible and interactive
4. **Chat System**: Add response patterns and conversation
5. **Auto-farming**: Implement automated farming logic

## Files Modified

- `GameServer.java` - Added admin command registration
- `AdminCommands.xml` - Added autobot command permissions
- `AdminAutobots.java` - Admin command handler
- `AutobotManager.java` - Core autobot management
- `Autobot.java` - Simplified autobot class

The system is now ready for basic testing and further development!