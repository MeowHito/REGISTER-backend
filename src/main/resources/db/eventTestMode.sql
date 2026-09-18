-- Admin-only "test mode" switch on an event: registrations skip the payment
-- gateway and are marked SUCCESS immediately (paymentMethod = 'test').
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there);
-- local dev gets the column from ddl-auto: create. Safe to re-run.

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `event` ADD COLUMN `testMode` bit(1) DEFAULT NULL',
    'SELECT "event.testMode already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'event'
    AND COLUMN_NAME = 'testMode'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
