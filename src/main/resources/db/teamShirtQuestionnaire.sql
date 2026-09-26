-- Customer requests, 2026-09: shirt categories (race / finisher / special) with ordering and
-- per-distance visibility, team registration (team size + team pricing), phone country codes,
-- per-event registration field configuration, sponsor questionnaire sections with new answer
-- types, and the admin "may approve organizers" right.
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there) BEFORE the
-- backend is rebuilt; local dev gets everything from ddl-auto: create. Safe to re-run.

CREATE TABLE IF NOT EXISTS `eventQuestionSection` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titleEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logoUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prefixPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `position` int DEFAULT NULL,
  `shareToken` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_eventQuestionSection_uuid` (`uuid`),
  KEY `FK_eventQuestionSection_createdBy` (`createdBy`),
  KEY `FK_eventQuestionSection_updatedBy` (`updatedBy`),
  KEY `FK_eventQuestionSection_eventId` (`eventId`),
  KEY `IDX_eventQuestionSection_shareToken` (`shareToken`),
  CONSTRAINT `FK_eventQuestionSection_createdBy` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_eventQuestionSection_updatedBy` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_eventQuestionSection_eventId` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `orderDetailShirt` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `orderDetailId` int NOT NULL,
  `shirtTypeId` int DEFAULT NULL,
  `shirtSizeId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_orderDetailShirt_uuid` (`uuid`),
  KEY `FK_orderDetailShirt_createdBy` (`createdBy`),
  KEY `FK_orderDetailShirt_updatedBy` (`updatedBy`),
  KEY `FK_orderDetailShirt_orderDetailId` (`orderDetailId`),
  KEY `FK_orderDetailShirt_shirtTypeId` (`shirtTypeId`),
  KEY `FK_orderDetailShirt_shirtSizeId` (`shirtSizeId`),
  CONSTRAINT `FK_orderDetailShirt_createdBy` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_orderDetailShirt_updatedBy` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_orderDetailShirt_orderDetailId` FOREIGN KEY (`orderDetailId`) REFERENCES `orderDetail` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_orderDetailShirt_shirtTypeId` FOREIGN KEY (`shirtTypeId`) REFERENCES `shirtType` (`id`),
  CONSTRAINT `FK_orderDetailShirt_shirtSizeId` FOREIGN KEY (`shirtSizeId`) REFERENCES `shirtSize` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `shirtType` ADD COLUMN `category` varchar(20) DEFAULT ''RACE''',
    'SELECT "shirtType.category already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shirtType' AND COLUMN_NAME = 'category'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `shirtType` ADD COLUMN `position` int DEFAULT NULL',
    'SELECT "shirtType.position already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shirtType' AND COLUMN_NAME = 'position'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `shirtType` ADD COLUMN `eventTypeIds` varchar(2000) DEFAULT NULL',
    'SELECT "shirtType.eventTypeIds already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shirtType' AND COLUMN_NAME = 'eventTypeIds'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `shirtSize` ADD COLUMN `position` int DEFAULT NULL',
    'SELECT "shirtSize.position already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shirtSize' AND COLUMN_NAME = 'position'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `eventType` ADD COLUMN `teamSize` int DEFAULT NULL',
    'SELECT "eventType.teamSize already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventType' AND COLUMN_NAME = 'teamSize'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `eventType` ADD COLUMN `teamPricing` varchar(20) DEFAULT NULL',
    'SELECT "eventType.teamPricing already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventType' AND COLUMN_NAME = 'teamPricing'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `event` ADD COLUMN `fieldConfig` longtext DEFAULT NULL',
    'SELECT "event.fieldConfig already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event' AND COLUMN_NAME = 'fieldConfig'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `eventSelectionField` ADD COLUMN `sectionId` int DEFAULT NULL',
    'SELECT "eventSelectionField.sectionId already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventSelectionField' AND COLUMN_NAME = 'sectionId'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `eventSelectionField` ADD COLUMN `position` int DEFAULT NULL',
    'SELECT "eventSelectionField.position already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventSelectionField' AND COLUMN_NAME = 'position'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `orderDetail` ADD COLUMN `phoneCountryCode` varchar(8) DEFAULT NULL',
    'SELECT "orderDetail.phoneCountryCode already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orderDetail' AND COLUMN_NAME = 'phoneCountryCode'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `orderDetail` ADD COLUMN `emergencyPhoneCountryCode` varchar(8) DEFAULT NULL',
    'SELECT "orderDetail.emergencyPhoneCountryCode already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orderDetail' AND COLUMN_NAME = 'emergencyPhoneCountryCode'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `orderDetail` ADD COLUMN `teamGroup` int DEFAULT NULL',
    'SELECT "orderDetail.teamGroup already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orderDetail' AND COLUMN_NAME = 'teamGroup'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `user` ADD COLUMN `phoneCountryCode` varchar(8) DEFAULT NULL',
    'SELECT "user.phoneCountryCode already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'phoneCountryCode'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `user` ADD COLUMN `emergencyPhoneCountryCode` varchar(8) DEFAULT NULL',
    'SELECT "user.emergencyPhoneCountryCode already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'emergencyPhoneCountryCode'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addColumn = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `user` ADD COLUMN `canApproveOrganizer` bit(1) DEFAULT NULL',
    'SELECT "user.canApproveOrganizer already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'canApproveOrganizer'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addFk = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `eventSelectionField` ADD CONSTRAINT `FK_eventSelectionField_sectionId` FOREIGN KEY (`sectionId`) REFERENCES `eventQuestionSection` (`id`)',
    'SELECT "FK_eventSelectionField_sectionId already exists"'
  )
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eventSelectionField' AND CONSTRAINT_NAME = 'FK_eventSelectionField_sectionId'
);
PREPARE stmt FROM @addFk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Existing styles are race shirts; existing rows keep their current order.
UPDATE `shirtType` SET `category` = 'RACE' WHERE `category` IS NULL OR `category` = '';
UPDATE `shirtType` SET `position` = `id` WHERE `position` IS NULL;
UPDATE `shirtSize` SET `position` = `id` WHERE `position` IS NULL;
UPDATE `eventType` SET `teamPricing` = 'PER_PERSON' WHERE `isTeam` = 1 AND `teamPricing` IS NULL;

-- Every current admin keeps the right to approve organizers, so nobody is locked out; take it
-- away per person from the user list afterwards.
UPDATE `user` u
  JOIN `role` r ON r.id = u.roleId
  SET u.canApproveOrganizer = 1
  WHERE r.roleType = 'admin' AND u.canApproveOrganizer IS NULL;
