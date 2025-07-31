package com.dogma.fleetstatus.util;

import com.dogma.fleetstatus.config.Constants;
import com.dogma.fleetstatus.dto.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * Утилиты для работы с ролями пользователей и проверки прав доступа
 * 
 * Предоставляет методы для извлечения роли из HTTP заголовков
 * и проверки доступа к различным полям данных в зависимости от роли
 */
public class RoleUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleUtils.class);
    private static final UserRole DEFAULT_ROLE = UserRole.DRIVER;
    
    /**
     * Извлекает роль пользователя из HTTP заголовков
     * 
     * @param headers HTTP заголовки запроса
     * @return роль пользователя, по умолчанию DRIVER если заголовок отсутствует или некорректен
     */
    public static UserRole extractRoleFromHeaders(HttpHeaders headers) {
        String roleHeader = headers.getFirst(Constants.USER_ROLE_HEADER);
        
        if (roleHeader == null || roleHeader.trim().isEmpty()) {
            logger.warn("X-User-Role header is missing or empty. Using default role: {}", DEFAULT_ROLE);
            return DEFAULT_ROLE;
        }
        
        try {
            UserRole role = UserRole.valueOf(roleHeader.toUpperCase());
            logger.debug("Extracted role: {} from header: {}", role, roleHeader);
            return role;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid role '{}' provided in X-User-Role header. Using default role: {}. Valid roles: {}", 
                roleHeader, DEFAULT_ROLE, UserRole.values());
            return DEFAULT_ROLE;
        }
    }
    
    /**
     * Проверяет, имеет ли пользователь доступ к конкретному полю данных
     * 
     * Матрица доступа:
     * - DRIVER: vin, odometer, timestamp
     * - MANAGER: + fuelLevel, engineStatus, location, speed, temperature, maintenanceDue, isOnline
     * - ADMIN: полный доступ ко всем полям
     * 
     * @param fieldName название поля для проверки доступа
     * @param role роль пользователя
     * @return true если доступ разрешен, false в противном случае
     */
    public static boolean hasAccessToField(String fieldName, UserRole role) {
        switch (fieldName) {
            case "vin":
            case "odometer":
            case "timestamp":
                return true; // Доступно всем ролям
                
            case "fuelLevel":
            case "engineStatus":
            case "location":
            case "speed":
            case "temperature":
            case "maintenanceDue":
            case "isOnline":
                return role == UserRole.MANAGER || role == UserRole.ADMIN;
                
            case "lastMaintenanceDate":
            case "driverId":
                return role == UserRole.ADMIN;
                
            default:
                return false;
        }
    }
} 