#!/bin/bash
set -e

# Configuration
CLUSTER_NAME="crypto-pqc"
NAMESPACE="crypto-pqc"

# Cache nix binary paths to avoid repeated evaluations
KIND_BIN=$(nix build nixpkgs#kind --print-out-paths --no-link 2>/dev/null)/bin/kind
if [ -x "$KIND_BIN" ]; then
    KIND="$KIND_BIN"
else
    KIND="nix run nixpkgs#kind --"
fi

echo "=========================================="
echo "PQC Crypto Services - Kind Deployment"
echo "=========================================="

# Services to build
SERVICES=(
    "api-gateway"
    "identity-service"
    "ca-authority"
    "cloud-sign"
    "validation-service"
    "public-portal"
    "signature-core"
)

echo "Building and Loading images into Kind..."

for service in "${SERVICES[@]}"; do
    echo "Processing $service..."
    
    # Build image
    docker build -t "crypto-pqc/$service:latest" -f "services/$service/Dockerfile" "services/$service"
    
    # Load into Kind
    $KIND load docker-image "crypto-pqc/$service:latest" --name "$CLUSTER_NAME"
done

echo "Applying Kubernetes manifests..."
kubectl kustomize k8s/overlays/dev | kubectl apply -f -

echo "Restarting deployments to pick up new images..."
kubectl -n "$NAMESPACE" rollout restart deployment "${SERVICES[@]}"

echo "Deployment complete! Monitoring pods..."
kubectl -n "$NAMESPACE" get pods -w
