#!/usr/bin/env bash
set -e

# Config
NET="crypto-net"
PG_PASS="changeme"
MINIO_PASS="changeme123"

echo "🚀 Starting Podman Local Simulation..."

# 0. Cleanup
echo "🧹 Cleaning up old containers..."
podman rm -f postgres redis minio identity-service pki-service tsa-service document-service api-gateway public-portal 2>/dev/null || true
podman network rm $NET 2>/dev/null || true

# 1. Network
echo "🌐 Creating Network '$NET'..."
podman network create $NET

# 2. Infrastructure
echo "🏗️  Starting Infrastructure..."

# Postgres
podman run -d --name postgres --net $NET \
  -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=$PG_PASS -e POSTGRES_DB=crypto_db \
  -p 5432:5432 docker.io/library/postgres:15

# Redis
podman run -d --name redis --net $NET \
  -p 6379:6379 docker.io/library/redis:7-alpine

# MinIO
podman run -d --name minio --net $NET \
  -e MINIO_ROOT_USER=admin -e MINIO_ROOT_PASSWORD=$MINIO_PASS \
  -p 9000:9000 -p 9001:9001 \
  docker.io/minio/minio server /data --console-address ":9001"

echo "⏳ Waiting for DB..."
sleep 5 # Simple wait, can be improved

# 3. PKI & Build
echo "🔐 Setup PKI..."
./scripts/setup_pki.sh

echo "🔨 Building Backend Jars..."
./gradlew clean build -x test
# Remove plain jars
find backend -name "*-plain.jar" -delete

# 4. Build & Run Services
echo "🐳 Building & Running Services..."

# Helper function
run_service() {
    NAME=$1
    BUILD_CTX=$2
    PORT=$3
    EXTRA_ARGS=$4
    
    echo "  -> $NAME..."
    podman build -t $NAME:latest $BUILD_CTX
    podman run -d --name $NAME --net $NET \
        -e SPRING_DATASOURCE_URL="jdbc:postgresql://postgres:5432/crypto_db" \
        -e SPRING_DATA_REDIS_HOST="redis" \
        -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=$PG_PASS \
        $EXTRA_ARGS \
        -p $PORT:$PORT \
        $NAME:latest
}

# Identity
run_service "identity-service" "backend/identity-service" "8081" ""

# PKI (Mount keys)
run_service "pki-service" "backend/pki-service" "8082" \
    "-v $(pwd)/infra/k3s/secrets/pki-ca.crt:/app/secrets/ca.crt -v $(pwd)/infra/k3s/secrets/pki-ca.key:/app/secrets/ca.key"

# TSA (Mount keys)
run_service "tsa-service" "backend/tsa-service" "8083" \
    "-v $(pwd)/infra/k3s/secrets/tsa-service.crt:/app/secrets/tsa.crt -v $(pwd)/infra/k3s/secrets/tsa-service.key:/app/secrets/tsa.key"

# Document (MinIO Env)
run_service "document-service" "backend/document-service" "8084" \
    "-e MINIO_ENDPOINT=http://minio:9000 -e MINIO_ACCESS_KEY=admin -e MINIO_SECRET_KEY=$MINIO_PASS"

# API Gateway (Service Discovery via DNS names)
run_service "api-gateway" "backend/api-gateway" "8080" \
    "-e IDENTITY_SERVICE_URL=http://identity-service:8081 -e PKI_SERVICE_URL=http://pki-service:8082 -e TSA_SERVICE_URL=http://tsa-service:8083 -e DOCUMENT_SERVICE_URL=http://document-service:8084"

# Frontend (Optional: Build or just assume dev?)
# User asked to build.
echo "  -> public-portal (Build)..."
podman build -t public-portal:latest apps/public-portal
podman run -d --name public-portal --net $NET \
    -e NUXT_PUBLIC_API_BASE=http://api-gateway:8080/api/v1 \
    -p 3000:3000 \
    public-portal:latest

echo "✅ All Services Started!"
podman ps
