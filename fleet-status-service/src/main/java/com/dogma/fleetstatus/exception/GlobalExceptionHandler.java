package com.dogma.fleetstatus.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений для всех контроллеров
 * 
 * Обеспечивает единообразную обработку ошибок и возврат
 * структурированных ответов клиентам
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Обработка исключения при отсутствии ТС
     */
    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleVehicleNotFound(VehicleNotFoundException ex) {
        logger.warn("Vehicle not found: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "VEHICLE_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Обработка общих исключений
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Создает структурированный ответ с информацией об ошибке
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("code", errorCode);
        response.put("message", message);
        return response;
    }
} 