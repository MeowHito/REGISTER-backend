-- Strip the source site's coloured promo note from imported event-calendar names, e.g.
--   CHIANGMAI CITY RUN ... <font style="color:#FF0000">(🎁ลงทะเบียนฟรี !)</font>
-- New imports are cleaned by EventCalendarImportServiceImpl.cleanTitle; this fixes rows pulled
-- before that. Re-runnable: only touches names that still contain a tag.

SET NAMES utf8mb4;

UPDATE `eventCalendar`
SET `eventName` = TRIM(REGEXP_REPLACE(
      REGEXP_REPLACE(
        REGEXP_REPLACE(`eventName`, '(?is)<font\\b[^>]*>.*?</font>', ' '),
        '<[^>]+>', ' '),
      '\\s+', ' '))
WHERE `eventName` LIKE '%<%>%';
