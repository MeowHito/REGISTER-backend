package com.actionth.membership.controller;

import com.actionth.membership.model.dto.AppErrorLogResponse;
import com.actionth.membership.model.request.AppErrorLogRequest;
import com.actionth.membership.service.AppErrorLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for logging endpoints.
 * These endpoints are public (no authentication required) to capture errors from logged-out users.
 */
@Slf4j
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final AppErrorLogService appErrorLogService;

    /**
     * Receives and stores error logs for monitoring and debugging.
     * Accepts errors from both frontend and backend sources.
     * 
     * Fire-and-forget pattern: Frontend doesn't wait for response.
     */
    @PostMapping("/frontend-error")
    public ResponseEntity<AppErrorLogResponse> logFrontendError(
            @RequestBody AppErrorLogRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Default source to FRONTEND for this endpoint
            if (request.getSource() == null || request.getSource().isBlank()) {
                request.setSource("FRONTEND");
            }

            AppErrorLogResponse response = appErrorLogService.saveErrorLog(request, httpRequest);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body(response);
            }
            
        } catch (Exception e) {
            log.error("[AppErrorLog] Failed to process error log request: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(AppErrorLogResponse.builder()
                            .success(false)
                            .message("Failed to save log")
                            .build());
        }
    }
}
