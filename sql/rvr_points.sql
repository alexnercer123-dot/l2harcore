-- ========================================
-- RvR (Race vs Race) Points System
-- ========================================

-- Table for storing RvR points for each race
CREATE TABLE IF NOT EXISTS `rvr_points` (
  `race` varchar(32) NOT NULL DEFAULT '',
  `points` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`race`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Initialize default values for player races
INSERT INTO `rvr_points` (`race`, `points`) VALUES
('HUMAN', 0),
('ELF', 0),
('DARK_ELF', 0),
('ORC', 0),
('DWARF', 0),
('KAMAEL', 0),
('ERTHEIA', 0)
ON DUPLICATE KEY UPDATE `points` = VALUES(`points`);