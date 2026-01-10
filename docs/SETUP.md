# PQC Digital Signature System - Setup Guide

This guide describes how to build and deploy the `crypt-final-public-administration` project locally.

## Prerequisites
- **Java 21+** (for Backend)
- **Node.js 22+** (for Frontend)
- **Podman** (or Docker)
- **Kubectl**
- **K3s** (or Kind)

## 1. Build from Source

### Backend Services
Build all Kotlin microservices:
```bash
./gradlew clean build -x test
```

### Frontend Application
Build the Vue 3 Public Portal:
```bash
cd apps/public-portal
npm install
npm run build
cd ../..
```

## 2. Deploy to K3s (Local)

We use a unified script to build container images and deploy Helm charts/Manifests.

```bash
# Deploys to the 'crypto-backend' namespace
./scripts/deploy_k3s.sh
```

**Verification:**
```bash
kubectl get pods -n crypto-backend
```

## 3. Accessing the System

- **Public Portal**: `http://localhost:3000` (via Ingress or NodePort)
- **API Gateway**: `http://localhost:8080`

## 4. Operational Maintenance

### Run Tests
```bash
# Backend Unit Tests
./gradlew test

# E2E Tests
cd tests/e2e
npx playwright test
```

### Reset Environment
To wipe the cluster and start fresh:
```bash
kubectl delete namespace crypto-backend
```
