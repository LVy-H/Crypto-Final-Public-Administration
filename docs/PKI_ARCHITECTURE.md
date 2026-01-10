# PKI Architecture - Certificate Authority Hierarchy

## Overview

This Public Key Infrastructure (PKI) implements a **3-tier CA hierarchy** with strict separation between offline and online components for maximum security.

```
┌─────────────────────────────────────────────────────────────┐
│                      ROOT CA (Offline)                       │
│                                                             │
│  • Air-gapped hardware, manual key ceremony                 │
│  • Algorithm: ML-DSA-87 or SLH-DSA-SHAKE-128F               │
│  • Validity: 20 years                                       │
│  • Tool: tools/offline-ca/OfflineCaTool                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Signs Intermediate CA CSR
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   INTERMEDIATE CA (Online)                   │
│                                                             │
│  • Hosted in pki-service (K8s)                              │
│  • Private key in HSM/Kubernetes Secret                     │
│  • Algorithm: ML-DSA-65                                     │
│  • Validity: 5 years                                        │
│  • Issues end-entity certificates automatically             │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Signs End-User CSRs
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 END-ENTITY CERTIFICATES                      │
│                                                             │
│  • Generated client-side in browser (pqc.ts)                │
│  • Private key stored in IndexedDB (encrypted)              │
│  • Algorithm: User choice (ML-DSA-44/65/87 or SLH-DSA)      │
│  • Validity: 1-3 years                                      │
│  • Used for: Document signing, authentication               │
└─────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

### 1. Root CA (Offline) - `tools/offline-ca/`

**Purpose**: Trust anchor for the entire PKI. NEVER connected to network.

**Key Operations**:
```bash
# Initialize Root CA (one-time ceremony)
./gradlew :offline-ca-cli:run --args="init-root \
    --name 'CN=Vietnam National Root CA, O=Government, C=VN' \
    --algo ML_DSA_87 \
    --days 7300 \
    --out-dir /secure/root-ca"

# Sign Intermediate CA CSR
./gradlew :offline-ca-cli:run --args="sign-csr \
    --csr /path/to/intermediate.csr \
    --key /secure/root-ca/root.key \
    --cert /secure/root-ca/root.crt \
    --out /path/to/intermediate.crt \
    --days 1825"
```

**Security Requirements**:
- Air-gapped machine (no network interfaces)
- Hardware security module (HSM) for key storage
- Multi-person control (M-of-N key ceremony)
- Physical security (vault/secure room)
- Audit logging of all operations

### 2. Intermediate CA (Online) - `backend/pki-service/`

**Purpose**: Issue end-entity certificates to users. Connects to network.

**Key Files**:
- `CsrService.kt` - Receives and validates CSRs
- `CertificateService.kt` - Issues certificates (when implemented)

**Flow**:
1. User submits CSR via `/api/pki/enroll`
2. CsrService validates Proof of Possession (POP)
3. CSR queued for operator approval (or auto-signed)
4. Certificate returned to user

### 3. End-User Keys (Browser) - `mock-ui/src/services/pqc.ts`

**Purpose**: Client-side key generation. Private keys NEVER leave browser.

**Supported Algorithms**:
| Algorithm | Type | Security Level | Use Case |
|-----------|------|---------------|----------|
| ML-DSA-44 | Lattice | NIST Level 2 | Lightweight devices |
| ML-DSA-65 | Lattice | NIST Level 3 | General purpose |
| ML-DSA-87 | Lattice | NIST Level 5 | High security |
| SLH-DSA-SHAKE-128F | Hash-based | NIST Level 1 | Conservative choice |

**Key Storage**:
- `IndexedDB` with AES-GCM encryption
- Passphrase-derived key (PBKDF2)
- Non-extractable from browser

## Certificate Issuance Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  User    │     │ Browser  │     │ PKI Svc  │     │ Root CA  │
│(Citizen) │     │ (pqc.ts) │     │ (K8s)    │     │(Offline) │
└────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘
     │                │                │                │
     │ 1. Register    │                │                │
     │───────────────>│                │                │
     │                │                │                │
     │                │ 2. Generate    │                │
     │                │    KeyPair     │                │
     │                │ (ML-DSA-65)    │                │
     │                │                │                │
     │                │ 3. Create CSR  │                │
     │                │    + Sign      │                │
     │                │────────────────>                │
     │                │                │                │
     │                │                │ 4. Validate    │
     │                │                │    POP         │
     │                │                │                │
     │                │                │ 5. Issue Cert  │
     │                │                │ (from Inter CA)│
     │                │<───────────────│                │
     │                │                │                │
     │                │ 6. Store Key   │                │
     │                │    (IndexedDB) │                │
     │<───────────────│                │                │
     │ 7. Success     │                │                │
```

## Security Considerations

1. **Root CA Isolation**: Root CA private key must NEVER be on a networked system
2. **Key Ceremony**: Root CA initialization requires witnessed ceremony with audit trail
3. **Short-Lived Intermediates**: Intermediate CA certs should be renewed every 5 years
4. **CRL/OCSP**: Certificate revocation must be implemented for production
5. **Post-Quantum Ready**: All algorithms are NIST-standardized PQC algorithms
