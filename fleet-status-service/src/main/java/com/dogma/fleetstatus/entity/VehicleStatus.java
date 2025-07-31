package com.dogma.fleetstatus.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Сущность для хранения статуса транспортного средства
 * 
 * Содержит всю информацию о текущем состоянии ТС включая:
 * - базовые данные (VIN, пробег, время)
 * - технические параметры (топливо, двигатель, температура)
 * - операционные данные (местоположение, скорость, водитель)
 * - служебную информацию (техосмотр, онлайн статус)
 */
@Entity
@Table(name = "vehicle_status")
@Data
public class VehicleStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный идентификатор транспортного средства
     */
    @Column(nullable = false)
    private String vin;
    
    /**
     * Текущий пробег в километрах
     */
    private Long odometer;
    
    /**
     * Время последнего обновления статуса
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

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
