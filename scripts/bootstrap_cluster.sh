#!/bin/bash
set -e

echo "🚀 Bootstrapping k3s Cluster for PQC Crypto Project..."

# Check kubectl
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl not found!"
    exit 1
fi

echo "✅ kubectl found"

# Create Namespaces
echo "📦 Creating Namespaces..."
kubectl create namespace infra --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace crypto-app --dry-run=client -o yaml | kubectl apply -f -

# Apply Infrastructure
echo "🏗️  Deploying Base Infrastructure (Redis, Postgres, MinIO)..."
kubectl apply -k infra/k3s/base

echo "🎉 Infrastructure bootstrapping complete!"
kubectl get pods -n infra
