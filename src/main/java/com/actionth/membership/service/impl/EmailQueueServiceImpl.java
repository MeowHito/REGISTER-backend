package com.actionth.membership.service.impl;

import com.actionth.membership.config.RabbitConfig;
import com.actionth.membership.model.EmailLog;
import com.actionth.membership.model.EmailQueue;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.dto.EmailAttachmentDTO;
import com.actionth.membership.model.dto.EmailQueueDashboardDto;
import com.actionth.membership.model.dto.EmailQueueDto;
import com.actionth.membership.model.request.SimpleEmailRequest;
import com.actionth.membership.repository.EmailLogRepository;
import com.actionth.membership.repository.EmailQueueRepository;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.service.AppConfigService;
import com.actionth.membership.service.EmailLogService;
import com.actionth.membership.service.EmailQueueService;
import com.actionth.membership.service.PaymentWebhookService;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailQueueServiceImpl implements EmailQueueService {

    private final OrderRepository orderRepository;
    private final EmailQueueRepository queueRepository;
    private final PaymentWebhookService paymentWebhookService;
    private final EmailLogService emailLogService;
    private final EmailLogRepository emailLogRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitConfig rabbitConfig;
    private final AppConfigService appConfigService;

    private static final String CFG_GLOBAL_LIMIT = "email.globalDailyLimit";
    private static final String CFG_QUEUE_LIMIT  = "email.queueDailyLimit";

    /** Read effective global limit: DB override → @Value fallback */
    private int getEffectiveGlobalLimit() {
        return appConfigService.getIntConfig(CFG_GLOBAL_LIMIT, globalDailyLimit);
    }

    /** Read effective queue sub-limit: DB override → @Value fallback */
    private int getEffectiveQueueLimit() {
        return appConfigService.getIntConfig(CFG_QUEUE_LIMIT, queueDailyLimit);
    }

    private static final String TYPE_CORRECTION = "CORRECTION";
    private static final String TYPE_RETRY = "RETRY";
    private static final String TYPE_OUTBOUND = "OUTBOUND";

    /** Correction emails only apply to orders paid before this cutoff (pre-deploy fix) */
    private static final OffsetDateTime CORRECTION_CUTOFF = OffsetDateTime.of(
            2026, 3, 20, 9, 57, 14, 667993000,
            java.time.ZoneOffset.UTC);

    @Value("${email-queue.daily-limit:50}")
    private int queueDailyLimit;

    @Value("${app.email.daily-limit:500}")
    private int globalDailyLimit;

    @Value("${app.env:DEV}")
    private String appEnv;

    @Value("${app.email.test-allowlist:}")
    private String testAllowlistRaw;

    private Set<String> testAllowlist;

    @PostConstruct
    private void initAllowlist() {
        if (testAllowlistRaw != null && !testAllowlistRaw.isBlank()) {
            testAllowlist = new HashSet<>();
            for (String e : testAllowlistRaw.split(",")) {
                testAllowlist.add(e.trim().toLowerCase());
            }
        } else {
            testAllowlist = Collections.emptySet();
        }
    }

    /** Returns true if the email is allowed to receive correction emails in the current env. */
    private boolean isCorrectionAllowed(String email) {
        if ("PROD".equals(appEnv)) return true;
        if (testAllowlist.isEmpty()) return false;
        return testAllowlist.contains(email.trim().toLowerCase());
    }

    private static final ZoneId BANGKOK_ZONE = ZoneId.of("Asia/Bangkok");

    @Override
    public Page<EmailQueueDto> getQueueList(int page, int size, String type, String status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdTime"));

        Specification<EmailQueue> spec = Specification.where(null);
        if (type != null && !type.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return queueRepository.findAll(spec, pageable).map(item -> {
            Orders order = item.getOrder();
            return EmailQueueDto.builder()
                    .id(item.getId())
                    .uuid(item.getUuid())
                    .type(item.getType())
                    .orderNo(order != null ? order.getOrderNo() : null)
                    .orderUuid(order != null ? order.getUuid() : null)
                    .eventName(order != null && order.getEvent() != null ? order.getEvent().getName() : null)
                    .eventUuid(order != null && order.getEvent() != null ? order.getEvent().getUuid() : null)
                    .recipientEmail(item.getRecipientEmail())
                    .subject(item.getSubject())
                    .emailLogId(item.getEmailLogId())
                    .status(item.getStatus())
                    .retryCount(item.getRetryCount())
                    .createdTime(item.getCreatedTime())
                    .processedAt(item.getProcessedAt())
                    .errorMessage(item.getErrorMessage())
                    .alreadySent(order != null && Boolean.TRUE.equals(order.getCorrectionEmailSent()))
                    .build();
        });
    }

    @Override
    public EmailQueueDashboardDto getDashboard() {
        OffsetDateTime startOfDay = LocalDate.now(BANGKOK_ZONE)
                .atStartOfDay(BANGKOK_ZONE)
                .toOffsetDateTime();

        long totalQueued = queueRepository.count();
        long pendingCount = queueRepository.count(statusSpec("PENDING"));
        long sentCount = queueRepository.count(statusSpec("SENT"));
        long failedCount = queueRepository.count(statusSpec("FAILED"));

        long queueSentToday = queueRepository.countSentTodayByType(TYPE_CORRECTION, startOfDay)
                + queueRepository.countSentTodayByType(TYPE_RETRY, startOfDay);
        int effectiveQueueLimit = getEffectiveQueueLimit();
        int queueRemainingToday = (int) Math.max(0, effectiveQueueLimit - queueSentToday);

        long globalSentToday = emailLogRepository.countEmailsToday(startOfDay);
        int effectiveGlobalLimit = getEffectiveGlobalLimit();
        int globalRemainingToday = (int) Math.max(0, effectiveGlobalLimit - globalSentToday);

        return EmailQueueDashboardDto.builder()
                .totalQueued(totalQueued)
                .pendingCount(pendingCount)
                .sentCount(sentCount)
                .failedCount(failedCount)
                .queueSentToday(queueSentToday)
                .queueDailyLimit(effectiveQueueLimit)
                .queueRemainingToday(queueRemainingToday)
                .globalSentToday(globalSentToday)
                .globalDailyLimit(effectiveGlobalLimit)
                .globalRemainingToday(globalRemainingToday)
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> processQueue() {
        Map<String, Object> result = new HashMap<>();

        OffsetDateTime startOfDay = LocalDate.now(BANGKOK_ZONE)
                .atStartOfDay(BANGKOK_ZONE)
                .toOffsetDateTime();

        // Global limit: counts ALL email attempts today (by createdAt for consistency)
        long globalSentToday = emailLogRepository.countEmailsToday(startOfDay);
        int effectiveGlobalLimit = getEffectiveGlobalLimit();
        long globalRemaining = Math.max(0, effectiveGlobalLimit - globalSentToday);

        if (globalRemaining <= 0) {
            result.put("success", false);
            result.put("message", String.format("ถึงขีดจำกัดอีเมลระบบวันนี้แล้ว (%d/วัน)", effectiveGlobalLimit));
            result.put("globalSentToday", globalSentToday);
            return result;
        }

        // Queue sub-limit: counts only CORRECTION + RETRY (not OUTBOUND)
        long batchSentToday = queueRepository.countSentTodayByType(TYPE_CORRECTION, startOfDay)
                + queueRepository.countSentTodayByType(TYPE_RETRY, startOfDay);
        long queueRemaining = Math.max(0, getEffectiveQueueLimit() - batchSentToday);

        int totalProcessed = 0;
        int totalSent = 0;
        int totalFailed = 0;
        List<String> failedDetails = new ArrayList<>();

        // --- Phase 1: OUTBOUND (deferred transactional emails) — global limit only ---
        List<EmailQueue> outboundItems = queueRepository.findPendingQueueByType(TYPE_OUTBOUND);
        if (!outboundItems.isEmpty()) {
            int outboundLimit = (int) Math.min(globalRemaining, outboundItems.size());
            for (int i = 0; i < outboundLimit; i++) {
                EmailQueue item = outboundItems.get(i);
                int claimed = queueRepository.claimItem(item.getId(), OffsetDateTime.now());
                if (claimed == 0) {
                    log.debug("[EmailQueue] OUTBOUND item id={} already claimed, skipping", item.getId());
                    continue;
                }
                try {
                    processQueueItem(item);
                    item.setStatus("SENT");
                    item.setProcessedAt(OffsetDateTime.now());
                    queueRepository.save(item);
                    totalSent++;
                } catch (Exception e) {
                    String detail = TYPE_OUTBOUND + ":" + item.getRecipientEmail();
                    log.error("[EmailQueue] OUTBOUND failed for {}: {}", detail, e.getMessage());
                    item.setStatus("FAILED");
                    item.setRetryCount(item.getRetryCount() + 1);
                    item.setProcessedAt(OffsetDateTime.now());
                    item.setErrorMessage(e.getMessage());
                    queueRepository.save(item);
                    totalFailed++;
                    failedDetails.add(detail);
                }
                totalProcessed++;
            }
            globalRemaining -= totalSent;
        }

        // --- Phase 2: CORRECTION + RETRY — queue sub-limit + remaining global limit ---
        if (globalRemaining > 0 && queueRemaining > 0) {
            List<EmailQueue> batchItems = queueRepository.findPendingQueueExcludingType(TYPE_OUTBOUND);
            if (!batchItems.isEmpty()) {
                int batchLimit = (int) Math.min(Math.min(queueRemaining, globalRemaining), batchItems.size());
                for (int i = 0; i < batchLimit; i++) {
                    EmailQueue item = batchItems.get(i);
                    int claimed = queueRepository.claimItem(item.getId(), OffsetDateTime.now());
                    if (claimed == 0) {
                        log.debug("[EmailQueue] Item id={} already claimed, skipping", item.getId());
                        continue;
                    }
                    try {
                        processQueueItem(item);
                        item.setStatus("SENT");
                        item.setProcessedAt(OffsetDateTime.now());
                        queueRepository.save(item);
                        totalSent++;
                    } catch (Exception e) {
                        String detail = item.getType() + ":" +
                                (item.getOrder() != null ? item.getOrder().getOrderNo() : item.getRecipientEmail());
                        log.error("[EmailQueue] Failed for {}: {}", detail, e.getMessage());
                        item.setStatus("FAILED");
                        item.setRetryCount(item.getRetryCount() + 1);
                        item.setProcessedAt(OffsetDateTime.now());
                        item.setErrorMessage(e.getMessage());
                        queueRepository.save(item);
                        totalFailed++;
                        failedDetails.add(detail);
                    }
                    totalProcessed++;
                }
            }
        }

        if (totalProcessed == 0) {
            result.put("success", true);
            result.put("message", "ไม่มีอีเมลรอส่ง");
            result.put("processedCount", 0);
            return result;
        }

        result.put("success", true);
        result.put("message", String.format("ประมวลผลคิวสำเร็จ %d/%d รายการ", totalSent, totalProcessed));
        result.put("processedCount", totalProcessed);
        result.put("sentCount", totalSent);
        result.put("failedCount", totalFailed);
        if (!failedDetails.isEmpty()) {
            result.put("failedDetails", failedDetails);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> enqueueCorrectionByEvent(String eventUuid) {
        Map<String, Object> result = new HashMap<>();

        List<Orders> successOrders = orderRepository.findSuccessOrdersByEventUuidBeforeCutoff(eventUuid, CORRECTION_CUTOFF);
        if (successOrders.isEmpty()) {
            result.put("success", false);
            result.put("message", "ไม่พบคำสั่งซื้อที่ชำระเงินแล้วก่อนวันที่กำหนดสำหรับกิจกรรมนี้");
            result.put("enqueuedCount", 0);
            return result;
        }

        Set<Integer> alreadySentOrderIds = new HashSet<>(orderRepository.findOrderIdsWithCorrectionEmailSent());
        int enqueuedCount = 0;
        int skippedAlreadySent = 0;
        int skippedAlreadyQueued = 0;
        int skippedNotAllowed = 0;

        for (Orders order : successOrders) {
            if (alreadySentOrderIds.contains(order.getId())) {
                skippedAlreadySent++;
            } else if (queueRepository.existsByOrderIdAndType(order.getId(), TYPE_CORRECTION)) {
                skippedAlreadyQueued++;
            } else {
                String email = order.getCreatedBy() != null ? order.getCreatedBy().getEmail() : null;
                if (email != null && !email.isBlank()) {
                    if (!isCorrectionAllowed(email)) {
                        log.debug("[EmailQueue] Correction skipped (not in test allowlist, env={}): order={}, email={}",
                                appEnv, order.getOrderNo(), email);
                        skippedNotAllowed++;
                        continue;
                    }
                    EmailQueue queueItem = EmailQueue.builder()
                            .type(TYPE_CORRECTION)
                            .order(order)
                            .recipientEmail(email)
                            .subject("[ประกาศสำคัญ] ยืนยันการสมัครเข้าร่วมกิจกรรม " +
                                    (order.getEvent() != null ? order.getEvent().getName().trim() : ""))
                            .status("PENDING")
                            .retryCount(0)
                            .scheduledAt(OffsetDateTime.now())
                            .build();
                    queueRepository.save(queueItem);
                    enqueuedCount++;
                }
            }
        }

        result.put("success", true);
        result.put("message", String.format("เพิ่มคิวส่งอีเมลแจ้งแก้ไขข้อมูล %d รายการ (ส่งแล้ว: %d, อยู่ในคิว: %d, ไม่อนุญาต: %d)",
                enqueuedCount, skippedAlreadySent, skippedAlreadyQueued, skippedNotAllowed));
        result.put("enqueuedCount", enqueuedCount);
        result.put("skippedAlreadySent", skippedAlreadySent);
        result.put("skippedAlreadyQueued", skippedAlreadyQueued);
        result.put("skippedNotAllowed", skippedNotAllowed);
        result.put("totalOrders", successOrders.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> enqueueCorrectionSingle(String orderUuid) {
        Map<String, Object> result = new HashMap<>();

        Optional<Orders> orderOpt = orderRepository.findByUuid(orderUuid);
        if (orderOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "ไม่พบคำสั่งซื้อ");
            return result;
        }

        Orders order = orderOpt.get();
        if (!"SUCCESS".equals(order.getPaymentStatus())) {
            result.put("success", false);
            result.put("message", "คำสั่งซื้อนี้ยังไม่ได้ชำระเงิน");
            return result;
        }

        if (order.getPaymentDateTime() == null || !order.getPaymentDateTime().isBefore(CORRECTION_CUTOFF)) {
            result.put("success", false);
            result.put("message", "คำสั่งซื้อนี้ชำระเงินหลังวันที่กำหนด ไม่จำเป็นต้องส่งอีเมลแจ้งแก้ไขข้อมูล");
            return result;
        }

        if (Boolean.TRUE.equals(order.getCorrectionEmailSent())) {
            result.put("success", false);
            result.put("message", "ส่งอีเมลแจ้งแก้ไขข้อมูลให้คำสั่งซื้อนี้ไปแล้ว");
            return result;
        }

        if (queueRepository.existsByOrderIdAndType(order.getId(), TYPE_CORRECTION)) {
            result.put("success", false);
            result.put("message", "คำสั่งซื้อนี้อยู่ในคิวส่งอีเมลแล้ว");
            return result;
        }

        String email = order.getCreatedBy() != null ? order.getCreatedBy().getEmail() : null;
        if (email == null || email.isBlank()) {
            result.put("success", false);
            result.put("message", "ไม่พบอีเมลของผู้สั่งซื้อ");
            return result;
        }

        if (!isCorrectionAllowed(email)) {
            result.put("success", false);
            result.put("message", String.format("อีเมล %s ไม่ได้รับอนุญาตในสภาพแวดล้อม %s", email, appEnv));
            return result;
        }

        EmailQueue queueItem = EmailQueue.builder()
                .type(TYPE_CORRECTION)
                .order(order)
                .recipientEmail(email)
                .subject("[ประกาศสำคัญ] ยืนยันการสมัครเข้าร่วมกิจกรรม " +
                        (order.getEvent() != null ? order.getEvent().getName().trim() : ""))
                .status("PENDING")
                .retryCount(0)
                .scheduledAt(OffsetDateTime.now())
                .build();
        queueRepository.save(queueItem);

        result.put("success", true);
        result.put("message", "เพิ่มคิวส่งอีเมลแจ้งแก้ไขข้อมูลสำเร็จ");
        result.put("orderNo", order.getOrderNo());
        return result;
    }

    @Override
    @Transactional
    public void sendOrQueue(SimpleEmailRequest request) {
        OffsetDateTime startOfDay = LocalDate.now(BANGKOK_ZONE)
                .atStartOfDay(BANGKOK_ZONE)
                .toOffsetDateTime();

        long globalSentToday = emailLogRepository.countEmailsToday(startOfDay);
        boolean underLimit = globalSentToday < getEffectiveGlobalLimit();

        Long emailLogId = null;
        if (request.getEmailLogId() != null && !request.getEmailLogId().isBlank()) {
            try {
                emailLogId = Long.parseLong(request.getEmailLogId());
            } catch (NumberFormatException e) {
                log.warn("[EmailQueue] sendOrQueue: invalid emailLogId '{}', tracking will be incomplete", request.getEmailLogId());
            }
        }

        if (underLimit) {
            rabbitTemplate.convertAndSend(rabbitConfig.getSimpleEmailQueueName(), request);

            EmailQueue queueItem = EmailQueue.builder()
                    .type(TYPE_OUTBOUND)
                    .emailLogId(emailLogId)
                    .recipientEmail(request.getTo())
                    .subject(request.getSubject())
                    .status("SENT")
                    .retryCount(0)
                    .scheduledAt(OffsetDateTime.now())
                    .processedAt(OffsetDateTime.now())
                    .build();
            queueRepository.save(queueItem);

            log.debug("[EmailQueue] OUTBOUND dispatched immediately: to={}", request.getTo());
        } else {
            EmailQueue queueItem = EmailQueue.builder()
                    .type(TYPE_OUTBOUND)
                    .emailLogId(emailLogId)
                    .recipientEmail(request.getTo())
                    .subject(request.getSubject())
                    .status("PENDING")
                    .retryCount(0)
                    .scheduledAt(OffsetDateTime.now())
                    .build();
            queueRepository.save(queueItem);

            log.warn("[EmailQueue] OUTBOUND deferred — global limit reached ({}/{}): to={}",
                    globalSentToday, getEffectiveGlobalLimit(), request.getTo());
        }
    }

    @Override
    public void sendDirectToRabbit(SimpleEmailRequest request) {
        rabbitTemplate.convertAndSend(rabbitConfig.getSimpleEmailQueueName(), request);
        log.debug("[EmailQueue] Sent directly to RabbitMQ (queue processing): to={}", request.getTo());
    }

    private void processQueueItem(EmailQueue item) {
        if (TYPE_CORRECTION.equals(item.getType())) {
            Orders order = item.getOrder();
            Long logId = paymentWebhookService.resendSuccessEmailWithCorrectionDirect(order, item.getEmailLogId());

            // Save emailLogId back to queue item for tracking
            if (item.getEmailLogId() == null && logId != null) {
                item.setEmailLogId(logId);
            }

            order.setCorrectionEmailSent(true);
            orderRepository.save(order);
        } else if (TYPE_RETRY.equals(item.getType()) || TYPE_OUTBOUND.equals(item.getType())) {
            processRetryItem(item);
        } else {
            log.warn("[EmailQueue] Unknown type '{}', skipping item id={}", item.getType(), item.getId());
        }
    }

    private void processRetryItem(EmailQueue item) {
        Long logId = item.getEmailLogId();
        if (logId == null) {
            throw new RuntimeException("RETRY queue item has no emailLogId, queue id=" + item.getId());
        }

        EmailLog originalLog = emailLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("EmailLog not found: id=" + logId));

        SimpleEmailRequest request = new SimpleEmailRequest();
        request.setTo(originalLog.getRecipientTo());
        request.setCc(originalLog.getRecipientCc());
        request.setSubject(originalLog.getSubject());
        request.setBody(originalLog.getEmailBody());
        request.setEmailLogId(originalLog.getId().toString());

        if (Boolean.TRUE.equals(originalLog.getHasAttachments())) {
            List<EmailAttachmentDTO> attachments = emailLogService.getAttachmentDTOs(originalLog.getId());
            if (attachments != null && !attachments.isEmpty()) {
                request.setAttachments(attachments);
            }
        }

        rabbitTemplate.convertAndSend(rabbitConfig.getSimpleEmailQueueName(), request);

        log.info("[EmailQueue] RETRY sent via RabbitMQ: EmailLog ID={}, To={}",
                originalLog.getId(), originalLog.getRecipientTo());
    }

    private Specification<EmailQueue> statusSpec(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    @Override
    public Map<String, Integer> getEmailLimitsConfig() {
        Map<String, Integer> config = new HashMap<>();
        config.put("globalDailyLimit", getEffectiveGlobalLimit());
        config.put("queueDailyLimit", getEffectiveQueueLimit());
        return config;
    }

    @Override
    public void cleanupStaleProcessing() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(30);
        int reset = queueRepository.resetStaleProcessingItems(cutoff);
        if (reset > 0) {
            log.warn("[EmailQueue] Reset {} stale PROCESSING items back to PENDING", reset);
        }
    }

    @Override
    public void updateEmailLimitsConfig(int newGlobalLimit, int newQueueLimit) {
        if (newGlobalLimit < 1 || newQueueLimit < 1) {
            throw new IllegalArgumentException("Limits must be at least 1");
        }
        appConfigService.setConfig(CFG_GLOBAL_LIMIT, String.valueOf(newGlobalLimit));
        appConfigService.setConfig(CFG_QUEUE_LIMIT, String.valueOf(newQueueLimit));
        log.info("[EmailQueue] Limits updated — global: {}/day, queue: {}/day", newGlobalLimit, newQueueLimit);
    }
}
