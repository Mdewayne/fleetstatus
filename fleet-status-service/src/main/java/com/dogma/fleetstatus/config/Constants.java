package com.dogma.fleetstatus.config;

/**
 * Константы приложения для централизованного управления настройками
 * 
 * ЗАЧЕМ НУЖНЫ: В боевых проектах важно централизованно управлять всеми настройками.
 * Это позволяет легко изменять поведение системы без изменения кода.
 * 
 * КАК ИСПОЛЬЗУЮТСЯ:
 * - Настройки пулов потоков для масштабирования
 * - Интервалы обновлений для производительности
 * - HTTP заголовки для безопасности
 * - API пути для консистентности
 * - Сообщения об ошибках для локализации
 */
public final class Constants {
    
    private Constants() {
        // Утилитный класс - запрещаем создание экземпляров
    }
    
    // ==================== HTTP ЗАГОЛОВКИ ====================
    
    // Заголовок для определения роли пользователя
    // Используется для фильтрации данных по правам доступа
    public static final String USER_ROLE_HEADER = "X-User-Role";
    
    // Заголовок для CORS (Cross-Origin Resource Sharing)
    // Разрешает доступ с других доменов
    public static final String CORS_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    
    // ==================== SSE НАСТРОЙКИ ====================
    
    // Префикс для данных в SSE сообщениях
    // Стандарт SSE требует формат "data: {json}\n\n"
    public static final String SSE_DATA_PREFIX = "data: ";
    
    // Префикс для событий в SSE сообщениях
    // Позволяет отправлять разные типы событий
    public static final String SSE_EVENT_PREFIX = "event: ";
    
    // Префикс для комментариев в SSE сообщениях
    // Используется для keep-alive сообщений
    public static final String SSE_COMMENT_PREFIX = ": ";
    
    // Маркер конца SSE сообщения
    // Каждое SSE сообщение должно заканчиваться двумя переносами строк
    public static final String SSE_END_MARKER = "\n\n";
    
    // ==================== СТАТУСЫ ДВИГАТЕЛЯ ====================
    
    // Статус: двигатель работает
    public static final String ENGINE_STATUS_RUNNING = "RUNNING";
    
    // Статус: двигатель остановлен
    public static final String ENGINE_STATUS_STOPPED = "STOPPED";
    
    // Статус: предупреждение двигателя
    public static final String ENGINE_STATUS_WARNING = "WARNING";
    
    // ==================== НАСТРОЙКИ ПУЛА ПОТОКОВ ====================
    
    // Начальный размер пула потоков (2 потока)
    // Создается при старте приложения
    public static final int DEFAULT_CORE_POOL_SIZE = 2;
    
    // Максимальный размер пула потоков (8 потоков)
    // Автоматическое расширение при росте нагрузки
    public static final int DEFAULT_MAX_POOL_SIZE = 8;
    
    // Емкость очереди задач (100 задач)
    // Буфер для задач, когда все потоки заняты
    public static final int DEFAULT_QUEUE_CAPACITY = 100;
    
    // Время жизни потока в секундах (60 секунд)
    // Потоки закрываются после 60 секунд бездействия
    public static final long DEFAULT_KEEP_ALIVE_SECONDS = 60L;
    
    // ==================== НАСТРОЙКИ ОБНОВЛЕНИЙ ====================
    
    // Интервал fallback обновлений (30 секунд)
    // Периодическая проверка данных для надежности
    public static final long DEFAULT_UPDATE_INTERVAL_MS = 30000L;
    
    // Минимальный интервал между отправками (1 секунда)
    // Защита от спама обновлений
    public static final long MIN_UPDATE_INTERVAL_MS = 1000L;
    
    // Таймаут SSE соединения (5 минут)
    // Соединение закрывается после 5 минут бездействия
    public static final long SSE_TIMEOUT_MS = 300000L;
    
    // ==================== API ПУТИ ====================
    
    // Базовый путь для API ТС
    public static final String API_BASE_PATH = "/api/vehicle";
    
    // Путь для получения/обновления статуса ТС
    public static final String STATUS_PATH = "/{vin}/status";
    
    // Путь для SSE стрима статуса ТС
    public static final String STREAM_PATH = "/{vin}/status/stream";
    
    // ==================== СООБЩЕНИЯ ОБ ОШИБКАХ ====================
    
    // Сообщение: ТС не найден
    // %s заменяется на VIN
    public static final String ERROR_VIN_NOT_FOUND = "Vehicle with VIN %s not found";
    
    // Сообщение: неверная роль пользователя
    // %s заменяется на роль
    public static final String ERROR_INVALID_ROLE = "Invalid user role: %s";
    
    // Сообщение: ошибка создания стрима
    // %s заменяется на VIN
    public static final String ERROR_STREAM_CREATION_FAILED = "Failed to create stream for VIN: %s";
} 