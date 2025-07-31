package com.dogma.fleetstatus.dto;

import com.dogma.fleetstatus.entity.VehicleStatus;
import com.dogma.fleetstatus.util.RoleUtils;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO для ответа со статусом транспортного средства
 * 
 * Содержит данные о ТС с фильтрацией по ролям пользователей.
 * Поля, к которым у пользователя нет доступа, исключаются из ответа.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleStatusResponse {
    
    private String vin;
    private Long odometer;
    private LocalDateTime timestamp;
    
    // Технические параметры (доступны MANAGER и ADMIN)
    private Integer fuelLevel;
    private String engineStatus;
    private String location;
    private Integer speed;
    private Double temperature;
    private Boolean maintenanceDue;
    private Boolean isOnline;
    
    // Административные данные (доступны только ADMIN)
    private LocalDateTime lastMaintenanceDate;
    private String driverId;
    
    /**
     * Создает DTO из сущности с фильтрацией по роли пользователя
     * 
     * @param entity сущность ТС
     * @param userRole роль пользователя
     * @return DTO с отфильтрованными данными
     */
    public static VehicleStatusResponse fromEntity(VehicleStatus entity, UserRole userRole) {
        VehicleStatusResponse response = new VehicleStatusResponse();
        
        // Базовые поля доступны всем ролям
        if (RoleUtils.hasAccessToField("vin", userRole)) {
            response.vin = entity.getVin();
        }
        if (RoleUtils.hasAccessToField("odometer", userRole)) {
            response.odometer = entity.getOdometer();
        }
        if (RoleUtils.hasAccessToField("timestamp", userRole)) {
            response.timestamp = entity.getTimestamp();
        }
        
        // Технические параметры
        if (RoleUtils.hasAccessToField("fuelLevel", userRole)) {
            response.fuelLevel = entity.getFuelLevel();
        }
        if (RoleUtils.hasAccessToField("engineStatus", userRole)) {
            response.engineStatus = entity.getEngineStatus();
        }
        if (RoleUtils.hasAccessToField("location", userRole)) {
            response.location = entity.getLocation();
        }
        if (RoleUtils.hasAccessToField("speed", userRole)) {
            response.speed = entity.getSpeed();
        }
        if (RoleUtils.hasAccessToField("temperature", userRole)) {
            response.temperature = entity.getTemperature();
        }
        if (RoleUtils.hasAccessToField("maintenanceDue", userRole)) {
            response.maintenanceDue = entity.getMaintenanceDue();
        }
        if (RoleUtils.hasAccessToField("isOnline", userRole)) {
            response.isOnline = entity.getIsOnline();
        }
        
        // Административные данные
        if (RoleUtils.hasAccessToField("lastMaintenanceDate", userRole)) {
            response.lastMaintenanceDate = entity.getLastMaintenanceDate();
        }
        if (RoleUtils.hasAccessToField("driverId", userRole)) {
            response.driverId = entity.getDriverId();
        }
        
        return response;
    }
    
    /**
     * Создает Map с данными для SSE отправки
     * 
     * @return Map с данными статуса
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        if (vin != null) map.put("vin", vin);
        if (odometer != null) map.put("odometer", odometer);
        if (timestamp != null) map.put("timestamp", timestamp);
        if (fuelLevel != null) map.put("fuelLevel", fuelLevel);
        if (engineStatus != null) map.put("engineStatus", engineStatus);
        if (location != null) map.put("location", location);
        if (speed != null) map.put("speed", speed);
        if (temperature != null) map.put("temperature", temperature);
        if (maintenanceDue != null) map.put("maintenanceDue", maintenanceDue);
        if (isOnline != null) map.put("isOnline", isOnline);
        if (lastMaintenanceDate != null) map.put("lastMaintenanceDate", lastMaintenanceDate);
        if (driverId != null) map.put("driverId", driverId);
        
        return map;
    }
    
    // Геттеры и сеттеры
    
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    
    public Long getOdometer() { return odometer; }
    public void setOdometer(Long odometer) { this.odometer = odometer; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Integer getFuelLevel() { return fuelLevel; }
    public void setFuelLevel(Integer fuelLevel) { this.fuelLevel = fuelLevel; }
    
    public String getEngineStatus() { return engineStatus; }
    public void setEngineStatus(String engineStatus) { this.engineStatus = engineStatus; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public Integer getSpeed() { return speed; }
    public void setSpeed(Integer speed) { this.speed = speed; }
    
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    
    public Boolean getMaintenanceDue() { return maintenanceDue; }
    public void setMaintenanceDue(Boolean maintenanceDue) { this.maintenanceDue = maintenanceDue; }
    
    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }
    
    public LocalDateTime getLastMaintenanceDate() { return lastMaintenanceDate; }
    public void setLastMaintenanceDate(LocalDateTime lastMaintenanceDate) { this.lastMaintenanceDate = lastMaintenanceDate; }
    
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
} 