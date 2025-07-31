package com.dogma.fleetstatus.controller;

import com.dogma.fleetstatus.config.Constants;
import com.dogma.fleetstatus.dto.VehicleStatusRequest;
import com.dogma.fleetstatus.entity.VehicleStatus;
import com.dogma.fleetstatus.service.VehicleStatusService;
import com.dogma.fleetstatus.service.VehicleStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST контроллер для управления статусом транспортных средств
 * 
 * Предоставляет API для:
 * - получения текущего статуса ТС
 * - обновления статуса ТС
 * - стриминга обновлений в реальном времени через SSE
 * - получения истории статусов
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH)
public class VehicleStatusController {
    
    private static final Logger logger = LoggerFactory.getLogger(VehicleStatusController.class);
    private final VehicleStatusService service;
    private final VehicleStreamService streamService;

    public VehicleStatusController(VehicleStatusService service, VehicleStreamService streamService) {
        this.service = service;
        this.streamService = streamService;
    }

    /**
     * Получает текущий статус ТС по VIN
     * 
     * @param vin уникальный идентификатор ТС
     * @return статус ТС или 404 если не найдено
     */
    @GetMapping(Constants.STATUS_PATH)
    public ResponseEntity<VehicleStatus> getStatus(@PathVariable String vin) {
        logger.debug("GET request for status of VIN: {}", vin);
        
        return service.getLatestStatus(vin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновляет статус ТС
     * 
     * @param vin уникальный идентификатор ТС
     * @param request данные для обновления статуса
     * @return обновленный статус ТС
     */
    @PostMapping(Constants.STATUS_PATH)
    public ResponseEntity<VehicleStatus> saveStatus(@PathVariable String vin,
                                                    @RequestBody VehicleStatusRequest request) {
        logger.info("POST request to update status for VIN: {}", vin);
        
        request.setVin(vin); // добавляем VIN из URL, если не передали в JSON
        VehicleStatus saved = service.saveStatus(request);
        
        // Немедленно уведомляем подписчиков об обновлении
        streamService.notifyStatusUpdate(vin, saved);
        
        logger.debug("Status updated successfully for VIN: {}", vin);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * SSE эндпоинт для стриминга обновлений статуса ТС в реальном времени
     * 
     * @param vin уникальный идентификатор ТС
     * @param headers HTTP заголовки для определения роли пользователя
     * @return SSE эмиттер для стриминга данных
     */
    @GetMapping(Constants.STREAM_PATH)
    public SseEmitter streamStatus(@PathVariable String vin, 
                                   @RequestHeader HttpHeaders headers) {
        logger.info("SSE stream request for VIN: {}", vin);
        return streamService.createVehicleStatusStream(vin, headers);
    }
    
    /**
     * Получает историю статусов ТС за указанный период
     * 
     * @param vin уникальный идентификатор ТС
     * @param startTime начало периода (опционально, по умолчанию 24 часа назад)
     * @param endTime конец периода (опционально, по умолчанию текущее время)
     * @return список статусов за период
     */
    @GetMapping("/{vin}/status/history")
    public ResponseEntity<List<VehicleStatus>> getStatusHistory(
            @PathVariable String vin,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        
        logger.debug("GET request for status history of VIN: {} from {} to {}", vin, startTime, endTime);
        
        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now().minusHours(24);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        
        List<VehicleStatus> history = service.getStatusHistory(vin, start, end);
        return ResponseEntity.ok(history);
    }
}
