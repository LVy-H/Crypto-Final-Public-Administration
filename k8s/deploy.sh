#!/bin/bash
# PQC Crypto Services - Kubernetes Deployment Script
# Usage: ./deploy.sh [dev|prod] [apply|delete]
# Supports both Minikube and Kind clusters

set -euo pipefail

ENVIRONMENT=${1:-dev}
ACTION=${2:-apply}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=========================================="
echo "PQC Crypto Services - Kubernetes Deployment"
echo "Environment: $ENVIRONMENT"
echo "Action: $ACTION"
echo "=========================================="

# Validate environment
if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
    echo "Error: Invalid environment. Use 'dev' or 'prod'"
    exit 1
fi

# Check prerequisites
command -v kubectl >/dev/null 2>&1 || { echo "kubectl is required but not installed."; exit 1; }

# Check cluster connection
if ! kubectl cluster-info >/dev/null 2>&1; then
    echo "Error: Cannot connect to Kubernetes cluster."
    exit 1
fi

# Detect cluster type
CLUSTER_TYPE="unknown"
if kubectl config current-context 2>/dev/null | grep -q "kind"; then
    CLUSTER_TYPE="kind"
    KIND_CLUSTER_NAME=$(kubectl config current-context | sed 's/kind-//')
    echo "Detected Kind cluster: $KIND_CLUSTER_NAME"
elif minikube status >/dev/null 2>&1; then
    CLUSTER_TYPE="minikube"
    echo "Detected Minikube cluster"
    eval $(minikube -p minikube docker-env)
else
    echo "Warning: Unknown cluster type. Will build images but may not load them."
fi

echo ""
echo "Building container images..."

# Build all service images
SERVICES="api-gateway identity-service ca-authority cloud-sign validation-service public-portal signature-core admin-portal"
for svc in $SERVICES; do
    echo "  Building $svc..."
    if [ -f "$SCRIPT_DIR/../services/$svc/Dockerfile" ]; then
        docker build -f "$SCRIPT_DIR/../services/$svc/Dockerfile" -t "crypto-pqc/$svc:latest" "$SCRIPT_DIR/../services" || \
        echo "    Warning: Build failed for $svc"
    elif [ -f "$SCRIPT_DIR/../apps/$svc/Dockerfile" ]; then
        docker build -t "crypto-pqc/$svc:latest" "$SCRIPT_DIR/../apps/$svc" || \
        echo "    Warning: Build failed for $svc"
    else
        echo "    Warning: Could not find Dockerfile for $svc"
    fi
done

# Load images into Kind cluster if applicable
if [[ "$CLUSTER_TYPE" == "kind" ]]; then
    echo ""
    echo "Loading images into Kind cluster..."
    for svc in $SERVICES; do
        echo "  Loading $svc..."
        nix run nixpkgs#kind -- load docker-image "crypto-pqc/$svc:latest" --name "$KIND_CLUSTER_NAME" 2>/dev/null || \
        echo "    Warning: Failed to load $svc image"
    done
fi

echo ""
echo "Applying Kubernetes manifests..."

# Apply using kustomize
if [[ "$ACTION" == "apply" ]]; then
    kubectl kustomize "$SCRIPT_DIR/overlays/$ENVIRONMENT" | kubectl apply -f -
    
    echo ""
    echo "Waiting for deployments to be ready..."
    kubectl -n crypto-pqc rollout status deployment/postgres --timeout=120s 2>/dev/null || true
    kubectl -n crypto-pqc rollout status deployment/api-gateway --timeout=120s 2>/dev/null || true
    kubectl -n crypto-pqc rollout status deployment/identity-service --timeout=120s 2>/dev/null || true
    
    echo ""
    echo "=========================================="
    echo "Deployment Status:"
    echo "=========================================="
    kubectl -n crypto-pqc get pods
    
    echo ""
    echo "Services:"
    kubectl -n crypto-pqc get svc
    
elif [[ "$ACTION" == "delete" ]]; then
    echo "Deleting resources..."
    kubectl kustomize "$SCRIPT_DIR/overlays/$ENVIRONMENT" | kubectl delete -f - --ignore-not-found
fi

echo ""
echo "Done!"
