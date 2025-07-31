package com.dogma.fleetstatus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Основная конфигурация приложения
 * 
 * Включает поддержку асинхронных операций для SSE стриминга
 * и планировщика задач для периодических обновлений
 */
@Configuration
@EnableAsync
@EnableScheduling
public class ApplicationConfig {
    // Конфигурация будет добавляться по мере необходимости
} 