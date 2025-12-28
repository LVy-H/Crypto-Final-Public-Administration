# System Architecture

## Overview

Post-Quantum Cryptography Digital Signature System for Public Administration.

## Architecture Diagram

```mermaid
flowchart TB
    subgraph External["External"]
        User([User Browser])
        Admin([Admin Browser])
    end

    subgraph DMZ["Zone A - Public/DMZ"]
        Portal["public-portal<br/>:3000<br/>(Nuxt.js)"]
        RSSP["rssp-gateway<br/>:8443<br/>(CSC API)"]
    end

    subgraph Internal["Zone B - Internal"]
        API["api-gateway<br/>:8080<br/>(Spring)"]
        Identity["identity-service<br/>:8081"]
        Org["org-service<br/>:8082"]
        Doc["doc-service<br/>:8083"]
        Validation["validation-service<br/>:8084"]
    end

    subgraph Secure["Zone C - Secure"]
        CA["ca-authority<br/>:8085<br/>(PKI)"]
        Sign["signature-core<br/>:8086"]
        Cloud["cloud-sign<br/>:8087"]
        HSM["softhsm<br/>:5657<br/>(PKCS#11)"]
        TSA["tsa-mock<br/>:8318<br/>(RFC 3161)"]
    end

    subgraph Data["Zone D - Data"]
        PG[(PostgreSQL<br/>:5432)]
    end

    User --> Portal
    Admin --> Portal
    Portal --> API

    API --> Identity
    API --> Org
    API --> Doc
    API --> Validation
    API --> Cloud

    Identity --> PG
    Org --> PG
    CA --> PG

    Cloud --> Sign
    Cloud --> CA
    Sign --> HSM
    Sign --> TSA

    RSSP --> Cloud
```

## Components

### Frontend (Zone A)
| Service | Port | Tech | Purpose |
|---------|------|------|---------|
| public-portal | 3000 | Nuxt.js | Unified citizen/admin UI |
| rssp-gateway | 8443 | Spring | CSC API for remote signing |

### Core Services (Zone B)
| Service | Port | Purpose |
|---------|------|---------|
| api-gateway | 8080 | Request routing, auth |
| identity-service | 8081 | User registration, KYC |
| org-service | 8082 | Organization management |
| doc-service | 8083 | Document storage |
| validation-service | 8084 | Signature verification |

### Secure Services (Zone C)
| Service | Port | Purpose |
|---------|------|---------|
| ca-authority | 8085 | PKI, certificate issuance |
| signature-core | 8086 | Dilithium signing |
| cloud-sign | 8087 | Cloud signing workflow |
| softhsm | 5657 | HSM emulation (PKCS#11) |
| tsa-mock | 8318 | Timestamp authority |

### Data (Zone D)
| Service | Port | Purpose |
|---------|------|---------|
| postgres | 5432 | Persistent storage |

## Security Zones

```mermaid
flowchart LR
    subgraph Z1["Zone A: Public"]
        direction TB
        P1[portal]
        P2[rssp-gateway]
    end

    subgraph Z2["Zone B: Internal"]
        direction TB
        I1[api-gateway]
        I2[services]
    end

    subgraph Z3["Zone C: Secure"]
        direction TB
        S1[ca/signing]
        S2[hsm]
    end

    subgraph Z4["Zone D: Data"]
        direction TB
        D1[postgres]
    end

    Z1 --> Z2
    Z2 --> Z3
    Z2 --> Z4
    Z3 --> Z4
```

## Cryptographic Algorithms

| Component | Algorithm | Level |
|-----------|-----------|-------|
| Root CA | ML-DSA-87 | NIST 5 |
| Provincial CA | ML-DSA-65 | NIST 3 |
| User Certificates | ML-DSA-44 | NIST 2 |
| TLS Key Exchange | X25519 + ML-KEM-768 | Hybrid |

## Deployment

```bash
# Apply to Kubernetes
kubectl apply -k infra/k8s/base

# Or with overlay
kubectl apply -k infra/k8s/overlays/dev
```
