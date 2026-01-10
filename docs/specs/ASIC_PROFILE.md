# GovTech ASiC-E PQC Profile Specification

## 1. Overview
This profile defines the **Associated Signature Container - Extended (ASiC-E)** format used for the GovTech PQC Digital Signature System. It follows **ETSI EN 319 162-1** standards but is adapted to support Post-Quantum Cryptography (ML-DSA / SLH-DSA) which are not yet fully standardized in CAdES.

## 2. Container Structure
The container MUST be a ZIP file (interoperable with PKZip) with the following structure:

```text
file.asic
├── mimetype                   (MUST be first, no compression)
├── META-INF/
│   └── signature.p7s          (The Detached CMS Signature)
│   └── manifest.xml           (Optional: Lists signed files)
└── document.pdf               (The original content)
```

### 2.1. Mimetype File
- **Path**: `mimetype` (Root)
- **Content**: `application/vnd.etsi.asic-e+zip`
- **Compression**: `STORE` (0). It must be uncompressed to allow magic-number detection.

### 2.2. Signature File
- **Path**: `META-INF/signature.p7s`
- **Format**: Cryptographic Message Syntax (CMS) as defined in RFC 5652.
- **Content-Type**: `application/pkcs7-signature`.

## 3. CMS Profile (PQC Adaptation)
Since standard CAdES does not yet fully support ML-DSA OIDs, we define a "Generic PQC CMS" profile.

### 3.1. SignedData
- **Version**: 3
- **DigestAlgorithm**: `id-sha384` (2.16.840.1.101.3.4.2.2).
- **EncapsulatedContentInfo**:
    - **ContentType**: `id-data` (1.2.840.113549.1.7.1).
    - **Content**: ABSENT (Detached Signature).

### 3.2. SignerInfo
- **SignatureAlgorithm**:
    - **ML-DSA-44**: `1.3.6.1.4.1.2.267.12.4.4` (OpenQuantumSafe OID / Draft).
    - **ML-DSA-65**: `1.3.6.1.4.1.2.267.12.6.5`.
    - **SLH-DSA-SHAKE-128F**: `1.3.9999.6.7` (Placeholder/Draft OID).
- **SignedAttributes**:
    - `contentType` (MUST be `id-data`).
    - `messageDigest` (SHA-384 of `document.pdf`).
    - `signingTime` (UTC time of client signing).

### 3.3. UnsignedAttributes (LTV)
- **signatureTimeStampToken**: A TimeStampToken (RFC 3161) from the GovTech TSA.
    - Ensures the signature existed before algorithm deprecation.

## 4. Verification Logic
1.  **Unzip** the `.asic` container.
2.  **Verify Structure**: Check for `mimetype` and `META-INF/signature.p7s`.
3.  **Identify Content**: Find the non-META-INF file (e.g., `document.pdf`).
4.  **Hash Content**: Calculate `SHA-384(document.pdf)`.
5.  **Verify CMS**:
    - Extract `SignedAttributes` digest.
    - Verification: `CMS_Verify(signature.p7s, detached_content=document.pdf)`.
    - **Math Check**: Use BouncyCastle PQC provider to verify ML-DSA signature.
6.  **Verify Trust**: Check `SignerCert` chains to `Government Root CA`.

## 5. Security Considerations
- **Zip Bombs**: The backend verification service MUST enforce a max-uncompressed-size limit (e.g., 100MB).
- **Mimetype Spoofing**: The validator MUST rely on the internal `mimetype` file, not the file extension.
