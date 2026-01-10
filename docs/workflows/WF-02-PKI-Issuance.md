# WF-02: PKI Issuance & Lifecycle

**Goal**: Issue `ML-DSA` certificates to verified entities while maintaining Root CA isolation.

## 1. Actors
- **Root CA Operator**: Human(s) in secure room.
- **Offline CA Tool**: `backend/offline-ca-cli`.
- **Intermediate CA**: `backend/pki-service`.
- **User Browser**: `apps/public-portal` (PQC WASM).

## 2. Process Flow

### Phase A: Root CA Initialization (Offline Ceremony)
*Trigger: System Setup (Year 0)*
1.  **Secure Room**: Disconnect all networks.
2.  **Run Tool**: `offline-ca-cli init-root --algo ML-DSA-87`.
3.  **Output**: `root.key` (Encrypted/Sharded), `root.crt` (Self-signed).
4.  **Distribution**: Copy `root.crt` to `infra/pki/truststore`.

### Phase B: Intermediate CA Enrollment
1.  **Intermediate CA** generates KeyPair (`ML-DSA-65`) in HSM.
    - *API*: `POST /api/v1/ca/init-csr`
2.  **Output**: `intermediate.csr`.
3.  **Transfer**: Operator copies `intermediate.csr` to USB.
4.  **Sign (Offline)**: User runs `offline-ca-cli sign-csr --in intermediate.csr`.
5.  **Output**: `intermediate.crt`.
6.  **Install**: Upload `intermediate.crt` back to `pki-service`.
    - *API*: `POST /api/v1/ca/upload-cert`

### Phase C: End-User Enrollment (Client-Side)
*Trigger: User clicks "Get Certificate"*
1.  **Browser** generates KeyPair (`ML-DSA-44/65`) in `IndexedDB`.
    - Private Key is **never** exported.
2.  **Browser** generates CSR (PKCS#10) signed by local Private Key.
3.  **Browser** sends CSR to `pki-service`.
    - *API*: `POST /api/v1/pki/enroll`
    - *Auth*: Requires `Bearer Token` (Verified Status).
4.  **PKI Service** validates:
    - Signature on CSR (Proof of Possession).
    - User Identity (from Token).
5.  **PKI Service** signs CSR using Intermediate CA Key.
6.  **Return**: X.509 Certificate (`user.crt`) returned to browser.
7.  **Storage**: Browser stores `user.crt` in `IndexedDB`.

## 3. Cryptographic Standards
- **Root**: ML-DSA-87 (NIST Level 5) - The Trust Anchor.
- **Intermediate**: ML-DSA-65 (NIST Level 3).
- **User**: ML-DSA-44 or ML-DSA-65 (NIST Level 2/3).
