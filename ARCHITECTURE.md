# Архитектура FleetStatus

## Этап 1: Минимальная структура

- Моно-репозиторий: все сервисы и инфраструктура в одном репозитории
- Сервисы:
  - `fleet-status-service` — основной CRUD сервис
  - `api-gateway` — точка входа (будет реализован позже)
- Инфраструктура: docker-compose, Postgres

## Дальнейшие этапы

1. Многоуровневое кэширование (Caffeine + Redis)
2. Введение Kafka и событийной архитектуры
3. AnomalyProcessorService (Kafka consumer)
4. JWT, rate limiting, circuit breaker на Gateway
5. Мониторинг (Prometheus, Grafana)

## Запуск

1. Перейти в папку `infrastructure/`
2. Запустить docker-compose

## Подробнее по этапам — см. комментарии в этом файле и README 