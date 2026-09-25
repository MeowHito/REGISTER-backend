-- Adds the "eventStats" back-office menu (/eventStats), the stats tabs that used to sit under the
-- dashboard's event table. It goes right below "dashboard" and every role that can see the dashboard
-- gets the same flags on it.
--
-- Run once against production before deploying the frontend that routes /eventStats.
-- Local dev re-seeds from DataLoader. Safe to re-run: each step checks for the row first.

-- 1. Make room at position 1 (only while the menu doesn't exist yet).
UPDATE `menu`
SET `position` = `position` + 1
WHERE `position` >= 1
  AND NOT EXISTS (SELECT 1 FROM (SELECT `id` FROM `menu` WHERE `title` = 'eventStats') AS existing);

-- 2. The menu row.
INSERT INTO `menu` (`active`, `createdTime`, `updatedTime`, `uuid`, `icon`, `isDisplay`, `isNoti`, `path`, `position`, `title`)
SELECT b'1', NOW(6), NOW(6), UUID(), 'PieChartOutlined', b'1', b'0', '/eventStats', 1, 'eventStats'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `menu` WHERE `title` = 'eventStats');

-- 3. Copy each role's dashboard permission onto it.
INSERT INTO `permission` (`active`, `createdTime`, `updatedTime`, `uuid`, `canCreate`, `canDelete`, `canRead`, `canUpdate`, `menuId`, `roleId`)
SELECT p.`active`, NOW(6), NOW(6), UUID(), p.`canCreate`, p.`canDelete`, p.`canRead`, p.`canUpdate`, stats.`id`, p.`roleId`
FROM `permission` p
JOIN `menu` dash ON dash.`id` = p.`menuId` AND dash.`title` = 'dashboard'
JOIN `menu` stats ON stats.`title` = 'eventStats'
WHERE NOT EXISTS (
  SELECT 1 FROM (SELECT `menuId`, `roleId` FROM `permission`) AS existing
  WHERE existing.`menuId` = stats.`id` AND existing.`roleId` = p.`roleId`
);
