#!/bin/bash

# Скрипт для запуска тестов с разными профилями

echo "🚀 Запуск тестов FleetStatus"
echo "================================"

# Функция для запуска юнит-тестов (H2)
run_unit_tests() {
    echo "📋 Запуск юнит-тестов (H2 in-memory)..."
    mvn test -Dspring.profiles.active=test
}

# Функция для запуска интеграционных тестов (Testcontainers)
run_integration_tests() {
    echo "🔗 Запуск интеграционных тестов (Testcontainers)..."
    mvn test -Dspring.profiles.active=testcontainers
}

# Функция для запуска всех тестов
run_all_tests() {
    echo "🎯 Запуск всех тестов..."
    mvn test
}

# Функция для запуска тестов с отчетом
run_tests_with_report() {
    echo "📊 Запуск тестов с отчетом..."
    mvn test jacoco:report
}

# Проверяем аргументы командной строки
case "$1" in
    "unit")
        run_unit_tests
        ;;
    "integration")
        run_integration_tests
        ;;
    "all")
        run_all_tests
        ;;
    "report")
        run_tests_with_report
        ;;
    *)
        echo "Использование: $0 {unit|integration|all|report}"
        echo ""
        echo "Опции:"
        echo "  unit       - Запуск только юнит-тестов (H2)"
        echo "  integration - Запуск только интеграционных тестов (Testcontainers)"
        echo "  all        - Запуск всех тестов"
        echo "  report     - Запуск всех тестов с отчетом покрытия"
        echo ""
        echo "Примеры:"
        echo "  $0 unit"
        echo "  $0 integration"
        echo "  $0 all"
        exit 1
        ;;
esac

echo ""
echo "✅ Тесты завершены!" 