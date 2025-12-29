# API Reference

## Base URL
```
https://api.gov-id.lvh.id.vn/api/v1
```

## Authentication
All endpoints use session cookies. Login via `/identity/login`.

---

## Sign Service

### POST /sign/remote
Sign document data with stored key.

**Request:**
```json
{
  "userId": "string | null",
  "keyAlias": "default",
  "dataBase64": "base64-encoded-document"
}
```

**Response:**
```json
{
  "signatureBase64": "base64-signature",
  "algorithm": "ML-DSA-44"
}
```

### POST /sign/generate-key
Generate new key pair for user.

**Request:**
```json
{
  "userId": "user123",
  "keyAlias": "my-signing-key",
  "algorithm": "ML-DSA-44"
}
```

**Response:**
```json
{
  "alias": "my-signing-key",
  "algorithm": "ML-DSA-44",
  "publicKeyBase64": "base64-public-key"
}
```

---

## Validation Service

### POST /validation/verify-document
Verify document signature (multipart).

**Request:** `multipart/form-data`
- `document`: File (the original document)
- `signature`: String (base64 signature)

**Response:**
```json
{
  "valid": true,
  "signer": "CN=Test User, O=Organization",
  "timestamp": "2025-01-01T12:00:00Z",
  "algorithm": "ML-DSA-44",
  "certificateChain": [
    { "subject": "...", "issuer": "...", "validity": "..." }
  ],
  "tsaCertificate": {
    "subject": "...", "issuer": "...", "validity": "..."
  }
}
```

---

## Algorithm Reference

| FIPS 204 Name | Bouncy Castle | Security Level |
|---------------|---------------|----------------|
| ML-DSA-44 | Dilithium2 | NIST Level 2 (128-bit) |
| ML-DSA-65 | Dilithium3 | NIST Level 3 (192-bit) |
| ML-DSA-87 | Dilithium5 | NIST Level 5 (256-bit) |
