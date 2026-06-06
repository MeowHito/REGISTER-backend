package com.actionth.membership.repository;

import com.actionth.membership.model.OrderRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRequestLogRepository extends JpaRepository<OrderRequestLog, Long> {

    /**
     * Find log entry by correlation ID.
     */
    Optional<OrderRequestLog> findByCorrelationId(String correlationId);

    /**
     * Find log entry by order number.
     */
    Optional<OrderRequestLog> findByOrderNo(String orderNo);

    /**
     * Find all failed requests for an event.
     */
    List<OrderRequestLog> findByEventIdAndStatus(String eventId, String status);

    /**
     * Find all logs created within a time range.
     */
    List<OrderRequestLog> findByCreatedTimeBetween(OffsetDateTime start, OffsetDateTime end);

    /**
     * Find all failed requests within a time range.
     */
    List<OrderRequestLog> findByStatusAndCreatedTimeBetween(String status, OffsetDateTime start, OffsetDateTime end);
}
