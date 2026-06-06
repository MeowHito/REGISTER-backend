package com.actionth.membership.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.EmailLog;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Page<EmailLog> findBySendStatus(String status, Pageable pageable);

    Page<EmailLog> findByRecipientTo(String toEmail, Pageable pageable);

    @Query("SELECT e FROM EmailLog e WHERE e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    Page<EmailLog> findByDateRange(@Param("startDate") OffsetDateTime startDate, 
                                    @Param("endDate") OffsetDateTime endDate, 
                                    Pageable pageable);

    List<EmailLog> findBySendStatusAndRetryCountLessThan(String status, Integer maxRetries);

    long countBySendStatus(String status);

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.sendStatus = 'SENT' AND e.sentAt >= :startOfDay")
    long countSentToday(@Param("startOfDay") OffsetDateTime startOfDay);

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.createdAt >= :startOfDay")
    long countEmailsToday(@Param("startOfDay") OffsetDateTime startOfDay);

    Page<EmailLog> findByOrderId(String orderId, Pageable pageable);

    /**
     * Find failed emails with daily sending limit exceeded error
     * that match specific registration or payment confirmation subjects
     */
    @Query("SELECT e FROM EmailLog e WHERE " +
           "e.sendStatus = 'FAILED' AND " +
           "e.retryCount < :maxRetries AND " +
           "(e.subject LIKE 'ยืนยันการสมัคร%' OR e.subject LIKE 'ชำระเงินการ%') AND " +
           "e.recipientTo IS NOT NULL AND e.recipientTo != '' AND " +
           "e.errorMessage LIKE '%Failed messages: com.sun.mail.smtp.SMTPSendFailedException: 550-5.4.5 Daily user sending limit exceeded%' " +
           "ORDER BY e.createdAt ASC")
    List<EmailLog> findFailedEmailsWithDailyLimitExceeded(@Param("maxRetries") Integer maxRetries, Pageable pageable);
}
