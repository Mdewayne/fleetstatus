package com.dogma.fleetstatus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Управляет динамическим пулом потоков для обработки SSE стримов
 * 
 * ЗАЧЕМ НУЖЕН: В боевых проектах может быть тысячи SSE соединений одновременно.
 * Если каждое соединение создает свой поток - система упадет от нехватки ресурсов.
 * Поэтому создаем пул потоков и распределяем нагрузку между ними.
 * 
 * КАК РАБОТАЕТ:
 * 1. Создаем начальный пул из 2 потоков
 * 2. Каждый поток может обрабатывать до 10 SSE соединений
 * 3. Когда нагрузка растет - автоматически добавляем новые потоки (до 8)
 * 4. Когда нагрузка падает - убираем лишние потоки
 * 5. Распределяем новые соединения на поток с наименьшей нагрузкой
 */
@Component
public class ExecutorPoolManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutorPoolManager.class);
    
    // ==================== КОНФИГУРАЦИЯ ====================
    
    // Начальный размер пула - создаем 2 потока при старте
    private static final int INITIAL_POOL_SIZE = 2;
    
    // Максимальный размер пула - не больше 8 потоков
    private static final int MAX_POOL_SIZE = 8;
    
    // Сколько SSE соединений может обрабатывать один поток
    // Если больше - создаем новый поток
    private static final int STREAMS_PER_THREAD = 10;
    
    // Минимальный размер пула - всегда оставляем хотя бы 1 поток
    private static final int MIN_POOL_SIZE = 1;
    
    // ==================== СОСТОЯНИЕ ПУЛА ====================
    
    // Массив всех созданных потоков (executors)
    // AtomicReferenceArray - потокобезопасный массив
    private final AtomicReferenceArray<ScheduledExecutorService> executors;
    
    // Текущий размер пула (сколько потоков активно)
    // AtomicInteger - потокобезопасный счетчик
    private final AtomicInteger currentPoolSize;
    
    // Карта: VIN -> индекс потока, который его обрабатывает
    // ConcurrentHashMap - потокобезопасная карта
    private final Map<String, Integer> streamToExecutorMap = new ConcurrentHashMap<>();
    
    // ==================== КОНСТРУКТОР ====================
    
    public ExecutorPoolManager() {
        // Создаем массив на максимальный размер пула
        this.executors = new AtomicReferenceArray<>(MAX_POOL_SIZE);
        
        // Инициализируем счетчик размера пула
        this.currentPoolSize = new AtomicInteger(INITIAL_POOL_SIZE);
        
        // Создаем первый поток при инициализации
        createNewExecutor(0);
        
        logger.info("ExecutorPoolManager initialized with {} executor", INITIAL_POOL_SIZE);
    }
    
    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================
    
    /**
     * Выбирает поток для обработки нового SSE соединения
     * 
     * ЛОГИКА:
     * 1. Находим поток с наименьшей нагрузкой
     * 2. Если нагрузка > 10 соединений на поток - создаем новый поток
     * 3. Возвращаем подходящий поток
     */
    public ScheduledExecutorService selectExecutor(String streamId) {
        // Получаем текущий размер пула
        int currentSize = currentPoolSize.get();
        
        // Находим поток с минимальной нагрузкой
        int bestIndex = findLeastLoadedExecutor(currentSize);
        int minLoad = getExecutorLoad(bestIndex);
        
        // Если минимальная нагрузка >= 10 и можем создать новый поток
        if (minLoad >= STREAMS_PER_THREAD && currentSize < MAX_POOL_SIZE) {
            // Расширяем пул - добавляем новый поток
            return expandPool(currentSize);
        }
        
        // Возвращаем поток с наименьшей нагрузкой
        return executors.get(bestIndex);
    }
    
    /**
     * Регистрирует SSE соединение в выбранном потоке
     * 
     * ЗАЧЕМ: Нужно помнить, какой поток обрабатывает какое соединение
     * для правильного подсчета нагрузки и очистки ресурсов
     */
    public void registerStream(String streamId, ScheduledExecutorService executor) {
        // Находим индекс потока в массиве
        int executorIndex = getExecutorIndex(executor);
        
        // Сохраняем связь: VIN -> индекс потока
        streamToExecutorMap.put(streamId, executorIndex);
        
        logger.debug("Registered stream {} to executor {}", streamId, executorIndex);
    }
    
    /**
     * Удаляет SSE соединение из пула
     * 
     * ЗАЧЕМ: Когда соединение закрывается, нужно:
     * 1. Убрать его из учета нагрузки
     * 2. Проверить, можно ли уменьшить размер пула
     */
    public void unregisterStream(String streamId) {
        // Удаляем из карты регистрации
        streamToExecutorMap.remove(streamId);
        
        // Проверяем, можно ли уменьшить пул
        considerPoolShrink();
    }
    
    // ==================== ПРИВАТНЫЕ МЕТОДЫ ====================
    
    /**
     * Создает новый поток (executor) с именованным потоком
     * 
     * ЗАЧЕМ ИМЕНА: В логах будет видно, какой поток что делает
     * Например: "vehicle-stream-0", "vehicle-stream-1"
     */
    private void createNewExecutor(int index) {
        // Создаем ScheduledExecutorService с одним потоком
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, r -> {
            // Создаем поток с именем
            Thread t = new Thread(r, "vehicle-stream-" + index);
            // Делаем поток демоном (завершится при остановке приложения)
            t.setDaemon(true);
            return t;
        });
        
        // Сохраняем поток в массив
        executors.set(index, executor);
        
        logger.info("Created new executor at index: {}", index);
    }
    
    /**
     * Находит поток с наименьшей нагрузкой
     * 
     * ЛОГИКА: Проходим по всем активным потокам и считаем,
     * сколько SSE соединений обрабатывает каждый
     */
    private int findLeastLoadedExecutor(int currentSize) {
        int bestIndex = 0;
        int minLoad = Integer.MAX_VALUE;
        
        // Проходим по всем активным потокам
        for (int i = 0; i < currentSize; i++) {
            ScheduledExecutorService executor = executors.get(i);
            
            // Проверяем, что поток существует и не закрыт
            if (executor != null && !executor.isShutdown()) {
                // Считаем нагрузку на этот поток
                int load = getExecutorLoad(i);
                
                // Если нагрузка меньше текущего минимума
                if (load < minLoad) {
                    minLoad = load;
                    bestIndex = i;
                }
            }
        }
        
        return bestIndex;
    }
    
    /**
     * Расширяет пул, добавляя новый поток
     * 
     * ЛОГИКА: Атомарно увеличиваем счетчик размера пула
     * и создаем новый поток
     */
    private ScheduledExecutorService expandPool(int currentSize) {
        int newIndex = currentSize;
        
        // Атомарно увеличиваем размер пула (защита от race conditions)
        if (currentPoolSize.compareAndSet(currentSize, currentSize + 1)) {
            // Создаем новый поток
            createNewExecutor(newIndex);
            
            logger.info("Expanded thread pool to {} executors due to high load", currentSize + 1);
            
            // Возвращаем новый поток
            return executors.get(newIndex);
        }
        
        // Если не удалось увеличить размер (другой поток уже сделал это)
        // возвращаем первый поток как fallback
        return executors.get(0);
    }
    
    /**
     * Считает количество SSE соединений для указанного потока
     * 
     * ЛОГИКА: Проходим по карте регистрации и считаем,
     * сколько соединений привязано к этому потоку
     */
    private int getExecutorLoad(int executorIndex) {
        return (int) streamToExecutorMap.values().stream()
            .filter(index -> index == executorIndex)
            .count();
    }
    
    /**
     * Находит индекс потока в массиве
     * 
     * ЗАЧЕМ: Нужно знать, под каким индексом поток хранится в массиве
     */
    private int getExecutorIndex(ScheduledExecutorService executor) {
        // Проходим по всем потокам и ищем нужный
        for (int i = 0; i < currentPoolSize.get(); i++) {
            if (executors.get(i) == executor) {
                return i;
            }
        }
        
        // Если не нашли - возвращаем 0 как fallback
        return 0;
    }
    
    /**
     * Проверяет возможность уменьшения размера пула
     * 
     * ЛОГИКА: Если последний поток не используется (0 соединений),
     * то можно его удалить для экономии ресурсов
     */
    private void considerPoolShrink() {
        int currentSize = currentPoolSize.get();
        
        // Не уменьшаем ниже минимального размера
        if (currentSize <= MIN_POOL_SIZE) {
            return;
        }
        
        // Проверяем нагрузку на последний поток
        int lastExecutorIndex = currentSize - 1;
        int lastExecutorLoad = getExecutorLoad(lastExecutorIndex);
        
        // Если последний поток не используется
        if (lastExecutorLoad == 0) {
            // Атомарно уменьшаем размер пула
            if (currentPoolSize.compareAndSet(currentSize, currentSize - 1)) {
                // Получаем поток и очищаем ячейку в массиве
                ScheduledExecutorService executor = executors.getAndSet(lastExecutorIndex, null);
                
                if (executor != null) {
                    // Закрываем поток
                    executor.shutdown();
                    
                    logger.info("Shrunk thread pool to {} executors due to low load", currentSize - 1);
                }
            }
        }
    }
    
    // ==================== МЕТОДЫ ДЛЯ МОНИТОРИНГА ====================
    
    /**
     * Возвращает текущий размер пула потоков
     * 
     * ЗАЧЕМ: Для мониторинга и отладки
     */
    public int getCurrentPoolSize() {
        return currentPoolSize.get();
    }
    
    /**
     * Возвращает количество активных SSE соединений
     * 
     * ЗАЧЕМ: Для мониторинга нагрузки
     */
    public int getActiveStreamsCount() {
        return streamToExecutorMap.size();
    }
    
    /**
     * Возвращает статистику по нагрузке на потоки
     * 
     * ЗАЧЕМ: Для анализа производительности
     */
    public Map<Integer, Integer> getExecutorLoadStats() {
        Map<Integer, Integer> stats = new ConcurrentHashMap<>();
        
        int currentSize = currentPoolSize.get();
        for (int i = 0; i < currentSize; i++) {
            stats.put(i, getExecutorLoad(i));
        }
        
        return stats;
    }
    
    // ==================== ОЧИСТКА РЕСУРСОВ ====================
    
    /**
     * Закрывает все потоки при остановке приложения
     * 
     * ЗАЧЕМ: Нужно корректно освободить ресурсы
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down ExecutorPoolManager with {} executors", currentPoolSize.get());
        
        int currentSize = currentPoolSize.get();
        
        // Закрываем все активные потоки
        for (int i = 0; i < currentSize; i++) {
            ScheduledExecutorService executor = executors.get(i);
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                
                try {
                    // Ждем завершения задач (но не больше 5 секунд)
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        // Если не завершились - принудительно останавливаем
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    // Если прервали ожидание - принудительно останавливаем
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // Очищаем карту регистрации
        streamToExecutorMap.clear();
        
        logger.info("ExecutorPoolManager shutdown completed");
    }
} 