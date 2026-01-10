# WF-03: Pure PQC Document Signing (ASiC-E)

**Goal**: Sign a document using client-side PQC key, packaging it in an ASiC-E container (Zip) to support PQC algorithms not yet recognized by standard PDF readers.

## 1. Actors
- **User**: Verified Citizen.
- **Browser**: Client logic (WASM PQC).
- **Document Service**: ASiC Containerizer.
- **TSA Service**: RFC 3161 Timestamping.

## 2. Process Flow

### Phase A: Preparation
1.  **User** uploads File (PDF/XML/Doc) to Portal.
2.  **Document Service** calculates `SHA-384` hash of the *original file*.
3.  **Browser** receives the hash.

### Phase B: Sole Control Authorization
1.  **UI** prompts: "Enter Passphrase to Sign".
2.  **Browser** decrypts Private Key (`ML-DSA-65`) from `IndexedDB`.
3.  **Browser** generates **Detached PQC Signature** (Raw Bytes).

### Phase C: Containerization (ASiC-E)
1.  **Browser** sends `{ docId, rawSignature, certificate }` to Backend.
2.  **Document Service** constructs **CMS (Cryptographic Message Syntax)**:
    - Wraps raw signature into a `SignedData` structure (OIDs for ML-DSA).
    - Embeds User Certificate.
3.  **TSA Timestamping**:
    - Backend sends CMS hash to TSA.
    - Receives Timestamp Token (TST).
    - Embeds TST into CMS as an `unsignedAttribute`.
4.  **Packaging**:
    - Backend creates a ZIP file (`.asic` or `.zip`).
    - Adds `mimetype` file (`application/vnd.etsi.asic-e+zip`).
    - Adds `original_doc.pdf`.
    - Adds `META-INF/signature.p7s` (The CMS object).

### Phase D: Storage & Delivery
1.  **System** stores the `.asic` file in MinIO.
2.  **User** downloads the `.asic` package.

### Phase E: Verification
*Since Adobe Reader cannot verify PQC signatures yet:*
1.  **User** uploads `.asic` file to **Verification Portal**.
2.  **Portal**:
    - Unzips container.
    - Extracts `signature.p7s` and `original_doc.pdf`.
    - Validates CMS structure and PQC Math (using Bouncy Castle).
    - Checks Chain of Trust (Root CA > Inter CA > User).
    - Checks Timestamp validity.
3.  **UI**: "✅ Valid PQC Signature from [Citizen Name] at [Time]".

## 3. Technical Constraints
- **Viewer**: Users rely on the Portal to verify (Vendor Lock-in until standards evolve).
- **Size**: ASiC files are roughly `Size(Doc) + Size(Sig)`.
