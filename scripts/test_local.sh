#!/usr/bin/env bash

# Остановить, если что-то упадёт
set -e

# Перейти в корень проекта, где лежит pom.xml
cd "$(dirname "$0")/.."

echo "=== 🏗️ Сборка проекта ==="
mvn clean install

echo "=== 🐳 Запуск Docker Compose ==="
docker-compose up -d

echo "=== 🧪 Запуск тестов ==="
mvn verify

echo "=== 🧹 Остановка Docker Compose ==="
docker-compose down

echo "✅ Готово!"
