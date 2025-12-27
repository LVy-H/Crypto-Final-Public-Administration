#!/bin/bash
set -e

# Use nix to ensure dependencies
KUBECTL="nix run nixpkgs#kubectl --"
KIND="nix run nixpkgs#kind --"

PROJECT_ROOT="$(dirname "$(dirname "$(dirname "$0")")")"
cd "$PROJECT_ROOT"

echo "=== Building Core Services ==="
./gradlew :core:ca-authority:bootJar :core:identity-service:bootJar :core:api-gateway:bootJar :core:org-service:bootJar :core:doc-service:bootJar -x test

echo "=== Building Docker Images ==="
docker build -t crypto-pqc/ca-authority:latest core/ca-authority/
docker build -t crypto-pqc/identity-service:latest core/identity-service/
docker build -t crypto-pqc/api-gateway:latest core/api-gateway/
docker build -t crypto-pqc/org-service:latest core/org-service/
docker build -t crypto-pqc/doc-service:latest core/doc-service/

echo "=== Loading Images into Kind ==="
IMAGES=("crypto-pqc/ca-authority:latest" "crypto-pqc/identity-service:latest" "crypto-pqc/api-gateway:latest" "crypto-pqc/org-service:latest" "crypto-pqc/doc-service:latest")

for img in "${IMAGES[@]}"; do
    echo "Loading $img..."
    $KIND load docker-image "$img" --name crypto-pqc
done

echo "=== Applying Kubernetes Manifests ==="
$KUBECTL apply -k infra/k8s/base/

echo "=== Waiting for Pods ==="
$KUBECTL -n crypto-pqc rollout status deployment/postgres --timeout=120s || true
$KUBECTL -n crypto-pqc rollout status deployment/ca-authority --timeout=120s || true
$KUBECTL -n crypto-pqc rollout status deployment/identity-service --timeout=120s || true

echo "=== Deployment Complete ==="
$KUBECTL get all -n crypto-pqc
