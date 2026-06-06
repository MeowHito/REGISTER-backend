package com.actionth.membership.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.actionth.membership.model.Contact;
import com.actionth.membership.model.dto.BibDocumentDTO;
import com.actionth.membership.model.dto.EmailAttachmentDTO;
import com.actionth.membership.model.request.SimpleEmailRequest;
import com.actionth.membership.model.request.TemplateEmailRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Public email service for application code.
 * All methods in this class send email requests to RabbitMQ queues asynchronously.
 * Actual email sending is handled by EmailConsumer -> EmailSenderService.
 */
@Slf4j
@Service
public class EmailService {

    @Autowired
    @Lazy
    private EmailQueueService emailQueueService;

    @Autowired
    private EmailSenderService emailSenderService;

    @Autowired
    private EmailLogService emailLogService;

    @Autowired
    private PdfService pdfService;

    @Value("${app.contact-mail-target}")
    private String contactMailTarget;

    @Value("${app.support-mail-cc}")
    private String supportMailCc;

    @Value("${app.env}")
    private String appEnv;

    private String hostName;

    @PostConstruct
    public void init() {
        if ("PROD".equals(appEnv)) {
            hostName = "https://register.action.in.th";
        } else if ("UAT".equals(appEnv)) {
            hostName = "https://membership.testuiapp.com";
        } else {
            hostName = "http://localhost:8888";
        }
    }

    public void sendResetPasswordMail(String to, String username, String uuid) {
        try {
            Map<String, Object> variables = Map.of(
                "username", username,
                "email", to,
                "resetUrl", hostName + "/changepassword/" + uuid
            );
            String emailContent = emailSenderService.processTemplate("reset-password", variables);

            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(to);
            request.setSubject("Reset your password");
            request.setBody(emailContent);

            var log = emailLogService.createLog(request);
            request.setEmailLogId(log.getId().toString());

            emailQueueService.sendOrQueue(request);
        } catch (Exception e) {
            log.error("Error occurred while sending reset password email:", e);
        }
    }

    public void sendAnnouncementMail(String to, String eventName, String title) {
        try {
            Map<String, Object> variables = Map.of(
                "eventName", eventName,
                "title", title
            );
            String emailContent = emailSenderService.processTemplate("announcement", variables);

            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(to);
            request.setSubject("ฝากข่าวประชาสัมพันธ์");
            request.setBody(emailContent);

            var log = emailLogService.createLog(request);
            request.setEmailLogId(log.getId().toString());

            emailQueueService.sendOrQueue(request);
        } catch (Exception e) {
            log.error("Error occurred while sending announcement email:", e);
        }
    }

    public void sendContactCustomerEmail(Contact contact) {
        try {
            Map<String, Object> variables = Map.of("name", contact.getName());
            String body = emailSenderService.processTemplate("contact-customer", variables);

            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(contact.getEmail());
            request.setSubject("เราได้รับข้อความของคุณแล้ว");
            request.setBody(body);

            var log = emailLogService.createLog(request);
            request.setEmailLogId(log.getId().toString());

            emailQueueService.sendOrQueue(request);
        } catch (Exception e) {
            log.error("Error occurred while sending contact customer email:", e);
        }
    }

    public void sendContactAdminEmail(Contact contact) {
        try {
            String tel = contact.getTel() != null ? contact.getTel() : "-";
            Map<String, Object> variables = Map.of(
                "name", contact.getName(),
                "email", contact.getEmail(),
                "tel", tel,
                "detail", contact.getDetail()
            );
            String body = emailSenderService.processTemplate("contact-admin", variables);

            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(contactMailTarget);
            request.setSubject("มีผู้ติดต่อจากเว็บไซต์");
            request.setBody(body);

            var log = emailLogService.createLog(request);
            request.setEmailLogId(log.getId().toString());

            emailQueueService.sendOrQueue(request);
        } catch (Exception e) {
            log.error("Error occurred while sending contact admin email:", e);
        }
    }

    public void sendEventCalendarConfirmMail(String to, String eventName) {
        try {
            Map<String, Object> variables = Map.of("eventName", eventName);
            String emailContent = emailSenderService.processTemplate("event-calendar-confirm", variables);

            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(to);
            request.setCc(supportMailCc);
            request.setSubject("แจ้งฝากกิจกรรมสำเร็จ");
            request.setBody(emailContent);

            var log = emailLogService.createLog(request);
            request.setEmailLogId(log.getId().toString());

            emailQueueService.sendOrQueue(request);
        } catch (Exception e) {
            log.error("Error occurred while sending event submitted email:", e);
        }
    }

    public void sendEventCalendarMail(String to, String eventName, boolean isApproved, String rejectReason) {
        try {
            String status = isApproved ? "ได้รับการอนุมัติ" : "ถูกปฏิเสธ";
            Map<String, Object> variables = Map.of(
                "eventName", eventName,
                "status", status,
                "isApproved", isApproved,
                "rejectReason", rejectReason != null ? rejectReason : ""
            );
            String emailContent = emailSenderService.processTemplate("event-calendar-status", variables);

            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(to);
            request.setCc(supportMailCc);
            request.setSubject("ฝากกิจกรรม");
            request.setBody(emailContent);

            var log = emailLogService.createLog(request);
            request.setEmailLogId(log.getId().toString());

            emailQueueService.sendOrQueue(request);
        } catch (Exception e) {
            log.error("Error occurred while sending event calendar email:", e);
        }
    }

    public void sendBibDocumentsEmail(String to, List<BibDocumentDTO> bibList) {
        try {
            List<EmailAttachmentDTO> attachments = emailSenderService.generateBibDocumentAttachments(bibList);
            
            String body = emailSenderService.processTemplate("bib-documents", Map.of());

            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(to);
            request.setSubject("Bib Documents for your registrations");
            request.setBody(body);
            request.setAttachments(attachments);

            var log = emailLogService.createLog(request);
            request.setEmailLogId(log.getId().toString());

            emailQueueService.sendOrQueue(request);
        } catch (Exception e) {
            log.error("Error occurred while sending bib documents email:", e);
        }
    }

    public void sendGeneralTemplateEmail(TemplateEmailRequest request) {
        try {
            String emailContent = emailSenderService.processTemplate(request.getTemplateName(), request.getVariables());

            SimpleEmailRequest simpleRequest = new SimpleEmailRequest();
            simpleRequest.setTo(request.getTo());
            simpleRequest.setCc(request.getCc());
            simpleRequest.setSubject(request.getSubject());
            simpleRequest.setOrderId(request.getOrderId());
            simpleRequest.setBody(emailContent);

            if ("payment-success".equals(request.getTemplateName())) {
                try {
                    String receiptHtml = emailSenderService.processTemplate("receipt-email", request.getVariables());
                    byte[] pdfBytes = pdfService.generatePdfFromHtml(receiptHtml);
                    
                    EmailAttachmentDTO attachment = new EmailAttachmentDTO();
                    attachment.setFilename("Receipt-" + (request.getOrderId() != null ? request.getOrderId() : "Unknown") + ".pdf");
                    attachment.setContentType("application/pdf");
                    attachment.setContent(pdfBytes);
                    
                    if (simpleRequest.getAttachments() == null) {
                        simpleRequest.setAttachments(new ArrayList<>());
                    }
                    simpleRequest.getAttachments().add(attachment);
                } catch (Exception ex) {
                    log.error("Failed to generate receipt PDF for order: {}", request.getOrderId(), ex);
                }
            }

            var log = emailLogService.createLog(simpleRequest);
            simpleRequest.setEmailLogId(log.getId().toString());

            emailQueueService.sendOrQueue(simpleRequest);
        } catch (Exception e) {
            log.error("Error occurred while sending general template email:", e);
            throw new RuntimeException("Failed to send template email", e);
        }
    }

    /**
     * Same as sendGeneralTemplateEmail but sends directly to RabbitMQ,
     * bypassing sendOrQueue to avoid creating duplicate OUTBOUND queue records.
     * Used when called from queue processing (e.g. CORRECTION items).
     */
    public void sendGeneralTemplateEmailDirect(TemplateEmailRequest request) {
        sendGeneralTemplateEmailDirect(request, null);
    }

    /**
     * Sends directly to RabbitMQ, reusing an existing EmailLog if provided.
     * When existingEmailLogId is non-null, skips createLog() to prevent duplicate log records.
     * @return the EmailLog ID used (new or existing)
     */
    public Long sendGeneralTemplateEmailDirect(TemplateEmailRequest request, Long existingEmailLogId) {
        try {
            String emailContent = emailSenderService.processTemplate(request.getTemplateName(), request.getVariables());

            SimpleEmailRequest simpleRequest = new SimpleEmailRequest();
            simpleRequest.setTo(request.getTo());
            simpleRequest.setCc(request.getCc());
            simpleRequest.setSubject(request.getSubject());
            simpleRequest.setOrderId(request.getOrderId());
            simpleRequest.setBody(emailContent);

            if ("payment-success".equals(request.getTemplateName())) {
                try {
                    String receiptHtml = emailSenderService.processTemplate("receipt-email", request.getVariables());
                    byte[] pdfBytes = pdfService.generatePdfFromHtml(receiptHtml);

                    EmailAttachmentDTO attachment = new EmailAttachmentDTO();
                    attachment.setFilename("Receipt-" + (request.getOrderId() != null ? request.getOrderId() : "Unknown") + ".pdf");
                    attachment.setContentType("application/pdf");
                    attachment.setContent(pdfBytes);

                    if (simpleRequest.getAttachments() == null) {
                        simpleRequest.setAttachments(new ArrayList<>());
                    }
                    simpleRequest.getAttachments().add(attachment);
                } catch (Exception ex) {
                    log.error("Failed to generate receipt PDF for order: {}", request.getOrderId(), ex);
                }
            }

            Long logId;
            if (existingEmailLogId != null) {
                simpleRequest.setEmailLogId(existingEmailLogId.toString());
                logId = existingEmailLogId;
            } else {
                var emailLog = emailLogService.createLog(simpleRequest);
                simpleRequest.setEmailLogId(emailLog.getId().toString());
                logId = emailLog.getId();
            }

            emailQueueService.sendDirectToRabbit(simpleRequest);
            return logId;
        } catch (Exception e) {
            log.error("Error occurred while sending direct template email:", e);
            throw new RuntimeException("Failed to send template email", e);
        }
    }

    public void sendInviteEmail(String to, String inviterName, String eventName, String role, String token) {
        try {
            String acceptUrl = hostName + "/invite/accept?token=" + token;
            Map<String, Object> variables = Map.of(
                "inviterName", inviterName != null ? inviterName : "ผู้ดูแลระบบ",
                "eventName", eventName != null ? eventName : "-",
                "role", role != null ? role : "viewer",
                "acceptUrl", acceptUrl
            );
            String emailContent = emailSenderService.processTemplate("invite-email-template", variables);

            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(to);
            request.setSubject("คำเชิญเข้าร่วมทีมจัดการกิจกรรม: " + (eventName != null ? eventName : ""));
            request.setBody(emailContent);

            var log = emailLogService.createLog(request);
            request.setEmailLogId(log.getId().toString());

            emailQueueService.sendOrQueue(request);
        } catch (Exception e) {
            log.error("Error occurred while sending invite email to {}: ", to, e);
        }
    }

    public void sendHelpRequestConfirmationEmail(String to, String name, String orderNo, String message) {
        try {
            java.util.Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("name", name != null ? name : "");
            variables.put("orderNo", orderNo != null ? orderNo : "");
            variables.put("message", message != null ? message : "");

            String body = emailSenderService.processTemplate("help-request-confirmation", variables);

            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(to);
            request.setSubject("เราได้รับคำร้องขอความช่วยเหลือของท่านแล้ว - " + orderNo);
            request.setBody(body);

            var emailLog = emailLogService.createLog(request);
            request.setEmailLogId(emailLog.getId().toString());

            emailQueueService.sendOrQueue(request);
        } catch (Exception e) {
            log.error("Error occurred while sending help request confirmation email:", e);
        }
    }

    public void sendHelpStatusUpdateEmail(String to, String name, String orderNo, String status, String adminNote) {
        try {
            java.util.Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("name", name != null ? name : "");
            variables.put("orderNo", orderNo != null ? orderNo : "");
            variables.put("status", status != null ? status : "");
            variables.put("adminNote", adminNote != null ? adminNote : "");

            String body = emailSenderService.processTemplate("help-status-update", variables);

            SimpleEmailRequest request = new SimpleEmailRequest();
            request.setTo(to);
            request.setSubject("อัปเดตสถานะคำร้องขอความช่วยเหลือ - " + orderNo);
            request.setBody(body);

            var emailLog = emailLogService.createLog(request);
            request.setEmailLogId(emailLog.getId().toString());

            emailQueueService.sendOrQueue(request);
        } catch (Exception e) {
            log.error("Error occurred while sending help status update email:", e);
        }
    }
}

