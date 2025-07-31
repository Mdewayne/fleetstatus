package com.dogma.fleetstatus.service;

import com.dogma.fleetstatus.dto.VehicleStatusRequest;
import com.dogma.fleetstatus.entity.VehicleStatus;
import com.dogma.fleetstatus.repository.VehicleStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления статусом транспортных средств
 * 
 * Обеспечивает:
 * - Сохранение и получение статуса ТС
 * - Получение истории статусов
 * - Проверку существования ТС
 */
@Service
@Transactional
public class VehicleStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(VehicleStatusService.class);
    
    private final VehicleStatusRepository repository;

    public VehicleStatusService(VehicleStatusRepository repository) {
        this.repository = repository;
    }

    /**
     * Сохраняет новый статус ТС
     */
    @CacheEvict(value = "vehicleStatus", key = "#request.vin")
    public VehicleStatus saveStatus(VehicleStatusRequest request) {
        logger.info("Saving status for VIN: {}", request.getVin());
        
        VehicleStatus status = new VehicleStatus();
        status.setVin(request.getVin());
        status.setOdometer(request.getOdometer());
        status.setTimestamp(LocalDateTime.now());
        status.setFuelLevel(request.getFuelLevel());
        status.setEngineStatus(request.getEngineStatus());
        status.setLocation(request.getLocation());
        status.setSpeed(request.getSpeed());
        status.setTemperature(request.getTemperature());
        status.setMaintenanceDue(request.getMaintenanceDue());
        status.setDriverId(request.getDriverId());
        status.setIsOnline(request.getIsOnline());
        status.setLastMaintenanceDate(request.getLastMaintenanceDate());
        
        VehicleStatus saved = repository.save(status);
        logger.debug("Status saved successfully for VIN: {}", request.getVin());
        return saved;
    }
    
    /**
     * Получает последний статус ТС по VIN
     */
    @Cacheable(value = "vehicleStatus", key = "#vin")
    @Transactional(readOnly = true)
    public Optional<VehicleStatus> getLatestStatus(String vin) {
        logger.debug("Getting latest status for VIN: {}", vin);
        return repository.findTopByVinOrderByTimestampDesc(vin);
    }
    
    /**
     * Получает последний статус ТС или выбрасывает исключение
     */
    @Transactional(readOnly = true)
    public VehicleStatus getLatestStatusOrThrow(String vin) {
        return getLatestStatus(vin)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vin));
    }
    
    /**
     * Проверяет существование ТС
     */
    @Transactional(readOnly = true)
    public boolean vehicleExists(String vin) {
        return repository.existsByVin(vin);
    }
    
    /**
     * Получает историю статусов ТС за период
     */
    @Transactional(readOnly = true)
    public List<VehicleStatus> getStatusHistory(String vin, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Getting status history for VIN: {} from {} to {}", vin, startTime, endTime);
        return repository.findByVinAndTimestampBetween(vin, startTime, endTime);
    }
}

