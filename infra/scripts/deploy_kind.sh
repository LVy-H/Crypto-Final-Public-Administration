#!/bin/bash
set -e

# Use nix to ensure dependencies - cache binary paths to avoid repeated evaluations
KUBECTL_BIN=$(nix build nixpkgs#kubectl --print-out-paths --no-link 2>/dev/null)/bin/kubectl
KIND_BIN=$(nix build nixpkgs#kind --print-out-paths --no-link 2>/dev/null)/bin/kind

# Use cached path if available, fallback to nix run
if [ -x "$KUBECTL_BIN" ]; then
    KUBECTL="$KUBECTL_BIN"
else
    KUBECTL="nix run nixpkgs#kubectl --"
fi
if [ -x "$KIND_BIN" ]; then
    KIND="$KIND_BIN"
else
    KIND="nix run nixpkgs#kind --"
fi

PROJECT_ROOT="$(dirname "$(dirname "$(dirname "$0")")")"
cd "$PROJECT_ROOT"

echo "=== Building Core Services ==="
./gradlew :core:ca-authority:bootJar :core:identity-service:bootJar :core:api-gateway:bootJar :core:org-service:bootJar :core:doc-service:bootJar :core:validation-service:bootJar -x test

echo "=== Building Docker Images ==="
docker build -t crypto-pqc/ca-authority:latest core/ca-authority/
docker build -t crypto-pqc/identity-service:latest core/identity-service/
docker build -t crypto-pqc/api-gateway:latest core/api-gateway/
docker build -t crypto-pqc/org-service:latest core/org-service/
docker build -t crypto-pqc/doc-service:latest core/doc-service/
docker build -t crypto-pqc/validation-service:latest core/validation-service/

echo "=== Loading Images into Kind ==="
IMAGES=("crypto-pqc/ca-authority:latest" "crypto-pqc/identity-service:latest" "crypto-pqc/api-gateway:latest" "crypto-pqc/org-service:latest" "crypto-pqc/doc-service:latest" "crypto-pqc/validation-service:latest")

for img in "${IMAGES[@]}"; do
    echo "Loading $img..."
    $KIND load docker-image "$img" --name crypto-pqc
done

echo "=== Applying Kubernetes Manifests ==="
$KUBECTL apply -k infra/k8s/base/

echo "=== Waiting for Pods ==="
$KUBECTL -n crypto-pqc rollout status statefulset/postgres --timeout=120s || true
$KUBECTL -n crypto-pqc rollout status deployment/ca-authority --timeout=120s || true
$KUBECTL -n crypto-pqc rollout status deployment/identity-service --timeout=120s || true

echo "=== Deployment Complete ==="
$KUBECTL get all -n crypto-pqc
