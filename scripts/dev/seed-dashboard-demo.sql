-- LOCAL DEV ONLY. Never run against production.
--
-- Creates "Demo Dashboard Run 2026": a copy of the seeded "Tokyo Marathon 2026" event with four
-- distances, two shirt types, age groups, two add-ons and ~2,200 registrations spread over the last
-- 45 days (mixed payment status / method / gender / age / province / hour), so every panel on
-- /eventStats has something to draw.
--
-- Re-runnable: it first deletes everything it created before (matched by the fixed demo uuids),
-- then inserts again. Local uses ddl-auto: create, so re-run it after every backend restart:
--   docker exec -i membership-mysql mysql -uroot -plocaldev membership_db < scripts/dev/seed-dashboard-demo.sql
-- To remove the demo without re-creating it, run only the "Clean up" section.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET SESSION cte_max_recursion_depth = 10000;

SET @demo_event_uuid = 'd0000000-0000-4000-8000-000000000001';
SET @total = 2200;

-- ---------------------------------------------------------------- Clean up
SET @demo_event_id = (SELECT id FROM event WHERE uuid = @demo_event_uuid);

DELETE oa FROM orderAddOn oa JOIN orders o ON o.id = oa.orderId WHERE o.eventId = @demo_event_id;
DELETE od FROM orderDetail od JOIN orders o ON o.id = od.orderId WHERE o.eventId = @demo_event_id;
DELETE FROM orders WHERE eventId = @demo_event_id;
DELETE FROM eventAddOn WHERE eventId = @demo_event_id;
DELETE ag FROM ageGroup ag JOIN eventType et ON et.id = ag.eventTypeId WHERE et.eventId = @demo_event_id;
DELETE ss FROM shirtSize ss JOIN shirtType st ON st.id = ss.shirtTypeId WHERE st.eventId = @demo_event_id;
DELETE FROM shirtType WHERE eventId = @demo_event_id;
DELETE FROM eventType WHERE eventId = @demo_event_id;
DELETE FROM eventPermission WHERE eventId = @demo_event_id;
DELETE FROM event WHERE id = @demo_event_id;

-- ---------------------------------------------------------------- Event (copied from Tokyo)
SET @src_event_id = (SELECT id FROM event WHERE name = 'Tokyo Marathon 2026' ORDER BY id LIMIT 1);

SET @cols = (
  SELECT GROUP_CONCAT(CONCAT('`', COLUMN_NAME, '`') ORDER BY ORDINAL_POSITION)
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'event' AND COLUMN_NAME NOT IN ('id', 'uuid', 'name', 'link')
);
SET @sql = CONCAT(
  'INSERT INTO event (`uuid`, `name`, `link`, ', @cols, ') ',
  'SELECT ''', @demo_event_uuid, ''', ''Demo Dashboard Run 2026'', ''demo-dashboard-run-2026'', ', @cols,
  ' FROM event WHERE id = ', @src_event_id
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @demo_event_id = LAST_INSERT_ID();

UPDATE event
SET isDraft = b'0',
    startRegistrationDate = TIMESTAMP(CURDATE() - INTERVAL 44 DAY),
    endRegistrationDate = TIMESTAMP(CURDATE() + INTERVAL 25 DAY, '23:59:00'),
    eventDate = TIMESTAMP(CURDATE() + INTERVAL 40 DAY, '05:00:00'),
    createdTime = NOW(6), updatedTime = NOW(6)
WHERE id = @demo_event_id;

-- Same collaborators as the source event, so an organizer who can see Tokyo sees the demo too.
SET @cols = (
  SELECT GROUP_CONCAT(CONCAT('`', COLUMN_NAME, '`') ORDER BY ORDINAL_POSITION)
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'eventPermission' AND COLUMN_NAME NOT IN ('id', 'uuid', 'eventId')
);
SET @sql = CONCAT(
  'INSERT INTO eventPermission (`uuid`, `eventId`, ', @cols, ') ',
  'SELECT UUID(), ', @demo_event_id, ', ', @cols, ' FROM eventPermission WHERE eventId = ', @src_event_id
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------- Distances
INSERT INTO eventType (active, createdTime, updatedTime, uuid, name, price, quota, isNoShirt, isTeam, discountNoShirt, eventDate, eventId) VALUES
  (b'1', NOW(6), NOW(6), 'd0000000-0000-4000-8001-000000000001', 'Full Marathon 42K', 1200, 800,  b'0', b'0', 0, TIMESTAMP(CURDATE() + INTERVAL 40 DAY, '04:00:00'), @demo_event_id),
  (b'1', NOW(6), NOW(6), 'd0000000-0000-4000-8001-000000000002', 'Half Marathon 21K', 900,  1000, b'0', b'0', 0, TIMESTAMP(CURDATE() + INTERVAL 40 DAY, '05:00:00'), @demo_event_id),
  (b'1', NOW(6), NOW(6), 'd0000000-0000-4000-8001-000000000003', '10K',               650,  800,  b'0', b'0', 0, TIMESTAMP(CURDATE() + INTERVAL 40 DAY, '06:00:00'), @demo_event_id),
  (b'1', NOW(6), NOW(6), 'd0000000-0000-4000-8001-000000000004', 'Fun Run 5K',        450,  400,  b'0', b'0', 0, TIMESTAMP(CURDATE() + INTERVAL 40 DAY, '06:30:00'), @demo_event_id);

SET @et42 = (SELECT id FROM eventType WHERE uuid = 'd0000000-0000-4000-8001-000000000001');
SET @et21 = (SELECT id FROM eventType WHERE uuid = 'd0000000-0000-4000-8001-000000000002');
SET @et10 = (SELECT id FROM eventType WHERE uuid = 'd0000000-0000-4000-8001-000000000003');
SET @et5  = (SELECT id FROM eventType WHERE uuid = 'd0000000-0000-4000-8001-000000000004');

-- Age groups: the same bands for both genders on every distance (AgeGroup.Gender is male / female).
INSERT INTO ageGroup (active, createdTime, updatedTime, uuid, gender, minAge, maxAge, position, eventTypeId)
SELECT b'1', NOW(6), NOW(6), UUID(), g.gender, b.minAge, b.maxAge, b.pos, et.id
FROM (SELECT @et42 AS id UNION ALL SELECT @et21 UNION ALL SELECT @et10 UNION ALL SELECT @et5) et
CROSS JOIN (SELECT 'male' AS gender UNION ALL SELECT 'female') g
CROSS JOIN (
  SELECT 16 AS minAge, 19 AS maxAge, 1 AS pos UNION ALL SELECT 20, 29, 2 UNION ALL SELECT 30, 39, 3
  UNION ALL SELECT 40, 49, 4 UNION ALL SELECT 50, 59, 5 UNION ALL SELECT 60, 99, 6
) b;

-- ---------------------------------------------------------------- Shirts
INSERT INTO shirtType (active, createdTime, updatedTime, uuid, name, description, eventId) VALUES
  (b'1', NOW(6), NOW(6), 'd0000000-0000-4000-8002-000000000001', 'Finisher Tee',  NULL, @demo_event_id),
  (b'1', NOW(6), NOW(6), 'd0000000-0000-4000-8002-000000000002', 'Running Shirt', NULL, @demo_event_id);
SET @stFinisher = (SELECT id FROM shirtType WHERE uuid = 'd0000000-0000-4000-8002-000000000001');
SET @stRunning  = (SELECT id FROM shirtType WHERE uuid = 'd0000000-0000-4000-8002-000000000002');

INSERT INTO shirtSize (active, createdTime, updatedTime, uuid, name, chestSize, lengthSize, shirtTypeId)
SELECT b'1', NOW(6), NOW(6), UUID(), s.name, s.chest, s.len, st.id
FROM (SELECT @stFinisher AS id UNION ALL SELECT @stRunning) st
CROSS JOIN (
  SELECT 'XS' AS name, 34 AS chest, 25 AS len UNION ALL SELECT 'S', 36, 26 UNION ALL SELECT 'M', 38, 27
  UNION ALL SELECT 'L', 40, 28 UNION ALL SELECT 'XL', 42, 29 UNION ALL SELECT '2XL', 44, 30
) s;

-- ---------------------------------------------------------------- Add-ons
INSERT INTO eventAddOn (active, createdTime, updatedTime, uuid, name, nameEn, perApplicant, price, quota, maxPerOrder, position, noteRequired, eventId) VALUES
  (b'1', NOW(6), NOW(6), 'd0000000-0000-4000-8003-000000000001', 'โรงแรม 1 คืน', 'Hotel (1 night)', b'0', 1800, 150,  1, 1, b'0', @demo_event_id),
  (b'1', NOW(6), NOW(6), 'd0000000-0000-4000-8003-000000000002', 'แพ็กเกจรูปถ่าย', 'Photo package',  b'1', 150,  NULL, 1, 2, b'0', @demo_event_id);
SET @addHotel = (SELECT id FROM eventAddOn WHERE uuid = 'd0000000-0000-4000-8003-000000000001');
SET @addPhoto = (SELECT id FROM eventAddOn WHERE uuid = 'd0000000-0000-4000-8003-000000000002');

-- ---------------------------------------------------------------- Orders (one runner each)
-- Every attribute comes from CRC32 of the row number, so re-runs produce the same data.
INSERT INTO orders (active, createdTime, updatedTime, uuid, orderNo, eventId, qty, paymentStatus, paymentMethod,
                    paymentDateTime, unitPrice, shippingFee, addOnTotal, couponDiscount, discountShirt, fee, feePercent,
                    totalPrice, totalAmountWithFee)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < @total),
base AS (
  SELECT n,
         CRC32(CONCAT('t', n)) % 100 AS rType,
         CRC32(CONCAT('d', n)) % 1000 AS rDay,
         CRC32(CONCAT('h', n)) % 100 AS rHour,
         CRC32(CONCAT('s', n)) % 100 AS rStatus,
         CRC32(CONCAT('m', n)) % 100 AS rMethod,
         CRC32(CONCAT('p', n)) % 100 AS rShip
  FROM seq
),
shaped AS (
  SELECT n,
         CASE WHEN rType < 32 THEN 1200 WHEN rType < 74 THEN 900 WHEN rType < 93 THEN 650 ELSE 450 END AS price,
         -- 15% land in the first three (early-bird) days, the rest spread over the next six weeks.
         CASE WHEN rDay < 150 THEN rDay % 3 ELSE 3 + FLOOR((rDay - 150) * 42 / 850) END AS dayNo,
         CASE WHEN rHour < 3 THEN n % 6 WHEN rHour < 18 THEN 6 + n % 5 WHEN rHour < 38 THEN 11 + n % 3
              WHEN rHour < 55 THEN 14 + n % 4 WHEN rHour < 92 THEN 18 + n % 5 ELSE 23 END AS hourNo,
         CASE WHEN rStatus < 82 THEN 'SUCCESS' WHEN rStatus < 93 THEN 'PENDING' WHEN rStatus < 97 THEN 'FAILED'
              WHEN rStatus < 99 THEN 'CANCELED' ELSE 'EXPIRED' END AS status,
         IF(rMethod < 72, 'QR Code', 'Credit Card') AS method,
         IF(rShip < 30, 60, 0) AS ship
  FROM base
),
timed AS (
  SELECT *, TIMESTAMP(CURDATE() - INTERVAL 44 DAY) + INTERVAL dayNo DAY + INTERVAL hourNo HOUR + INTERVAL (n % 60) MINUTE AS created
  FROM shaped
)
SELECT b'1', created, created,
       CONCAT('d0000000-0000-4000-9000-', LPAD(n, 12, '0')),
       CONCAT('DEMO', LPAD(n, 6, '0')),
       @demo_event_id, 1, status, method,
       IF(status = 'SUCCESS', created + INTERVAL 4 MINUTE, NULL),
       price, ship, 0, 0, 0, ROUND((price + ship) * 0.03, 2), 3,
       price + ship, ROUND((price + ship) * 1.03, 2)
FROM timed;

-- ---------------------------------------------------------------- Runners
INSERT INTO orderDetail (active, createdTime, updatedTime, uuid, orderId, eventTypeId, shirtTypeId, shirtSizeId,
                         firstName, lastName, email, gender, age, birthDate, province, price, shippingFee, netPrice, isSelf)
SELECT b'1', o.createdTime, o.createdTime,
       CONCAT('d0000000-0000-4000-9001-', LPAD(x.n, 12, '0')),
       o.id,
       CASE o.unitPrice WHEN 1200 THEN @et42 WHEN 900 THEN @et21 WHEN 650 THEN @et10 ELSE @et5 END,
       x.shirtType,
       (SELECT ss.id FROM shirtSize ss WHERE ss.shirtTypeId = x.shirtType AND ss.name = x.size),
       CONCAT('Runner', x.n), 'Demo', CONCAT('runner', x.n, '@demo.local'),
       x.gender, x.age, o.createdTime - INTERVAL x.age YEAR - INTERVAL (x.n % 300) DAY,
       x.province, o.unitPrice, o.shippingFee, o.unitPrice + o.shippingFee, b'1'
FROM orders o
JOIN (
  SELECT n,
         IF(CRC32(CONCAT('g', n)) % 100 < 58, 'male', 'female') AS gender,
         CASE WHEN ra < 6 THEN 16 + n % 4 WHEN ra < 30 THEN 20 + n % 10 WHEN ra < 62 THEN 30 + n % 10
              WHEN ra < 85 THEN 40 + n % 10 WHEN ra < 96 THEN 50 + n % 10 ELSE 60 + n % 11 END AS age,
         IF(rt < 74, @stFinisher, @stRunning) AS shirtType,
         CASE WHEN rs < 5 THEN 'XS' WHEN rs < 22 THEN 'S' WHEN rs < 50 THEN 'M' WHEN rs < 75 THEN 'L'
              WHEN rs < 92 THEN 'XL' ELSE '2XL' END AS size,
         CASE WHEN rp < 2 THEN NULL
              WHEN rp < 38 THEN (SELECT uuid FROM countryState WHERE stateEn = 'Bangkok' LIMIT 1)
              ELSE (SELECT uuid FROM countryState WHERE stateEn = ELT(1 + rp % 12,
                     'Chiang Mai', 'Nonthaburi', 'Chon Buri', 'Khon Kaen', 'Phuket', 'Songkhla',
                     'Nakhon Ratchasima', 'Rayong', 'Pathum Thani', 'Samut Prakan', 'Chiang Rai', 'Udon Thani') LIMIT 1)
         END AS province
  FROM (
    SELECT n,
           CRC32(CONCAT('a', n)) % 100 AS ra,
           CRC32(CONCAT('t', n)) % 100 AS rt,
           CRC32(CONCAT('z', n)) % 100 AS rs,
           CRC32(CONCAT('v', n)) % 100 AS rp
    FROM (WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < @total) SELECT n FROM seq) s
  ) r
) x ON o.uuid = CONCAT('d0000000-0000-4000-9000-', LPAD(x.n, 12, '0'));

-- ---------------------------------------------------------------- Add-on purchases
-- ~6% of orders book the hotel (per order), ~20% of runners buy the photo package (per runner).
INSERT INTO orderAddOn (active, createdTime, updatedTime, uuid, name, nameEn, qty, unitPrice, totalPrice, addOnId, orderId, orderDetailId)
SELECT b'1', o.createdTime, o.createdTime, UUID(), 'โรงแรม 1 คืน', 'Hotel (1 night)', 1, 1800, 1800, @addHotel, o.id, NULL
FROM orders o
WHERE o.eventId = @demo_event_id AND CRC32(CONCAT('x', o.orderNo)) % 100 < 6
UNION ALL
SELECT b'1', o.createdTime, o.createdTime, UUID(), 'แพ็กเกจรูปถ่าย', 'Photo package', 1, 150, 150, @addPhoto, o.id, od.id
FROM orders o
JOIN orderDetail od ON od.orderId = o.id
WHERE o.eventId = @demo_event_id AND CRC32(CONCAT('y', o.orderNo)) % 100 < 20;

-- Add-ons are part of the order total, like a real checkout.
UPDATE orders o
JOIN (SELECT orderId, SUM(totalPrice) AS addOns FROM orderAddOn GROUP BY orderId) a ON a.orderId = o.id
SET o.addOnTotal = a.addOns,
    o.totalPrice = o.totalPrice + a.addOns,
    o.fee = ROUND((o.totalPrice + a.addOns) * 0.03, 2),
    o.totalAmountWithFee = ROUND((o.totalPrice + a.addOns) * 1.03, 2)
WHERE o.eventId = @demo_event_id;

SELECT (SELECT COUNT(*) FROM orders WHERE eventId = @demo_event_id) AS demo_orders,
       (SELECT COUNT(*) FROM orderDetail od JOIN orders o ON o.id = od.orderId WHERE o.eventId = @demo_event_id) AS demo_runners,
       (SELECT COUNT(*) FROM orderAddOn oa JOIN orders o ON o.id = oa.orderId WHERE o.eventId = @demo_event_id) AS demo_addons;
