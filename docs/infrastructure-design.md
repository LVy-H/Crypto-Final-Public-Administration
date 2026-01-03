# Infrastructure & Network Design

Post-Quantum Cryptography Digital Signature System for Public Administration.

## 1. Network Architecture

### 1.1 Network Topology

The system is designed for deployment in a Kubernetes environment (Kind for development, Standard K8s for Production) with a strict separation of concerns using Namespaces and Network Policies.

```mermaid
graph TB
    subgraph "External Network"
        INET[("Internet")]
        USER[("Citizens")]
        ADMIN[("Administrators")]
    end

    subgraph "Edge Layer"
        NGINX["NGINX Ingress Controller<br/>:443/HTTPS<br/>TLS 1.3"]
    end

    subgraph "Kubernetes Cluster"
        subgraph "namespace: crypto-pqc"
            
            subgraph "Presentation Zone"
                PP["Public Portal<br/>(Nuxt 4)<br/>:3000"]
            end

            subgraph "Gateway Zone"
                GW["API Gateway<br/>(Spring Cloud Gateway)<br/>:8080"]
            end
            
            subgraph "Business Logic & Crypto Zone"
                IS["Identity Service<br/>:8081"]
                CA["CA Authority<br/>:8082 / :8083"]
                CS["Cloud Sign<br/>:8084"]
                VS["Validation Service<br/>:8085"]
            end
            
            subgraph "Persistence Zone"
                PG[("PostgreSQL<br/>:5432")]
                REDIS[("Redis<br/>:6379")]
            end
            
            subgraph "Security Hardware Zone"
                HSM["SoftHSM<br/>PKCS#11"]
            end
        end
    end

    USER --> INET
    ADMIN --> INET
    INET --> NGINX
    NGINX --> PP
    PP --> GW
    
    %% Internal Service Mesh Communication
    GW --> IS
    GW --> CA
    GW --> CS
    GW --> VS
    
    %% Data Access
    IS --> PG
    IS --> REDIS
    CA --> PG
    CS --> PG
    IS --> PG
    
    %% HSM Access
    CA --> HSM
    CS --> HSM
```

### 1.2 DNS Configuration

| Domain | Target | Purpose |
|--------|--------|---------|
| `portal.gov-id.lvh.id.vn` | Ingress → Public Portal | Citizen & Admin UI |
| `api.gov-id.lvh.id.vn` | Ingress → API Gateway | Unified REST API Endpoint |

### 1.3 Service Port Mapping

| Service | Container Port | Service Port (ClusterIP) | Protocol | Notes |
|---------|----------------|--------------------------|----------|-------|
| NGINX Ingress | 443 | 443 | HTTPS | TLS 1.3 Termination |
| Public Portal | 3000 | 80 | HTTP | Internal traffic only |
| API Gateway | 8080 | 8080 | HTTP | Central entry point |
| Identity Service | 8081 | 8081 | HTTP | Auth, KYC, User Mgmt |
| CA Authority | 8082 | 8082 | HTTP | PKI Core Operations |
| CA Authority (RA) | 8083 | 8083 | HTTP | Registration Authority Ops |
| Cloud Sign | 8084 | 8084 | HTTP | CSC Signing Operations |
| Validation SVC | 8080 | 8085 | HTTP | Signature Verification |
| PostgreSQL | 5432 | 5432 | TCP | Primary Data Store |

---

## 2. Network Layers (OSI Model)

### 2.1 Layer Stack

```mermaid
graph LR
    subgraph "Layer 7 - Application"
        HTTP["HTTP/1.1 & HTTP/2<br/>REST APIs (JSON)"]
    end

    subgraph "Layer 6 - Presentation"
        TLS["TLS 1.3<br/>Strong Ciphers Only"]
    end

    subgraph "Layer 5 - Session"
        JWT["Stateless Auth<br/>(JWT / X-Auth-Token)"]
        TOTP["Transaction Auth<br/>(Cloud Signing)"]
    end

    subgraph "Layer 4 - Transport"
        TCP["TCP"]
    end

    subgraph "Layer 3 - Network"
        K8S["Kubernetes Pod Network<br/>(CNI: kindnet/calico)"]
    end

    subgraph "Layer 2 - Data Link"
        VETH["Container Virtual Interface"]
    end

    subgraph "Layer 1 - Physical"
        PHY["Host Infrastructure"]
    end

    HTTP --> TLS
    TLS --> JWT
    JWT --> TCP
    TCP --> K8S
    K8S --> VETH
    VETH --> PHY
```

---

## 3. Operating System & Container Design

### 3.1 Container Base Images

We prioritize minimal, secure base images to reduce attack surface.

| Component Type | Base Image | Size (Approx) | Rationale |
|----------------|------------|---------------|-----------|
| **Java Microservices** | `eclipse-temurin:21-jre` | ~250MB | Official Java 21 JRE for Spring Boot 3 & Bouncy Castle 1.83 compatibility |
| **Frontend/Node** | `node:20-slim` | ~180MB | Minimal Node.js 20 environment for Nuxt 4 SSR |
| **Ingress** | `nginx:alpine` | ~25MB | Alpine-based high-performance proxy |
| **Database** | `postgres:15-alpine` | ~80MB | Stable, lightweight Persistence |

### 3.2 Resource Allocation Strategy

Resource limits are tuned for **ML-DSA (Dilithium)** operations, which are computationally intensive and memory-heavy compared to traditional ECDSA/RSA.

| Service | CPU Request | CPU Limit | Memory Request | Memory Limit | Critical Reason |
|---------|-------------|-----------|----------------|--------------|-----------------|
| **CA Authority** | 100m | 1000m | 512Mi | **2Gi** | ML-DSA Key Generation requires significant heap space |
| **Cloud Sign** | 100m | 1000m | 512Mi | **2Gi** | Concurrent ML-DSA Signing operations |
| **Validation SVC** | 50m | 500m | 512Mi | 1Gi | Signature verification is moderately intensive |
| **API Gateway** | 100m | 1000m | 512Mi | 768Mi | Routing & Rate Limiting throughput |
| **Identity SVC** | 100m | 500m | 256Mi | 512Mi | Standard CRUD operations |

### 3.3 Security Hardening

All Pods are configured with strict security contexts to adhere to non-root policies:

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000      # Application user
  fsGroup: 1000        # Filesystem group access
  readOnlyRootFilesystem: true  # Prevents runtime malware persistence
  allowPrivilegeEscalation: false
  capabilities:
    drop: ["ALL"]      # Drop NET_ADMIN, SYS_ADMIN, etc.
```

---

## 4. Application Architecture

### 4.1 Technology Stack & Dependencies

```mermaid
graph TB
    subgraph "Frontend"
        NUXT["Nuxt 4.x"] --> VUE["Vue 3"]
        VUE --> TS["TypeScript"]
        VUE --> TW["TailwindCSS"]
    end

    subgraph "Backend Core"
        SB["Spring Boot 3.4.1"] --> JAVA["Java 21 LTS"]
        SB --> SC["Spring Cloud 2023.x"]
        SB --> SD["Spring Data JPA"]
    end

    subgraph "Cryptography Core"
        BC["Bouncy Castle 1.83<br/>(bcprov-jdk18on)"]
        BCPKIX["Bouncy Castle PKIX 1.83<br/>(bcpkix-jdk18on)"]
        
        JAVA --> BC
        JAVA --> BCPKIX
    end

    subgraph "PQC Implementation"
        MLDSA["ML-DSA (FIPS 204)<br/>Post-Quantum Signatures"]
        BC --> MLDSA
    end
```

### 4.2 Application Integration Layers

1.  **Transport Layer**:
    *   **External**: HTTPS (TLS 1.3) via NGINX Ingress.
    *   **Internal**: HTTP via Kubernetes Service Mesh (ClusterIP).

2.  **API Gateway Layer (Spring Cloud Gateway)**:
    *   Centralized authentication check (Session Token validation).
    *   Route dispatching:
        *   `/api/v1/auth/**` -> Identity Service
        *   `/api/v1/certificates/**` -> CA Authority
        *   `/csc/v1/**` -> Cloud Sign (Cloud Signing Consortium API)
        *   `/validation/**` -> Validation Service

3.  **Cryptographic Layer (Common Library)**:
    *   Centralized `common-crypto` library used by CA, Cloud Sign, and Validation services.
    *   Wraps Bouncy Castle 1.83 APIs for **ML-DSA-44/65/87** keypair generation, signing, and verification.
    *   Abstracts `KeyStore` interactions (PKCS#11 for HSM, PKCS#12 for local dev).

4.  **Hardware Abstraction Layer**:
    *   Services communicate with **SoftHSMv2** via the PKCS#11 interface.
    *   Ensures private keys never leave the secure boundary (software simulation of FIPS 140-2 Level 3 HSM).

### 4.3 Service Dependencies

| Service | Upstream Dependencies | Downstream Dependencies |
|---------|-----------------------|-------------------------|
| **Public Portal** | Browser/User | API Gateway |
| **API Gateway** | Public Portal | Identity, CA, Cloud Sign, Validation |
| **Identity SVC** | API Gateway | PostgreSQL, Redis |
| **CA Authority** | API Gateway, Cloud Sign | PostgreSQL, HSM |
| **Cloud Sign** | API Gateway | PostgreSQL, HSM, CA Authority (for cert validation) |
| **Validation SVC** | API Gateway | CA Authority (for chain validation via CRL/OCSP) |

---

## 5. Security Architecture

### 5.1 Network Security Zones

We define strict zones to control traffic flow and data access.

```mermaid
graph LR
    subgraph DMZ["Zone A: DMZ (Public Edge)"]
        INGRESS["NGINX Ingress"]
        PORTAL["Public Portal"]
    end

    subgraph APP["Zone B: Application Trusted"]
        GW["API Gateway"]
        IS["Identity Service"]
    end

    subgraph SECURE["Zone C: High Security (PQC Core)"]
        CA["CA Authority"]
        CS["Cloud Sign"]
        VS["Validation Service"]
        HSM["SoftHSM"]
    end

    subgraph DATA["Zone D: Persistence"]
        DB["PostgreSQL"]
        CACHE["Redis"]
    end

    INTERNET((Internet)) -->|HTTPS| INGRESS
    INGRESS -->|HTTP| PORTAL
    PORTAL -->|HTTP| GW
    
    GW -->|Internal API| IS
    GW -->|Internal API| CA
    GW -->|Internal API| CS
    GW -->|Internal API| VS
    
    IS -->|JDBC| DB
    CA -->|JDBC| DB
    CS -->|JDBC| DB
    
    CA -->|PKCS11| HSM
    CS -->|PKCS11| HSM
    
    style DMZ fill:#fff3e0
    style APP fill:#e8f5e9
    style SECURE fill:#e3f2fd
    style DATA fill:#f3e5f5
```

### 5.2 Access Control Rules

1.  **Public Access**: Only port 443 (HTTPS) is exposed to the internet via NGINX.
2.  **Database Access**: Restricted to specific microservices (Identity, CA, Cloud Sign). **No direct external access**.
3.  **HSM Access**: Restricted strictly to `ca-authority` and `cloud-sign` pods.
4.  **Inter-Service Communication**:
    *   Frontend **MUST** go through API Gateway.
    *   No direct calls from Frontend to backend microservices.

### 5.3 Authentication & Authorization

*   **User Auth**: Session-based authentication using Redis (for stateful sessions) and opaque tokens passed via `X-Auth-Token`.
*   **Signing Auth**:
    *   **Level 1**: Session authentication.
    *   **Level 2 (Step-up)**: **TOTP** (Time-based One-Time Password) required for every signing operation via Cloud Sign, adhering to **Use Sole Control** principles.

---

## 6. Data Flows

### 6.1 Request Lifecycle

```mermaid
sequenceDiagram
    participant User
    participant NGINX
    participant Portal
    participant Gateway
    participant Service
    participant DB

    User->>NGINX: HTTPS Request (Secure)
    NGINX->>Portal: Forward (Internal HTTP)
    Portal->>Gateway: API Call (x-auth-token)
    Gateway->>Gateway: Rate Limit & Auth Check
    Gateway->>Service: Route to Microservice
    Service->>DB: SQL Query
    DB-->>Service: Result Set
    Service-->>Gateway: JSON Response
    Gateway-->>Portal: Forward Response
    Portal-->>User: Rendered UI / Data
```

