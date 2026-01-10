#!/usr/bin/env bash
set -e

echo "🚀 Starting Local Docker Simulation..."

# 0. Setup PKI (Keys & Secrets)
./scripts/setup_pki.sh

echo "🔨 Building backend..."
./gradlew clean build -x test

echo "🧹 Removing plain JARs..."
find backend -name "*-plain.jar" -delete

echo "🐳 Starting Docker Compose..."
# Use --build to ensure fresh images from the JARs
docker-compose up --build -d

echo "✅ Stack is starting up!"
echo "   - Postgres: 5432"
echo "   - Redis: 6379"
echo "   - MinIO: 9000/9001"
echo "   - API Gateway: 8080"
echo "   - Public Portal: 3000"
echo "   Logs: docker-compose logs -f"
