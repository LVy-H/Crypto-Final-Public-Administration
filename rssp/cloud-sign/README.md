# Cloud Signing Service (CSC)

## Overview
The **Cloud Signing Service** enables remote digital signatures. It implements a subset of the Cloud Signature Consortium (CSC) API standard. It securely manages user keys and performs signing operations on their behalf, strictly controlled by authorization tokens (SAD).

## Features
- **CSC Compliance**: Implements standard endpoints (`/csc/v1/sign`, etc.).
- **Secure Key Storage**: Manages keys (currently software-based, extensible to HSM).
- **Signature Activation**: Requires **Signature Activation Data (SAD)** (a high-privilege JWT) to authorize every signing operation.
- **Identity Enforcement**: Validates that the requestor has a `VERIFIED` identity status before allowing access.

## API Reference

### Key Management
- **Generate Key Pair**
    - `POST /csc/v1/keys/generate`
    - Header: `Authorization: Bearer <sad_token>`
    - Body: `{"alias": "user_key_1", "algorithm": "mldsa65"}`
- **Generate CSR**
    - `POST /csc/v1/keys/csr`
    - Header: `Authorization: Bearer <sad_token>`
    - Body: `{"alias": "user_key_1", "subject": "CN=User Name"}`

### Signing
- **Sign Hash**
    - `POST /csc/v1/sign`
    - Header: `Authorization: Bearer <sad_token>`
    - Body: `{"keyAlias": "user_key_1", "dataHashBase64": "<base64_hash>", "algorithm": "mldsa65"}`
    - Response: `{"signatureBase64": "..."}`

## Development & Mocking

### Prerequisites
- Java 17+
- **OpenSSL** (optional, if used for underlying crypto).

### Environment Variables
| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | **Must match Identity Service**. Used to validate the SAD token. |

### Mocking Dependencies
- **Identity Service (SAD Token)**: This service *only* validates tokens; it doesn't call Identity Service directly.
    - **How to Mock**: To test this service independently, you do **not** need the Identity Service running. You only need to generate a self-signed JWT using the same `JWT_SECRET` that `cloud-sign` is configured with.
    - **Token Requirements**:
        - `sub`: username
        - `identity_status`: `VERIFIED`
        - `exp`: Future timestamp
- **Validation**:
    - Ensure your mock token has the correct `identity_status` or the `SadValidator` will reject it.

### Running Locally
```bash
./gradlew :services:cloud-sign:bootRun
```
Service runs on port `8084` (internal) or via Gateway.
