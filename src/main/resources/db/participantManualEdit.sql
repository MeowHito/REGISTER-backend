-- Back-office manual edits of a participant: who changed the runner last, when, and the full
-- change history (JSON). The participant list highlights rows with manualEditedTime set.
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there);
-- local dev gets the columns from ddl-auto: create. Safe to re-run.

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `orderDetail` ADD COLUMN `manualEditedTime` datetime(6) DEFAULT NULL',
    'SELECT "orderDetail.manualEditedTime already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orderDetail' AND COLUMN_NAME = 'manualEditedTime'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `orderDetail` ADD COLUMN `manualEditedBy` varchar(255) DEFAULT NULL',
    'SELECT "orderDetail.manualEditedBy already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orderDetail' AND COLUMN_NAME = 'manualEditedBy'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `orderDetail` ADD COLUMN `manualEditLog` longtext DEFAULT NULL',
    'SELECT "orderDetail.manualEditLog already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orderDetail' AND COLUMN_NAME = 'manualEditLog'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
