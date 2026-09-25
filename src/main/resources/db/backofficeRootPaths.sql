-- Back-office pages moved from /backoffice/<page> to /<page> (e.g. /backoffice/setting -> /setting).
-- Rewrites the stored sidebar paths and bell-notification links to match.
--
-- Run once against an existing production database, together with the frontend deploy
-- (the new frontend has no /backoffice routes). Local dev re-seeds from DataLoader.
-- Safe to re-run: rows already rewritten no longer match the LIKE.

UPDATE `menu`
SET `path` = SUBSTRING(`path`, CHAR_LENGTH('/backoffice') + 1)
WHERE `path` LIKE '/backoffice/%';

UPDATE `notification`
SET `link` = SUBSTRING(`link`, CHAR_LENGTH('/backoffice') + 1)
WHERE `link` LIKE '/backoffice/%';
