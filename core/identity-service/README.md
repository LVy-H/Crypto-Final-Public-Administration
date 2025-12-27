# Identity Service

## Overview
The **Identity Service** is the backbone of user management and authentication. It handles user registration, secure login (issuing JWTs), and a robust identity verification workflow. It ensures that only verified users can access sensitive cryptographic operations.

## Features
- **User Authentication**: Secure registration and login with BCrypt password hashing.
- **JWT Issuance**: Generates signed JWTs containing user roles (`CITIZEN`, `ADMIN`) and identity status (`UNVERIFIED`, `PENDING`, `VERIFIED`).
- **Identity Verification Workflow**:
    - Users request verification.
    - Admins review and approve/reject requests.
    - Generates **Identity Assertion** (a signed JWT) upon verification.

## API Reference

### Authentication
- **Register**
    - `POST /api/v1/auth/register`
    - Body: `{"username": "...", "password": "...", "email": "..."}`
    - response: `{"message": "User registered successfully"}`
- **Login**
    - `POST /api/v1/auth/login`
    - Body: `{"username": "...", "password": "..."}`
    - Response: `{"token": "<jwt_token>", "user": {...}}`

### Identity Management
- **Request Verification**
    - `POST /api/v1/identity/verify-request`
    - Header: `Authorization: Bearer <token>`
- **Check Status**
    - `GET /api/v1/identity/status`
    - Header: `Authorization: Bearer <token>`

### Admin Operations
- **Approve Verification**
    - `POST /api/v1/identity/approve/{username}`
    - Header: `Authorization: Bearer <admin_token>`
- **List Pending**
    - `GET /api/v1/identity/pending`
    - Header: `Authorization: Bearer <admin_token>`

## Development & Mocking

### Prerequisites
- Java 17+
- PostgreSQL Database (or Docker container)

### Environment Variables
| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | JDBC URL for Postgres (e.g., `jdbc:postgresql://localhost:5432/crypto_main`) |
| `SPRING_DATASOURCE_USERNAME` | DB Username |
| `SPRING_DATASOURCE_PASSWORD` | DB Password |
| `JWT_SECRET` | **CRITICAL**: Secret key for signing JWTs. Must match other services (Cloud Sign). |

### Mocking for Dependent Services
If you are developing *other* services that depend on Identity Service (like Cloud Sign), you can mock this service by helping yourself generate valid JWTs.
- **Mock JWT format**:
  ```json
  {
    "sub": "username",
    "role": "CITIZEN",
    "identity_status": "VERIFIED",
    "exp": <future_timestamp>
  }
  ```
- Use the same `JWT_SECRET` locally to sign this token so the dependent service accepts it.

### Running Locally
1. Start Postgres: `docker-compose up postgres-db -d`
2. Run Service: `./gradlew :services:identity-service:bootRun`
