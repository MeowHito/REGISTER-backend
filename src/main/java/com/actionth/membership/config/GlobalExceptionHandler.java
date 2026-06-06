package com.actionth.membership.config;

import java.sql.SQLException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.exception.EmailMismatchException;
import com.actionth.membership.exception.EventModificationException;
import com.actionth.membership.exception.LockAcquisitionException;
import com.actionth.membership.exception.QuotaExceededException;
import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.exception.ValidationException;
import com.actionth.membership.response.Response;

import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Optional;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(ex.getMessage(), request.getDescription(false));
        log.error("Unhandled exception: {}", errorDetails.getMessage(), ex);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException ex,
            WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(ex.getMessage(), request.getDescription(false));
        log.warn("Resource not found: {}", errorDetails.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorDetails> handleValidationException(ValidationException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(ex.getMessage(), request.getDescription(false));
        log.warn("Validation error: {}", errorDetails.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorDetails> handleSQLException(SQLException ex, WebRequest request) {
        String userFriendlyMessage = errorHandler(ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(userFriendlyMessage, request.getDescription(false));
        log.error("SQL exception: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorDetails> handleBusinessException(BusinessException ex, WebRequest request) {
    ErrorDetails errorDetails = new ErrorDetails(ex.getMessage(), request.getDescription(false));
    log.warn("Business exception: {}", errorDetails.getMessage());
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
}

    @ExceptionHandler(EmailMismatchException.class)
    public ResponseEntity<Response<Object>> handleEmailMismatchException(EmailMismatchException ex, WebRequest request) {
        log.warn("Email mismatch on invitation accept: {}", ex.getMessage());
        Map<String, String> data = new java.util.HashMap<>();
        data.put("invitedEmail", ex.getInvitedEmail());
        return new ResponseEntity<>(
            new Response<>(data, ex.getMessage(), false),
            HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Response<Object>> handleQuotaExceededException(QuotaExceededException ex, WebRequest request) {
        log.warn("Quota exceeded: {}", ex.getMessage());
        return new ResponseEntity<>(new Response<>(ex.getQuotaValidationError(), ex.getMessage(), false), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LockAcquisitionException.class)
    public ResponseEntity<Response<Object>> handleLockAcquisitionException(LockAcquisitionException ex, WebRequest request) {
        log.warn("Lock acquisition failed: {}", ex.getMessage());
        return new ResponseEntity<>(
            new Response<>(null, "ระบบมีผู้ใช้งานจำนวนมาก กรุณาลองใหม่อีกครั้ง", false), 
            HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    @ExceptionHandler(EventModificationException.class)
    public ResponseEntity<Response<Object>> handleEventModificationException(EventModificationException ex, WebRequest request) {
        log.warn("Event modification blocked: {}", ex.getMessage());
        
        Map<String, String> errorData = new java.util.HashMap<>();
        errorData.put("field", ex.getField());
        errorData.put("itemName", ex.getItemName());
        
        return new ResponseEntity<>(
            new Response<>(errorData, ex.getMessage(), false), 
            HttpStatus.BAD_REQUEST
        );
    }

    public String errorHandler(String reason) {
        String newReason = "ระบบขัดข้อง";
        if (reason != null && !reason.isEmpty()) {
            if (reason.contains("Duplicate entry")) {
                newReason = "ข้อมูลซ้ำกับในระบบ";
            } else if (reason.contains("Incorrect result size: expected 1, actual 0")) {
                newReason = "ไม่พบข้อมูล";
            } else if (reason.contains("Unknown column")) {
                newReason = "ไม่พบฟิลด์ที่ดึงข้อมูล";
            }
        }
        log.error(Optional.ofNullable(reason).orElse("") + " | " + newReason);
        return newReason;
    }
}

@Data
@AllArgsConstructor
class ErrorDetails {
    private String message;
    private String details;
}
