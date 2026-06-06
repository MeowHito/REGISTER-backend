package com.actionth.membership.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import com.actionth.membership.config.RabbitConfig;
import com.actionth.membership.model.EmailAttachmentLog;
import com.actionth.membership.model.EmailLog;
import com.actionth.membership.model.EmailQueue;
import com.actionth.membership.model.dto.EmailAttachmentDTO;
import com.actionth.membership.model.request.SimpleEmailRequest;
import com.actionth.membership.repository.EmailAttachmentLogRepository;
import com.actionth.membership.repository.EmailLogRepository;
import com.actionth.membership.repository.EmailQueueRepository;


import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailLogService {

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private EmailAttachmentLogRepository emailAttachmentLogRepository;

    @Autowired
    private AWSService awsService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitConfig rabbitConfig;

    @Autowired
    private EmailQueueRepository emailQueueRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final String TYPE_RETRY = "RETRY";


    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_RETRYING = "RETRYING";

    /**
     * Create a new email log entry when email is queued.
     * Uses REQUIRES_NEW to commit independently of outer transaction,
     * ensuring the log record is visible to RabbitMQ consumers immediately.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailLog createLog(SimpleEmailRequest request) {
        EmailLog emailLog = new EmailLog();
        emailLog.setOrderId(request.getOrderId());
        emailLog.setRecipientTo(request.getTo());
        emailLog.setRecipientCc(request.getCc());
        emailLog.setSubject(request.getSubject());
        emailLog.setEmailBody(request.getBody());
        emailLog.setHasAttachments(request.getAttachments() != null && !request.getAttachments().isEmpty());
        emailLog.setAttachmentCount(request.getAttachments() != null ? request.getAttachments().size() : 0);
        emailLog.setSendStatus(STATUS_PENDING);
        emailLog.setRetryCount(0);
        
        EmailLog saved = emailLogRepository.save(emailLog);

        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            String s3Prefix = "email-logs/" + saved.getId() + "/";
            
            for (EmailAttachmentDTO attachmentDTO : request.getAttachments()) {
                File tempFile = null;
                try {
                    String ext = "";
                    if (attachmentDTO.getFilename().contains(".")) {
                        ext = attachmentDTO.getFilename().substring(attachmentDTO.getFilename().lastIndexOf("."));
                    }
                    tempFile = File.createTempFile("email-att-" + UUID.randomUUID(), ext);
                    
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        fos.write(attachmentDTO.getContent());
                    }

                    String uploadedFileName = awsService.uploadFile(s3Prefix, tempFile, false);

                    EmailAttachmentLog attachmentLog = new EmailAttachmentLog();
                    attachmentLog.setEmailLog(saved);
                    attachmentLog.setFilename(attachmentDTO.getFilename());
                    attachmentLog.setContentType(attachmentDTO.getContentType());
                    attachmentLog.setPrefix(s3Prefix);
                    attachmentLog.setFileKey(uploadedFileName);
                    
                    emailAttachmentLogRepository.save(attachmentLog);

                } catch (Exception e) {
                    log.error("Failed to upload attachment {} for log id: {}", attachmentDTO.getFilename(), saved.getId(), e);
                } finally {
                    if (tempFile != null && tempFile.exists()) {
                        try {
                            Files.delete(tempFile.toPath());
                        } catch (java.io.IOException e) {
                            log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath(), e);
                        }
                    }
                }
            }
        }
        
        return saved;
    }

    /**
     * Get attachments for a specific log, downloading content from S3
     */
    public List<EmailAttachmentDTO> getAttachmentDTOs(Long logId) {
        List<EmailAttachmentLog> logs = emailAttachmentLogRepository.findByEmailLogId(logId);
        List<EmailAttachmentDTO> dtos = new ArrayList<>();
        
        for (EmailAttachmentLog attLog : logs) {
            try {
                String urlStr = awsService.getSharedUrl(attLog.getPrefix(), attLog.getFileKey(), false);
                if (urlStr != null) {
                    URL url = new URL(urlStr);
                    try (BufferedInputStream in = new BufferedInputStream(url.openStream());
                        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer, 0, 1024)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        
                        dtos.add(new EmailAttachmentDTO(attLog.getFilename(), out.toByteArray(), attLog.getContentType()));
                    }
                }
            } catch (Exception e) {
                log.error("Failed to download attachment {} for log id: {}", attLog.getFilename(), logId, e);
            }
        }
        return dtos;
    }

    /**
     * Get raw attachment logs
     */
    public List<EmailAttachmentLog> getAttachments(Long logId) {
        return emailAttachmentLogRepository.findByEmailLogId(logId);
    }

    /**
     * Update log to mark email as successfully sent.
     * Validates state transition: only PENDING or RETRYING → SENT is allowed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsSent(Long logId) {
        log.info("markAsSent called for logId: {}", logId);
        java.util.Optional<EmailLog> opt = emailLogRepository.findById(logId);
        if (opt.isEmpty()) {
            log.error("Email log not found for ID: {}. Cannot mark as sent.", logId);
            return;
        }
        EmailLog emailLog = opt.get();
        String current = emailLog.getSendStatus();
        if (STATUS_SENT.equals(current)) {
            log.info("Email log ID: {} already SENT, skipping", logId);
            return;
        }
        if (!STATUS_PENDING.equals(current) && !STATUS_RETRYING.equals(current)) {
            log.warn("Email log ID: {} has status '{}', cannot transition to SENT. Skipping.", logId, current);
            return;
        }
        emailLog.setSendStatus(STATUS_SENT);
        emailLog.setSentAt(OffsetDateTime.now());
        emailLogRepository.save(emailLog);
        log.info("Email log ID: {} marked as SENT", logId);
    }

    /**
     * Update log to mark email as failed.
     * Validates state transition: only PENDING or RETRYING → FAILED is allowed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long logId, String errorMessage) {
        log.info("markAsFailed called for logId: {}, error: {}", logId, errorMessage);
        java.util.Optional<EmailLog> opt = emailLogRepository.findById(logId);
        if (opt.isEmpty()) {
            log.error("Email log not found for ID: {}. Cannot mark as failed.", logId);
            return;
        }
        EmailLog emailLog = opt.get();
        String current = emailLog.getSendStatus();
        if (STATUS_FAILED.equals(current)) {
            log.info("Email log ID: {} already FAILED, skipping", logId);
            return;
        }
        if (!STATUS_PENDING.equals(current) && !STATUS_RETRYING.equals(current)) {
            log.warn("Email log ID: {} has status '{}', cannot transition to FAILED. Skipping.", logId, current);
            return;
        }
        emailLog.setSendStatus(STATUS_FAILED);
        emailLog.setErrorMessage(errorMessage);
        emailLogRepository.save(emailLog);
        log.info("Email log ID: {} marked as FAILED", logId);
    }

    /**
     * Increment retry count
     */
    @Transactional
    public void incrementRetryCount(Long logId) {
        emailLogRepository.findById(logId).ifPresent(emailLog -> {
            emailLog.setRetryCount(emailLog.getRetryCount() + 1);
            emailLog.setSendStatus(STATUS_RETRYING);
            emailLogRepository.save(emailLog);
        });
    }

    /**
     * Get email logs with pagination
     */
    public Page<EmailLog> getEmailLogs(Pageable pageable) {
        return emailLogRepository.findAll(pageable);
    }

    /**
     * Get email logs by order ID
     */
    public Page<EmailLog> getEmailLogsByOrderId(String orderId, Pageable pageable) {
        return emailLogRepository.findByOrderId(orderId, pageable);
    }

    /**
     * Get email logs by status
     */
    public Page<EmailLog> getEmailLogsByStatus(String status, Pageable pageable) {
        return emailLogRepository.findBySendStatus(status, pageable);
    }

    /**
     * Get email logs by recipient
     */
    public Page<EmailLog> getEmailLogsByRecipient(String email, Pageable pageable) {
        return emailLogRepository.findByRecipientTo(email, pageable);
    }

    /**
     * Get email log by ID
     */
    public EmailLog getEmailLogById(Long id) {
        return emailLogRepository.findById(id).orElse(null);
    }

    /**
     * Get email statistics
     */
    public EmailLogStats getStats() {
        EmailLogStats stats = new EmailLogStats();
        stats.setTotalSent(emailLogRepository.countBySendStatus(STATUS_SENT));
        stats.setTotalFailed(emailLogRepository.countBySendStatus(STATUS_FAILED));
        stats.setTotalPending(emailLogRepository.countBySendStatus(STATUS_PENDING));
        stats.setTotalRetrying(emailLogRepository.countBySendStatus(STATUS_RETRYING));
        return stats;
    }

    @lombok.Data
    public static class EmailLogStats {
        private long totalSent;
        private long totalFailed;
        private long totalPending;
        private long totalRetrying;
    }

    /**
     * Enqueue failed emails into EmailQueue (type=RETRY) instead of sending directly.
     * Each email's status update + queue insert is wrapped in its own REQUIRES_NEW
     * transaction for atomicity — if one fails, others continue independently.
     * 
     * @return Number of emails enqueued for retry
     */
    public int resendFailedEmailsWithDailyLimitExceeded() {
        List<EmailLog> failedEmails = emailLogRepository.findFailedEmailsWithDailyLimitExceeded(
                3, PageRequest.of(0, 500));
        
        int enqueuedCount = 0;
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        
        for (EmailLog emailLog : failedEmails) {
            try {
                Boolean success = txTemplate.execute(status -> {
                    // Skip if already queued for retry
                    if (emailQueueRepository.existsByEmailLogIdAndType(emailLog.getId(), TYPE_RETRY)) {
                        log.info("EmailLog ID={} already queued for retry, skipping", emailLog.getId());
                        return false;
                    }

                    // Mark as RETRYING in email_log
                    emailLog.setRetryCount(emailLog.getRetryCount() + 1);
                    emailLog.setSendStatus(STATUS_RETRYING);
                    emailLogRepository.saveAndFlush(emailLog);
                    
                    // Enqueue into email_queue with type=RETRY
                    EmailQueue queueItem = EmailQueue.builder()
                            .type(TYPE_RETRY)
                            .emailLogId(emailLog.getId())
                            .recipientEmail(emailLog.getRecipientTo())
                            .subject(emailLog.getSubject())
                            .status("PENDING")
                            .retryCount(0)
                            .scheduledAt(OffsetDateTime.now())
                            .build();
                    emailQueueRepository.save(queueItem);
                    
                    log.info("Enqueued failed email for retry: EmailLog ID={}, Subject={}, To={}", 
                             emailLog.getId(), emailLog.getSubject(), emailLog.getRecipientTo());
                    return true;
                });
                if (Boolean.TRUE.equals(success)) {
                    enqueuedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to enqueue email for retry: EmailLog ID={}", emailLog.getId(), e);
            }
        }
        
        log.info("Enqueued {} failed emails for retry via EmailQueue", enqueuedCount);
        return enqueuedCount;
    }
}
