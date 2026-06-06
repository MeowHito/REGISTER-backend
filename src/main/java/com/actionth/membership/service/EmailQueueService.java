package com.actionth.membership.service;

import com.actionth.membership.model.dto.EmailQueueDashboardDto;
import com.actionth.membership.model.dto.EmailQueueDto;
import com.actionth.membership.model.request.SimpleEmailRequest;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface EmailQueueService {

    /** Get paginated queue list for backoffice UI (all types, optional type/status filter) */
    Page<EmailQueueDto> getQueueList(int page, int size, String type, String status);

    /** Get dashboard stats (all types) */
    EmailQueueDashboardDto getDashboard();

    /** Process the pending email queue (called by scheduler or admin), respecting daily limit */
    Map<String, Object> processQueue();

    /** Enqueue correction emails for all SUCCESS orders of a given event */
    Map<String, Object> enqueueCorrectionByEvent(String eventUuid);

    /** Enqueue a single order's correction email */
    Map<String, Object> enqueueCorrectionSingle(String orderUuid);

    /**
     * Send email via RabbitMQ immediately if under global daily limit,
     * otherwise save as PENDING in email queue for deferred delivery.
     * Used by EmailService for all outbound transactional emails.
     */
    void sendOrQueue(SimpleEmailRequest request);

    /**
     * Send email directly to RabbitMQ, bypassing queue record creation.
     * Used when processing queue items to avoid duplicate OUTBOUND records.
     */
    void sendDirectToRabbit(SimpleEmailRequest request);

    /** Get current email limit configuration (globalDailyLimit, queueDailyLimit) */
    Map<String, Integer> getEmailLimitsConfig();

    /** Persist new limit values to AppConfig so they take effect immediately without restart */
    void updateEmailLimitsConfig(int globalDailyLimit, int queueDailyLimit);

    /** Reset stale PROCESSING items (stuck > 30 min) back to PENDING */
    void cleanupStaleProcessing();
}
