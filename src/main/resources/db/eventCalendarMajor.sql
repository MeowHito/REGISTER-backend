-- "Major" flag on event-calendar entries: an admin picks headline races (e.g. World Major
-- Marathons) and the nearest upcoming one is shown as the banner on /eventCalendar.
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there);
-- local dev gets this from ddl-auto: create. Re-runnable: MySQL 8.0 has no
-- "ADD COLUMN IF NOT EXISTS", so the step is guarded via information_schema.

SET @addIsMajor := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `eventCalendar` ADD COLUMN `isMajor` bit(1) NOT NULL DEFAULT b''0''',
    'SELECT "eventCalendar.isMajor already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventCalendar' AND COLUMN_NAME = 'isMajor');
PREPARE stmt FROM @addIsMajor; EXECUTE stmt; DEALLOCATE PREPARE stmt;
