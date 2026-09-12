-- Add-on packages (accommodation, photo packages, shuttle seats, …) that an
-- admin or organizer attaches to an event, and what buyers picked at checkout.
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there);
-- local dev gets these tables from ddl-auto: create.

CREATE TABLE IF NOT EXISTS `eventAddOn` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nameEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` mediumtext COLLATE utf8mb4_unicode_ci,
  `descriptionEn` mediumtext COLLATE utf8mb4_unicode_ci,
  `category` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `imageUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prefixPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` decimal(38,2) DEFAULT NULL,
  `quota` int DEFAULT NULL,
  `maxPerOrder` int DEFAULT NULL,
  `perApplicant` bit(1) DEFAULT NULL,
  `noteLabel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `noteLabelEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `noteRequired` bit(1) DEFAULT NULL,
  `position` int DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_eventAddOn_uuid` (`uuid`),
  KEY `FK_eventAddOn_createdBy` (`createdBy`),
  KEY `FK_eventAddOn_updatedBy` (`updatedBy`),
  KEY `FK_eventAddOn_eventId` (`eventId`),
  CONSTRAINT `FK_eventAddOn_createdBy` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_eventAddOn_updatedBy` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_eventAddOn_eventId` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `orderAddOn` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nameEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unitPrice` double DEFAULT NULL,
  `qty` int DEFAULT NULL,
  `totalPrice` double DEFAULT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `orderId` int NOT NULL,
  `addOnId` int DEFAULT NULL,
  `orderDetailId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_orderAddOn_uuid` (`uuid`),
  KEY `FK_orderAddOn_createdBy` (`createdBy`),
  KEY `FK_orderAddOn_updatedBy` (`updatedBy`),
  KEY `FK_orderAddOn_orderId` (`orderId`),
  KEY `FK_orderAddOn_addOnId` (`addOnId`),
  KEY `FK_orderAddOn_orderDetailId` (`orderDetailId`),
  CONSTRAINT `FK_orderAddOn_createdBy` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_orderAddOn_updatedBy` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_orderAddOn_orderId` FOREIGN KEY (`orderId`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_orderAddOn_addOnId` FOREIGN KEY (`addOnId`) REFERENCES `eventAddOn` (`id`),
  CONSTRAINT `FK_orderAddOn_orderDetailId` FOREIGN KEY (`orderDetailId`) REFERENCES `orderDetail` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Order-level snapshot of what the add-ons came to, so reports don't have to
-- re-aggregate orderAddOn for every row. MySQL 8.0 has no
-- "ADD COLUMN IF NOT EXISTS", so guard it to keep this file re-runnable.
SET @addColumn := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `orders` ADD COLUMN `addOnTotal` double DEFAULT NULL',
    'SELECT "orders.addOnTotal already exists"'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'addOnTotal'
);
PREPARE stmt FROM @addColumn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
