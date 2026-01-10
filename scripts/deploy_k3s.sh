#!/usr/bin/env bash
set -e

# 0. Setup PKI (Keys & Secrets)
./scripts/setup_pki.sh

echo "🔨 Building backend locally..."
./gradlew clean build -x test

echo "🧹 Removing plain JARs to avoid copy ambiguity..."
find backend -name "*-plain.jar" -delete

echo "🐳 Building Container Images (Podman)..."

# Define services with their paths
declare -A service_paths=(
    ["api-gateway"]="backend/api-gateway"
    ["identity-service"]="backend/identity-service"
    ["pki-service"]="backend/pki-service"
    ["document-service"]="backend/document-service"
    ["tsa-service"]="backend/tsa-service"
    ["public-portal"]="apps/public-portal"
)

# Build and Import loop
for service in "${!service_paths[@]}"; do
    path="${service_paths[$service]}"
    echo "  - Building $service from $path..."
    
    # Build image
    podman build -t "$service:latest" "$path"
    
    echo "  - Importing $service:latest..."
    # Import to K3s
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
