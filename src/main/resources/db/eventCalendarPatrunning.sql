-- Event-calendar import moves from joggingandrunning.com to patrunning.com's monthly calendar.
-- Removes every row pulled from the old site (they would duplicate the new import) and the old
-- REST watermark, which the new importer no longer uses. Manual submissions (source IS NULL)
-- are untouched. Re-runnable: a second run deletes nothing.

SET NAMES utf8mb4;

DELETE FROM `eventCalendar` WHERE `source` = 'joggingandrunning.com';

DELETE FROM `appConfig` WHERE `name` = 'eventCalendarImport.watermark';
