package com.actionth.membership.repository;

import com.actionth.membership.model.Orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Integer>, JpaSpecificationExecutor<Orders> {

	long countByOrderNoStartingWith(String prefix);

	@Modifying
	@Query("""
			    UPDATE Orders o
			    SET o.paymentStatus = :paymentStatus,
			        o.paymentDateTime = :paymentDateTime
			    WHERE o.orderNo = :orderNo
			""")
	int updatePaymentStatusByOrderNo(String orderNo, String paymentStatus, OffsetDateTime paymentDateTime);

	Optional<Orders> findByOrderNo(String orderNo);

	Optional<Orders> findByUuid(String uuid);

	@Query("SELECT o FROM Orders o WHERE o.uuid = :identifier OR o.orderNo = :identifier")
	Optional<Orders> findByUuidOrOrderNo(@Param("identifier") String identifier);

	// ค้นหารายการที่สถานะยังเป็น 'PENDING' และเลยกำหนดเวลาชำระเงินแล้ว
	@Query("SELECT o FROM Orders o WHERE o.paymentStatus = 'PENDING' AND o.paymentDueDatetime < :now")
	List<Orders> findOverduePayments(OffsetDateTime now);

	// อัปเดตรายการที่เลยกำหนดให้เป็น 'Failed'
	@Transactional
	@Modifying
	@Query("UPDATE Orders o SET o.paymentStatus = 'FAILED' " +
			"WHERE o.paymentStatus = 'PENDING' AND o.paymentDueDatetime < CURRENT_TIMESTAMP")
	int updateOverduePayments(OffsetDateTime now);

	Optional<Orders> findByPaymentToken(String paymentToken);

	@Query(value = """
			    SELECT q.name, q.unitPrice, SUM(q.qty) AS qty, (q.unitPrice * SUM(q.qty)) AS total
			    FROM (
			        SELECT od.id, et.id AS eventTypeId, od.pricingId, CONCAT(et.name, ' ', pt.name) AS name, pc.price AS unitPrice, 1 AS qty
			        FROM orders o
			        JOIN event e ON o.eventId = e.id
			        JOIN orderDetail od ON od.orderId = o.id
			        JOIN eventType et ON od.eventTypeId = et.id
			        JOIN pricing pc ON od.pricingId = pc.id
			        JOIN paymentType pt ON pc.paymentTypeId = pt.id
			        WHERE e.uuid = :eventUuid
			        AND o.createdTime BETWEEN :startDate AND :endDate
			        AND o.paymentStatus = 'SUCCESS'
			        UNION
			        SELECT od.id, et.id AS eventTypeId, od.pricingId, et.name AS name, od.price AS unitPrice, 1 AS qty
			        FROM orders o
			        JOIN event e ON o.eventId = e.id
			        JOIN orderDetail od ON od.orderId = o.id AND od.pricingId IS NULL
			        JOIN eventType et ON od.eventTypeId = et.id
			        WHERE e.uuid = :eventUuid
			        AND o.createdTime BETWEEN :startDate AND :endDate
			        AND o.paymentStatus = 'SUCCESS'
			    ) AS q
			    GROUP BY q.eventTypeId, q.pricingId, q.unitPrice
			    ORDER BY q.eventTypeId, q.pricingId
			""", nativeQuery = true)
	List<Map<String, Object>> summarizeOrderFinance(
			@Param("eventUuid") String eventUuid,
			@Param("startDate") OffsetDateTime startDate,
			@Param("endDate") OffsetDateTime endDate);

	@Query(value = """
			SELECT
			        SUM(o.couponDiscount) AS totalDiscountCoupon,
			        SUM(o.discountShirt) AS totalDiscountShirt,
			        SUM(o.shippingFee) AS totalShippingFee,
			        SUM(o.unitPrice) AS totalAmount,
			        SUM(o.totalPrice) AS totalNetAmount,
			        SUM(o.fee) AS totalServiceFee,
			        SUM(totalAmountWithFee) AS totalAmountWithFee
			FROM orders o
			JOIN event e ON o.eventId = e.id
			WHERE e.uuid = :eventUuid AND o.createdTime BETWEEN :startDate AND :endDate
			        AND o.paymentStatus = 'SUCCESS'
			""", nativeQuery = true)
	List<Map<String, Object>> summarizeOrderFinanceMaster(
			@Param("eventUuid") String eventUuid,
			@Param("startDate") OffsetDateTime startDate,
			@Param("endDate") OffsetDateTime endDate);

	@Query(value = """
			    SELECT
			        q.eventId, q.eventTypeId, q.pricingId, q.eventTypeName, q.paymentTypeName, q.unitPrice,
			        COUNT(q.id) AS qty,
			        COUNT(q.id) * MAX(q.unitPrice) AS total
			    FROM (
			        SELECT od.id, e.id AS eventId, et.id AS eventTypeId, od.pricingId, et.name AS eventTypeName, pt.name AS paymentTypeName, pc.price AS unitPrice
			        FROM orders o
			        JOIN event e ON o.eventId = e.id
			        JOIN orderDetail od ON od.orderId = o.id
			        JOIN eventType et ON od.eventTypeId = et.id
			        JOIN pricing pc ON od.pricingId = pc.id
			        JOIN paymentType pt ON pc.paymentTypeId = pt.id
			        WHERE e.uuid = :eventUuid
			        AND o.createdTime BETWEEN :startDate AND :endDate
			        AND o.paymentStatus = 'SUCCESS'
			        UNION
			        SELECT od.id, e.id AS eventId, et.id AS eventTypeId, od.pricingId, et.name AS eventTypeName, "Normal" AS paymentTypeName, et.price AS unitPrice
			        FROM orders o
			        JOIN event e ON o.eventId = e.id
			        JOIN orderDetail od ON od.orderId = o.id AND od.pricingId IS NULL
			        JOIN eventType et ON od.eventTypeId = et.id
			        WHERE e.uuid = :eventUuid
			        AND o.createdTime BETWEEN :startDate AND :endDate
			        AND o.paymentStatus = 'SUCCESS'
			    ) AS q
			    GROUP BY q.eventId, q.eventTypeId, q.pricingId
			    ORDER BY q.eventId, q.eventTypeId, q.pricingId
			""", nativeQuery = true)
	List<Map<String, Object>> summarizeOrderFinanceDetail(
			@Param("eventUuid") String eventUuid,
			@Param("startDate") OffsetDateTime startDate,
			@Param("endDate") OffsetDateTime endDate);

	@Query(value = """
			    SELECT
			    e.uuid,
			    c.runNo AS contractNo,
			    e.name AS eventName,
			    o.paymentMethod AS paymentMethod,
			    SUM(o.unitPrice) AS registrationFee,
			    SUM(o.shippingFee) AS shippingFee
			FROM orders o
			JOIN event e ON o.eventId = e.id
			JOIN contract c ON c.eventId = e.id
			WHERE o.createdTime BETWEEN :startDate AND :endDate
			AND o.paymentStatus = 'SUCCESS'
			GROUP BY o.paymentMethod, c.runNo, e.id
			ORDER BY o.paymentMethod, c.runNo
			""", nativeQuery = true)
	List<Map<String, Object>> summarizeRevenue(@Param("startDate") OffsetDateTime startDate,
			@Param("endDate") OffsetDateTime endDate);

	@Query(value = """
			SELECT
			    od.uuid,
			    e.name AS eventName,
			    o.orderNo AS orderId,
			    o.scbTransactionId AS transactionId,
			    DATE_FORMAT(o.paymentDateTime, '%Y-%m-%d %H:%i:%s') AS paymentDateTime,
			    CONCAT(od.firstName, IFNULL(CONCAT(' ', od.lastName), '')) AS fullName,
			    od.price AS price,
			    od.couponDiscount AS discountCoupon,
			    od.discountShirt AS discountShirt,
			    od.shippingFee AS shippingFee,
			    od.netPrice AS netPrice,
			    et.name AS eventTypeName,
			    o.paymentStatus AS paymentStatus,
			    o.paymentMethod AS paymentMethod,
			    DATE_FORMAT(o.createdTime, '%Y-%m-%d %H:%i:%s') AS registrationDateTime
			FROM orders o
			JOIN event e ON o.eventId = e.id
			JOIN orderDetail od ON od.orderId = o.id
			JOIN eventType et ON et.id = od.eventTypeId
			WHERE e.uuid = :eventUuid
			    AND o.createdTime BETWEEN :startDate AND :endDate
			    AND o.paymentStatus = 'SUCCESS'
			ORDER BY o.paymentDateTime DESC
			""", nativeQuery = true)
	List<Map<String, Object>> summarizeRevenueDetail(@Param("eventUuid") String eventUuid,
			@Param("startDate") OffsetDateTime startDate,
			@Param("endDate") OffsetDateTime endDate);

	@Query(value = """
			SELECT
			    od.uuid,
			    e.name AS eventName,
			    o.orderNo AS orderId,
			    o.scbTransactionId AS transactionId,
			    DATE_FORMAT(o.paymentDateTime, '%Y-%m-%d %H:%i:%s') AS paymentDateTime,
			    CONCAT(od.firstName, IFNULL(CONCAT(' ', od.lastName), '')) AS fullName,
			    od.price AS registrationFee,
			    od.couponDiscount AS discountCoupon,
			    od.discountShirt AS discountShirt,
			    od.shippingFee AS shippingFee,
			    od.netPrice AS totalAmount,
			    et.name AS eventTypeName,
			    o.paymentStatus AS paymentStatus,
			    o.paymentMethod AS paymentMethod,
			    DATE_FORMAT(o.createdTime, '%Y-%m-%d %H:%i:%s') AS registrationDateTime
			FROM orders o
			JOIN event e ON o.eventId = e.id
			JOIN orderDetail od ON od.orderId = o.id
			JOIN eventType et ON et.id = od.eventTypeId
			WHERE (:eventUuid IS NULL OR e.uuid = :eventUuid)
			    AND o.createdTime BETWEEN :startDate AND :endDate
			ORDER BY o.paymentDateTime DESC
			""", nativeQuery = true)
	List<Map<String, Object>> summarizeRegistrant(@Param("eventUuid") String eventUuid,
			@Param("startDate") OffsetDateTime startDate,
			@Param("endDate") OffsetDateTime endDate);

	@Query("""
			    SELECT COUNT(od)
			    FROM OrderDetail od
			    JOIN od.order o
			    JOIN o.event e
			    WHERE e.uuid = :eventUuid
			      AND od.active = true
			      AND o.paymentStatus = :status
			""")
	Long countOrdersByStatus(@Param("eventUuid") String eventUuid,
			@Param("status") String status);

	@Query("""
			    SELECT COUNT(od)
			    FROM OrderDetail od
			    JOIN od.order o
			    JOIN o.event e
			    WHERE e.uuid = :eventUuid
			      AND od.active = true
			      AND o.paymentStatus IN :statuses
			""")
	Long countOrdersByStatuses(@Param("eventUuid") String eventUuid,
			@Param("statuses") List<String> statuses);

	@Query(value = """
			SELECT SUM(o.unitPrice)
			FROM orders o
			JOIN event e ON o.eventId = e.id
			WHERE e.uuid = :eventUuid
			  AND o.paymentStatus = 'SUCCESS'
			""", nativeQuery = true)
	Long sumRegistrationUnitPrice(@Param("eventUuid") String eventUuid);

	@Query(value = """
			SELECT SUM(o.shippingFee)
			FROM orders o
			JOIN event e ON o.eventId = e.id
			WHERE e.uuid = :eventUuid
			  AND o.paymentStatus = 'SUCCESS'
			""", nativeQuery = true)
	Long sumShippingFee(@Param("eventUuid") String eventUuid);

	@Query(value = """
			SELECT SUM(o.totalPrice)
			FROM orders o
			JOIN event e ON o.eventId = e.id
			WHERE e.uuid = :eventUuid
			  AND o.paymentStatus = 'SUCCESS'
			""", nativeQuery = true)
	Long sumTotalNetAmount(@Param("eventUuid") String eventUuid);

	@Query("SELECT o FROM Orders o JOIN o.event e WHERE e.uuid = :eventUuid AND o.paymentStatus = 'SUCCESS' AND e.eventDate > CURRENT_TIMESTAMP")
	List<Orders> findSuccessOrdersByEventUuid(@Param("eventUuid") String eventUuid);

	@Query("SELECT o FROM Orders o JOIN o.event e WHERE e.uuid = :eventUuid AND o.paymentStatus = 'SUCCESS' AND o.paymentDateTime < :cutoff AND e.eventDate > CURRENT_TIMESTAMP")
	List<Orders> findSuccessOrdersByEventUuidBeforeCutoff(@Param("eventUuid") String eventUuid, @Param("cutoff") OffsetDateTime cutoff);

	@Query("SELECT o.id FROM Orders o WHERE o.correctionEmailSent = true")
	List<Integer> findOrderIdsWithCorrectionEmailSent();

	@Query("SELECT o FROM Orders o JOIN o.event e WHERE o.paymentStatus = 'SUCCESS' AND e.eventDate > CURRENT_TIMESTAMP")
	List<Orders> findAllSuccessOrders();

}
