# Security Practices

## Key Management

### CA Private Keys
- **At Rest:** Encrypted with AES-256-GCM
- **Master Key:** Stored in `CA_MASTER_KEY` environment variable
- **Generation:** `openssl rand -base64 32`

### User Signing Keys
- **Development:** In-memory storage (SoftwareKeyStorageService)
- **Production:** HSM via PKCS#11 (HsmKeyStorageService)

### Profile Configuration
| Profile | Key Storage | Encryption |
|---------|-------------|------------|
| `dev` | In-memory | Optional |
| `prod` | HSM/PKCS#11 | Required |

---

## Cryptographic Algorithms

| Purpose | Algorithm | Standard |
|---------|-----------|----------|
| Primary Signing | ECDSA P-384 | NIST SP 800-57 |
| PQC Signing | ML-DSA-65 | FIPS 204 |
| Key Encryption | AES-256-GCM | NIST SP 800-38D |
| Hashing | SHA-384 | FIPS 180-4 |

---

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `CA_MASTER_KEY` | 32-byte base64 key for CA encryption | Prod |
| `hsm.enabled` | Enable HSM mode | Prod |
| `hsm.library` | PKCS#11 library path | Prod |
| `hsm.user-pin` | HSM user PIN | Prod |

---

## Security Zones

| Zone | Services | Access |
|------|----------|--------|
| Public (A) | api-gateway, public-portal | External |
| Internal (B) | identity, signature-core | mTLS |
| Secure (C) | ca-authority, HSM | Isolated |

---

## Compliance

- **Vietnam:** Decree 130/2018, Circular 41/2017
- **Crypto Standards:** NIST FIPS 204 (ML-DSA), FIPS 186-5 (ECDSA)
- **PKI:** X.509v3, RFC 5280
