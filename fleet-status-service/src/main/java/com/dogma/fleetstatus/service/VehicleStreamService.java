package com.dogma.fleetstatus.service;

import com.dogma.fleetstatus.dto.UserRole;
import com.dogma.fleetstatus.dto.VehicleStatusResponse;
import com.dogma.fleetstatus.entity.VehicleStatus;
import com.dogma.fleetstatus.util.RoleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

/**
 * Основной сервис для создания и управления SSE стримами статуса ТС
 * 
 * ЗАЧЕМ НУЖЕН: Координирует работу всех компонентов системы SSE стриминга.
 * Это "мозг" системы, который решает, когда и как отправлять данные клиентам.
 * 
 * КАК РАБОТАЕТ:
 * 1. Получает запрос на создание SSE стрима
 * 2. Выбирает подходящий поток из пула (ExecutorPoolManager)
 * 3. Запускает планировщик периодических обновлений (UpdateScheduler)
 * 4. Использует кэширование для экономии трафика (StreamDataManager)
 * 5. Обрабатывает немедленные обновления при POST запросах
 * 6. Автоматически очищает ресурсы при завершении стрима
 */
@Service
public class VehicleStreamService {
    
    private static final Logger logger = LoggerFactory.getLogger(VehicleStreamService.class);
    
    // ==================== ЗАВИСИМОСТИ ====================
    
    // Основной сервис для работы с данными ТС
    private final VehicleStatusService vehicleStatusService;
    
    // Управляет пулом потоков для обработки SSE соединений
    private final ExecutorPoolManager executorPoolManager;
    
    // Кэширует данные и контролирует частоту отправки
    private final StreamDataManager dataManager;
    
    // Планирует периодические обновления для надежности
    private final UpdateScheduler updateScheduler;
    
    // ==================== КОНСТРУКТОР ====================
    
    public VehicleStreamService(VehicleStatusService vehicleStatusService,
                               ExecutorPoolManager executorPoolManager,
                               StreamDataManager dataManager,
                               UpdateScheduler updateScheduler) {
        this.vehicleStatusService = vehicleStatusService;
        this.executorPoolManager = executorPoolManager;
        this.dataManager = dataManager;
        this.updateScheduler = updateScheduler;
        
        logger.info("VehicleStreamService initialized with smart event-driven updates");
    }
    
    // ==================== ОСНОВНЫЕ ПУБЛИЧНЫЕ МЕТОДЫ ====================
    
    /**
     * Создает SSE стрим для мониторинга статуса ТС
     * 
     * ЛОГИКА СОЗДАНИЯ СТРИМА:
     * 1. Проверяем существование ТС в БД
     * 2. Создаем SSE emitter с таймаутом
     * 3. Выбираем подходящий поток из пула
     * 4. Регистрируем стрим в пуле
     * 5. Запускаем планировщик периодических обновлений
     * 6. Настраиваем обработчики завершения стрима
     * 
     * ЗАЧЕМ СЛОЖНАЯ ЛОГИКА: В боевых проектах нужно обеспечить:
     * - Надежность соединений (fallback механизмы)
     * - Эффективность (кэширование, контроль частоты)
     * - Масштабируемость (пул потоков)
     * - Мониторинг (логирование всех операций)
     */
    public SseEmitter createVehicleStatusStream(String vin, org.springframework.http.HttpHeaders headers) {
        logger.info("Creating vehicle status stream for VIN: {}", vin);
        
        // Шаг 1: Создаем SSE emitter с таймаутом 5 минут
        SseEmitter emitter = new SseEmitter(300000L); // 5 минут
        
        // Шаг 2: Извлекаем роль пользователя из заголовков
        UserRole userRole = RoleUtils.extractRoleFromHeaders(headers);
        logger.debug("User role for VIN {}: {}", vin, userRole);
        
        // Шаг 3: Проверяем существование ТС в БД
        if (!validateVehicleExists(vin, emitter)) {
            // ТС не найден - возвращаем emitter с ошибкой
            return emitter;
        }
        
        // Шаг 4: Настраиваем обработку стрима
        setupStreamProcessing(vin, userRole, emitter);
        
        // Шаг 5: Настраиваем обработчики завершения стрима
        setupCompletionHandlers(vin, emitter);
        
        logger.info("Vehicle status stream created successfully for VIN: {}", vin);
        return emitter;
    }
    
    /**
     * Уведомляет подписчиков об обновлении статуса ТС
     * 
     * ЛОГИКА НЕМЕДЛЕННОГО ОБНОВЛЕНИЯ:
     * 1. Создаем DTO с фильтрацией по роли пользователя
     * 2. Проверяем, нужно ли отправлять обновление (через StreamDataManager)
     * 3. Если нужно - отправляем данные через активный SseEmitter
     * 4. Обновляем кэш в StreamDataManager
     * 
     * ЗАЧЕМ НЕМЕДЛЕННОЕ ОБНОВЛЕНИЕ: Когда ТС отправляет новые данные через POST,
     * клиенты должны получить их сразу, не ждать следующего fallback цикла.
     */
    public void notifyStatusUpdate(String vin, VehicleStatus status) {
        logger.info("Notifying subscribers about status update for VIN: {}", vin);
        
        // Создаем DTO с фильтрацией по роли (пока используем ADMIN как дефолт)
        VehicleStatusResponse response = VehicleStatusResponse.fromEntity(status, UserRole.ADMIN);
        
        // Проверяем, нужно ли отправлять обновление через StreamDataManager
        if (dataManager.shouldSendUpdate(vin, response)) {
            // TODO: Найти активный SseEmitter и отправить данные
            // Пока просто логируем и обновляем кэш
            logger.info("Would send immediate update for VIN: {} with data: {}", vin, response);
            dataManager.updateLastSentData(vin, response);
        } else {
            logger.debug("Skipping update for VIN: {} - no changes or too frequent", vin);
        }
    }
    
    // ==================== МЕТОДЫ ДЛЯ МОНИТОРИНГА ====================
    
    /**
     * Возвращает количество активных SSE соединений
     * 
     * ЗАЧЕМ: Для мониторинга нагрузки на систему
     */
    public int getActiveStreamsCount() {
        return executorPoolManager.getActiveStreamsCount();
    }
    
    /**
     * Возвращает текущий размер пула потоков
     * 
     * ЗАЧЕМ: Для мониторинга производительности
     */
    public int getCurrentPoolSize() {
        return executorPoolManager.getCurrentPoolSize();
    }
    
    /**
     * Возвращает статистику по нагрузке на потоки
     * 
     * ЗАЧЕМ: Для анализа производительности и балансировки
     */
    public java.util.Map<Integer, Integer> getExecutorLoadStats() {
        return executorPoolManager.getExecutorLoadStats();
    }
    
    // ==================== ПРИВАТНЫЕ МЕТОДЫ ====================
    
    /**
     * Проверяет существование ТС и завершает emitter с ошибкой если не найден
     * 
     * ЗАЧЕМ: Нужно сразу сообщить клиенту, если ТС не существует,
     * а не ждать fallback цикла
     */
    private boolean validateVehicleExists(String vin, SseEmitter emitter) {
        // Получаем статус ТС из БД
        Optional<VehicleStatus> vehicleStatus = vehicleStatusService.getLatestStatus(vin);
        
        if (vehicleStatus.isEmpty()) {
            // ТС не найден - логируем предупреждение и завершаем emitter с ошибкой
            logger.warn("Vehicle with VIN {} not found for streaming", vin);
            emitter.completeWithError(new RuntimeException("Vehicle not found"));
            return false;
        }
        
        return true;
    }
    
    /**
     * Настраивает обработку стрима: выбор потока и запуск планировщика
     * 
     * ЛОГИКА:
     * 1. Выбираем поток с наименьшей нагрузкой из пула
     * 2. Регистрируем стрим в выбранном потоке
     * 3. Запускаем fallback планировщик для надежности
     * 
     * ЗАЧЕМ ТАК СЛОЖНО: Обеспечиваем:
     * - Равномерное распределение нагрузки между потоками
     * - Автоматическое масштабирование при росте нагрузки
     * - Надежность через fallback механизмы
     */
    private void setupStreamProcessing(String vin, UserRole userRole, SseEmitter emitter) {
        // Шаг 1: Выбираем поток с наименьшей нагрузкой из пула
        java.util.concurrent.ScheduledExecutorService executor = executorPoolManager.selectExecutor(vin);
        
        // Шаг 2: Регистрируем стрим в выбранном потоке
        executorPoolManager.registerStream(vin, executor);
        
        // Шаг 3: Запускаем fallback планировщик для надежности
        updateScheduler.startFallbackScheduler(vin, userRole, vehicleStatusService, 
                                              dataManager, emitter, executor);
    }
    
    /**
     * Настраивает обработчики завершения стрима
     * 
     * ЛОГИКА: При любом завершении стрима (успешном, по таймауту, с ошибкой)
     * нужно корректно освободить все ресурсы:
     * - Остановить планировщик
     * - Удалить из пула потоков
     * - Очистить кэш данных
     * 
     * ЗАЧЕМ ОЧИСТКА: Предотвращает утечки памяти и ресурсов
     */
    private void setupCompletionHandlers(String vin, SseEmitter emitter) {
        // Обработчик успешного завершения стрима
        emitter.onCompletion(() -> {
            logger.info("Vehicle status stream completed for VIN: {}", vin);
            cleanupStream(vin);
        });
        
        // Обработчик таймаута стрима
        emitter.onTimeout(() -> {
            logger.warn("Vehicle status stream timeout for VIN: {}", vin);
            cleanupStream(vin);
        });
        
        // Обработчик ошибок стрима
        emitter.onError((ex) -> {
            logger.error("Vehicle status stream error for VIN: {}", vin, ex);
            cleanupStream(vin);
        });
    }
    
    /**
     * Очищает ресурсы при завершении стрима
     * 
     * ЛОГИКА ОЧИСТКИ:
     * 1. Останавливаем планировщик периодических обновлений
     * 2. Удаляем стрим из пула потоков
     * 3. Очищаем кэш данных для этого VIN
     * 
     * ЗАЧЕМ ПОЛНАЯ ОЧИСТКА: Предотвращает:
     * - Утечки памяти (кэш данных)
     * - Нагрузку на систему (планировщики)
     * - Некорректную статистику (пул потоков)
     */
    private void cleanupStream(String vin) {
        // Шаг 1: Останавливаем планировщик периодических обновлений
        updateScheduler.stopScheduler(vin);
        
        // Шаг 2: Удаляем стрим из пула потоков
        executorPoolManager.unregisterStream(vin);
        
        // Шаг 3: Очищаем кэш данных для этого VIN
        dataManager.clearStreamData(vin);
        
        logger.debug("Cleaned up resources for VIN: {}", vin);
    }
} 