# GovTech PQC Digital Signature System

A compliance-ready digital signature platform for Vietnam's government agencies, implementing **Decree 23/2025/ND-CP** and **Circular 15/2025/TT-BKHCN** requirements.

## 🎯 Compliance Status

| Requirement | Status | Implementation |
|------------|--------|----------------|
| **Sole Control (Article 20)** | ✅ Compliant | OTP-based Signature Activation Protocol (SAP) |
| **Standard Cryptography** | ✅ Compliant | ECDSA P-384 (primary) + ML-DSA (Dilithium) hybrid |
| **Secure Key Storage** | ✅ Compliant | SoftHSM2/PKCS#11 - keys never leave HSM boundary |
| **Subordinate CA Trust** | ✅ Compliant | CSR workflow for National Root CA integration |
| **Long-Term Validation** | ✅ Compliant | RFC 3161 timestamping (TSA integration) |
| **Network Segmentation** | ✅ Compliant | K8s NetworkPolicies with 3 security zones |

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ZONE A: PUBLIC (DMZ)                        │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐          │
│   │Public Portal │    │ Admin Portal │    │ RSSP Gateway │          │
│   └──────┬───────┘    └──────┬───────┘    └──────┬───────┘          │
└──────────┼───────────────────┼───────────────────┼──────────────────┘
           │                   │                   │
           ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       ZONE B: INTERNAL (Trust)                      │
│   ┌────────────┐  ┌─────────────┐  ┌────────────┐  ┌────────────┐   │
│   │API Gateway │──│Identity Svc │──│Validation  │──│Doc Service │   │
│   └─────┬──────┘  └─────────────┘  └────────────┘  └─────┬──────┘   │
│         │                                                │          │
│   ┌─────┴──────┐  ┌─────────────┐  ┌────────────┐        │          │
│   │ PostgreSQL │  │  TSA Mock   │  │ Org Service│        │          │
│   └────────────┘  └─────────────┘  └────────────┘        │          │
└─────────────────────────────────────────────────────────────────────┘
           │                                                 │
           ▼                                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     ZONE C: SECURE (Air-Gapped)                     │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐          │
│   │ CA Authority │────│  Cloud Sign  │────│   SoftHSM    │          │
│   │  (Sub-CA)    │    │   (RSSP)     │    │  (PKCS#11)   │          │
│   └──────────────┘    └──────────────┘    └──────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
```

## 🔐 Security Features

### Signature Activation Protocol (SAP)
```java
// Two-step signing enforces "Sole Control"
POST /csc/v1/sign/init     → Returns challengeId + OTP
POST /csc/v1/sign/confirm  → Verifies OTP, executes signature
```

### Hybrid Cryptography
- **Primary**: ECDSA P-384 (`secp384r1`) - Government standard, recognized by PDF readers
- **Secondary**: ML-DSA-65 (Dilithium) - Post-quantum future-proofing

### HSM Integration
- Private keys generated and stored within PKCS#11 boundary
- Only `KeyHandle` references exposed to application layer
- `C_Sign` operations happen inside HSM

### Subordinate CA Workflow
```
1. POST /api/v1/ca/init-csr  → Generate CSR for National Root
2. (Manual) Submit CSR to Ban Cơ yếu
3. POST /api/v1/ca/upload-cert → Import signed certificate
```

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Kubernetes (Kind recommended for local dev)
- Java 21+ / Gradle 8+

### Deploy to Kind
```bash
cd infra/k8s
./deploy.sh dev apply
```

### Run Tests
```bash
./gradlew test                    # Unit tests
./e2e_test_phase7.sh              # E2E tests
```

## 📁 Project Structure

```
├── apps/
│   ├── public-portal/            # Citizen-facing Nuxt.js app
│   └── admin-portal/             # Admin Nuxt.js app
├── core/
│   ├── ca-authority/             # Certificate Authority (Sub-CA)
│   ├── identity-service/         # Authentication & JWT
│   ├── validation-service/       # Signature verification
│   └── doc-service/              # PDF signing & timestamping
├── rssp/
│   ├── cloud-sign/               # Remote Signing (CSC API)
│   └── rssp-gateway/             # CSC API Gateway
├── libs/
│   └── common-crypto/            # Shared crypto services
│       ├── StandardCryptoService # ECDSA P-384
│       ├── HybridSigningService  # ECDSA + Dilithium
│       ├── PqcCryptoService      # ML-DSA (Dilithium)
│       └── TsaClient             # RFC 3161 timestamping
└── infra/
    ├── k8s/                      # Kubernetes manifests
    │   └── base/
    │       └── network-policies.yaml  # Security zone enforcement
    └── docker/
        ├── softhsm/              # HSM mock
        └── tsa-mock/             # TSA mock
```

## 📜 Key Components

| Component | Purpose | Port |
|-----------|---------|------|
| `api-gateway` | API routing, TLS termination | 8080 |
| `identity-service` | JWT auth, token blacklist | 8081 |
| `ca-authority` | Certificate issuance, CRL | 8082 |
| `cloud-sign` | Remote signing (RSSP) | 8084 |
| `validation-service` | Signature verification | 8085 |
| `softhsm` | PKCS#11 key storage | 2345 |
| `tsa-mock` | RFC 3161 timestamps | 8318 |

## 📋 Regulatory Compliance

This system is designed to comply with:

- **Decree 23/2025/ND-CP** - Digital signatures in government
- **Circular 15/2025/TT-BKHCN** - Technical standards for e-signatures
- **FIPS 140-2** - Cryptographic module requirements (via PKCS#11)
- **eIDAS** - EU electronic signatures (for interoperability)
- **CSC API v2.0** - Cloud Signature Consortium standard

## 🔄 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-28 | Phase 7: Architecture fixes (SAP, ECDSA, HSM, Sub-CA, LTV) |
| 0.9.0 | 2025-12-27 | Phase 6: JWT blacklist, RBAC |
| 0.8.0 | 2025-12-26 | Phase 5: E2E tests, security audit |

---

**Note**: This is a development/testing environment. For production deployment:
1. Replace SoftHSM with certified HSM (nCipher/Thales)
2. Obtain certificates from National Root CA (Ban Cơ yếu Chính phủ)
3. Deploy to certified government cloud infrastructure
