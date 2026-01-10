# WF-03: Pure PQC Document Signing

**Goal**: Sign a PDF using a client-side PQC key, strictly ensuring "Sole Control" (Article 20).

## 1. Actors
- **User**: Verified Citizen with Cert.
- **Browser**: Client logic (`pqc.ts`).
- **Document Service**: `backend/document-service`.
- **TSA Service**: `backend/tsa-service`.

## 2. Process Flow

### Phase A: Preparation
1.  **User** uploads PDF to Portal.
2.  **Document Service** stores PDF and returns `docId`.
    - *API*: `POST /api/v1/documents/upload`
3.  **Browser** downloads PDF (or header bytes) for hashing.
4.  **Browser** calculates `SHA-384` hash of the document.

### Phase B: Sole Control Authorization
1.  **UI** prompts user: "Enter Passphrase to Sign".
2.  **User** enters Passphrase.
3.  **Browser** retrieves encrypted Private Key from `IndexedDB`.
4.  **Browser** decrypts Private Key in memory (AES-GCM).
    - *Security*: If passphrase is wrong, signing fails locally. Server never sees key/passphrase.

### Phase C: Signature Generation (WASM)
1.  **Browser** calls `liboqs.sign(hash, privateKey, algo)`.
    - *Algorithm*: `ML-DSA-44` (or configured algo).
2.  **Output**: Raw Signature bytes (~2.4KB).

### Phase D: Formatting & Timestamping (LTV)
1.  **Browser** sends `{ docId, signature, certificate }` to Backend.
    - *API*: `POST /api/v1/documents/finalize`
2.  **Document Service** verifies:
    - Signature matches Hash + Public Key.
    - Certificate is valid (not revoked).
3.  **Document Service** calls **TSA Service** to get a Timestamp Token (RFC 3161).
    - *Purpose*: Prove signature existed at time T (Long Term Validation).
4.  **Document Service** embeds:
    - PQC Signature
    - X.509 Certificate
    - TSA Token
    - Validation Chain
    ...into the PDF (PAdES / CMS).

### Phase E: Verification
1.  **Recipient** opens PDF in Portal or Adobe Reader (future).
2.  **Verifier** checks:
    - Hash integrity.
    - Signature validity (using Public Key).
    - Timestamp validity.

## 3. Technical Constraints
- **Signature Size**: ML-DSA sigs are large. We reserve ~10KB in the CMS container.
- **Latency**: Validation involves multiple checks.
