package com.dogma.fleetstatus;

import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.core.env.Environment;   
// import java.util.Arrays;

/**
 * Главный класс приложения для системы мониторинга автопарка
 * 
 * Spring Boot приложение для:
 * - Отслеживания статуса транспортных средств в реальном времени
 * - Предоставления REST API для управления данными ТС
 * - SSE стриминга обновлений для клиентов
 * - Управления правами доступа на основе ролей пользователей
 */
@SpringBootApplication
@EnableCaching
public class FleetStatusApplication {

    /**
     * Точка входа в приложение
     * 
     * @param args аргументы командной строки
     */
	public static void main(String[] args) {
		SpringApplication.run(FleetStatusApplication.class, args);
        // ConfigurableApplicationContext ctx = SpringApplication.run(FleetStatusApplication.class, args);
        // Environment env = ctx.getEnvironment();

        // System.out.println("🔍🔍🔍 PROFILE: " + Arrays.toString(env.getActiveProfiles()));
        // System.out.println("🔍🔍🔍 redis.host = " + env.getProperty("spring.redis.host"));
        // System.out.println("🔍🔍🔍 redis.port = " + env.getProperty("spring.redis.port"));
        // System.out.println("🔍🔍🔍 redis.time-to-live = " + env.getProperty("spring.cache.redis.time-to-live"));
	}
}
