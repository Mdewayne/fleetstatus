package com.dogma.fleetstatus.service;

import com.dogma.fleetstatus.dto.UserRole;
import com.dogma.fleetstatus.dto.VehicleStatusResponse;
import com.dogma.fleetstatus.entity.VehicleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Планирует и выполняет периодические обновления для SSE стримов
 * 
 * ЗАЧЕМ НУЖЕН: В боевых проектах SSE соединения могут "зависнуть" или потерять данные.
 * Например, ТС может временно потерять связь, но клиент не узнает об этом.
 * Поэтому нужен fallback механизм - периодическая проверка и отправка данных.
 * 
 * КАК РАБОТАЕТ:
 * 1. Каждый SSE стрим получает свой планировщик
 * 2. Планировщик каждые 30 секунд проверяет данные в БД
 * 3. Если ТС найден - отправляет актуальные данные
 * 4. Если ТС не найден - увеличивает счетчик попыток
 * 5. При превышении лимита попыток - закрывает стрим
 * 6. Автоматически останавливается при завершении стрима
 */
@Component
public class UpdateScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateScheduler.class);
    
    // ==================== КОНФИГУРАЦИЯ ====================
    
    // Интервал проверки данных (30 секунд)
    // Fallback каждые 30 секунд для обеспечения надежности
    private static final int FALLBACK_CHECK_INTERVAL_SECONDS = 30;
    
    // Максимальное количество попыток для новых ТС (30 попыток = 15 минут)
    // Если ТС никогда не был найден, даем ему 15 минут на появление
    private static final int MAX_RETRY_ATTEMPTS = 30;
    
    // Максимальное количество попыток для существующих ТС (180 попыток = 1.5 часа)
    // Если ТС был найден, но потом пропал, даем ему больше времени на восстановление
    private static final int MAX_RETRY_ATTEMPTS_EXTENDED = 180;
    
    // ==================== СОСТОЯНИЕ ПЛАНИРОВЩИКА ====================
    
    // Активные задачи планировщика для каждого VIN
    // Ключ: VIN, Значение: ScheduledFuture задачи
    private final Map<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();
    
    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================
    
    /**
     * Запускает периодическую проверку для указанного VIN
     * 
     * ЛОГИКА:
     * 1. Создаем задачу, которая выполняется каждые 30 секунд
     * 2. Задача проверяет данные в БД и отправляет обновления
     * 3. Обрабатывает ошибки и восстанавливает соединения
     * 4. Автоматически закрывает стрим при превышении лимитов
     * 
     * ЗАЧЕМ FALLBACK: Обеспечивает надежность SSE соединений:
     * - Если ТС временно недоступен - ждем восстановления
     * - Если ТС потерян навсегда - закрываем соединение
     * - Если данные не обновляются - все равно отправляем актуальные
     */
    public void startFallbackScheduler(String vin, 
                                      UserRole userRole,
                                      VehicleStatusService vehicleStatusService,
                                      StreamDataManager dataManager,
                                      SseEmitter emitter,
                                      ScheduledExecutorService executor) {
        
        // Счетчики для отслеживания попыток
        // Используем массивы для передачи по ссылке в лямбда-выражении
        final int[] retryCount = {0};        // Количество неудачных попыток
        final boolean[] hasSentData = {false}; // Был ли ТС найден хотя бы раз
        
        // Создаем задачу периодической проверки
        ScheduledFuture<?> task = executor.scheduleAtFixedRate(() -> {
            try {
                // Выполняем fallback обновление
                performFallbackUpdate(vin, userRole, vehicleStatusService, dataManager, 
                                    emitter, retryCount, hasSentData);
            } catch (Exception e) {
                // Логируем неожиданные ошибки и закрываем стрим
                logger.error("Unexpected error in fallback scheduler for VIN: {}", vin, e);
                emitter.completeWithError(e);
            }
        }, FALLBACK_CHECK_INTERVAL_SECONDS, FALLBACK_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        // Сохраняем ссылку на задачу для возможности остановки
        activeTasks.put(vin, task);
        
        logger.debug("Started fallback scheduler for VIN: {}", vin);
    }
    
    /**
     * Останавливает планировщик для указанного VIN
     * 
     * ЗАЧЕМ: Когда SSE соединение закрывается, нужно остановить
     * периодическую проверку, чтобы не тратить ресурсы
     */
    public void stopScheduler(String vin) {
        // Получаем задачу из карты активных задач
        ScheduledFuture<?> task = activeTasks.remove(vin);
        
        if (task != null) {
            // Отменяем задачу (false = не прерываем выполняющуюся задачу)
            task.cancel(false);
            
            logger.debug("Stopped fallback scheduler for VIN: {}", vin);
        }
    }
    
    /**
     * Останавливает все активные планировщики
     * 
     * ЗАЧЕМ: При остановке приложения нужно корректно освободить ресурсы
     */
    public void stopAllSchedulers() {
        logger.info("Stopping {} active schedulers", activeTasks.size());
        
        // Отменяем все активные задачи
        activeTasks.values().forEach(task -> task.cancel(false));
        
        // Очищаем карту активных задач
        activeTasks.clear();
    }
    
    // ==================== ПРИВАТНЫЕ МЕТОДЫ ====================
    
    /**
     * Выполняет fallback обновление с проверкой данных и обработкой ошибок
     * 
     * ЛОГИКА:
     * 1. Получаем актуальный статус из БД
     * 2. Если ТС найден - сбрасываем счетчик попыток и отправляем данные
     * 3. Если ТС не найден - увеличиваем счетчик и обрабатываем ошибку
     * 4. При превышении лимита - закрываем стрим
     * 
     * ЗАЧЕМ СЛОЖНАЯ ЛОГИКА: Разные стратегии для разных ситуаций:
     * - Новый ТС: даем время на появление
     * - Существующий ТС: даем больше времени на восстановление
     */
    private void performFallbackUpdate(String vin, 
                                      UserRole userRole,
                                      VehicleStatusService vehicleStatusService,
                                      StreamDataManager dataManager,
                                      SseEmitter emitter,
                                      int[] retryCount,
                                      boolean[] hasSentData) throws IOException {
        
        // Шаг 1: Получаем актуальный статус из БД
        Optional<VehicleStatus> currentStatus = vehicleStatusService.getLatestStatus(vin);
        
        if (currentStatus.isPresent()) {
            // ТС найден - обрабатываем успешный случай
            
            // Сбрасываем счетчик попыток (ТС снова доступен)
            retryCount[0] = 0;
            
            // Отмечаем, что ТС был найден хотя бы раз
            hasSentData[0] = true;
            
            // Создаем DTO с фильтрацией по роли пользователя
            VehicleStatusResponse response = VehicleStatusResponse.fromEntity(
                currentStatus.get(), userRole);
            
            // Шаг 2: Проверяем, нужно ли отправлять обновление
            // (через StreamDataManager для экономии трафика)
            if (dataManager.shouldSendUpdate(vin, response)) {
                // Отправляем данные через SSE
                emitter.send(response);
                
                // Обновляем кэш в StreamDataManager
                dataManager.updateLastSentData(vin, response);
                
                logger.debug("Sent fallback vehicle status update for VIN: {}, role: {}", vin, userRole);
            }
            
        } else {
            // ТС не найден - обрабатываем ошибку
            handleVehicleNotFound(vin, retryCount, hasSentData, emitter);
        }
    }
    
    /**
     * Обрабатывает ситуацию, когда ТС не найден
     * 
     * ЛОГИКА:
     * 1. Увеличиваем счетчик попыток
     * 2. Если ТС раньше был найден - даем больше времени на восстановление
     * 3. Если ТС никогда не был найден - закрываем быстрее
     * 4. При превышении лимита - закрываем стрим
     * 
     * ЗАЧЕМ РАЗНЫЕ ЛИМИТЫ: Разные стратегии для разных ситуаций:
     * - Новый ТС: может быть опечатка в VIN или ТС еще не зарегистрирован
     * - Существующий ТС: может быть временные проблемы связи
     */
    private void handleVehicleNotFound(String vin, 
                                      int[] retryCount, 
                                      boolean[] hasSentData, 
                                      SseEmitter emitter) {
        
        // Увеличиваем счетчик попыток
        retryCount[0]++;
        
        if (hasSentData[0]) {
            // Случай 1: ТС раньше был найден, но сейчас недоступен
            // Даем больше времени на восстановление (6 минут)
            
            if (retryCount[0] > MAX_RETRY_ATTEMPTS_EXTENDED) {
                // Превышен лимит попыток - закрываем стрим
                logger.warn("Vehicle with VIN {} lost connection for {} minutes, completing stream", 
                    vin, MAX_RETRY_ATTEMPTS_EXTENDED * FALLBACK_CHECK_INTERVAL_SECONDS / 60);
                emitter.complete();
            } else {
                // Логируем предупреждение о временной недоступности
                logger.warn("Vehicle with VIN {} temporarily unavailable, retry {}/{}", 
                    vin, retryCount[0], MAX_RETRY_ATTEMPTS_EXTENDED);
            }
        } else {
            // Случай 2: ТС никогда не был найден
            // Закрываем быстрее (1 минута)
            
            if (retryCount[0] > MAX_RETRY_ATTEMPTS) {
                // Превышен лимит попыток - закрываем стрим
                logger.warn("Vehicle with VIN {} not found after {} attempts, completing stream", 
                    vin, MAX_RETRY_ATTEMPTS);
                emitter.complete();
            } else {
                // Логируем отладочную информацию
                logger.debug("Vehicle with VIN {} not found, retry {}/{}", 
                    vin, retryCount[0], MAX_RETRY_ATTEMPTS);
            }
        }
    }
    
    // ==================== МЕТОДЫ ДЛЯ МОНИТОРИНГА ====================
    
    /**
     * Возвращает количество активных планировщиков
     * 
     * ЗАЧЕМ: Для мониторинга нагрузки на систему
     */
    public int getActiveSchedulersCount() {
        return activeTasks.size();
    }
    
    /**
     * Возвращает список активных VIN
     * 
     * ЗАЧЕМ: Для отладки и мониторинга
     */
    public java.util.Set<String> getActiveVins() {
        return activeTasks.keySet();
    }
} 