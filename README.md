# L2 Hardcore Server (Age of Splendor)

Welcome to the L2 Hardcore Server project - a Lineage 2 Classic server emulator based on L2J Mobius with hardcore/permanent death mechanics.

## Project Overview

This is a Lineage 2 Classic server emulator specifically designed with hardcore gameplay mechanics. When a character dies, they are permanently removed from the game, making every decision and battle critical. This implementation is based on the L2J Mobius project, specifically tailored for the "Age of Splendor" chronicle.

## Features

### Core Gameplay
- **Permanent Death System**: When your character dies, they are permanently removed from the game
- **Hardcore Mechanics**: Every battle matters as death has permanent consequences
- **Character Protection**: Special protection mechanics for new players
- **Clan Systems**: Clan wars, reputation, and clan halls with unique mechanics

### Technical Features
- Based on L2J Mobius Classic 1.5 Age of Splendor
- Java 24+ required
- Ant 1.8.2+ build system
- Modular architecture with separate LoginServer and GameServer components

### Game Systems
- Clan levels up to 5 with special skills
- Clan wars with reputation point mechanics
- Castle sieges for PvP content
- 31 character classes with first and second class transfers
- Level cap of 75 with planned expansions
- Transformation skills with unique bonuses
- Extensive hunting zones with rebalanced monsters
- Raid bosses with enhanced rewards
- Epic jewelry upgrade system
- Quest system improvements

## Project Structure

```
├── java/                 # Java source code
│   └── org.l2jmobius/    # Main package
│       ├── commons/      # Common utilities and libraries
│       ├── gameserver/   # Game server implementation
│       ├── loginserver/  # Login server implementation
│       ├── tools/        # Utility tools
│       └── Config.java   # Main configuration
├── dist/                 # Distribution files
│   ├── libs/             # Required libraries
│   ├── game/             # Game server files
│   ├── login/            # Login server files
│   └── db_installer/     # Database installer
├── docs/                 # Documentation files
└── build.xml             # Ant build file
```

## Requirements

- Java 24 or higher
- Apache Ant 1.8.2 or higher
- MySQL database server
- At least 4GB RAM recommended

## Building the Project

```bash
# Compile the source code
ant compile

# Create JAR files
ant jar

# Create distribution package
ant
```

## Running the Server

1. Set up the MySQL database using the database installer
2. Configure the server settings in the config files
3. Start the LoginServer first:
   ```bash
   java -jar dist/libs/LoginServer.jar
   ```
4. Start the GameServer:
   ```bash
   java -jar dist/libs/GameServer.jar
   ```

## Key Hardcore Features

### Death Penalty
- Permanent character removal upon death
- Special protection for low-level characters
- 7-day waiting period for character deletion

### Clan Wars
- Reputation-based warfare system
- Multi-clan war declarations
- Clan skill progression through warfare

### Character Progression
- Enhanced experience and SP rates
- Modified drop and spoil rates
- Transformation skills for combat advantages

## Configuration

Main configuration files are located in the config directories of both login and game servers. Key settings include:

- Database connection parameters
- Server networking settings
- Gameplay rates and modifiers
- Hardcore-specific mechanics

## Contributing

This project is based on the L2J Mobius project. Contributions are welcome through pull requests. Please ensure your code follows the existing style and conventions.

## License

This project is licensed under the GNU General Public License v3.0. See the LICENSE file for details.

## Disclaimer

This project is for educational purposes only. Lineage 2 is a registered trademark of NCSoft Corporation. This emulator is not affiliated with or endorsed by NCSoft.