-- PDPA disclosure consent (per-event switch + the runner's answer on the order).
-- event.pdpaConsentEnabled: the event shows the consent prompt before registration.
-- orders.pdpaConsent / pdpaConsentAt: true / false when asked, null when the event did not ask.
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there); local dev gets the
-- columns from ddl-auto: create. Safe to re-run: every step is guarded via information_schema.

SET @addEventPdpa := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `event` ADD COLUMN `pdpaConsentEnabled` bit(1) DEFAULT NULL',
    'SELECT "event.pdpaConsentEnabled already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event' AND COLUMN_NAME = 'pdpaConsentEnabled');
PREPARE stmt FROM @addEventPdpa; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @addOrderPdpa := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `orders` ADD COLUMN `pdpaConsent` bit(1) DEFAULT NULL',
    'SELECT "orders.pdpaConsent already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'pdpaConsent');
PREPARE stmt FROM @addOrderPdpa; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @addOrderPdpaAt := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `orders` ADD COLUMN `pdpaConsentAt` datetime(6) DEFAULT NULL',
    'SELECT "orders.pdpaConsentAt already exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'pdpaConsentAt');
PREPARE stmt FROM @addOrderPdpaAt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
