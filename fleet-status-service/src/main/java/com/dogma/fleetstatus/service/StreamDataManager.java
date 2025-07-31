package com.dogma.fleetstatus.service;

import com.dogma.fleetstatus.dto.VehicleStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управляет кэшированием и проверкой данных для SSE стримов
 * 
 * ЗАЧЕМ НУЖЕН: В боевых проектах важно экономить трафик и ресурсы.
 * Если отправлять одни и те же данные каждые 30 секунд - это расточительно.
 * Поэтому кэшируем последние отправленные данные и отправляем только при изменениях.
 * 
 * КАК РАБОТАЕТ:
 * 1. Кэшируем последние отправленные данные для каждого VIN
 * 2. При получении новых данных сравниваем с кэшем
 * 3. Отправляем только если данные изменились
 * 4. Контролируем частоту отправки (не чаще 1 раза в секунду)
 * 5. Автоматически очищаем кэш при завершении стрима
 */
@Component
public class StreamDataManager {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamDataManager.class);
    
    // ==================== КОНФИГУРАЦИЯ ====================
    
    // Минимальный интервал между отправками (1 секунда)
    // Защита от спама обновлений
    private static final int MIN_UPDATE_INTERVAL_MS = 1000;
    
    // ==================== КЭШ ДАННЫХ ====================
    
    // Кэш последних отправленных данных для каждого VIN
    // Ключ: VIN, Значение: последние отправленные данные
    private final Map<String, VehicleStatusResponse> lastSentData = new ConcurrentHashMap<>();
    
    // Время последней отправки для каждого VIN
    // Ключ: VIN, Значение: timestamp последней отправки
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    
    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================
    
    /**
     * Проверяет, нужно ли отправлять обновление данных
     * 
     * ЛОГИКА ПРОВЕРКИ:
     * 1. Проверяем частоту отправки (не чаще 1 раза в секунду)
     * 2. Проверяем, изменились ли данные по сравнению с последней отправкой
     * 3. Если данные изменились и прошло достаточно времени - разрешаем отправку
     * 
     * ЗАЧЕМ ТАК СЛОЖНО: В боевых проектах это критично для:
     * - Экономии трафика (особенно мобильный интернет)
     * - Снижения нагрузки на сервер
     * - Предотвращения спама в логах
     */
    public boolean shouldSendUpdate(String vin, VehicleStatusResponse newData) {
        // Шаг 1: Проверяем частоту отправки
        if (isUpdateTooFrequent(vin)) {
            // Слишком часто - пропускаем отправку
            return false;
        }
        
        // Шаг 2: Проверяем, изменились ли данные
        if (hasDataChanged(vin, newData)) {
            // Данные изменились - разрешаем отправку
            return true;
        }
        
        // Данные не изменились - пропускаем отправку
        logger.debug("Skipping update for VIN: {} - no changes in data", vin);
        return false;
    }
    
    /**
     * Обновляет кэш после успешной отправки данных
     * 
     * ЗАЧЕМ: Нужно сохранить отправленные данные и время отправки
     * для правильной работы проверок в будущем
     */
    public void updateLastSentData(String vin, VehicleStatusResponse data) {
        // Сохраняем отправленные данные в кэш
        lastSentData.put(vin, data);
        
        // Сохраняем время отправки
        lastUpdateTime.put(vin, System.currentTimeMillis());
        
        logger.debug("Updated cache for VIN: {}", vin);
    }
    
    /**
     * Очищает данные для указанного VIN при завершении стрима
     * 
     * ЗАЧЕМ: Когда SSE соединение закрывается, нужно освободить память
     * и убрать данные из кэша, чтобы они не мешали новым соединениям
     */
    public void clearStreamData(String vin) {
        // Удаляем данные из кэша
        lastSentData.remove(vin);
        
        // Удаляем время последней отправки
        lastUpdateTime.remove(vin);
        
        logger.debug("Cleared cache for VIN: {}", vin);
    }
    
    /**
     * Очищает все кэшированные данные (при завершении работы)
     * 
     * ЗАЧЕМ: При остановке приложения нужно освободить всю память
     */
    public void clearAllData() {
        // Очищаем все кэши
        lastSentData.clear();
        lastUpdateTime.clear();
        
        logger.info("Cleared all stream data cache");
    }
    
    // ==================== ПРИВАТНЫЕ МЕТОДЫ ====================
    
    /**
     * Проверяет, не слишком ли часто отправляются обновления
     * 
     * ЛОГИКА: Сравниваем текущее время с временем последней отправки.
     * Если прошло меньше 1 секунды - считаем отправку слишком частой.
     * 
     * ЗАЧЕМ ЗАЩИТА: Предотвращает спам обновлений при быстрых изменениях данных.
     * Например, если ТС отправляет данные каждые 100мс, мы не будем
     * пересылать их клиентам каждые 100мс, а только раз в секунду.
     */
    private boolean isUpdateTooFrequent(String vin) {
        // Получаем текущее время
        long currentTime = System.currentTimeMillis();
        
        // Получаем время последней отправки для этого VIN
        Long lastUpdate = lastUpdateTime.get(vin);
        
        // Если есть запись о последней отправке
        if (lastUpdate != null) {
            // Вычисляем, сколько времени прошло с последней отправки
            long timeSinceLastUpdate = currentTime - lastUpdate;
            
            // Если прошло меньше минимального интервала
            if (timeSinceLastUpdate < MIN_UPDATE_INTERVAL_MS) {
                logger.debug("Skipping update for VIN: {} - too frequent ({}ms since last update)", 
                    vin, timeSinceLastUpdate);
                return true;
            }
        }
        
        // Если записи нет или прошло достаточно времени
        return false;
    }
    
    /**
     * Проверяет, изменились ли данные по сравнению с последней отправкой
     * 
     * ЛОГИКА: Сравниваем новые данные с данными из кэша.
     * Если данных в кэше нет - считаем, что данные изменились (первая отправка).
     * 
     * КАК СРАВНЕНИЕ: Используется метод equals() из VehicleStatusResponse,
     * который сравнивает все поля объекта.
     * 
     * ЗАЧЕМ СРАВНЕНИЕ: Отправляем только при реальных изменениях:
     * - Новый пробег
     * - Изменение уровня топлива
     * - Изменение местоположения
     * - Изменение статуса двигателя
     * и т.д.
     */
    private boolean hasDataChanged(String vin, VehicleStatusResponse newData) {
        // Получаем последние отправленные данные из кэша
        VehicleStatusResponse lastData = lastSentData.get(vin);
        
        // Если данных в кэше нет - это первая отправка
        if (lastData == null) {
            logger.debug("First time sending data for VIN: {}", vin);
            return true;
        }
        
        // Сравниваем новые данные с последними отправленными
        // Используется equals() метод из VehicleStatusResponse
        boolean hasChanged = !lastData.equals(newData);
        
        if (hasChanged) {
            logger.debug("Data changed for VIN: {} - sending update", vin);
        }
        
        return hasChanged;
    }
    
    // ==================== МЕТОДЫ ДЛЯ МОНИТОРИНГА ====================
    
    /**
     * Возвращает количество VIN в кэше
     * 
     * ЗАЧЕМ: Для мониторинга использования памяти
     */
    public int getCacheSize() {
        return lastSentData.size();
    }
    
    /**
     * Возвращает данные из кэша для указанного VIN
     * 
     * ЗАЧЕМ: Для отладки и мониторинга
     */
    public VehicleStatusResponse getCachedData(String vin) {
        return lastSentData.get(vin);
    }
    
    /**
     * Возвращает время последней отправки для указанного VIN
     * 
     * ЗАЧЕМ: Для отладки проблем с частотой отправки
     */
    public Long getLastUpdateTime(String vin) {
        return lastUpdateTime.get(vin);
    }
} 