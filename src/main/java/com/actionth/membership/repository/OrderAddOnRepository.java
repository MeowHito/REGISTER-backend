package com.actionth.membership.repository;

import java.util.List;

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
}
