package com.actionth.membership.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.OrderDetailShirt;

@Repository
public interface OrderDetailShirtRepository extends JpaRepository<OrderDetailShirt, Integer> {

    @Query("""
            SELECT COUNT(s) > 0
            FROM OrderDetailShirt s
            JOIN s.orderDetail od
            JOIN od.order o
            WHERE s.shirtSize.id = :shirtSizeId
              AND o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')
            """)
    boolean existsByShirtSizeIdAndActiveOrder(@Param("shirtSizeId") Integer shirtSizeId);

    @Query("""
            SELECT COUNT(s) > 0
            FROM OrderDetailShirt s
            JOIN s.orderDetail od
            JOIN od.order o
            WHERE s.shirtType.id = :shirtTypeId
              AND o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')
            """)
    boolean existsByShirtTypeIdAndActiveOrder(@Param("shirtTypeId") Integer shirtTypeId);

    /** Rows: [shirtSize uuid, count] over the extra shirts of every committed order of the event. */
    @Query("""
            SELECT s.shirtSize.uuid, COUNT(s)
            FROM OrderDetailShirt s
            JOIN s.orderDetail od
            JOIN od.order o
            WHERE o.event.id = :eventId
              AND s.shirtSize IS NOT NULL
              AND o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')
            GROUP BY s.shirtSize.uuid
            """)
    List<Object[]> countBySizeForEvent(@Param("eventId") Integer eventId);
}
