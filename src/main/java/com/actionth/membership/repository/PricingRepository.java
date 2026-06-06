package com.actionth.membership.repository;

import com.actionth.membership.model.Pricing;
import com.actionth.membership.projection.PricingAvailabilityProjection;
import com.actionth.membership.projection.UuidIdProjection;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PricingRepository extends JpaRepository<Pricing, Integer> {
    Optional<Pricing> findByUuid(String uuid);
    List<UuidIdProjection> findAllByUuidIn(Set<String> uuids);

    @Query(value = """
            SELECT COALESCE(SUM(p.quota), 0)
            FROM pricing p
            JOIN eventType et ON et.id = p.eventTypeId
            JOIN paymentType pt ON pt.id = p.paymentTypeId
            WHERE et.id = :eventTypeId
                AND (pt.endDate IS NULL OR pt.endDate >= NOW())
            """, nativeQuery = true)
    Long sumActivePricingQuotaByEventTypeId(@Param("eventTypeId") Integer eventTypeId);

    @Query(value = """
            SELECT 
                p.uuid AS pricingUuid,
                p.price AS price,
                p.quota AS quota,
                pt.name AS paymentName,
                COALESCE((SELECT COUNT(od.id) FROM orderDetail od 
                    JOIN orders o ON o.id = od.orderId 
                    JOIN pricing pr ON pr.id = od.pricingId 
                    WHERE pr.uuid = p.uuid AND o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')), 0) AS usedQuota
            FROM pricing p
            JOIN eventType et ON et.id = p.eventTypeId
            JOIN event e ON e.id = et.eventId
            JOIN paymentType pt ON pt.id = p.paymentTypeId
            WHERE et.id = :eventTypeId
                AND (e.startRegistrationDate IS NULL OR e.startRegistrationDate <= NOW())
                AND (pt.endDate IS NULL OR pt.endDate >= NOW())
            HAVING usedQuota < p.quota
            ORDER BY pt.endDate ASC
            """, nativeQuery = true)
    List<PricingAvailabilityProjection> findAvailablePricingWithQuota(@Param("eventTypeId") Integer eventTypeId);

    @Query(value = """
            SELECT 
                p.uuid AS pricingUuid,
                p.price AS price,
                p.quota AS quota,
                pt.name AS paymentName,
                COALESCE((SELECT COUNT(od.id) FROM orderDetail od 
                    JOIN orders o ON o.id = od.orderId 
                    JOIN pricing pr ON pr.id = od.pricingId 
                    WHERE pr.uuid = p.uuid AND o.paymentStatus IN ('SUCCESS', 'PENDING', 'REVIEW')), 0) AS usedQuota
            FROM pricing p
            JOIN eventType et ON et.id = p.eventTypeId
            JOIN paymentType pt ON pt.id = p.paymentTypeId
            WHERE et.id = :eventTypeId
            ORDER BY pt.endDate ASC
            """, nativeQuery = true)
    List<PricingAvailabilityProjection> findAllPricingsWithQuota(@Param("eventTypeId") Integer eventTypeId);
}
