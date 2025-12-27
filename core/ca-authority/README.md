# CA Authority Service

## Overview
The **CA Authority Service** manages the Public Key Infrastructure (PKI) for the system. It implements a recursive Certificate Authority hierarchy and acts as the Registration Authority (RA). It handles the lifecycle of certificates: initialization, issuance, and revocation.

## Features
- **Recursive CA Hierarchy**: Support for Root CAs, Intermediate CAs, and Issuing CAs with unlimited depth.
- **Registration Authority (RA)**: Interfaces for users to request certificates (requires identity verification).
- **Certificate Lifecycle**:
    - **Issue**: Signs CSRs to produce certificates.
    - **Revoke**: Marks certificates or entire CA chains as revoked.
- **External RA Support**: Allows third-party entities to register and act as RAs.
- **Algorithm Agnostic**: Supports PQC algorithms (ML-DSA-65, ML-DSA-87) via generic abstraction.

## API Reference

### CA Lifecycle Management
- **Initialize Root CA**
    - `POST /api/v1/ca/root/init`
    - Body: `{"name": "National Root CA"}`
- **Create Subordinate CA**
    - `POST /api/v1/ca/{parentId}/subordinate`
    - Body: `{"name": "Gov CA", "type": "ISSUING_CA"}`
- **List All CAs**
    - `GET /api/v1/ca/all`

### Certificate Issuance (RA)
- **User Registration (Request Certificate)**
    - `POST /api/v1/ra/request`
    - **Security**: Requires bearer token with `identity_status: VERIFIED`.
    - Body: `{"username": "...", "csr": "..."}` or auto-generated logic.

### Revocation
- **Revoke Certificate**
    - `POST /api/v1/ca/revoke/{certId}`
- **Revoke CA Chain**
    - `POST /api/v1/ca/revoke-ca/{caId}`

## Development & Mocking

### Prerequisites
- Java 17+
- PostgreSQL
- **Bouncy Castle / PQC Provider**: Ensure the JVM has the necessary security providers enabled (handled by the app).

### Environment Variables
| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | Postgres Connection URL. |
| `SPRING_DATASOURCE_USERNAME` | DB Username. |
| `SPRING_DATASOURCE_PASSWORD` | DB Password. |

### Mocking Dependencies
- **Identity Service**: When calling `POST /api/v1/ra/request`, you need a valid JWT.
    - **Mocking**: Generate a JWT locally with `identity_status: VERIFIED` using the configured `JWT_SECRET`.
- **Cloud Sign**: The RA service calls `cloud-sign` service to generate keys/CSRs for users during registration (if not provided).
    - **Mocking**: You may need to mock responses from `http://cloud-sign:8084` if you are running integration tests.

### Running Locally
```bash
./gradlew :services:ca-authority:bootRun
```
Access Swagger UI (if enabled) or API at `http://localhost:8082`.
