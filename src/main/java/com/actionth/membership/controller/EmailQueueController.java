package com.actionth.membership.controller;

import com.actionth.membership.model.dto.EmailQueueDashboardDto;
import com.actionth.membership.model.dto.EmailQueueDto;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.EmailQueueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/email-queue")
@RequiredArgsConstructor
public class EmailQueueController {

    private final EmailQueueService emailQueueService;

    /**
     * Get dashboard stats for all email types
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Response<EmailQueueDashboardDto>> getDashboard() {
        EmailQueueDashboardDto dashboard = emailQueueService.getDashboard();
        return ResponseEntity.ok(Response.<EmailQueueDashboardDto>builder()
                .data(dashboard)
                .message("Success")
                .success(true)
                .build());
    }

    /**
     * Get current email limit configuration (reads from AppConfig, falls back to @Value defaults)
     */
    @GetMapping("/config")
    public ResponseEntity<Response<Map<String, Integer>>> getConfig() {
        return ResponseEntity.ok(Response.<Map<String, Integer>>builder()
                .data(emailQueueService.getEmailLimitsConfig())
                .message("Success")
                .success(true)
                .build());
    }

    /**
     * Update email limit configuration — takes effect immediately without restart
     */
    @PutMapping("/config")
    public ResponseEntity<Response<Map<String, Integer>>> updateConfig(
            @RequestBody Map<String, Integer> body) {
        int globalLimit = body.getOrDefault("globalDailyLimit", 0);
        int queueLimit  = body.getOrDefault("queueDailyLimit", 0);
        emailQueueService.updateEmailLimitsConfig(globalLimit, queueLimit);
        return ResponseEntity.ok(Response.<Map<String, Integer>>builder()
                .data(emailQueueService.getEmailLimitsConfig())
                .message("อัปเดตค่า limit สำเร็จ")
                .success(true)
                .build());
    }

    /**
     * Get paginated queue list with optional type and status filters
     */
    @GetMapping("/queue")
    public ResponseEntity<Response<Page<EmailQueueDto>>> getQueueList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Page<EmailQueueDto> queueList = emailQueueService.getQueueList(page, size, type, status);
        return ResponseEntity.ok(Response.<Page<EmailQueueDto>>builder()
                .data(queueList)
                .message("Success")
                .success(true)
                .build());
    }

    /**
     * Manually trigger queue processing (admin action).
     * Returns immediately — processing runs in the background.
     */
    @PostMapping("/process-queue")
    public ResponseEntity<Map<String, Object>> processQueue() {
        log.info("[EmailQueue] Manual queue processing triggered");
        // Fire-and-forget: return immediately so the HTTP request doesn't block
        CompletableFuture.runAsync(() -> {
            try {
                emailQueueService.processQueue();
            } catch (Exception e) {
                log.error("[EmailQueue] Background processQueue failed", e);
            }
        });
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "เริ่มประมวลผลคิวแล้ว กรุณารอสักครู่แล้วกดรีเฟรช");
        return ResponseEntity.ok(result);
    }

    /**
     * Enqueue correction emails for all paid orders of a specific event
     */
    @PostMapping("/enqueue/correction/event/{eventUuid}")
    public ResponseEntity<Map<String, Object>> enqueueCorrectionByEvent(@PathVariable String eventUuid) {
        log.info("[EmailQueue] Enqueue correction request for eventUuid: {}", eventUuid);
        Map<String, Object> result = emailQueueService.enqueueCorrectionByEvent(eventUuid);
        return ResponseEntity.ok(result);
    }

    /**
     * Enqueue a single order's correction email
     */
    @PostMapping("/enqueue/correction/order/{orderUuid}")
    public ResponseEntity<Map<String, Object>> enqueueCorrectionSingle(@PathVariable String orderUuid) {
        log.info("[EmailQueue] Enqueue correction single request for orderUuid: {}", orderUuid);
        Map<String, Object> result = emailQueueService.enqueueCorrectionSingle(orderUuid);
        return ResponseEntity.ok(result);
    }
}
