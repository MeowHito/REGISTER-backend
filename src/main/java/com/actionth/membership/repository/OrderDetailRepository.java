package com.actionth.membership.repository;

import com.actionth.membership.model.OrderDetail;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailRepository
		extends JpaRepository<OrderDetail, Integer>, JpaSpecificationExecutor<OrderDetail> {

	Optional<OrderDetail> findByUuid(String uuid);

	List<OrderDetail> findByEventTypeId(Integer eventTypeId);

	@Query("""
			SELECT COUNT(od)
			FROM OrderDetail od
			JOIN od.order o
			JOIN od.pricing p
			WHERE o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW') AND p.uuid = :pricingId
			""")
	Long countByPricingIdAndPaymentStatus(@Param("pricingId") String pricingId);

	@Query(value = """
			SELECT COUNT(od.id)
			FROM orderDetail od
			JOIN orders o ON o.id = od.orderId
			LEFT JOIN pricing p ON p.id = od.pricingId
			LEFT JOIN paymentType pt ON pt.id = p.paymentTypeId
			WHERE o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')
			    AND od.eventTypeId = :eventTypeId
			    AND (od.pricingId IS NULL OR pt.endDate < NOW())
			""", nativeQuery = true)
	Long countByEventTypeIdWithExpiredOrNullPricing(@Param("eventTypeId") Integer eventTypeId);

	@Query("""
			SELECT COUNT(od)
			FROM OrderDetail od
			JOIN od.order o
			WHERE o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW') AND od.eventType.id = :eventTypeId
			""")
	Long countRegisteredByEventTypeId(@Param("eventTypeId") Integer eventTypeId);

	@Query("""
			SELECT COUNT(od) > 0
			FROM OrderDetail od
			JOIN od.order o
			WHERE od.shirtSize.id = :shirtSizeId
			  AND o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')
			""")
	boolean existsByShirtSizeIdAndActiveOrder(@Param("shirtSizeId") Integer shirtSizeId);

	@Query("""
			SELECT COUNT(od) > 0
			FROM OrderDetail od
			JOIN od.order o
			WHERE od.shirtType.id = :shirtTypeId
			  AND o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')
			""")
	boolean existsByShirtTypeIdAndActiveOrder(@Param("shirtTypeId") Integer shirtTypeId);

	@Query("""
			SELECT COUNT(od) > 0
			FROM OrderDetail od
			JOIN od.order o
			WHERE o.event.id = :eventId
			  AND o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')
			""")
	boolean existsByEventIdAndActiveOrder(@Param("eventId") Integer eventId);

	@Query("""
			SELECT COUNT(od) > 0
			FROM OrderDetail od
			JOIN od.order o
			WHERE od.pricing.id = :pricingId
			  AND o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')
			""")
	boolean existsByPricingIdAndActiveOrder(@Param("pricingId") Integer pricingId);

	@Query("""
			SELECT COUNT(od)
			FROM OrderDetail od
			JOIN od.order o
			WHERE od.pricing.id = :pricingId
			  AND o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')
			""")
	Long countByPricingIdAndActiveOrder(@Param("pricingId") Integer pricingId);

	@Query("""
			SELECT od
			FROM OrderDetail od
			JOIN od.order o
			WHERE od.eventType.id = :eventTypeId
			  AND o.paymentStatus = 'SUCCESS'
			""")
	List<OrderDetail> getParticipantByEventTypeId(@Param("eventTypeId") Integer eventTypeId);

	@Query("""
			SELECT od.idNo
			FROM OrderDetail od
			JOIN od.order o
			JOIN o.event e
			WHERE e.uuid = :eventUuid
			  AND o.paymentStatus = 'SUCCESS'
			""")
	List<String> getParticipantByEventId(@Param("eventUuid") String uuid);

	@Query(value = """
			SELECT
			    od.uuid AS participantUuid,
			    u.email AS email,
			    od.prefixPath AS prefixPath,
			    od.pictureUrl AS pictureUrl,
			    e.uuid AS eventUuid,
			    e.link AS eventLink,
			    COALESCE(NULLIF(TRIM(e.link), ''), e.uuid) AS eventKey
			FROM orderDetail od
			JOIN orders o
			    ON o.id = od.orderId
			JOIN event e
			    ON e.id = o.eventId
			JOIN user u
			    ON u.id = o.createdBy
			WHERE (e.uuid = :eventId OR e.link = :eventId)
			  AND u.email IS NOT NULL
			  AND o.paymentStatus = 'SUCCESS'
			""", nativeQuery = true)
	List<Map<String, Object>> findAllByEventUuid(@Param("eventId") String eventUuid);

	List<OrderDetail> findByOrderId(Integer orderId);

	@Query(value = """
			SELECT
			    od.*
			FROM orderDetail od
			JOIN (
			    SELECT
			        iod.id,
			        ROW_NUMBER() OVER (PARTITION BY iod.idNo ORDER BY o.createdTime DESC) AS rn
			    FROM orderDetail iod
			    LEFT JOIN user u
			        ON u.idNo = iod.idNo
			       AND u.id = :userId
			    JOIN orders o
			        ON o.id = iod.orderId
			    WHERE u.idNo IS NULL
			      AND iod.idNo IS NOT NULL
			      AND iod.idNo <> ''
			      AND o.createdBy = :userId
			) ranked_details
			    ON od.id = ranked_details.id
			WHERE ranked_details.rn = 1
			""", nativeQuery = true)
	List<OrderDetail> findLastedApplicatsInOrderDetail(@Param("userId") Integer userId);

	@Query(value = """
			SELECT
			    od.*
			FROM orderDetail od
			JOIN orders o
			    ON o.id = od.orderId
			JOIN event e
			    ON e.id = o.eventId
			WHERE (e.uuid = :eventId OR e.link = :eventId)
			  AND (
			        od.firstName = :name
			     OR od.lastName = :name
			     OR od.idNo = :name
			     OR od.bibNo = :name
			  )
			  AND o.paymentStatus = 'SUCCESS'
			""", nativeQuery = true)
	List<OrderDetail> findByEventAndName(@Param("eventId") String eventId, @Param("name") String name);

	Optional<OrderDetail> findByOrderIdAndIdNo(Integer orderId, String idNo);

	@Modifying
	@Query("UPDATE OrderDetail od SET od.couponUsed = false WHERE od.order.id = :orderId")
	int resetCouponUsedByOrderId(@Param("orderId") Integer orderId);

	@Query("""
			SELECT od
			FROM OrderDetail od
			JOIN od.order o
			JOIN o.event e
			WHERE (e.uuid = :eventKey OR e.link = :eventKey)
			  AND o.paymentStatus = 'SUCCESS'
			  AND (
			        LOWER(od.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
			     OR LOWER(od.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
			     OR LOWER(od.firstNameEn) LIKE LOWER(CONCAT('%', :q, '%'))
			     OR LOWER(od.lastNameEn)  LIKE LOWER(CONCAT('%', :q, '%'))
			     OR od.idNo = :exact
			     OR od.bibNo = :exact
			     OR od.uuid = :exact
			)
			""")
	Page<OrderDetail> searchParticipants(
			@Param("eventKey") String eventKey,
			@Param("q") String q,
			@Param("exact") String exact,
			Pageable pageable);

	@Query("""
			SELECT od
			FROM OrderDetail od
			JOIN od.order o
			JOIN o.event e
			WHERE (e.uuid = :eventKey OR e.link = :eventKey)
			  AND o.paymentStatus = 'SUCCESS'
			  AND (
			        (LOWER(od.firstName) LIKE LOWER(CONCAT('%', :p1, '%')) AND LOWER(od.lastName) LIKE LOWER(CONCAT('%', :p2, '%')))
			     OR (LOWER(od.firstName) LIKE LOWER(CONCAT('%', :p2, '%')) AND LOWER(od.lastName) LIKE LOWER(CONCAT('%', :p1, '%')))
			     OR LOWER(CONCAT(COALESCE(od.firstName,''), ' ', COALESCE(od.lastName,''))) LIKE LOWER(CONCAT('%', :full, '%'))
			     OR (LOWER(od.firstNameEn) LIKE LOWER(CONCAT('%', :p1, '%')) AND LOWER(od.lastNameEn) LIKE LOWER(CONCAT('%', :p2, '%')))
			     OR (LOWER(od.firstNameEn) LIKE LOWER(CONCAT('%', :p2, '%')) AND LOWER(od.lastNameEn) LIKE LOWER(CONCAT('%', :p1, '%')))
			     OR LOWER(CONCAT(COALESCE(od.firstNameEn,''), ' ', COALESCE(od.lastNameEn,''))) LIKE LOWER(CONCAT('%', :full, '%'))
			     OR (LOWER(od.firstName) LIKE LOWER(CONCAT('%', :full, '%')))
			     OR (LOWER(od.lastName) LIKE LOWER(CONCAT('%', :full, '%')))
			     OR LOWER(od.firstNameEn) LIKE LOWER(CONCAT('%', :full, '%'))
			     OR LOWER(od.lastNameEn)  LIKE LOWER(CONCAT('%', :full, '%'))
			     OR od.idNo = :exact
			     OR od.bibNo = :exact
			     OR od.uuid = :exact
			  )
			""")
	Page<OrderDetail> searchParticipantsFullName(
			@Param("eventKey") String eventKey,
			@Param("p1") String p1,
			@Param("p2") String p2,
			@Param("full") String full,
			@Param("exact") String exact,
			Pageable pageable);

	@Query("""
			  SELECT DISTINCT TRIM(od.teamClub)
			  FROM OrderDetail od
			  JOIN od.order o
			  JOIN od.eventType et
			  WHERE et.uuid = :eventTypeId
			    AND od.teamClub IS NOT NULL
			    AND TRIM(od.teamClub) <> ''
			    AND o.paymentStatus = 'SUCCESS'
			    AND (:search IS NULL OR LOWER(od.teamClub) LIKE LOWER(CONCAT('%', :search, '%')))
			  ORDER BY TRIM(od.teamClub) ASC
			""")
	Slice<String> findTeamClubsByEventType(
			@Param("eventTypeId") String eventTypeId,
			@Param("search") String search,
			Pageable pageable);
}
