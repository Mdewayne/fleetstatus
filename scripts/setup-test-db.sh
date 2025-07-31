#!/bin/bash

# Скрипт для настройки тестовой базы данных PostgreSQL
# Запускать перед интеграционными тестами

echo "Настройка тестовой базы данных PostgreSQL..."

# Проверяем, запущен ли PostgreSQL
if ! pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
    echo "ОШИБКА: PostgreSQL не запущен на localhost:5432"
    echo "Запустите PostgreSQL и повторите попытку"
    exit 1
fi

# Создаем тестовую базу данных
echo "Создание тестовой базы данных..."
psql -h localhost -U postgres -c "DROP DATABASE IF EXISTS fleetdb_test;"
psql -h localhost -U postgres -c "CREATE DATABASE fleetdb_test;"

echo "Тестовая база данных fleetdb_test создана успешно!"
echo ""
echo "Теперь можно запускать интеграционные тесты:"
echo "mvn test -Dspring.profiles.active=postgres" 