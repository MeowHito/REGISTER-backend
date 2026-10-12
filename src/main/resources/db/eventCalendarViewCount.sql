-- View counter on event-calendar entries: bumped each time a visitor opens an entry's link
-- from /eventCalendar (card or Major banner); shown to admins in /eventCalendarList and on
-- the public calendar.
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there);
-- local dev gets this from ddl-auto: create. Re-runnable: MySQL 8.0 has no
-- "ADD COLUMN IF NOT EXISTS", so the step is guarded via information_schema.

SET @addViewCount := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `eventCalendar` ADD COLUMN `viewCount` int NOT NULL DEFAULT 0',
    'SELECT "eventCalendar.viewCount already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventCalendar' AND COLUMN_NAME = 'viewCount');
PREPARE stmt FROM @addViewCount; EXECUTE stmt; DEALLOCATE PREPARE stmt;
