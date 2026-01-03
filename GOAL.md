# Project Goals & Architecture Decisions

> **Purpose**: This document defines the project's core goals and architectural constraints.  
> Review this before making significant changes to avoid breaking the design.

---

## 🎯 Primary Goal

Build a **Post-Quantum Cryptography (PQC) Digital Signature System** for Vietnamese public administration that is:
- **Quantum-Safe**: Resistant to quantum computer attacks
- **NIST Compliant**: Uses standardized FIPS 203/204 algorithms
- **Production Ready**: Suitable for government document signing

---

## 🔐 Cryptographic Architecture

### Pure ML-DSA Strategy (FIPS 204)

> **Decision**: 100% Post-Quantum. No ECDSA/RSA for new operations.

| Component | Algorithm | Security Level |
|-----------|-----------|:-------------:|
| **Root CA** | ML-DSA-87 | NIST Level 5 (256-bit) |
| **Sub-CA** | ML-DSA-87 | NIST Level 5 |
| **Provincial CA** | ML-DSA-65 | NIST Level 3 (192-bit) |
| **District RA** | ML-DSA-44 | NIST Level 2 (128-bit) |
| **User Signing** | ML-DSA-44 | NIST Level 2 |
| **mTLS Certs** | ML-DSA-65 | NIST Level 3 |

### Encryption at Rest: Kyber + AES-256-GCM (FIPS 203)

```
Document → AES-256-GCM(DEK) → Encrypted File
               DEK → Kyber768(User Key) → Encapsulation
```

| Component | Algorithm | Standard |
|-----------|-----------|----------|
| Key Encapsulation | Kyber768 | FIPS 203 (ML-KEM) |
| Symmetric Encryption | AES-256-GCM | FIPS 197 |
| Authentication Tag | 128-bit | NIST SP 800-38D |

---

## 🏛️ PKI Hierarchy (Decree 23/2025)

```
┌─────────────────────────────────────────────────────────┐
│                    FULL ML-DSA CHAIN                    │
│                                                          │
│  National Root (NEAC, External) ──────────────────────┐ │
│  ML-DSA-87 (offline)                                  │ │
│                                                        │ │
│  ┌─────────────────────────────────────────────────┐ │ │
│  │ Ministry Sub-CA (ML-DSA-87)                     │ │ │
│  │    ├── Provincial CA (ML-DSA-65)               │ │ │
│  │    │      └── District RA (ML-DSA-44)          │ │ │
│  │    │             └── User Cert (ML-DSA-44) ────┴─┴─┘
│  │    └── Org CA (ML-DSA-65)                       │    
│  │           └── Employee Cert (ML-DSA-44)         │    
│  └─────────────────────────────────────────────────┘    
└─────────────────────────────────────────────────────────┘
```

---

## 👥 RBAC + ABAC Model

### Role Hierarchy

| Role | Level | Scope | Key Permissions |
|------|:-----:|-------|-----------------|
| POLICY_OFFICER | 0 | National | MANAGE_CA, ASSIGN_OFFICER |
| ISSUING_OFFICER | 1 | Provincial | MANAGE_RA, ISSUE_CERT |
| RA_OFFICER | 2 | District/Org | ISSUE_CERT, VERIFY_IDENTITY |
| CITIZEN | - | Self | Request own certs |

### ABAC Constraints

| Attribute | Constraint |
|-----------|------------|
| `user.raId` | RA scope |
| `user.orgId` | Organization scope |
| `user.province` | Geographic scope |

---

## 🏗️ Service Architecture

### Active Services ✅

| Service | Port | Purpose |
|---------|------|---------|
| api-gateway | 8080 | Request routing, rate limiting |
| identity-service | 8081 | Authentication, KYC |
| ca-authority | 8082 | PKI, certificate issuance |
| cloud-sign | 8084 | Remote signing (CSC API) |
| validation-service | 8085 | Signature verification |
| doc-service | 8086 | Document storage with encryption |

### Removed Services ❌

| Service | Reason |
|---------|--------|
| org-service | Scope reduction |
| signature-core | Merged into cloud-sign |
| tsa-mock | Using external TSA |

---

## 📋 Development Rules

### DO ✅

1. **Use ML-DSA-65** for all new signing operations
2. **Use Kyber768** for key encapsulation
3. **Use PqcCryptoService** for crypto operations
4. **Add deprecation annotations** when removing features
5. **Run `./gradlew build -x test`** before committing
6. **Add TOTP** for signing and CA operations
7. **Enforce ABAC** for scope-based access control

### DON'T ❌

1. ❌ Create new ECDSA keys for signing
2. ❌ Use HybridSigningService (deprecated)
3. ❌ Use StandardCryptoService (deprecated)
4. ❌ Store unencrypted documents for private data
5. ❌ Reference deleted services (org-service, signature-core)
6. ❌ Allow same person to request and approve CA

---

## 📜 Standards Compliance

| Standard | Status |
|----------|:------:|
| **FIPS 204** (ML-DSA) | ✅ |
| **FIPS 203** (ML-KEM) | ✅ |
| **RFC 5280** (X.509) | ✅ |
| **RFC 3161** (TSA) | ✅ |
| **CSC API v2.0** | ✅ |
| **Vietnam Decree 23/2025** | ✅ |

---

## 🔗 Key Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Bouncy Castle | 1.83 | PQC algorithms |
| Spring Boot | 3.4.x | Framework |
| PostgreSQL | 16+ | Database |
| Redis | 7+ | Session storage |

---

## 📁 Key File Locations

### Cryptography
- `libs/common-crypto/` - Shared crypto services
  - `PqcCryptoService.java` - ML-DSA signing ✅
  - `MlKemEncryptionService.java` - Kyber encryption ✅
  - ~~`StandardCryptoService.java`~~ - Deprecated
  - ~~`HybridSigningService.java`~~ - Deprecated

### Document Storage
- `core/doc-service/`
  - `Document.java` - Entity with encryption fields
  - `FileStorageService.java` - Encrypted file storage

---

## 📚 Reference Documents

See detailed design in:
- [pki_goals_principles.md](file:///home/hoang/.gemini/antigravity/brain/d28267f7-3058-49f8-a958-c77d62d2cfd9/pki_goals_principles.md)
- [process_goals.md](file:///home/hoang/.gemini/antigravity/brain/d28267f7-3058-49f8-a958-c77d62d2cfd9/process_goals.md)
- [bottom_up_redesign.md](file:///home/hoang/.gemini/antigravity/brain/d28267f7-3058-49f8-a958-c77d62d2cfd9/bottom_up_redesign.md)

---

## 🚀 Deployment Checklist

Before deploying:
- [ ] All services build: `./gradlew build -x test`
- [ ] K8s manifests valid: `kubectl apply --dry-run=client -k infra/k8s/base`
- [ ] mTLS certs use ML-DSA-65
- [ ] No deprecated service references
