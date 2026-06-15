-- ---------------------------------------------------------------------------
-- Seed data for the `countryState` table (used by the event-creation province
-- dropdown via GET /public-api/master/countryState).
--
-- The dropdown showed "ไม่มีข้อมูล" because:
--   1. the query referenced a non-existent `active` column (fixed in
--      MasterDataRepository.getAllCountryState), and
--   2. the table itself had no rows.
--
-- This script populates `countryState` with the 77 Thai provinces by copying
-- them from the already-seeded `geoProvinces` table, so no external data dump
-- is required. It is NON-DESTRUCTIVE and idempotent: `INSERT IGNORE` skips rows
-- whose primary key already exists, so re-running it will not duplicate or wipe
-- any existing (e.g. multi-country) data.
--
-- If you also need provinces for other countries, append additional INSERT
-- rows below following the same column layout.
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO `countryState` (`id`, `uuid`, `countryEn`, `countryLocal`, `stateEn`, `stateLocal`, `stateType`)
SELECT
    `code`            AS `id`,
    UUID()            AS `uuid`,
    'Thailand'        AS `countryEn`,
    'ประเทศไทย'        AS `countryLocal`,
    `nameEn`          AS `stateEn`,
    `nameTh`          AS `stateLocal`,
    'Province'        AS `stateType`
FROM `geoProvinces`
WHERE `active` = 1;
