#!/bin/bash
set -e

# 0. Setup PKI (Keys & Secrets)
./scripts/setup_pki.sh

echo "🔨 Building backend locally..."
./gradlew clean build -x test

echo "🧹 Removing plain JARs to avoid copy ambiguity..."
find backend -name "*-plain.jar" -delete

echo "🐳 Building Container Images (Podman)..."
# Use podman compose
podman compose build

echo "📦 Importing Images to K3s..."
# Ensure we export the images we just built
services=("identity-service" "pki-service" "tsa-service" "document-service" "api-gateway")

for service in "${services[@]}"; do
    echo "  - Importing $service:latest..."
    # Use podman save. 
    # Note: K3s usually requires root to import to its containerd.
    podman save "$service:latest" | sudo k3s ctr images import -
    echo "    Imported."
done

echo "🚀 Applying Kubernetes Manifests..."
# Apply Infrastructure (if not already up)
# kubectl apply -f infra/k3s/base/

# Apply Apps
kubectl apply -f infra/k3s/apps/
kubectl apply -f infra/k3s/secrets/pki-secrets.yaml

echo "✅ Deployment Triggered. Check status with: kubectl get pods -n crypto-backend"
