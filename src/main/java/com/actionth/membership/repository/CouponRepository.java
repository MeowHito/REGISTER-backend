package com.actionth.membership.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.model.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer>, JpaSpecificationExecutor<Coupon> {
    @Query("SELECT c FROM Coupon c GROUP BY c.bucketName")
    List<Coupon> findGroupedByBucketName();

    @Query("SELECT c FROM Coupon c GROUP BY c.bucketName")
    Page<Coupon> findAllGroupedByBucketName(Pageable pageable);

    @Query(value = """
                SELECT * FROM coupon c
                JOIN (
                    SELECT bucketName, MAX(id) AS maxId
                    FROM coupon
                    GROUP BY bucketName
                ) grouped ON c.id = grouped.maxId
                ORDER BY
                    CASE WHEN :sortField = 'couponName' AND :sortDirection = 'asc' THEN c.couponName END ASC,
                    CASE WHEN :sortField = 'couponName' AND :sortDirection = 'desc' THEN c.couponName END DESC,
                    CASE WHEN :sortField = 'id' AND :sortDirection = 'asc' THEN c.id END ASC,
                    CASE WHEN :sortField = 'id' AND :sortDirection = 'desc' THEN c.id END DESC
            """, countQuery = """
                SELECT COUNT(*) FROM (
                    SELECT bucketName
                    FROM coupon
                    GROUP BY bucketName
                ) AS grouped
            """, nativeQuery = true)
    Page<Coupon> findGroupedByBucketNameWithSorting(
            @Param("sortField") String sortField,
            @Param("sortDirection") String sortDirection,
            Pageable pageable);

    Optional<Coupon> findByUuid(String uuid);

    Optional<Coupon> findFirstByBucketName(String bucketName);

    List<Coupon> findByBucketName(String bucketName);

    Page<Coupon> findByBucketName(String bucketName, Pageable pageable);

    void deleteByUuid(String uuid);

    void deleteByUuidInAndRedeemByIsNull(List<String> uuids);

    void deleteByBucketNameAndStatusAndRedeemByIsNull(String bucketName, String status);

    @Query("""
                SELECT c FROM Coupon c
                WHERE c.event.uuid IN :eventIds
                AND c.type IN :types
                AND c.status = :status
                GROUP BY c.bucketName
            """)
    List<Coupon> findGroupedByBucketNameByEventIdsAndTypeAndStatus(
            @Param("eventIds") List<String> eventIds,
            @Param("types") List<String> types,
            @Param("status") String status);

    @Query("""
                SELECT c FROM Coupon c
                LEFT JOIN c.redeemBy od
                LEFT JOIN od.order o
                WHERE c.couponCode = :couponCode
                AND c.event.uuid = :eventId
                AND c.status = :status
                AND (c.redeemBy IS NULL OR o.uuid = :orderUuid)
                AND (c.startTime IS NULL OR c.startTime <= CURRENT_TIMESTAMP)
                AND (c.expiryTime IS NULL OR c.expiryTime >= CURRENT_TIMESTAMP)
                AND c.active = true
            """)
    List<Coupon> findAllByCouponCodeAndEventIdAndStatusAndRedeemByIsNullOrOrderUuid(
            @Param("couponCode") String couponCode,
            @Param("eventId") String eventId,
            @Param("status") String status,
            @Param("orderUuid") String orderUuid);

    @Query("""
                SELECT c.bucketName, COUNT(c)
                FROM Coupon c
                WHERE c.redeemTime IS NOT NULL
                GROUP BY c.bucketName
            """)
    List<Object[]> countUsedCouponsByBucketName();

    boolean existsByCouponCode(String couponCode);

    List<Coupon> findAllByUuidIn(List<String> uuids);

    Optional<Coupon> findByCouponCode(String couponCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Coupon> findFirstByCouponCodeAndRedeemByIsNull(String couponCode);

    Optional<Coupon> findFirstByCouponCodeAndRedeemByIsNullAndRunnerIdNo(String couponCode, String runnerIdNo);

    Optional<Coupon> findByCouponCodeAndRedeemBy_Id(String couponCode, Integer orderDetailId);

    List<Coupon> findByRedeemBy_IdIn(List<Integer> orderDetailIds);

    @Transactional
    @Modifying
    @Query("""
                UPDATE Coupon c
                   SET c.redeemBy = NULL,
                       c.redeemTime = NULL
                 WHERE c.redeemBy.id IN (
                       SELECT od.id
                         FROM OrderDetail od
                         JOIN od.order o
                        WHERE o.paymentStatus = 'FAILED'
                 )
            """)
    int releaseCouponsFromFailedOrders();

}
