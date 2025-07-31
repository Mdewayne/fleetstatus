# FleetStatus Microservices

## Структура репозитория

- `fleet-status-service/` — основной сервис управления статусом транспорта
- `api-gateway/` — точка входа (Spring Cloud Gateway, будет реализован на следующих этапах)
- `infrastructure/` — инфраструктурные файлы (docker-compose и т.д.)

## Быстрый старт

1. Перейдите в папку `infrastructure/`
2. Запустите инфраструктуру:
   ```sh
   docker-compose up --build
   ```
3. Сервис будет доступен на портах, указанных в docker-compose.yml

## Документация
- Архитектура и этапы развития: см. ARCHITECTURE.md 