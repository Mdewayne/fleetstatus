#!/bin/bash

echo "🚀 Запускаю docker-compose..."
docker-compose up -d

echo "✅ Все сервисы запущены."
docker-compose ps
