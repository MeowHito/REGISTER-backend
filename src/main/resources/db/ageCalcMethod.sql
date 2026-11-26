-- Per-event choice of how a runner's race age is worked out (age groups, orderDetail.age).
-- event.ageCalcMethod: 'BIRTH_YEAR' (race year - birth year) or 'EXACT_DATE' (full years on race day).
-- NULL keeps the original rule (registration year - birth year), so existing events are unchanged.
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there); local dev gets the
-- column from ddl-auto: create. Safe to re-run: guarded via information_schema.

SET @addAgeCalcMethod := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `event` ADD COLUMN `ageCalcMethod` varchar(20) DEFAULT NULL',
    'SELECT "event.ageCalcMethod already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event' AND COLUMN_NAME = 'ageCalcMethod');
PREPARE stmt FROM @addAgeCalcMethod; EXECUTE stmt; DEALLOCATE PREPARE stmt;
