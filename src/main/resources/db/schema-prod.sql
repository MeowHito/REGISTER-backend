-- Production DB schema for membership_db (structure only, no data).
-- Generated from the JPA entities (53 tables: app + Quartz QRTZ_*).
-- First production deploy on an EMPTY database:
--   1) CREATE DATABASE membership_db;
--   2) mysql ... membership_db < schema-prod.sql
--   3) keep JPA_DDL_AUTO=none (or validate); the app seeds menus/roles/admin on startup.
-- (This already INCLUDES the Quartz tables, so you do NOT also need quartz_tables.sql.)

USE membership_db;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_BLOB_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `BLOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_CALENDARS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `CALENDAR_NAME` varchar(200) NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_CRON_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `CRON_EXPRESSION` varchar(120) NOT NULL,
  `TIME_ZONE_ID` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_FIRED_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `ENTRY_ID` varchar(95) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `INSTANCE_NAME` varchar(200) NOT NULL,
  `FIRED_TIME` bigint NOT NULL,
  `SCHED_TIME` bigint NOT NULL,
  `PRIORITY` int NOT NULL,
  `STATE` varchar(16) NOT NULL,
  `JOB_NAME` varchar(200) DEFAULT NULL,
  `JOB_GROUP` varchar(200) DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`),
  KEY `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
  KEY `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_JOB_DETAILS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) NOT NULL,
  `IS_DURABLE` varchar(1) NOT NULL,
  `IS_NONCONCURRENT` varchar(1) NOT NULL,
  `IS_UPDATE_DATA` varchar(1) NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) NOT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `QRTZ_LOCKS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_LOCKS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `LOCK_NAME` varchar(40) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_SCHEDULER_STATE` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `INSTANCE_NAME` varchar(200) NOT NULL,
  `LAST_CHECKIN_TIME` bigint NOT NULL,
  `CHECKIN_INTERVAL` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `REPEAT_COUNT` bigint NOT NULL,
  `REPEAT_INTERVAL` bigint NOT NULL,
  `TIMES_TRIGGERED` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `STR_PROP_1` varchar(512) DEFAULT NULL,
  `STR_PROP_2` varchar(512) DEFAULT NULL,
  `STR_PROP_3` varchar(512) DEFAULT NULL,
  `INT_PROP_1` int DEFAULT NULL,
  `INT_PROP_2` int DEFAULT NULL,
  `LONG_PROP_1` bigint DEFAULT NULL,
  `LONG_PROP_2` bigint DEFAULT NULL,
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL,
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint DEFAULT NULL,
  `PREV_FIRE_TIME` bigint DEFAULT NULL,
  `PRIORITY` int DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint NOT NULL,
  `END_TIME` bigint DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) DEFAULT NULL,
  `MISFIRE_INSTR` smallint DEFAULT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
  KEY `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ageGroup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ageGroup` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `gender` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `maxAge` int DEFAULT NULL,
  `minAge` int DEFAULT NULL,
  `position` int DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventTypeId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ganc1rylu9ki8y0jb62v2252o` (`uuid`),
  KEY `FKkxtg5771xu72vw8o6kk8qwcq1` (`createdBy`),
  KEY `FKokhjojwh40dbhvjucwtcddp0l` (`updatedBy`),
  KEY `FKh8nob118ahbrkhucydiw34iwf` (`eventTypeId`),
  CONSTRAINT `FKh8nob118ahbrkhucydiw34iwf` FOREIGN KEY (`eventTypeId`) REFERENCES `eventType` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKkxtg5771xu72vw8o6kk8qwcq1` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKokhjojwh40dbhvjucwtcddp0l` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` mediumtext COLLATE utf8mb4_unicode_ci,
  `isRead` bit(1) DEFAULT NULL,
  `prefixPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `startDate` datetime(6) DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_81rux23cabgq2myk8ormf1xfl` (`uuid`),
  KEY `FKh6q35qivf0bkuu6fgfwq3nr40` (`createdBy`),
  KEY `FKcmosqjunjnf8nbrcv8lhx73nc` (`updatedBy`),
  KEY `FK8stog25vphhov3j0k6la1k97m` (`eventId`),
  CONSTRAINT `FK8stog25vphhov3j0k6la1k97m` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`),
  CONSTRAINT `FKcmosqjunjnf8nbrcv8lhx73nc` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKh6q35qivf0bkuu6fgfwq3nr40` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `appConfig`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `appConfig` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_syj2h34gifb2m1njtjkgip6i8` (`uuid`),
  KEY `FKp9s5yaagh2o6h33hb0dmywy9u` (`createdBy`),
  KEY `FKl4an1ftphlhjg2evnm6r8v5g4` (`updatedBy`),
  CONSTRAINT `FKl4an1ftphlhjg2evnm6r8v5g4` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKp9s5yaagh2o6h33hb0dmywy9u` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `appErrorLog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `appErrorLog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `clientIp` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `clientTimestamp` datetime(6) DEFAULT NULL,
  `context` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `httpMethod` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `httpStatus` int DEFAULT NULL,
  `level` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `logId` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` text COLLATE utf8mb4_unicode_ci,
  `meta` text COLLATE utf8mb4_unicode_ci,
  `requestData` text COLLATE utf8mb4_unicode_ci,
  `responseData` text COLLATE utf8mb4_unicode_ci,
  `sessionId` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stack` text COLLATE utf8mb4_unicode_ci,
  `statusText` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `userAgent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `userId` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contact` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` mediumtext COLLATE utf8mb4_unicode_ci,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_4fwve81luv6kmesfif3w9174c` (`uuid`),
  KEY `FKok3465jh55phhapuk8snmrc9u` (`createdBy`),
  KEY `FKodbal9qsbk6vrmb5pwere9tni` (`updatedBy`),
  CONSTRAINT `FKodbal9qsbk6vrmb5pwere9tni` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKok3465jh55phhapuk8snmrc9u` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `contract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contract` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `accountName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `accountNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amphoe` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bankAccountPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bankbook` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `certificatePath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contractDate` datetime(6) DEFAULT NULL,
  `contractPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customerName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customerPosition` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customerSeal` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customerSignature` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detail` mediumtext COLLATE utf8mb4_unicode_ci,
  `district` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `endDate` datetime(6) DEFAULT NULL,
  `idCardPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isUploadContract` bit(1) DEFAULT NULL,
  `organizerName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `otherDocumentPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `powerOfAttorneyPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pp20Path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prefixPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `providerName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `providerPosition` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `providerSeal` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `providerSignature` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `province` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `runNo` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `startDate` datetime(6) DEFAULT NULL,
  `taxNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `zipcode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_1nnvsu9af5w0bwm7kj4mrrbpo` (`uuid`),
  UNIQUE KEY `UK_jgrlc2e4ga3hjvwr1i0je3lk3` (`runNo`),
  KEY `FK8rkqmbfu7qoj8enth5916933s` (`createdBy`),
  KEY `FKip23q293x9dq2a3oy8e3sdjit` (`updatedBy`),
  KEY `FKc4cpb4gmiwx5tyhtiy60g42v0` (`eventId`),
  CONSTRAINT `FK8rkqmbfu7qoj8enth5916933s` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKc4cpb4gmiwx5tyhtiy60g42v0` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`),
  CONSTRAINT `FKip23q293x9dq2a3oy8e3sdjit` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `contractLog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contractLog` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `accountName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `accountNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actionType` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amphoe` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bankAccountPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bankbook` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `certificatePath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contractDate` datetime(6) DEFAULT NULL,
  `contractId` int DEFAULT NULL,
  `contractPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customerName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customerPosition` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customerSeal` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customerSignature` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detail` mediumtext COLLATE utf8mb4_unicode_ci,
  `district` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `endDate` datetime(6) DEFAULT NULL,
  `eventId` int DEFAULT NULL,
  `idCardPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isUploadContract` bit(1) DEFAULT NULL,
  `organizerName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `otherDocumentPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `powerOfAttorneyPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pp20Path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prefixPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `providerName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `providerPosition` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `providerSeal` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `providerSignature` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `province` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `runNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `startDate` datetime(6) DEFAULT NULL,
  `taxNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `zipcode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_rr9qkocw2j4k4dikme5u1p1hp` (`uuid`),
  KEY `FKsmrwya6rl8o73lrt1dul37btt` (`createdBy`),
  KEY `FKbxfrr50t7dhgujee6oy18tefm` (`updatedBy`),
  CONSTRAINT `FKbxfrr50t7dhgujee6oy18tefm` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKsmrwya6rl8o73lrt1dul37btt` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `countryState`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `countryState` (
  `id` int NOT NULL,
  `countryEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `countryLocal` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stateEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stateLocal` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stateType` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `bucketName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `couponCode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `couponName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deductionPercentage` bigint DEFAULT NULL,
  `expiryTime` datetime(6) DEFAULT NULL,
  `limitCoupon` int DEFAULT NULL,
  `oldEventName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `redeemTime` datetime(6) DEFAULT NULL,
  `runnerIdNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `startTime` datetime(6) DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int DEFAULT NULL,
  `oldEventId` int DEFAULT NULL,
  `redeemBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_2o685cnfrct53beb9mhfk00bs` (`uuid`),
  KEY `FKt5nfvinfn55le51ew25cpr3mt` (`createdBy`),
  KEY `FKnt3xvb6mp5ci4xt5rfhl42s0t` (`updatedBy`),
  KEY `FKnellffp1fg5ojdr432093l1lv` (`eventId`),
  KEY `FKrgwapiblmk8wdsexh10rle44x` (`oldEventId`),
  KEY `FK5a9hfhl9bbd7jncjboio1bygh` (`redeemBy`),
  CONSTRAINT `FK5a9hfhl9bbd7jncjboio1bygh` FOREIGN KEY (`redeemBy`) REFERENCES `orderDetail` (`id`),
  CONSTRAINT `FKnellffp1fg5ojdr432093l1lv` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`),
  CONSTRAINT `FKnt3xvb6mp5ci4xt5rfhl42s0t` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKrgwapiblmk8wdsexh10rle44x` FOREIGN KEY (`oldEventId`) REFERENCES `event` (`id`),
  CONSTRAINT `FKt5nfvinfn55le51ew25cpr3mt` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `emailLog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emailLog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attachmentCount` int DEFAULT NULL,
  `attachmentPath` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `emailBody` mediumtext COLLATE utf8mb4_unicode_ci,
  `errorMessage` text COLLATE utf8mb4_unicode_ci,
  `hasAttachments` bit(1) DEFAULT NULL,
  `orderId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recipientCc` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recipientTo` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `retryCount` int DEFAULT NULL,
  `sendStatus` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sentAt` datetime(6) DEFAULT NULL,
  `subject` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `emailLogAttachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emailLogAttachment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contentType` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fileKey` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `prefix` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emailLogId` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKo7ilugy6vhuu4br5pq6cobbw8` (`emailLogId`),
  CONSTRAINT `FKo7ilugy6vhuu4br5pq6cobbw8` FOREIGN KEY (`emailLogId`) REFERENCES `emailLog` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `emailQueue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emailQueue` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `emailLogId` bigint DEFAULT NULL,
  `errorMessage` text COLLATE utf8mb4_unicode_ci,
  `processedAt` datetime(6) DEFAULT NULL,
  `recipientEmail` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `retryCount` int DEFAULT NULL,
  `scheduledAt` datetime(6) DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `orderId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_kf9qdlou33lfyj4x9ntehj6rh` (`uuid`),
  KEY `FKsm5kh6ky0qjh2tcom1qwtpem0` (`createdBy`),
  KEY `FKt06lsk4jlmjvupe9so5q0ksyi` (`updatedBy`),
  KEY `FK6qy3jc7dsv2doq71pos9p6x3e` (`orderId`),
  CONSTRAINT `FK6qy3jc7dsv2doq71pos9p6x3e` FOREIGN KEY (`orderId`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKsm5kh6ky0qjh2tcom1qwtpem0` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKt06lsk4jlmjvupe9so5q0ksyi` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` mediumtext COLLATE utf8mb4_unicode_ci,
  `endRegistrationDate` datetime(6) DEFAULT NULL,
  `eventDate` datetime(6) DEFAULT NULL,
  `eventFontColor` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `eventPrimaryColor` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `eventSecondaryColor` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `eventTypeTitle` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `generalInfoTitle` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isDraft` bit(1) DEFAULT NULL,
  `link` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logoUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `organizerName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pictureUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prefixPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingFee` decimal(19,2) DEFAULT NULL,
  `showChecklist` bit(1) DEFAULT NULL,
  `startRegistrationDate` datetime(6) DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `organizerId` int DEFAULT NULL,
  `provinceId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_i0eo1d5d5i9q5gyawl5e9q44c` (`uuid`),
  UNIQUE KEY `UK_hn7hyivcqo3ma7sylmdyeypqn` (`link`),
  KEY `FK81n6uxloedayw1borjm866sph` (`createdBy`),
  KEY `FKduex4j7celt6rbammo12mtg21` (`updatedBy`),
  KEY `FKf3vv8kvyvdcsaw82893vn3uy5` (`organizerId`),
  CONSTRAINT `FK81n6uxloedayw1borjm866sph` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKduex4j7celt6rbammo12mtg21` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKf3vv8kvyvdcsaw82893vn3uy5` FOREIGN KEY (`organizerId`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eventCalendar`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventCalendar` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `eventDate` datetime(6) DEFAULT NULL,
  `eventName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `eventType` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `extraDetail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isApproved` bit(1) DEFAULT NULL,
  `link` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rejectReason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `submitterName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_j8hrdq64oj934ps1kw0mmtd6e` (`uuid`),
  KEY `FK6pwixw44jh0vmq11atdug3qdp` (`createdBy`),
  KEY `FKd1aitpioremubhxn9dymqxgye` (`updatedBy`),
  CONSTRAINT `FK6pwixw44jh0vmq11atdug3qdp` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKd1aitpioremubhxn9dymqxgye` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eventCondition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventCondition` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_9v3glb0n47n8oj89p1ardnnvm` (`uuid`),
  KEY `FKgca5igeb9khuxcfd273h4gaob` (`createdBy`),
  KEY `FK84mfa5rwr6gj7dd2qur3ten73` (`updatedBy`),
  KEY `FKe6iy7mrl7nj9u70igf4mcf8h` (`eventId`),
  CONSTRAINT `FK84mfa5rwr6gj7dd2qur3ten73` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKe6iy7mrl7nj9u70igf4mcf8h` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKgca5igeb9khuxcfd273h4gaob` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eventDetail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventDetail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` mediumtext COLLATE utf8mb4_unicode_ci,
  `position` int DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_qbc1e5etpgb1dwl6n1j64tcih` (`uuid`),
  KEY `FKma828cr3ew884o7gsmqrcnp6x` (`createdBy`),
  KEY `FKte3uw1mtj7ckvkha3nlg2vw0d` (`updatedBy`),
  KEY `FKq0j1p8u4wo4t2w6gy1ygtwsih` (`eventId`),
  CONSTRAINT `FKma828cr3ew884o7gsmqrcnp6x` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKq0j1p8u4wo4t2w6gy1ygtwsih` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKte3uw1mtj7ckvkha3nlg2vw0d` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eventInvitation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventInvitation` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `acceptedAt` datetime(6) DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expiresAt` datetime(6) NOT NULL,
  `invitedByName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `acceptedByUserId` int DEFAULT NULL,
  `eventId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_1l4691vprqownm4e6js29g937` (`uuid`),
  UNIQUE KEY `UK_gwau5i5rtftmqwaa9r8pufmga` (`token`),
  KEY `FK1vc78pyg415iva0492kljr4ig` (`createdBy`),
  KEY `FK2qjbw4ws2y2qo42awceukk3gn` (`updatedBy`),
  KEY `FK8ba1302mr0w85udelbr323syh` (`acceptedByUserId`),
  KEY `FK81tx2519yjnam0eepmx7i216o` (`eventId`),
  CONSTRAINT `FK1vc78pyg415iva0492kljr4ig` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK2qjbw4ws2y2qo42awceukk3gn` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK81tx2519yjnam0eepmx7i216o` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK8ba1302mr0w85udelbr323syh` FOREIGN KEY (`acceptedByUserId`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eventPermission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventPermission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `canDelete` bit(1) NOT NULL,
  `canRead` bit(1) NOT NULL,
  `canUpdate` bit(1) NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int NOT NULL,
  `userId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_24t28ovfh8tm5x02nc8qiu5k0` (`uuid`),
  KEY `FK9xf6iko5c6dxeb6kid5if5tgv` (`createdBy`),
  KEY `FK2up2wo3iyo31icx330xxas4wb` (`updatedBy`),
  KEY `FKo8tvfopdkausy5ppb24kyvllv` (`eventId`),
  KEY `FK7twg3g0rbawv9bsbt07hh2ice` (`userId`),
  CONSTRAINT `FK2up2wo3iyo31icx330xxas4wb` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK7twg3g0rbawv9bsbt07hh2ice` FOREIGN KEY (`userId`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK9xf6iko5c6dxeb6kid5if5tgv` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKo8tvfopdkausy5ppb24kyvllv` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eventSelectionField`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventSelectionField` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `required` bit(1) NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titleEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int DEFAULT NULL,
  `eventTypeId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_i0qn6ldmbqvov763xmtfe7gh3` (`uuid`),
  KEY `FK2ravq6f94qc3flfnfipsaor4t` (`createdBy`),
  KEY `FK36aoifjwq1j09mahist1rirma` (`updatedBy`),
  KEY `FKrympi02l1om073n12swfoe376` (`eventId`),
  KEY `FKrrur7s2227miakg5vkg4ivlry` (`eventTypeId`),
  CONSTRAINT `FK2ravq6f94qc3flfnfipsaor4t` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK36aoifjwq1j09mahist1rirma` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKrrur7s2227miakg5vkg4ivlry` FOREIGN KEY (`eventTypeId`) REFERENCES `eventType` (`id`),
  CONSTRAINT `FKrympi02l1om073n12swfoe376` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eventSelectionOption`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventSelectionOption` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `inputType` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `position` int NOT NULL,
  `value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `valueEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `selectionFieldId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_3h4gvqe7i6ms8wpposqbclxa2` (`uuid`),
  KEY `FKgj8uy37qtxyk4ijpteqiqel4x` (`createdBy`),
  KEY `FKik03sd0ibswg8hpqf2usxy7vu` (`updatedBy`),
  KEY `FKi5h8gkco06tco9pd73q01opcs` (`selectionFieldId`),
  CONSTRAINT `FKgj8uy37qtxyk4ijpteqiqel4x` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKi5h8gkco06tco9pd73q01opcs` FOREIGN KEY (`selectionFieldId`) REFERENCES `eventSelectionField` (`id`),
  CONSTRAINT `FKik03sd0ibswg8hpqf2usxy7vu` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eventType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventType` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discountNoShirt` decimal(19,2) DEFAULT NULL,
  `eventDate` datetime(6) DEFAULT NULL,
  `isNoShirt` bit(1) DEFAULT NULL,
  `isTeam` bit(1) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` decimal(19,2) DEFAULT NULL,
  `quota` int DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_nt3y08jvgbeg3i7k6n01h9yd5` (`uuid`),
  KEY `FKtnilyqm2uo6c6l4j1aldgk5x1` (`createdBy`),
  KEY `FKlvpl6835iwk04aprvb3gl7mj8` (`updatedBy`),
  KEY `FKadewlp4h4jxohnerxgoe8asvi` (`eventId`),
  CONSTRAINT `FKadewlp4h4jxohnerxgoe8asvi` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKlvpl6835iwk04aprvb3gl7mj8` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKtnilyqm2uo6c6l4j1aldgk5x1` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `geoDistricts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `geoDistricts` (
  `code` int NOT NULL,
  `active` bit(1) DEFAULT NULL,
  `nameEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nameTh` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provinceCode` int DEFAULT NULL,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `geoProvinces`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `geoProvinces` (
  `code` int NOT NULL,
  `active` bit(1) DEFAULT NULL,
  `nameEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nameTh` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `geoSubdistricts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `geoSubdistricts` (
  `code` int NOT NULL,
  `active` bit(1) DEFAULT NULL,
  `districtCode` int DEFAULT NULL,
  `nameEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nameTh` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `postalCode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `helpRequest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `helpRequest` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `adminNote` text COLLATE utf8mb4_unicode_ci,
  `attachmentUrl` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` text COLLATE utf8mb4_unicode_ci,
  `orderUuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_lswtsr9p1e3yeh0qmtvc2opw8` (`uuid`),
  KEY `FK9yb5gvifl2s2a52riwbof7f6d` (`createdBy`),
  KEY `FK5s98ijf468c2y6qa51rder588` (`updatedBy`),
  CONSTRAINT `FK5s98ijf468c2y6qa51rder588` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK9yb5gvifl2s2a52riwbof7f6d` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `jobExecutionLog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jobExecutionLog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `completedTime` datetime(6) DEFAULT NULL,
  `createdAt` datetime(6) NOT NULL,
  `durationMs` bigint DEFAULT NULL,
  `errorMessage` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `firedTime` datetime(6) NOT NULL,
  `instanceName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `jobGroup` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `jobName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` int DEFAULT NULL,
  `scheduledTime` datetime(6) NOT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `triggerGroup` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `triggerName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `mediaFile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mediaFile` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `path` mediumtext COLLATE utf8mb4_unicode_ci,
  `prefixPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refId` int DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_3clbotmwpy5wdsgbcdbyyiyjk` (`uuid`),
  KEY `FKcpsf0jvg8svfa0vhjykjdfwdu` (`createdBy`),
  KEY `FKk6jlrw6508ffw5eppquoim083` (`updatedBy`),
  CONSTRAINT `FKcpsf0jvg8svfa0vhjykjdfwdu` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKk6jlrw6508ffw5eppquoim083` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menu` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `badgeKey` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `disabled` bit(1) DEFAULT NULL,
  `icon` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isDisplay` bit(1) DEFAULT NULL,
  `isNoti` bit(1) DEFAULT NULL,
  `path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `position` int DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_fw3co259lcog5dgu4secpii02` (`uuid`),
  KEY `FKpjk7j73a9rd58kpsxoho7ah1h` (`createdBy`),
  KEY `FKo4vbfej6npbs50dqtl0h1yra5` (`updatedBy`),
  CONSTRAINT `FKo4vbfej6npbs50dqtl0h1yra5` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKpjk7j73a9rd58kpsxoho7ah1h` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `orderDetail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orderDetail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `age` int DEFAULT NULL,
  `amphoe` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bibNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `birthDate` datetime(6) DEFAULT NULL,
  `bloodType` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `couponDiscount` double DEFAULT NULL,
  `couponUsed` bit(1) DEFAULT NULL,
  `deliveryMethod` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `discountShirt` double DEFAULT NULL,
  `district` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emergencyContact` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emergencyPhone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emergencyRelation` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `firstName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `firstNameEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `healthIssues` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isSelf` bit(1) DEFAULT NULL,
  `lastName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastNameEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nationality` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `netPrice` double DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pictureUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prefixPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` double DEFAULT NULL,
  `province` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receiveShirt` bit(1) DEFAULT NULL,
  `rules` bit(1) DEFAULT NULL,
  `selectionAnswers` json DEFAULT NULL,
  `shippingAddress` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingAmphoe` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingDistrict` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingFee` double DEFAULT NULL,
  `shippingProvince` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingZipcode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `teamClub` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `zipcode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventTypeId` int DEFAULT NULL,
  `orderId` int NOT NULL,
  `pricingId` int DEFAULT NULL,
  `shirtSizeId` int DEFAULT NULL,
  `shirtTypeId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_jc9u4vwy71vkk96ky43frvp9l` (`uuid`),
  KEY `FK6kaxrky7cbtv8uqhp1sgp7d8d` (`createdBy`),
  KEY `FKhkq4yfmrqe8y6ng45hmdc7t7k` (`updatedBy`),
  KEY `FKg9idhp1q5q0f54lp58eteyyau` (`eventTypeId`),
  KEY `FKb51qdl923yoolek2tjtyati00` (`orderId`),
  KEY `FKq32m28a9kxitl89o40v35rbbi` (`pricingId`),
  KEY `FK3wg4p90pq7671d7ukvm9ipe4n` (`shirtSizeId`),
  KEY `FK9a7pl39du11bhfyd4u37vr43` (`shirtTypeId`),
  CONSTRAINT `FK3wg4p90pq7671d7ukvm9ipe4n` FOREIGN KEY (`shirtSizeId`) REFERENCES `shirtSize` (`id`),
  CONSTRAINT `FK6kaxrky7cbtv8uqhp1sgp7d8d` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK9a7pl39du11bhfyd4u37vr43` FOREIGN KEY (`shirtTypeId`) REFERENCES `shirtType` (`id`),
  CONSTRAINT `FKb51qdl923yoolek2tjtyati00` FOREIGN KEY (`orderId`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKg9idhp1q5q0f54lp58eteyyau` FOREIGN KEY (`eventTypeId`) REFERENCES `eventType` (`id`),
  CONSTRAINT `FKhkq4yfmrqe8y6ng45hmdc7t7k` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKq32m28a9kxitl89o40v35rbbi` FOREIGN KEY (`pricingId`) REFERENCES `pricing` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `orderRequestLog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orderRequestLog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `clientIp` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correlationId` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `detailsCount` int DEFAULT NULL,
  `errorCode` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `errorMessage` text COLLATE utf8mb4_unicode_ci,
  `eventId` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `orderNo` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processingTimeMs` bigint DEFAULT NULL,
  `requestBody` text COLLATE utf8mb4_unicode_ci,
  `requestType` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cancelledBy` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelledDateTime` datetime(6) DEFAULT NULL,
  `correctionEmailSent` bit(1) DEFAULT NULL,
  `coupon` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `couponDiscount` double DEFAULT NULL,
  `discountShirt` double DEFAULT NULL,
  `fee` double DEFAULT NULL,
  `feePercent` double DEFAULT NULL,
  `orderNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `paymentDateTime` datetime(6) DEFAULT NULL,
  `paymentDueDatetime` datetime(6) DEFAULT NULL,
  `paymentMethod` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `paymentStatus` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `paymentToken` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qty` int DEFAULT NULL,
  `refNo2` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refNo3` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewReason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scbTransactionId` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingFee` double DEFAULT NULL,
  `tokenExpireAt` datetime(6) DEFAULT NULL,
  `totalAmountWithFee` double DEFAULT NULL,
  `totalPrice` double DEFAULT NULL,
  `unitPrice` double DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_8trmqe3eqy2ut1xr2i2atcaip` (`uuid`),
  UNIQUE KEY `UK_qid14htug2oktfd0sft587nut` (`orderNo`),
  KEY `FKrefj1yj28vawjqo3j8o17w3r9` (`createdBy`),
  KEY `FKlo7y68gxov57wbl30n55uj6ri` (`updatedBy`),
  KEY `FKqddowj599f9ognmeux5v6y2f9` (`eventId`),
  CONSTRAINT `FKlo7y68gxov57wbl30n55uj6ri` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKqddowj599f9ognmeux5v6y2f9` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`),
  CONSTRAINT `FKrefj1yj28vawjqo3j8o17w3r9` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `paymentType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `paymentType` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `endDate` datetime(6) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_e9hsuxj7723j4qdvbrly0c9bb` (`uuid`),
  KEY `FKjg3krc6786lhda6hraraxnidy` (`createdBy`),
  KEY `FKiuj08w64sld4e2sxlyffn0l1h` (`updatedBy`),
  KEY `FKg5oujur280xa9uq5gfvt9t7r4` (`eventId`),
  CONSTRAINT `FKg5oujur280xa9uq5gfvt9t7r4` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKiuj08w64sld4e2sxlyffn0l1h` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKjg3krc6786lhda6hraraxnidy` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `paymentWebhookLog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `paymentWebhookLog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` double DEFAULT NULL,
  `currency` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `logType` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `orderId` int DEFAULT NULL,
  `orderNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payloadJson` longtext COLLATE utf8mb4_unicode_ci,
  `paymentProvider` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `paymentStatusAtWebhookTime` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reasonType` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receivedDateTime` datetime(6) NOT NULL,
  `transactionId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `canCreate` bit(1) NOT NULL,
  `canDelete` bit(1) NOT NULL,
  `canRead` bit(1) NOT NULL,
  `canUpdate` bit(1) NOT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `menuId` int NOT NULL,
  `roleId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_demnsbulyf6plpowb2cfn1gen` (`uuid`),
  KEY `FK1thlq4d8h2nmmqxqy6opj82xl` (`createdBy`),
  KEY `FKg1teefif8heuiivr3p2wu5xnr` (`updatedBy`),
  KEY `FKmesapqxyy5tgc3dul2lbl8xm0` (`menuId`),
  KEY `FK6tifald0nadxb7a7stta4c0pu` (`roleId`),
  CONSTRAINT `FK1thlq4d8h2nmmqxqy6opj82xl` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK6tifald0nadxb7a7stta4c0pu` FOREIGN KEY (`roleId`) REFERENCES `role` (`id`),
  CONSTRAINT `FKg1teefif8heuiivr3p2wu5xnr` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKmesapqxyy5tgc3dul2lbl8xm0` FOREIGN KEY (`menuId`) REFERENCES `menu` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pricing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pricing` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(19,2) DEFAULT NULL,
  `quota` int DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventTypeId` int NOT NULL,
  `paymentTypeId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_g9b6ji6ipe1211ui06cog3l9d` (`uuid`),
  KEY `FKo6bfe5c4fwtm1h6xnxsk3wc86` (`createdBy`),
  KEY `FK4ki2cls6ja5vxgg0a8d6f6um8` (`updatedBy`),
  KEY `FKgoj1d06kmmrk15f11tkwt7ls8` (`eventTypeId`),
  KEY `FK54t0x5cc6ohbmlhc1x9hyug8r` (`paymentTypeId`),
  CONSTRAINT `FK4ki2cls6ja5vxgg0a8d6f6um8` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FK54t0x5cc6ohbmlhc1x9hyug8r` FOREIGN KEY (`paymentTypeId`) REFERENCES `paymentType` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKgoj1d06kmmrk15f11tkwt7ls8` FOREIGN KEY (`eventTypeId`) REFERENCES `eventType` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKo6bfe5c4fwtm1h6xnxsk3wc86` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `roleType` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_k5dwya5n8n7y3m2opvmm7qjcc` (`uuid`),
  UNIQUE KEY `UK_bjxn5ii7v7ygwx39et0wawu0q` (`role`),
  KEY `FKnss8q8xknag2srmok5k8x7l76` (`createdBy`),
  KEY `FKngxb35n0dkhrc9w7sw3j4lhdo` (`updatedBy`),
  CONSTRAINT `FKngxb35n0dkhrc9w7sw3j4lhdo` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKnss8q8xknag2srmok5k8x7l76` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `shirtSize`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shirtSize` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `chestSize` decimal(19,2) DEFAULT NULL,
  `lengthSize` decimal(19,2) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `shirtTypeId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_lo3u0ieavf1gapquu33no2fsb` (`uuid`),
  KEY `FKrxtmt9hm9et9ivm131s2b85ja` (`createdBy`),
  KEY `FKc591f0sqrnhfrryibj9c4k8yl` (`updatedBy`),
  KEY `FKhmgfbtu76mye4uvq4gydmib7c` (`shirtTypeId`),
  CONSTRAINT `FKc591f0sqrnhfrryibj9c4k8yl` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKhmgfbtu76mye4uvq4gydmib7c` FOREIGN KEY (`shirtTypeId`) REFERENCES `shirtType` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKrxtmt9hm9et9ivm131s2b85ja` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `shirtType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shirtType` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `eventId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_36g8dnr2cst8mcxc5be1x0x1x` (`uuid`),
  KEY `FKlrbg53eb6812nbhwpihlhauxv` (`createdBy`),
  KEY `FK68qlub1nlc0wd6r4sc85rfitg` (`updatedBy`),
  KEY `FKm30qt4rr9flcmgtdygd7uwewl` (`eventId`),
  CONSTRAINT `FK68qlub1nlc0wd6r4sc85rfitg` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKlrbg53eb6812nbhwpihlhauxv` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKm30qt4rr9flcmgtdygd7uwewl` FOREIGN KEY (`eventId`) REFERENCES `event` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sliders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sliders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `alignment` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descriptionEn` text COLLATE utf8mb4_unicode_ci,
  `descriptionTh` text COLLATE utf8mb4_unicode_ci,
  `imageUrl` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `position` int NOT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_nxjahh8wb0dg4d3vusd0f72pt` (`uuid`),
  KEY `idx_position` (`position`),
  KEY `idx_active` (`active`),
  KEY `FK1rxvhj38995jtn990gbngtwic` (`createdBy`),
  KEY `FKkq2g8fcl9urkwaf3q8g7l9570` (`updatedBy`),
  CONSTRAINT `FK1rxvhj38995jtn990gbngtwic` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKkq2g8fcl9urkwaf3q8g7l9570` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `systemAnnouncement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `systemAnnouncement` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `endDate` datetime(6) DEFAULT NULL,
  `message` text COLLATE utf8mb4_unicode_ci,
  `startDate` datetime(6) DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_t3nqld42ltf1r4yrtmibv4vo5` (`uuid`),
  KEY `FKpngb5plhi1rmdmr1q5rp671dt` (`createdBy`),
  KEY `FK221bm9kyd4kvottpnetbb9o25` (`updatedBy`),
  CONSTRAINT `FK221bm9kyd4kvottpnetbb9o25` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKpngb5plhi1rmdmr1q5rp671dt` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amphoe` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `birthDate` datetime(6) DEFAULT NULL,
  `bloodType` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `companyName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `district` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `emergencyContact` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emergencyPhone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emergencyRelation` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `firstName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `firstNameEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `healthIssues` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idNo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isApprover` bit(1) DEFAULT NULL,
  `lastName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastNameEn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nationality` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pictureUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prefixPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `province` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingAddress` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingAmphoe` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingDistrict` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingProvince` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shippingZipcode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signatureUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `zipcode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `roleId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_1xc1iry6gqjrvh5cpajiq7l2f` (`uuid`),
  UNIQUE KEY `UK_ob8kqyqqgmefl0aco34akdtpe` (`email`),
  KEY `FKt8b01a13ndrte11q8od3oa8pk` (`createdBy`),
  KEY `FKihbi9gytwui3g8iy130ahtsm0` (`updatedBy`),
  KEY `FK8yhl7wdo39n3ee04f8rpajces` (`roleId`),
  CONSTRAINT `FK8yhl7wdo39n3ee04f8rpajces` FOREIGN KEY (`roleId`) REFERENCES `role` (`id`),
  CONSTRAINT `FKihbi9gytwui3g8iy130ahtsm0` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKt8b01a13ndrte11q8od3oa8pk` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `userToken`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userToken` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `createdTime` datetime(6) DEFAULT NULL,
  `updatedTime` datetime(6) DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdBy` int DEFAULT NULL,
  `updatedBy` int DEFAULT NULL,
  `userId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_si3ce1tym1agtxltwd6mm4dl8` (`uuid`),
  KEY `FKn9fkpg053baldeltpppy2wyfw` (`createdBy`),
  KEY `FKdvg2trsohqim1ehenen22ma2` (`updatedBy`),
  KEY `FKq89m02pakvtyfukylgw4iqwgt` (`userId`),
  CONSTRAINT `FKdvg2trsohqim1ehenen22ma2` FOREIGN KEY (`updatedBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKn9fkpg053baldeltpppy2wyfw` FOREIGN KEY (`createdBy`) REFERENCES `user` (`id`),
  CONSTRAINT `FKq89m02pakvtyfukylgw4iqwgt` FOREIGN KEY (`userId`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

