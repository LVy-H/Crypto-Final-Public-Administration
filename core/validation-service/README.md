# Validation Service

## Overview
The **Validation Service** acts as a trusted third party to verify digital signatures. It checks not just the mathematical validity of the signature but also the trust chain of the signing certificate ensuring it is issued by a valid CA in the system hierarchy.

## Features
- **Signature Verification**: Verifies signatures for supported algorithms (ML-DSA, etc.).
- **Chain Validation**: (Planned) Verifies the certificate path up to the Trusted Root.
- **Format Agnostic**: Validates raw signatures against data hashes.

## API Reference

### Verification
- **Verify Signature**
    - `POST /api/v1/validation/verify`
    - Body:
      ```json
      {
        "dataBase64": "...", // Original data or hash
        "signatureBase64": "...",
        "publicKeyPem": "..." // OR Certificate
      }
      ```
    - Response: `{"valid": true, "message": "Signature is valid"}`

## Development & Mocking

### Independency
This service is highly independent. It does not require user authentication (Identity Service) or access to the signing keys (Cloud Sign). It is a pure logic service.

### Mocking
- **Input Data**: You can generate test vectors (Key pair, Data, Signature) using specific tools (like Python scripts or OpenSSL) and feed them into this API to verify correctness.
- **Dependencies**: It interacts with the Database if it needs to check for revoked certificates (CRL/OCSP logic).
    - **Mock DB**: Use h2 or a local Postgres instance.

### Running Locally
```bash
./gradlew :services:validation-service:bootRun
```
Service runs on port `8083` (internal).
