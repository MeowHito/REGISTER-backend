-- Event condition popup text.
-- eventCondition.detail: long text shown in a popup when the runner taps the {braced} part of a
-- condition on the confirm-before-payment step. Null = the condition has no popup.
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there); local dev gets the
-- column from ddl-auto: create. Safe to re-run: guarded via information_schema.

SET @addConditionDetail := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `eventCondition` ADD COLUMN `detail` text DEFAULT NULL',
    'SELECT "eventCondition.detail already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventCondition' AND COLUMN_NAME = 'detail');
PREPARE stmt FROM @addConditionDetail; EXECUTE stmt; DEALLOCATE PREPARE stmt;
