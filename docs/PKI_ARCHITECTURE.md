# PKI Architecture - Certificate Authority Hierarchy

## 1. Overview
This Public Key Infrastructure (PKI) implements a **3-tier CA hierarchy** designed for **Post-Quantum Security**.

```
┌─────────────────────────────────────────────────────────────┐
│                      ROOT CA (Offline)                       │
│                                                             │
│  • Air-gapped hardware, manual key ceremony                 │
│  • Algorithm: ML-DSA-87 (NIST Level 5)                      │
│  • Validity: 20 years                                       │
│  • Tool: backend/offline-ca-cli                             │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   INTERMEDIATE CA (Online)                   │
│                                                             │
│  • Hosted in pki-service (K8s)                              │
│  • Private key in HSM (Simulated via SoftHSM)               │
│  • Algorithm: ML-DSA-65 (NIST Level 3)                      │
│  • Validity: 5 years                                        │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 END-ENTITY CERTIFICATES                      │
│                                                             │
│  • Derived in Client Browser (IndexedDB)                    │
│  • Validity: 1-3 years                                      │
│  • Algorithm: ML-DSA-44 or ML-DSA-65                        │
└─────────────────────────────────────────────────────────────┘
```

## 2. Component Responsibilities

### Root CA (`backend/offline-ca-cli`)
- **State**: Strictly Offline.
- **Storage**: Private Key Sharded (M-of-N).
- **Function**: Signs Intermediate CA CSRs.

### Intermediate CA (`backend/pki-service`)
- **State**: Online (Zone B).
- **Storage**: HSM / Kubernetes Secret.
- **Function**: Automates issuance for Citizens.

### End-User Keys (`apps/public-portal`)
- **State**: Client-side only.
- **Storage**: IndexedDB (Encrypted).
- **Function**: Generates Detached PQC Signatures.

## 3. Signing Format: ASiC-E
Detailed in **[ASiC Profile](specs/ASIC_PROFILE.md)**.
- We do **NOT** use PAdES (PDF Signatures) due to lack of standard PQC support.
- We use **ASiC-E** (Zip container) wrapping the Document + PQC CMS.

## 4. Key Lifecycle
1.  **Generation**: Client-side (WASM).
2.  **Usage**: User decrypts with Passphrase.
3.  **Rotation**: Keys rotate every 1-3 years.
4.  **Revocation**: OCSP Responder (in `pki-service`).
