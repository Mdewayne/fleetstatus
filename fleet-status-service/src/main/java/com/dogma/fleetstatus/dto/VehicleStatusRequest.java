package com.dogma.fleetstatus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO для запроса обновления статуса транспортного средства
 * 
 * Используется для получения данных от клиентов при обновлении
 * статуса ТС через REST API
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehicleStatusRequest {
    
    /**
     * Уникальный идентификатор транспортного средства
     */
    private String vin;
    
    /**
     * Текущий пробег в километрах
     */
    private Long odometer;
    
    // Технические параметры
    
    /**
     * Уровень топлива в процентах (0-100)
     */
    private Integer fuelLevel;
    
    /**
     * Статус двигателя: RUNNING, STOPPED, WARNING
     */
    private String engineStatus;
    
    /**
     * Географические координаты или адрес местоположения
     */
    private String location;
    
    /**
     * Текущая скорость в км/ч
     */
    private Integer speed;
    
    /**
     * Температура двигателя в градусах Цельсия
     */
    private Double temperature;
    
    // Операционные данные
    
    /**
     * Флаг необходимости технического обслуживания
     */
    private Boolean maintenanceDue;
    
    /**
     * Дата последнего технического обслуживания
     */
    private LocalDateTime lastMaintenanceDate;
    
    /**
     * Идентификатор водителя, управляющего ТС
     */
    private String driverId;
    
    /**
     * Статус подключения ТС к системе мониторинга
     */
    private Boolean isOnline;
}
