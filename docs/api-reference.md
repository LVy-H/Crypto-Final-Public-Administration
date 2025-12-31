# API Reference

Base URL: `https://api.gov-id.lvh.id.vn/api/v1`

## Authentication

Session-based authentication via Redis. All endpoints except `/auth/*` require valid session.

---

## Auth Service (`/api/v1/auth`)

### POST /auth/login
Authenticate and create session.

```json
// Request
{ "username": "user@example.com", "password": "password123" }

// Response
{ "message": "Login successful", "sessionId": "abc123", "username": "user@example.com" }
```

### POST /auth/register
Register new citizen account.

```json
// Request
{ "username": "newuser", "password": "securepass", "email": "user@example.com" }

// Response
{ "message": "User added to system", "username": "newuser" }
```

### POST /auth/logout
Invalidate current session.

```json
// Response
{ "message": "Logged out successfully" }
```

---

## Identity Service (`/api/v1/identity`)

### POST /identity/verify-request
Request identity verification.

```json
// Response
{ "message": "Verification request submitted successfully", "status": "PENDING" }
```

### GET /identity/status
Get current identity verification status.

```json
// Response
{ "username": "user@example.com", "status": "VERIFIED" }
```

### POST /identity/approve/{username}
*Admin only.* Approve user identity verification.

```json
// Response
{ "message": "User verified successfully", "username": "user", "status": "VERIFIED" }
```

---

## Signing Service (`/api/v1/sign`)

### POST /sign/generate-key
Generate new ML-DSA key pair.

```json
// Request
{ "userId": "user123", "keyAlias": "my-key", "algorithm": "ML-DSA-44" }

// Response
{ "alias": "my-key", "algorithm": "ML-DSA-44", "publicKeyBase64": "..." }
```

### POST /sign/remote
Sign document with stored key.

```json
// Request
{ "userId": "user123", "keyAlias": "my-key", "dataBase64": "SGVsbG8gV29ybGQ=" }

// Response
{ "signatureBase64": "...", "algorithm": "ML-DSA-44" }
```

---

## Validation Service (`/api/v1/validation`)

### POST /validation/verify-document
Verify document signature. **Multipart form-data.**

| Field | Type | Description |
|-------|------|-------------|
| document | File | Original document |
| signature | String | Base64-encoded signature |

```json
// Response
{
  "valid": true,
  "signer": "CN=Test User, O=Organization",
  "timestamp": "2025-01-01T12:00:00Z",
  "algorithm": "ML-DSA-44",
  "certificateChain": [
    { "subject": "CN=Test User", "issuer": "CN=Provincial CA", "validity": "2025-2026" }
  ],
  "tsaCertificate": {
    "subject": "CN=TSA", "issuer": "CN=Root CA", "validity": "2024-2029"
  }
}
```

---

## CA Authority (`/api/v1/ca`)

### GET /ca/list
List all Certificate Authorities.

```json
// Response
[
  { "id": 1, "name": "Root CA", "level": "ROOT", "status": "ACTIVE" },
  { "id": 2, "name": "Provincial CA", "level": "INTERMEDIATE", "parentId": 1 }
]
```

### POST /ca/init-root
Initialize root CA. *Admin only.*

```json
// Request
{ "name": "Root CA", "algorithm": "ML-DSA-87" }

// Response
{ "id": 1, "name": "Root CA", "certificate": "-----BEGIN CERTIFICATE-----..." }
```

### GET /ca/{id}/chain
Get certificate chain for CA.

```json
// Response
{ "chain": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----" }
```

### GET /ca/{id}/crl
Get Certificate Revocation List for CA.

```json
// Response
{ "crl": "-----BEGIN X509 CRL-----\n...\n-----END X509 CRL-----" }
```

### POST /ca/{parentId}/create-child
Create subordinate CA.

```json
// Request
{ "name": "Provincial CA", "algorithm": "ML-DSA-65" }

// Response
{ "id": 2, "name": "Provincial CA", "certificate": "..." }
```

---

## Registration Service (`/api/v1/registration`)

### POST /registration/request
Request user certificate.

```json
// Request
{ "userId": "user123", "subjectDn": "CN=John Doe,O=Gov,C=VN" }

// Response
{ "requestId": "req-123", "status": "PENDING" }
```

### POST /registration/approve/{requestId}
*Admin only.* Approve certificate request.

```json
// Response
{ "certificate": "-----BEGIN CERTIFICATE-----...", "status": "ISSUED" }
```

---

## Algorithm Reference

| FIPS 204 | Bouncy Castle | Security Level | Use Case |
|----------|---------------|----------------|----------|
| ML-DSA-44 | Dilithium2 | NIST Level 2 | User certificates |
| ML-DSA-65 | Dilithium3 | NIST Level 3 | Provincial CAs |
| ML-DSA-87 | Dilithium5 | NIST Level 5 | Root CA |

---

## Error Responses

```json
{ "timestamp": "2025-01-01T12:00:00Z", "status": 400, "error": "Bad Request", "message": "Details" }
```

| Code | Description |
|------|-------------|
| 400 | Invalid request body |
| 401 | Not authenticated |
| 403 | Insufficient permissions |
| 404 | Resource not found |
| 500 | Internal server error |
