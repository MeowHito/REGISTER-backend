package com.actionth.membership.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.OrderAddOn;

@Repository
public interface OrderAddOnRepository extends JpaRepository<OrderAddOn, Integer> {

	/**
	 * Units of one add-on already committed. Mirrors the quota rule used for
	 * pricing: an order counts as soon as it exists (PENDING) and keeps counting
	 * while it is under review or paid, so two buyers cannot claim the last room.
	 */
	@Query("""
			SELECT COALESCE(SUM(oa.qty), 0)
			FROM OrderAddOn oa
			JOIN oa.order o
			WHERE o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW') AND oa.addOn.uuid = :addOnId
			""")
	Long sumUsedQtyByAddOnUuid(@Param("addOnId") String addOnId);

	@Query("""
			SELECT oa.addOn.uuid, COALESCE(SUM(oa.qty), 0)
			FROM OrderAddOn oa
			JOIN oa.order o
			WHERE o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW') AND oa.addOn.event.id = :eventId
			GROUP BY oa.addOn.uuid
			""")
	List<Object[]> sumUsedQtyByEventId(@Param("eventId") Integer eventId);

	@Query("""
			SELECT COUNT(oa) > 0
			FROM OrderAddOn oa
			JOIN oa.order o
			WHERE o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW') AND oa.addOn.id = :addOnId
			""")
	boolean existsByAddOnIdAndActiveOrder(@Param("addOnId") Integer addOnId);

	/**
	 * Add-on sales of one event for the finance report: one row per add-on and
	 * snapshotted unit price, paid orders only, same date window as the
	 * registration rows.
	 */
	@Query(value = """
			SELECT
			    oa.name AS name,
			    oa.unitPrice AS unitPrice,
			    SUM(oa.qty) AS qty,
			    SUM(oa.totalPrice) AS total
			FROM orderAddOn oa
			JOIN orders o ON oa.orderId = o.id
			JOIN event e ON o.eventId = e.id
			WHERE e.uuid = :eventUuid
			    AND o.createdTime BETWEEN :startDate AND :endDate
			    AND o.paymentStatus = 'SUCCESS'
			    AND (oa.active IS NULL OR oa.active = 1)
			GROUP BY oa.addOnId, oa.name, oa.unitPrice
			ORDER BY MIN(oa.id)
			""", nativeQuery = true)
	List<Map<String, Object>> summarizeAddOnFinance(@Param("eventUuid") String eventUuid,
			@Param("startDate") OffsetDateTime startDate,
			@Param("endDate") OffsetDateTime endDate);
}
