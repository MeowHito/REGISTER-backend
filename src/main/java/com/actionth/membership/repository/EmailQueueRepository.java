package com.actionth.membership.repository;

import com.actionth.membership.model.EmailQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueue, Integer>, JpaSpecificationExecutor<EmailQueue> {

    @Query("SELECT q FROM EmailQueue q WHERE q.status = 'PENDING' ORDER BY q.createdTime ASC")
    List<EmailQueue> findPendingQueue();

    @Query("SELECT q FROM EmailQueue q WHERE q.status = 'PENDING' AND q.type = :type ORDER BY q.createdTime ASC")
    List<EmailQueue> findPendingQueueByType(@Param("type") String type);

    @Query("SELECT q FROM EmailQueue q WHERE q.status = 'PENDING' AND q.type != :type ORDER BY q.createdTime ASC")
    List<EmailQueue> findPendingQueueExcludingType(@Param("type") String type);

    @Query("SELECT COUNT(q) FROM EmailQueue q WHERE q.status = 'SENT' AND q.processedAt >= :startOfDay")
    long countSentToday(@Param("startOfDay") OffsetDateTime startOfDay);

    @Query("SELECT COUNT(q) FROM EmailQueue q WHERE q.status = 'SENT' AND q.type = :type AND q.processedAt >= :startOfDay")
    long countSentTodayByType(@Param("type") String type, @Param("startOfDay") OffsetDateTime startOfDay);

    boolean existsByOrderIdAndType(Integer orderId, String type);

    boolean existsByEmailLogIdAndType(Long emailLogId, String type);

    /**
     * Atomically claim a PENDING item by setting its status to PROCESSING.
     * Runs in REQUIRES_NEW so the status change is visible to concurrent transactions immediately.
     * @return 1 if claimed, 0 if already claimed by another process
     */
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE EmailQueue q SET q.status = 'PROCESSING', q.updatedTime = :now WHERE q.id = :id AND q.status = 'PENDING'")
    int claimItem(@Param("id") Integer id, @Param("now") OffsetDateTime now);

    /**
     * Reset stale PROCESSING items (stuck longer than cutoff) back to PENDING.
     */
    @Modifying
    @Transactional
    @Query("UPDATE EmailQueue q SET q.status = 'PENDING' WHERE q.status = 'PROCESSING' AND q.updatedTime < :cutoff")
    int resetStaleProcessingItems(@Param("cutoff") OffsetDateTime cutoff);
}
