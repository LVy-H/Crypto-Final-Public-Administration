# GovTech API V1 Specification

## Service Overview
- **Base URL**: `/api/v1`
- **Auth**: Bearer Token (JWT) required for all non-public endpoints.
- **Content-Type**: `application/json` (unless specified otherwise).

## 1. Authentication (`identity-service`)

### POST `/auth/register`
Create a new citizen account.
- **Request**:
  ```json
  { "username": "00123456789", "password": "...", "fullName": "Nguyen Van A" }
  ```
- **Response**: `201 Created`

### POST `/auth/login`
- **Request**: `{ "username": "...", "password": "..." }`
- **Response**:
  ```json
  { "token": "eyJhbG...", "refreshToken": "..." }
  ```

---

## 2. PKI Operations (`pki-service`)

### POST `/pki/enroll`
Request a new ML-DSA certificate.
- **Header**: `Authorization: Bearer <token>`
- **RequestBody**:
  ```json
  {
    "csr": "-----BEGIN CERTIFICATE REQUEST...-----",
    "algo": "ML-DSA-65"
  }
  ```
- **Response**:
  ```json
  {
    "certificate": "-----BEGIN CERTIFICATE...-----",
    "chain": ["-----BEGIN CERTIFICATE...-----"]
  }
  ```

---

## 3. ASiC Signing (`document-service`)

### POST `/documents/upload`
Upload an original document to prepare for signing.
- **Type**: `multipart/form-data`
- **File**: `document.pdf`
- **Response**:
  ```json
  {
    "docId": "550e8400-e29b-41d4-a716-446655440000",
    "hash": "a4b3c2d...", // SHA-384 of the file
    "algo": "SHA-384"
  }
  ```

### POST `/documents/finalize-asic`
Submit a detached PQC signature to create an ASiC container.
- **Request**:
  ```json
  {
    "docId": "550e8400-e29b-41d4-a716-446655440000",
    "signature": "Base64(Raw_ML-DSA_Bytes)",
    "certificate": "Base64(User_Cert_DER)"
  }
  ```
- **Process**:
  1. Backend validates `signature` against `doc.hash` + `certificate`.
  2. Backend wraps `signature` in CMS (`SignedData`).
  3. Backend requests Timestamp from TSA.
  4. Backend zips `doc.pdf` + `signature.p7s` -> `.asic`.
- **Response**:
  ```json
  {
    "asicDownloadUrl": "/api/v1/documents/download/550e8400.../package.asic"
  }
  ```

### POST `/documents/verify-asic`
Verify an uploaded ASiC file.
- **Type**: `multipart/form-data`
- **File**: `package.asic`
- **Response**:
  ```json
  {
    "isValid": true,
    "signer": "Nguyen Van A",
    "timestamp": "2026-01-10T15:00:00Z",
    "algorithm": "ML-DSA-65",
    "originalFileName": "contract.pdf"
  }
  ```
