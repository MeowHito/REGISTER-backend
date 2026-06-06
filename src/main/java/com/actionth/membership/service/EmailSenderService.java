package com.actionth.membership.service;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.model.dto.BibDocumentDTO;
import com.actionth.membership.model.dto.EmailAttachmentDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Internal service for actually sending emails using JavaMailSender.
 * This service should ONLY be called by EmailConsumer, not directly by application code.
 */
@Slf4j
@Service
public class EmailSenderService {

    @Value("${app.report-path}")
    private String reportPath;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private ReportService reportService;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private EmailLogService emailLogService;

    @Value("${app.email-logo-url}")
    private String emailLogoUrl;

    public String processTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            context.setVariable("logoUrl", emailLogoUrl);
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            log.error("Error processing template: {}", templateName, e);
            return "";
        }
    }

    public void sendEmail(String to, String cc, String subject, String content, List<EmailAttachmentDTO> attachments, String emailLogId) {
        log.info("[EmailSender] START sendEmail — logId: {}, to: {}, subject: {}, attachments: {}",
                emailLogId, to, subject, attachments != null ? attachments.size() : 0);

        Long logId = null;
        if (emailLogId != null && !emailLogId.isBlank()) {
            try {
                logId = Long.parseLong(emailLogId);
            } catch (NumberFormatException e) {
                log.warn("[EmailSender] Invalid emailLogId '{}', status tracking will be incomplete", emailLogId);
            }
        }

        try {
            log.info("[EmailSender] logId: {} — Building MIME message...", emailLogId);
            MimeMessageHelper helper = new MimeMessageHelper(javaMailSender.createMimeMessage(), true, "UTF-8");
            helper.setTo(to);
            if (cc != null && !cc.isBlank()) {
                helper.setCc(cc);
            }
            helper.setSubject(subject);
            helper.setText(content, true);

            if (attachments != null && !attachments.isEmpty()) {
                for (EmailAttachmentDTO attachment : attachments) {
                    ByteArrayResource resource = new ByteArrayResource(attachment.getContent());
                    helper.addAttachment(attachment.getFilename(), resource, attachment.getContentType());
                }
            }

            log.info("[EmailSender] logId: {} — Sending via SMTP...", emailLogId);
            javaMailSender.send(helper.getMimeMessage());
            log.info("[EmailSender] logId: {} — SMTP send OK. Marking as SENT...", emailLogId);

            if (logId != null) {
                try {
                    emailLogService.markAsSent(logId);
                    log.info("[EmailSender] logId: {} — markAsSent completed successfully", emailLogId);
                } catch (Exception ex) {
                    log.error("[EmailSender] logId: {} — Email was sent but markAsSent FAILED: {}", emailLogId, ex.getMessage(), ex);
                }
            } else {
                log.warn("[EmailSender] logId is null — skipping markAsSent");
            }
        } catch (Exception e) {
            log.error("[EmailSender] logId: {} — SMTP send FAILED: {}", emailLogId, e.getMessage(), e);
            if (logId != null) {
                try {
                    emailLogService.markAsFailed(logId, e.getMessage());
                    log.info("[EmailSender] logId: {} — markAsFailed completed", emailLogId);
                } catch (Exception ex) {
                    log.error("[EmailSender] logId: {} — markAsFailed also FAILED: {}", emailLogId, ex.getMessage(), ex);
                }
            }
            throw new BusinessException("Failed to send email: " + e.getMessage(), e);
        }
        log.info("[EmailSender] END sendEmail — logId: {}", emailLogId);
    }

    public List<EmailAttachmentDTO> generateBibDocumentAttachments(List<BibDocumentDTO> bibList) {
        String logoShirt = reportPath + "/logoShirt.png";
        String logoBib = reportPath + "/logoBib.png";

        File tmpShirt = null;
        File tmpBib = null;
        File bgGradient = null;

        try {
            tmpShirt = File.createTempFile("shirt-", ".png");
            tmpBib = File.createTempFile("bib-", ".png");
            bgGradient = reportService.renderGradientBackground(595, 842, new Color(0x3399FF), new Color(0xE0F0FF),
                    "diagonal");

            try (InputStream in1 = getClass().getResourceAsStream(logoShirt)) {
                Files.copy(in1, tmpShirt.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            try (InputStream in2 = getClass().getResourceAsStream(logoBib)) {
                Files.copy(in2, tmpBib.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            List<EmailAttachmentDTO> attachments = new ArrayList<>();
            int index = 1;
            for (BibDocumentDTO dto : bibList) {
                byte[] imageBytes = reportService.generateBibDocumentImage(dto, tmpShirt, tmpBib, bgGradient);
                if (imageBytes != null && imageBytes.length > 0) {
                    EmailAttachmentDTO attachment = new EmailAttachmentDTO(
                            "Bib-" + index + ".png",
                            imageBytes,
                            "image/png");
                    attachments.add(attachment);
                    index++;
                }
            }

            log.info("Generated {} bib document attachment(s)", attachments.size());
            return attachments;
        } catch (Exception e) {
            log.error("Failed to generate bib document attachments: {}", e.getMessage(), e);
            return List.of();
        } finally {
            deleteTempFile(tmpShirt);
            deleteTempFile(tmpBib);
            deleteTempFile(bgGradient);
        }
    }

    private void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch (java.io.IOException e) {
                log.warn("Failed to delete temporary file: {}", file.getAbsolutePath(), e);
            }
        }
    }
}
