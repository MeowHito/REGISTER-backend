package com.actionth.membership.service.impl;

import com.actionth.membership.model.AppErrorLog;
import com.actionth.membership.model.dto.AppErrorLogResponse;
import com.actionth.membership.model.request.AppErrorLogRequest;
import com.actionth.membership.repository.AppErrorLogRepository;
import com.actionth.membership.service.AppErrorLogService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppErrorLogServiceImpl implements AppErrorLogService {

    private final AppErrorLogRepository appErrorLogRepository;
    private final ObjectMapper objectMapper;

    private static final String SOURCE_FRONTEND = "FRONTEND";
    private static final String SOURCE_BACKEND = "BACKEND";

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_STACK_LENGTH = 5000;
    private static final int MAX_RESPONSE_DATA_LENGTH = 10000;
    private static final int MAX_REQUEST_DATA_LENGTH = 10000;

    @Override
    public AppErrorLogResponse saveErrorLog(AppErrorLogRequest request, 
            HttpServletRequest httpRequest) {
        
        String logId = UUID.randomUUID().toString().substring(0, 12);
        
        try {
            if (request.getLevel() == null || request.getLevel().isBlank()) {
                log.warn("[AppErrorLog] Missing required field: level");
                return AppErrorLogResponse.builder()
                        .success(false)
                        .message("Missing required field: level")
                        .build();
            }
            if (request.getContext() == null || request.getContext().isBlank()) {
                log.warn("[AppErrorLog] Missing required field: context");
                return AppErrorLogResponse.builder()
                        .success(false)
                        .message("Missing required field: context")
                        .build();
            }
            if (request.getMessage() == null || request.getMessage().isBlank()) {
                log.warn("[AppErrorLog] Missing required field: message");
                return AppErrorLogResponse.builder()
                        .success(false)
                        .message("Missing required field: message")
                        .build();
            }

            // Determine source: use request.source if provided, default to FRONTEND for REST endpoint
            String source = SOURCE_FRONTEND;
            if (request.getSource() != null && !request.getSource().isBlank()) {
                source = request.getSource().toUpperCase();
            }

            Map<String, Object> meta = request.getAdditionalFields();

            OffsetDateTime clientTimestamp = null;
            if (request.getTimestamp() != null && !request.getTimestamp().isBlank()) {
                try {
                    clientTimestamp = OffsetDateTime.parse(request.getTimestamp());
                } catch (Exception e) {
                    log.debug("[AppErrorLog] Failed to parse timestamp: {}", request.getTimestamp());
                }
            }

            String responseDataJson = null;
            if (request.getResponseData() != null) {
                try {
                    responseDataJson = objectMapper.writeValueAsString(request.getResponseData());
                    responseDataJson = truncate(responseDataJson, MAX_RESPONSE_DATA_LENGTH);
                } catch (Exception e) {
                    responseDataJson = String.valueOf(request.getResponseData());
                }
            }

            String clientIp = extractClientIp(httpRequest);
            String userId = extractUserId(httpRequest);
            String sessionId = null;
            if (httpRequest.getSession(false) != null) {
                sessionId = httpRequest.getSession(false).getId();
            }

            AppErrorLog errorLog = AppErrorLog.builder()
                    .logId(logId)
                    .source(source)
                    .level(sanitizeLevel(request.getLevel()))
                    .context(truncate(request.getContext(), 100))
                    .message(truncate(request.getMessage(), MAX_MESSAGE_LENGTH))
                    .stack(truncate(request.getStack(), MAX_STACK_LENGTH))
                    .url(truncate(request.getUrl(), 500))
                    .userAgent(truncate(request.getUserAgent(), 500))
                    .userId(userId)
                    .httpMethod(request.getMethod() != null ? request.getMethod().toUpperCase() : null)
                    .httpStatus(request.getStatus())
                    .statusText(truncate(request.getStatusText(), 100))
                    .responseData(responseDataJson)
                    .requestData(truncate(request.getRequestData(), MAX_REQUEST_DATA_LENGTH))
                    .meta(serializeMeta(meta))
                    .clientTimestamp(clientTimestamp)
                    .clientIp(clientIp)
                    .sessionId(sessionId)
                    .build();

            appErrorLogRepository.save(errorLog);

            log.info("[AppErrorLog] Saved: logId={}, source={}, level={}, context={}, message={}", 
                    logId, source, errorLog.getLevel(), errorLog.getContext(), 
                    truncate(errorLog.getMessage(), 100));

            return AppErrorLogResponse.builder()
                    .success(true)
                    .message("Log recorded")
                    .logId(logId)
                    .build();

        } catch (Exception e) {
            log.error("[AppErrorLog] Failed to save error log: {}", e.getMessage(), e);
            return AppErrorLogResponse.builder()
                    .success(false)
                    .message("Failed to save log")
                    .build();
        }
    }

    @Override
    public void logBackendError(String level, String context, String message, String stack) {
        try {
            String logId = UUID.randomUUID().toString().substring(0, 12);

            AppErrorLog errorLog = AppErrorLog.builder()
                    .logId(logId)
                    .source(SOURCE_BACKEND)
                    .level(sanitizeLevel(level))
                    .context(truncate(context, 100))
                    .message(truncate(message, MAX_MESSAGE_LENGTH))
                    .stack(truncate(stack, MAX_STACK_LENGTH))
                    .build();

            appErrorLogRepository.save(errorLog);

            log.info("[AppErrorLog] Backend error saved: logId={}, context={}, message={}",
                    logId, context, truncate(message, 100));

        } catch (Exception e) {
            log.error("[AppErrorLog] Failed to save backend error log: {}", e.getMessage(), e);
        }
    }

    private String serializeMeta(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            log.warn("[AppErrorLog] Failed to serialize meta: {}", e.getMessage());
            return null;
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private String extractUserId(HttpServletRequest request) {
        try {
            if (request.getSession(false) != null) {
                Object user = request.getSession(false).getAttribute("user");
                if (user != null) {
                    if (user instanceof Map) {
                        Object id = ((Map<?, ?>) user).get("id");
                        return id != null ? String.valueOf(id) : null;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[AppErrorLog] Failed to extract user ID: {}", e.getMessage());
        }
        return null;
    }

    private String sanitizeLevel(String level) {
        if (level == null) {
            return "error";
        }
        String normalized = level.toLowerCase().trim();
        if (Set.of("error", "warn", "info").contains(normalized)) {
            return normalized;
        }
        return "error";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
