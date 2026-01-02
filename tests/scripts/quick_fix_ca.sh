#!/bin/bash
set -e

# Define root dir
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

echo "1. Wiping stale CA data inside pod..."
POD_NAME=$(kubectl -n crypto-pqc get pods -l app=ca-authority -o jsonpath='{.items[0].metadata.name}')
if [ -z "$POD_NAME" ]; then
    echo "Error: ca-authority pod not found"
    exit 1
fi
kubectl -n crypto-pqc exec "$POD_NAME" -- rm -rf /secure/ca/root-ca-key.pem /secure/ca/root-ca-cert.pem /secure/ca/failed-keys || echo "Clean directory or partial failure"

echo "2. Building CA Authority JAR..."
./gradlew :core:ca-authority:build -x test

echo "3. Redeploying via deploy.sh..."
# Run deploy.sh updates images and restarts pods
./infra/k8s/deploy.sh dev apply

echo "4. Waiting for CA Authority to be ready..."
kubectl -n crypto-pqc rollout status deployment/ca-authority --timeout=60s

echo "5. Initializing Root CA..."
python3 tests/scripts/init_ca.py

echo "6. Quick Fix Complete."
