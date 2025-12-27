#!/bin/bash
set -e

# Use nix to ensure dependencies
KUBECTL="nix run nixpkgs#kubectl --"
KIND="nix run nixpkgs#kind --"

echo "Building Docker Images..."
# Assuming you have docker-compose available or build manually
# docker-compose build 

# Services to load
SERVICES=("postgres:16-alpine" "api-gateway" "identity-service" "ca-authority" "cloud-sign" "validation-service" "public-portal" "admin-portal")

echo "Loading images into Kind..."
for img in "${SERVICES[@]}"; do
    # If image is local custom image (not postgres), tag might be latest
    if [[ "$img" != "postgres:16-alpine" ]]; then
       img="$img:latest"
    fi
    $KIND load docker-image "$img" --name crypto-pqc
done

echo "Applying Manifests..."
$KUBECTL apply -f k8s/base.yaml
$KUBECTL apply -f k8s/postgres.yaml
$KUBECTL apply -f k8s/services.yaml
$KUBECTL apply -f k8s/frontend.yaml
$KUBECTL apply -f k8s/ingress.yaml

echo "Waiting for pods..."
$KUBECTL wait --namespace crypto-system --for=condition=ready pod --selector=app=postgres --timeout=90s

echo "Deployment Complete!"
$KUBECTL get all -n crypto-system
