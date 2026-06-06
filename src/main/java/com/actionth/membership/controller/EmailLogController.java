package com.actionth.membership.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.config.RabbitConfig;
import java.util.List;

import com.actionth.membership.model.dto.EmailAttachmentDTO;
import com.actionth.membership.model.EmailLog;
import com.actionth.membership.model.request.SimpleEmailRequest;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.EmailLogService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/email-logs")
public class EmailLogController {

    @Autowired
    private EmailLogService emailLogService;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private RabbitConfig rabbitConfig;

    /**
     * Get email logs with pagination
     */
    @GetMapping
    public Response<Page<EmailLog>> getEmailLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<EmailLog> logs;
            
            if (status != null && !status.isBlank()) {
                logs = emailLogService.getEmailLogsByStatus(status.toUpperCase(), pageable);
            } else if (email != null && !email.isBlank()) {
                logs = emailLogService.getEmailLogsByRecipient(email, pageable);
            } else {
                logs = emailLogService.getEmailLogs(pageable);
            }
            
            return new Response<>(logs, "Email logs retrieved successfully", true);
        } catch (Exception e) {
            log.error("Error retrieving email logs:", e);
            return new Response<>(null, "Error retrieving email logs: " + e.getMessage(), false);
        }
    }

    /**
     * Get email logs by order ID
     */
    @GetMapping("/order/{orderId}")
    public Response<Page<EmailLog>> getEmailLogsByOrder(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<EmailLog> logs = emailLogService.getEmailLogsByOrderId(orderId, pageable);
            return new Response<>(logs, "Email logs retrieved successfully", true);
        } catch (Exception e) {
            log.error("Error retrieving email logs for order:", e);
            return new Response<>(null, "Error retrieving email logs: " + e.getMessage(), false);
        }
    }

    /**
     * Get email log by ID
     */
    @GetMapping("/{id}")
    public Response<EmailLog> getEmailLogById(@PathVariable Long id) {
        try {
            EmailLog log = emailLogService.getEmailLogById(id);
            if (log == null) {
                return new Response<>(null, "Email log not found", false);
            }
            return new Response<>(log, "Email log retrieved successfully", true);
        } catch (Exception e) {
            log.error("Error retrieving email log:", e);
            return new Response<>(null, "Error retrieving email log: " + e.getMessage(), false);
        }
    }

    /**
     * Get email statistics
     */
    @GetMapping("/stats")
    public Response<EmailLogService.EmailLogStats> getStats() {
        try {
            return new Response<>(emailLogService.getStats(), "Stats retrieved successfully", true);
        } catch (Exception e) {
            log.error("Error retrieving email stats:", e);
            return new Response<>(null, "Error retrieving stats: " + e.getMessage(), false);
        }
    }

    /**
     * Resend a failed email
     */
    @PostMapping("/{id}/resend")
    public Response<Void> resendEmail(@PathVariable Long id) {
        try {
            EmailLog emailLog = emailLogService.getEmailLogById(id);
            
            if (emailLog == null) {
                return new Response<>(null, "Email log not found", false);
            }
            
            // Check if retry limit reached (e.g., max 3 retries)
            if (emailLog.getRetryCount() >= 3) {
                return new Response<>(null, "Maximum retry limit reached", false);
            }
            
            emailLogService.incrementRetryCount(id);
            
            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(emailLog.getRecipientTo());
            request.setCc(emailLog.getRecipientCc());
            request.setSubject(emailLog.getSubject());
            request.setBody(emailLog.getEmailBody());
            request.setEmailLogId(id.toString());
            
            if (Boolean.TRUE.equals(emailLog.getHasAttachments())) {
                List<EmailAttachmentDTO> attachments = emailLogService.getAttachmentDTOs(id);
                if (attachments != null && !attachments.isEmpty()) {
                    request.setAttachments(attachments);
                }
            }
            
            rabbitTemplate.convertAndSend(rabbitConfig.getSimpleEmailQueueName(), request);
            
            return new Response<>(null, "Email queued for resending", true);
        } catch (Exception e) {
            log.error("Error resending email:", e);
            return new Response<>(null, "Error resending email: " + e.getMessage(), false);
        }
    }
}
