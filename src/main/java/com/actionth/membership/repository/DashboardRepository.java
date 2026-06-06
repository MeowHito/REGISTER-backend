package com.actionth.membership.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.OrderDetail;

import java.util.List;
import java.util.Map;

@Repository
public interface DashboardRepository extends JpaRepository<OrderDetail, Integer> {

  @Query("""
          SELECT COUNT(od)
          FROM OrderDetail od
          JOIN od.order o
          JOIN o.event e
          WHERE e.uuid = :eventUuid
            AND od.active = true
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
      """)
  Long countByEvent(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  @Query("""
          SELECT COUNT(od)
          FROM OrderDetail od
          JOIN od.order o
          JOIN o.event e
          WHERE e.uuid = :eventUuid
            AND od.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
      """)
  Long countPaidByEvent(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  @Query("""
          SELECT new map(et.name as eventType, COUNT(od) as count)
          FROM OrderDetail od
          JOIN od.order o
          JOIN od.eventType et
          JOIN et.event e
          WHERE e.uuid = :eventUuid
            AND od.active = true
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY et.name
          ORDER BY et.name
      """)
  List<Map<String, Object>> countByEventType(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  @Query("""
          SELECT new map(et.name as eventType, COUNT(od) as total)
          FROM OrderDetail od
          JOIN od.order o
          JOIN od.eventType et
          JOIN et.event e
          WHERE e.uuid = :eventUuid
            AND od.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY et.name
          ORDER BY et.name
      """)
  List<Map<String, Object>> countPaidByEventType(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  @Query("""
          SELECT p.gender AS gender, COUNT(p.id) AS count
          FROM OrderDetail p
          JOIN p.order o
          JOIN o.event e
          WHERE e.uuid = :eventUuid
            AND p.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY p.gender
      """)
  List<Map<String, Object>> countPaidGenderByEvent(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  @Query("""
          SELECT et.name AS eventType, p.gender AS gender, COUNT(p.id) AS count
          FROM OrderDetail p
          JOIN p.order o
          JOIN p.eventType et
          JOIN et.event e
          WHERE e.uuid = :eventUuid
            AND p.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY et.name, p.gender
          ORDER BY et.name, p.gender
      """)
  List<Map<String, Object>> countPaidGenderByEventType(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  @Query(value = """
          SELECT
            CASE
              WHEN ag.minAge IS NOT NULL AND ag.maxAge IS NOT NULL THEN CONCAT(ag.minAge, '-', ag.maxAge)
              WHEN ag.minAge IS NULL     AND ag.maxAge IS NOT NULL THEN CONCAT('0-', ag.maxAge)
              WHEN ag.minAge IS NOT NULL AND ag.maxAge IS NULL     THEN CONCAT(ag.minAge, '+')
              ELSE 'UNKNOWN'
            END AS ageGroup,
            ag.gender AS gender,
            COUNT(p.id) AS count
          FROM orderDetail p
          JOIN orders o      ON o.id = p.orderId
          JOIN eventType et  ON et.id = p.eventTypeId
          JOIN ageGroup ag   ON ag.eventTypeId = et.id
          JOIN event e       ON e.id = et.eventId
          WHERE e.uuid = :eventUuid
            AND p.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND (
              p.age IS NOT NULL
              OR (p.birthDate IS NOT NULL AND p.createdTime IS NOT NULL)
            )
            AND ag.gender IS NOT NULL
            AND LOWER(p.gender) = LOWER(ag.gender)
            AND (
              ag.minAge IS NULL OR
              COALESCE(
                p.age,
                (YEAR(p.createdTime) - YEAR(p.birthDate))
              ) >= ag.minAge
            )
            AND (
              ag.maxAge IS NULL OR
              COALESCE(
                p.age,
                (YEAR(p.createdTime) - YEAR(p.birthDate))
              ) <= ag.maxAge
            )
            AND (
              :isAdmin = TRUE
              OR EXISTS (
                  SELECT 1
                  FROM eventPermission ep
                  WHERE ep.eventId = e.id
                    AND ep.userId  = :userId
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY ag.minAge, ag.maxAge, ag.gender
          ORDER BY COALESCE(ag.minAge, 0), ag.gender
      """, nativeQuery = true)
  List<Map<String, Object>> countPaidAgeGroupByEvent(@Param("eventUuid") String eventUuid, @Param("userId") Integer userId, @Param("isAdmin") boolean isAdmin);

  @Query(value = """
          SELECT
            et.name AS eventType,
            CASE
              WHEN ag.minAge IS NOT NULL AND ag.maxAge IS NOT NULL THEN CONCAT(ag.minAge, '-', ag.maxAge)
              WHEN ag.minAge IS NULL     AND ag.maxAge IS NOT NULL THEN CONCAT('0-', ag.maxAge)
              WHEN ag.minAge IS NOT NULL AND ag.maxAge IS NULL     THEN CONCAT(ag.minAge, '+')
              ELSE 'UNKNOWN'
            END AS ageGroup,
            ag.gender AS gender,
            COUNT(p.id) AS count
          FROM orderDetail p
          JOIN orders o      ON o.id = p.orderId
          JOIN eventType et  ON et.id = p.eventTypeId
          JOIN ageGroup ag   ON ag.eventTypeId = et.id
          JOIN event e       ON e.id = et.eventId
          WHERE e.uuid = :eventUuid
            AND p.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND (
              p.age IS NOT NULL
              OR (p.birthDate IS NOT NULL AND p.createdTime IS NOT NULL)
            )
            AND ag.gender IS NOT NULL
            AND LOWER(p.gender) = LOWER(ag.gender)
            AND (
              ag.minAge IS NULL OR
              COALESCE(
                p.age,
                (YEAR(p.createdTime) - YEAR(p.birthDate))
              ) >= ag.minAge
            )
            AND (
              ag.maxAge IS NULL OR
              COALESCE(
                p.age,
                (YEAR(p.createdTime) - YEAR(p.birthDate))
              ) <= ag.maxAge
            )
            AND (
              :isAdmin = TRUE
              OR EXISTS (
                  SELECT 1
                  FROM eventPermission ep
                  WHERE ep.eventId = e.id
                    AND ep.userId  = :userId
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY et.name, ag.minAge, ag.maxAge, ag.gender
          ORDER BY et.name, COALESCE(ag.minAge, 0), ag.gender
      """, nativeQuery = true)
  List<Map<String, Object>> countPaidAgeGroupByEventType(@Param("eventUuid") String eventUuid, @Param("userId") Integer userId, @Param("isAdmin") boolean isAdmin);

  @Query("""
          SELECT st.name AS shirtType, ss.name AS shirtSize, COUNT(p.id) AS count
          FROM OrderDetail p
          JOIN p.order o
          JOIN o.event e
          JOIN p.shirtSize ss
          JOIN p.shirtType st
          WHERE e.uuid = :eventUuid
            AND p.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY st.name, ss.id, ss.name
          ORDER BY st.name, ss.id
      """)
  List<Map<String, Object>> countPaidShirtByEvent(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  @Query("""
          SELECT et.name AS eventType, st.name AS shirtType, ss.name AS shirtSize, COUNT(p.id) AS count
          FROM OrderDetail p
          JOIN p.order o
          JOIN o.event e
          JOIN p.shirtSize ss
          JOIN p.shirtType st
          JOIN p.eventType et
          WHERE e.uuid = :eventUuid
            AND p.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY et.name, st.name, ss.id, ss.name
          ORDER BY et.name, st.name, ss.id
      """)
  List<Map<String, Object>> countPaidShirtByEventType(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  // นับค่าว่างเป็น UNKNOWN, ถ้าไม่มี shippingProvince ใช้ province
  @Query("""
          SELECT COALESCE(
                  NULLIF(TRIM(od.shippingProvince), ''),
                  NULLIF(TRIM(od.province), ''),
                  'UNKNOWN'
                ) AS province,
                COUNT(od.id) AS cnt
          FROM OrderDetail od
          JOIN od.order o
          JOIN o.event e
          WHERE e.uuid = :eventUuid
            AND od.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY COALESCE(
                    NULLIF(TRIM(od.shippingProvince), ''),
                    NULLIF(TRIM(od.province), ''),
                    'UNKNOWN'
                  )
          ORDER BY
            CASE
              WHEN COALESCE(NULLIF(TRIM(od.shippingProvince), ''),
                            NULLIF(TRIM(od.province), ''),
                            'UNKNOWN') = 'UNKNOWN' THEN 1
              ELSE 0
            END,
            COUNT(od.id) DESC
      """)
  List<Map<String, Object>> countPaidByProvince(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  // reg dash - ตามวันและเวลาที่จ่าย
  interface TimeBucketCountProjection {
    java.sql.Timestamp getDateTime();

    Integer getCount();
  }

  @Query(value = """
          SELECT
            STR_TO_DATE(DATE_FORMAT(o.paymentDateTime, '%Y-%m-%d %H:00:00'), '%Y-%m-%d %H:%i:%s') AS dateTime,
            COUNT(p.id) AS count
          FROM orderDetail p
          JOIN orders o ON o.id = p.orderId
          JOIN event  e ON e.id = o.eventId
          WHERE e.uuid = :eventUuid
            AND p.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND o.paymentDateTime IS NOT NULL
            AND (
              :isAdmin = TRUE
              OR EXISTS (
                  SELECT 1
                  FROM eventPermission ep
                  WHERE ep.eventId = e.id
                    AND ep.userId  = :userId
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY DATE_FORMAT(o.paymentDateTime, '%Y-%m-%d %H:00:00')
          ORDER BY DATE_FORMAT(o.paymentDateTime, '%Y-%m-%d %H:00:00')
      """, nativeQuery = true)
  List<TimeBucketCountProjection> countPaidByRegisterDate(@Param("eventUuid") String eventUuid, @Param("userId") Integer userId, @Param("isAdmin") boolean isAdmin);

  // overview dash - ตามวันที่สมัคร
  @Query("""
          SELECT new map(DATE(p.createdTime) AS date, COUNT(p.id) AS total)
          FROM OrderDetail p
          JOIN p.order o
          JOIN o.event e
          WHERE e.uuid = :eventUuid
            AND p.active = true
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY DATE(p.createdTime)
          ORDER BY DATE(p.createdTime)
      """)
  List<Map<String, Object>> countPerDay(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  // overview dash - ตามวันที่จ่าย
  @Query("""
          SELECT new map(DATE(o.paymentDateTime) as date, COUNT(od) as total)
          FROM OrderDetail od
          JOIN od.order o
          JOIN o.event e
          WHERE e.uuid = :eventUuid
            AND od.active = true
            AND o.paymentStatus = 'SUCCESS'
            AND o.paymentDateTime IS NOT NULL
            AND (
              :isAdmin = true
              OR EXISTS (
                  SELECT 1
                  FROM EventPermission ep
                  WHERE ep.event = e
                    AND ep.user.uuid = :userUuid
                    AND ep.canRead = true
                    AND ep.active = true
              )
            )
          GROUP BY DATE(o.paymentDateTime)
          ORDER BY DATE(o.paymentDateTime)
      """)
  List<Map<String, Object>> countPaidParticipantsByPaymentDate(@Param("eventUuid") String eventUuid, @Param("userUuid") String userUuid, @Param("isAdmin") boolean isAdmin);

  // reg dash - Payment methods (รวม CANCEL เข้ากับ FAILED)
  @Query(value = """
        SELECT
          COALESCE(NULLIF(TRIM(o.paymentMethod), ''), 'UNKNOWN') AS method,
          CASE
            WHEN o.paymentStatus = 'SUCCESS' THEN 'SUCCESS'
            WHEN o.paymentStatus = 'PENDING' THEN 'PENDING'
            WHEN o.paymentStatus IN ('FAILED','CANCEL','CANCELED','CANCELLED') THEN 'FAILED'
            ELSE 'UNKNOWN'
          END AS status,
          COUNT(od.id) AS count
        FROM orders o
        JOIN event e ON o.eventId = e.id
        JOIN orderDetail od ON od.orderId = o.id AND od.active = TRUE
        WHERE e.uuid = :eventUuid
          AND (
              (o.paymentStatus='SUCCESS')
            OR (o.paymentStatus='PENDING')
            OR (o.paymentStatus IN ('FAILED','CANCEL','CANCELED','CANCELLED'))
          )
          AND (
            :isAdmin = TRUE
            OR EXISTS (
                SELECT 1
                FROM eventPermission ep
                WHERE ep.eventId = e.id
                  AND ep.userId  = :userId
                  AND ep.canRead = true
                  AND ep.active = true
            )
          )
        GROUP BY COALESCE(NULLIF(TRIM(o.paymentMethod), ''), 'UNKNOWN'),
                 CASE
                   WHEN o.paymentStatus = 'SUCCESS' THEN 'SUCCESS'
                   WHEN o.paymentStatus = 'PENDING' THEN 'PENDING'
                   WHEN o.paymentStatus IN ('FAILED','CANCEL','CANCELED','CANCELLED') THEN 'FAILED'
                   ELSE 'FAILED'
                 END
        ORDER BY method, status
      """, nativeQuery = true)
  List<Map<String, Object>> countPaymentStatusByMethod(@Param("eventUuid") String eventUuid, @Param("userId") Integer userId, @Param("isAdmin") boolean isAdmin);

  // reg dash - Failure reasons (FAILED vs CANCELED)
  @Query(value = """
        SELECT
          CASE
            WHEN o.paymentStatus = 'FAILED' THEN 'FAILED'
            WHEN o.paymentStatus IN ('CANCEL','CANCELED','CANCELLED') THEN 'CANCELED'
          END AS reason,
          COUNT(od.id) AS count
        FROM orders o
        JOIN event e ON o.eventId = e.id
        JOIN orderDetail od ON od.orderId = o.id AND od.active = TRUE
        WHERE e.uuid = :eventUuid
          AND o.paymentStatus IN ('FAILED','CANCEL','CANCELED','CANCELLED')
          AND (
            :isAdmin = TRUE
            OR EXISTS (
                SELECT 1
                FROM eventPermission ep
                WHERE ep.eventId = e.id
                  AND ep.userId  = :userId
                  AND ep.canRead = true
                  AND ep.active = true
            )
          )
        GROUP BY 1
        ORDER BY 1
      """, nativeQuery = true)
  List<Map<String, Object>> countFailureReasons(@Param("eventUuid") String eventUuid, @Param("userId") Integer userId, @Param("isAdmin") boolean isAdmin);

}
