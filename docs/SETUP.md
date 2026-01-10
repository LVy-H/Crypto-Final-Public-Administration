# PQC Digital Signature System - Setup Guide

Complete guide to deploy the system from scratch.

## Prerequisites

- **Nix** (with flakes enabled)
- **Docker** (for building images)
- **kubectl** (Kubernetes CLI)
- **WireGuard** config (for public access tunnel)

## Quick Start

```bash
# 1. Clone and enter directory
cd Crypto-Final-Public-Administration

# 2. Create Kind cluster
nix run nixpkgs#kind -- create cluster --name crypto-pqc --config infra/kind/kind-config.yaml

# 3. Setup secrets
cp infra/k8s/base/.env.example infra/k8s/base/.env
# Edit .env with real credentials

# 4. Build and deploy
./scripts/deploy.sh

# 5. Start port-forward (local access)
kubectl port-forward svc/api-gateway -n crypto-pqc 8091:8080 &

# 6. Access portal
open http://portal.gov-id.lvh.id.vn:8091
```

---

## Detailed Steps

### 1. Create Kind Cluster

```bash
# Create cluster with ingress support
nix run nixpkgs#kind -- create cluster \
  --name crypto-pqc \
  --config infra/kind/kind-config.yaml

# Verify
kubectl cluster-info
kubectl get nodes
```

### 2. Configure Secrets

```bash
# Copy template
cp infra/k8s/base/.env.example infra/k8s/base/.env

# Edit with secure values
cat > infra/k8s/base/.env << 'EOF'
postgres-username=admin
postgres-password=$(openssl rand -base64 32)
ca-master-key=$(openssl rand -hex 32)
EOF
```

### 3. Build Docker Images

```bash
# Build all Java services
./gradlew bootJar

# Build Docker images
./scripts/build-images.sh

# Load into Kind
for img in api-gateway identity-service ca-authority \
           signature-core validation-service public-portal; do
  nix run nixpkgs#kind -- load docker-image $img:latest --name crypto-pqc
done
```

### 4. Deploy to Kubernetes

```bash
# Apply base manifests
kubectl apply -k infra/k8s/base

# Wait for pods
kubectl wait --for=condition=Ready pods --all -n crypto-pqc --timeout=300s

# Check status
kubectl get pods -n crypto-pqc
```

### 5. Setup Ingress

```bash
# Deploy nginx ingress
kubectl apply -f infra/k8s/base/nginx-pqc-ingress.yaml

# Verify
kubectl get pods -n ingress-nginx
```

### 6. Setup WireGuard Tunnel (Optional - Public Access)

```bash
# Create WireGuard secret
kubectl apply -f infra/k8s/base/wg-secret.yaml

# Deploy WireGuard proxy
kubectl apply -f infra/k8s/base/wg-proxy.yaml

# Verify tunnel
kubectl exec -n ingress-nginx deploy/wg-proxy -c wireguard -- wg show
```

---

## Access URLs

| Service | Local URL | Public URL |
|---------|-----------|------------|
| Portal | http://portal.gov-id.lvh.id.vn:8091 | https://portal.gov-id.lvh.id.vn |
| API | http://api.gov-id.lvh.id.vn:8091 | https://api.gov-id.lvh.id.vn |

---

## Troubleshooting

### Pods not starting
```bash
kubectl describe pod <pod-name> -n crypto-pqc
kubectl logs <pod-name> -n crypto-pqc
```

### Database connection issues
```bash
# Check postgres
kubectl exec -it postgres-0 -n crypto-pqc -- psql -U admin -l
```

### WireGuard not connecting
```bash
kubectl logs deploy/wg-proxy -n ingress-nginx -c wireguard
kubectl exec -n ingress-nginx deploy/wg-proxy -c wireguard -- wg show
```

### Rebuild and redeploy single service
```bash
./gradlew :core:ca-authority:bootJar
docker build -t ca-authority:latest -f core/ca-authority/Dockerfile .
nix run nixpkgs#kind -- load docker-image ca-authority:latest --name crypto-pqc
kubectl rollout restart deployment/ca-authority -n crypto-pqc
```

---

## Cleanup

```bash
# Delete cluster
nix run nixpkgs#kind -- delete cluster --name crypto-pqc
```
