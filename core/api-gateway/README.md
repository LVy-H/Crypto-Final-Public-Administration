# API Gateway Service

## Overview
The **API Gateway** serves as the single entry point for the Crypto Public Administration system. It handles request routing, load balancing, and strictly enforces network boundaries. All external traffic must pass through this gateway to reach backend microservices.

## Features
- **Centralized Routing**: dynamically routes requests to `identity-service`, `ca-authority`, `cloud-sign`, and `validation-service`.
- **Protocol Translation**: Handles incoming HTTP/HTTPS requests.
- **Service Isolation**: Hides internal microservice architecture from the public internet.

## Configuration & Development

### Prerequisites
- Java 17+
- Docker & Docker Compose (optional for full stack run)

### Environment Variables
| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Port the gateway listens on. |
| `SSL_ENABLED` | `false` | Access toggle for mTLS (future use). |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile. |

### Running Locally
```bash
./gradlew :services:api-gateway:bootRun
```
The service will start on `http://localhost:8080`.

## Routing Table
| Path | Target Service | Description |
|------|----------------|-------------|
| `/api/v1/auth/**` | `identity-service` | Authentication & Login |
| `/api/v1/identity/**` | `identity-service` | Identity Verification |
| `/api/v1/ra/**` | `ca-authority` | Registration Authority |
| `/api/v1/ca/**` | `ca-authority` | Certificate Management |
| `/csc/v1/**` | `cloud-sign` | Cloud Signing (CSC API) |
| `/api/v1/validation/**` | `validation-service` | Signature Validation |

## Testing & Mocking
To test the gateway independently:
1. **Mock Internal Services**: The gateway expects services to be resolvable explicitly (e.g., `http://identity-service:8081`).
2. **Local Overrides**: In `application.yml`, you can override route URIs to point to local mock servers (e.g., `http://localhost:9001`) for isolated testing.
