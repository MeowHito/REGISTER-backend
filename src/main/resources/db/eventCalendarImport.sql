-- Event-calendar import from joggingandrunning.com (ImportEventCalendarJob).
-- Adds the columns that mark a row as pulled from another site so re-syncs can
-- upsert by (source, sourceId) instead of duplicating.
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there);
-- local dev gets these from ddl-auto: create. Re-runnable: MySQL 8.0 has no
-- "ADD COLUMN IF NOT EXISTS", so every step is guarded via information_schema.

SET @addSource := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `eventCalendar` ADD COLUMN `source` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL',
    'SELECT "eventCalendar.source already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventCalendar' AND COLUMN_NAME = 'source');
PREPARE stmt FROM @addSource; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @addSourceId := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `eventCalendar` ADD COLUMN `sourceId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL',
    'SELECT "eventCalendar.sourceId already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventCalendar' AND COLUMN_NAME = 'sourceId');
PREPARE stmt FROM @addSourceId; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @addSourceUrl := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `eventCalendar` ADD COLUMN `sourceUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL',
    'SELECT "eventCalendar.sourceUrl already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventCalendar' AND COLUMN_NAME = 'sourceUrl');
PREPARE stmt FROM @addSourceUrl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @addSourceUpdatedAt := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `eventCalendar` ADD COLUMN `sourceUpdatedAt` datetime(6) DEFAULT NULL',
    'SELECT "eventCalendar.sourceUpdatedAt already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventCalendar' AND COLUMN_NAME = 'sourceUpdatedAt');
PREPARE stmt FROM @addSourceUpdatedAt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @addIndex := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `eventCalendar` ADD INDEX `IDX_eventCalendar_source` (`source`, `sourceId`)',
    'SELECT "IDX_eventCalendar_source already exists"')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventCalendar' AND INDEX_NAME = 'IDX_eventCalendar_source');
PREPARE stmt FROM @addIndex; EXECUTE stmt; DEALLOCATE PREPARE stmt;
