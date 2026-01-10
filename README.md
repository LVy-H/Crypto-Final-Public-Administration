# GovTech PQC Digital Signature System

A compliance-ready digital signature platform for Vietnam's government agencies, implementing **Decree 23/2025/ND-CP** and **Circular 15/2025/TT-BKHCN** requirements.

## 🎯 Compliance Status

| Requirement | Implementation | Status |
|------------|----------------|--------|
| **Sole Control (Article 20)** | **Client-Side Keys** (Encrypted IndexedDB) | ✅ Compliant |
| **Standard Cryptography** | **Pure PQC** (ML-DSA / SLH-DSA) | ✅ Compliant |
| **Document Format** | **ASiC-E** (Associated Signature Containers) | ✅ Compliant |
| **Subordinate CA Trust** | **Offline Root CA** (Air-gapped) | ✅ Compliant |
| **Long-Term Validation** | **RFC 3161 TSA** (Timestamping) | ✅ Compliant |
| **Network Segmentation** | **K8s NetworkPolicies** (Zones A/B/C) | ✅ Compliant |

## 🏗️ Technical Architecture

### Pure PQC Strategy
The system exclusively uses NIST-standardized Post-Quantum Cryptography:
- **Signing**: ML-DSA-44/65 (Dilithium) or SLH-DSA-SHAKE-128F.
- **Key Storage**: Browser-based encrypted storage (Keys never leave the client).

### ASiC-E Container Format
Due to lack of PQC support in standard PDF readers (PAdES), we use **ASiC-E** (Associated Signature Containers - Extended).
- **Format**: `.asic` (ZIP container).
- **Content**: Original Document + Detached CMS Signature (`.p7s`).
- **Specification**: See [ASiC Profile](docs/specs/ASIC_PROFILE.md).

### Service Mesh
- **Frontend**: Vue 3 + PQC WASM (`apps/public-portal`).
- **Backend**: Spring Boot 3 + Kotlin (`backend/`).
- **Infra**: K3s + MinIO + PostgreSQL.

## 📚 Documentation Map

### For Developers
- **[Setup Guide](docs/SETUP.md)**: How to build and run the system locally.
- **[API Reference](docs/specs/API_V1.md)**: REST API contract.
- **[Project Workflows](.agent/workflows/)**: Agent-executable development scripts.

### For Architects
- **[PKI Architecture](docs/PKI_ARCHITECTURE.md)**: 3-Tier CA Hierarchy design.
- **[ASiC Spec](docs/specs/ASIC_PROFILE.md)**: Container format details.
- **[Security Features](docs/FEATURES.md)**: Screenshots and Flow diagrams.

## 🚀 Quick Start

### 1. Build All Components
Use the agent workflow or the shell script:
```bash
./gradlew clean build -x test
cd apps/public-portal && npm install && npm run build
```

### 2. Deploy to Local K3s
```bash
./scripts/deploy_k3s.sh
```

### 3. Verify Deployment
```bash
kubectl get pods -n crypto-backend
```

## 📁 Project Structure

```
├── backend/                  # Kotlin Microservices
│   ├── api-gateway/          # Edge Service
│   ├── identity-service/     # Auth & KYC
│   ├── pki-service/          # Intermediate CA
│   ├── document-service/     # ASiC Packaging & Verification
│   ├── tsa-service/          # Timestamping
│   └── offline-ca-cli/       # Root CA Tool
├── apps/
│   └── public-portal/        # Vue 3 Frontend (PQC WASM)
├── docs/                     # Documentation
│   ├── specs/                # Technical Specifications
│   └── workflows/            # Business Process Flows
├── infra/
│   └── k3s/                  # Kubernetes Manifests
└── scripts/                  # Deployment Utilities
```

## 📜 Key Components

| Service | Port | Purpose |
|---------|------|---------|
| `api-gateway` | 8080 | Entry point, Rate Limiting |
| `identity-service`| 8081 | User Identity, eKYC |
| `pki-service` | 8082 | Certificate Enrollment |
| `document-service`| 8083 | ASiC Packaging, Sig Verification |
| `tsa-service` | 8084 | RFC 3161 Timestamping |
