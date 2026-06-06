package com.actionth.membership.utils;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.actionth.membership.model.request.SimpleEmailRequest;
import com.actionth.membership.service.EmailSenderService;

import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ consumer that processes email requests from the simple-email queue.
 */
@Slf4j
@Component
public class EmailConsumer {

    @Autowired
    private EmailSenderService emailSenderService;

    @RabbitListener(queues = "${spring.rabbitmq.queue.simple-email}")
    public void handleSimpleEmailQueue(SimpleEmailRequest request) {
        log.info("[EmailConsumer] Picked up message — emailLogId: {}, to: {}, subject: {}",
                request.getEmailLogId(), request.getTo(), request.getSubject());
        try {
            emailSenderService.sendEmail(
                    request.getTo(),
                    request.getCc(),
                    request.getSubject(),
                    request.getBody(),
                    request.getAttachments(),
                    request.getEmailLogId());
            log.info("[EmailConsumer] Successfully processed emailLogId: {}", request.getEmailLogId());
        } catch (Exception e) {
            log.error("[EmailConsumer] Failed to process emailLogId: {}, to: {}, subject: {}",
                    request.getEmailLogId(), request.getTo(), request.getSubject(), e);
        }
    }
}
