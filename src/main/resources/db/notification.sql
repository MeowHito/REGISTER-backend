-- Back-office bell notifications (new organizer to approve, help requests,
-- orders sent to payment review, new paid orders, event invitations, …).
--
-- Run once against an existing production database (JPA_DDL_AUTO=none there);
-- local dev gets this table from ddl-auto: create. Re-runnable.

CREATE TABLE IF NOT EXISTS `notification` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `link` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `readAt` datetime(6) DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `recipientId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_notification_uuid` (`uuid`),
  KEY `IDX_notification_recipient_read` (`recipientId`, `readAt`),
  KEY `FK_notification_createdBy` (`createdBy`),
  KEY `FK_notification_updatedBy` (`updatedBy`),
  CONSTRAINT `FK_notification_createdBy` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_notification_updatedBy` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_notification_recipientId` FOREIGN KEY (`recipientId`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
