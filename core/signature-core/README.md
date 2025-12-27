# Signature Core Library

## Overview
**Signature Core** is a shared library and foundation service for the Crypto Public Administration system. It encapsulates the core cryptographic primitives, algorithm implementations (ML-DSA-44/65/87), and shared utility classes used by other services.

## Role in Architecture
Unlike other microservices, `signature-core` is primarily a **dependency** for:
- `cloud-sign` (for signing logic)
- `ca-authority` (for certificate generation)
- `validation-service` (for verification logic)

Although it is containerized in `docker-compose.yml`, it likely serves as a placeholder or a common execution environment for shared logic. In a production environment, its classes are packaged as a JAR and included in the consumer services.

## Development

### Key Packages
- `com.gov.crypto.core`: Core crypto algorithms.
- `com.gov.crypto.model`: Shared data models (if applicable).
- `com.gov.crypto.util`: Encoding/Decoding utilities (Base64, PEM).

### How to Use
To use this in another service, add it as a dependency in `build.gradle.kts`:
```kotlin
implementation(project(":services:signature-core"))
```

### Testing
Since this is a library:
- **Unit Tests**: Run exhaustive unit tests here to ensure crypto correctness.
- **No HTTP API**: It does not expose REST endpoints. Testing should focus on class-level functionality.

### Running
You typically do not "run" this mock service directly. Its build output is consumed by others.
