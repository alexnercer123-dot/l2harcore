-- L2Autobots Database Schema for L2J Mobius Hardcore
-- Based on original L2Autobots schema adapted for L2J Mobius

-- Main autobots table
CREATE TABLE IF NOT EXISTS `autobots` (
  `obj_Id` INT UNSIGNED NOT NULL DEFAULT 0,
  `char_name` VARCHAR(35) NOT NULL,
  `level` TINYINT UNSIGNED DEFAULT 1,
  `maxHp` MEDIUMINT UNSIGNED DEFAULT NULL,
  `curHp` MEDIUMINT UNSIGNED DEFAULT NULL,
  `maxCp` MEDIUMINT UNSIGNED DEFAULT NULL,
  `curCp` MEDIUMINT UNSIGNED DEFAULT NULL,
  `maxMp` MEDIUMINT UNSIGNED DEFAULT NULL,
  `curMp` MEDIUMINT UNSIGNED DEFAULT NULL,
  `face` TINYINT UNSIGNED DEFAULT 0,
  `hairStyle` TINYINT UNSIGNED DEFAULT 0,
  `hairColor` TINYINT UNSIGNED DEFAULT 0,
  `sex` TINYINT UNSIGNED DEFAULT 0,
  `heading` MEDIUMINT DEFAULT 0,
  `x` MEDIUMINT DEFAULT 0,
  `y` MEDIUMINT DEFAULT 0,
  `z` MEDIUMINT DEFAULT 0,
  `exp` BIGINT UNSIGNED DEFAULT 0,
  `expBeforeDeath` BIGINT UNSIGNED DEFAULT 0,
  `sp` INT UNSIGNED NOT NULL DEFAULT 0,
  `reputation` INT DEFAULT 0,
  `pvpkills` SMALLINT UNSIGNED DEFAULT 0,
  `pkkills` SMALLINT UNSIGNED DEFAULT 0,
  `clanid` INT UNSIGNED DEFAULT 0,
  `race` TINYINT UNSIGNED DEFAULT 0,
  `classid` TINYINT UNSIGNED DEFAULT 0,
  `base_class` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `deletetime` BIGINT DEFAULT 0,
  `cancraft` TINYINT UNSIGNED DEFAULT 0,
  `title` VARCHAR(21) DEFAULT '',
  `title_color` MEDIUMINT UNSIGNED DEFAULT 0xECF9A2,
  `name_color` MEDIUMINT UNSIGNED DEFAULT 0xFFFFFF,
  `accesslevel` MEDIUMINT DEFAULT 0,
  `online` TINYINT UNSIGNED DEFAULT 0,
  `onlinetime` INT DEFAULT 0,
  `lastAccess` BIGINT UNSIGNED DEFAULT 0,
  `clan_privs` MEDIUMINT UNSIGNED DEFAULT 0,
  `wantspeace` TINYINT UNSIGNED DEFAULT 0,
  `power_grade` TINYINT UNSIGNED DEFAULT 0,
  `nobless` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `hero` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `subpledge` SMALLINT NOT NULL DEFAULT 0,
  `lvl_joined_academy` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `apprentice` INT UNSIGNED NOT NULL DEFAULT 0,
  `sponsor` INT UNSIGNED NOT NULL DEFAULT 0,
  `clan_join_expiry_time` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `clan_create_expiry_time` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `death_penalty_level` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  `pc_point` INT UNSIGNED NOT NULL DEFAULT 0,
  `fame` INT UNSIGNED NOT NULL DEFAULT 0,
  `bookmarkslot` INT UNSIGNED NOT NULL DEFAULT 0,
  `vitality_points` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  `language` VARCHAR(2) DEFAULT 'en',
  `creationDate` BIGINT UNSIGNED DEFAULT 0,
  `modificationDate` BIGINT UNSIGNED DEFAULT 0,
  PRIMARY KEY (`obj_Id`),
  KEY `clanid` (`clanid`),
  KEY `char_name` (`char_name`),
  KEY `online` (`online`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Autobot behavior preferences table
CREATE TABLE IF NOT EXISTS `autobot_behaviors` (
  `obj_Id` INT UNSIGNED NOT NULL,
  `behavior_type` VARCHAR(50) NOT NULL DEFAULT 'DEFAULT',
  `combat_prefs` TEXT DEFAULT NULL,
  `social_prefs` TEXT DEFAULT NULL,
  `activity_prefs` TEXT DEFAULT NULL,
  `skill_prefs` TEXT DEFAULT NULL,
  `respawn_action` VARCHAR(50) DEFAULT 'RETURN_TO_DEATH_LOCATION',
  `auto_farm_enabled` TINYINT UNSIGNED DEFAULT 0,
  `auto_farm_radius` SMALLINT UNSIGNED DEFAULT 800,
  `target_monsters_only` TINYINT UNSIGNED DEFAULT 1,
  `use_potions` TINYINT UNSIGNED DEFAULT 1,
  `return_to_spawn` TINYINT UNSIGNED DEFAULT 1,
  `follow_summoner` TINYINT UNSIGNED DEFAULT 0,
  `assist_party` TINYINT UNSIGNED DEFAULT 1,
  `last_update` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`obj_Id`),
  FOREIGN KEY (`obj_Id`) REFERENCES `autobots`(`obj_Id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Autobot chat responses table  
CREATE TABLE IF NOT EXISTS `autobot_chat_data` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `obj_Id` INT UNSIGNED NOT NULL,
  `trigger_type` ENUM('CONTAINS', 'STARTS_WITH', 'EQUALS', 'REGEX') DEFAULT 'CONTAINS',
  `trigger_text` VARCHAR(255) NOT NULL,
  `response_text` TEXT NOT NULL,
  `response_chance` TINYINT UNSIGNED DEFAULT 100,
  `cooldown_seconds` SMALLINT UNSIGNED DEFAULT 5,
  `enabled` TINYINT UNSIGNED DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `obj_Id` (`obj_Id`),
  FOREIGN KEY (`obj_Id`) REFERENCES `autobots`(`obj_Id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Autobot sessions tracking
CREATE TABLE IF NOT EXISTS `autobot_sessions` (
  `obj_Id` INT UNSIGNED NOT NULL,
  `session_start` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `session_end` TIMESTAMP NULL DEFAULT NULL,
  `total_kills` INT UNSIGNED DEFAULT 0,
  `total_deaths` INT UNSIGNED DEFAULT 0,
  `exp_gained` BIGINT UNSIGNED DEFAULT 0,
  `items_collected` INT UNSIGNED DEFAULT 0,
  `distance_traveled` BIGINT UNSIGNED DEFAULT 0,
  `last_activity` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`obj_Id`, `session_start`),
  FOREIGN KEY (`obj_Id`) REFERENCES `autobots`(`obj_Id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Autobot skill configurations
CREATE TABLE IF NOT EXISTS `autobot_skills` (
  `obj_Id` INT UNSIGNED NOT NULL,
  `skill_id` SMALLINT UNSIGNED NOT NULL,
  `skill_level` TINYINT UNSIGNED NOT NULL,
  `auto_use` TINYINT UNSIGNED DEFAULT 1,
  `priority` TINYINT UNSIGNED DEFAULT 50,
  `conditions` VARCHAR(255) DEFAULT NULL,
  `cooldown_respect` TINYINT UNSIGNED DEFAULT 1,
  PRIMARY KEY (`obj_Id`, `skill_id`),
  FOREIGN KEY (`obj_Id`) REFERENCES `autobots`(`obj_Id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Insert default behavior templates
INSERT IGNORE INTO `autobot_behaviors` (`obj_Id`, `behavior_type`, `combat_prefs`, `social_prefs`, `activity_prefs`) VALUES
(0, 'FIGHTER_TEMPLATE', '{"aggroRange": 800, "attackRange": 150, "useSkills": true, "targetPriority": "CLOSEST"}', '{"randomWalk": true, "chatEnabled": true, "socialDistance": 300}', '{"activityType": "ALWAYS_ACTIVE", "logoutTime": 0, "restTime": 0}'),
(0, 'ARCHER_TEMPLATE', '{"aggroRange": 1000, "attackRange": 600, "useSkills": true, "kitingEnabled": true, "kitingDistance": 200}', '{"randomWalk": true, "chatEnabled": true, "socialDistance": 500}', '{"activityType": "ALWAYS_ACTIVE", "logoutTime": 0, "restTime": 0}'),
(0, 'MAGE_TEMPLATE', '{"aggroRange": 900, "attackRange": 700, "useSkills": true, "preferMagicAttacks": true, "manaThreshold": 30}', '{"randomWalk": true, "chatEnabled": true, "socialDistance": 400}', '{"activityType": "ALWAYS_ACTIVE", "logoutTime": 0, "restTime": 0}');