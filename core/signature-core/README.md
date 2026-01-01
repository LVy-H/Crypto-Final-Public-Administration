# Signature Core Library

> ⚠️ **DEPRECATED**: This service is deprecated. Use `cloud-sign` (RSSP) for all signing operations.

## Deprecation Notice

As of December 2024, the signing functionality has been moved to the `cloud-sign` service which provides:
- TOTP-based Sole Control (2-step verification)
- Database/HSM-backed key storage (persistent across restarts)
- CSC (Cloud Signature Consortium) compliant API

### Migration Guide

**Before** (deprecated):
```
public-portal → signature-core → in-memory keys (lost on restart)
```

**After** (current):
```
public-portal → api-gateway → cloud-sign → PostgreSQL/HSM keys
```

Frontend should call:
- `POST /csc/v1/sign/init` - Initialize signing
- `POST /csc/v1/sign/confirm` - Confirm with TOTP
- `POST /api/v1/credentials/totp/setup` - Enroll TOTP

---

## Legacy Overview
**Signature Core** was a shared library for core cryptographic primitives (ML-DSA-44/65/87). Its classes are now in `libs/common-crypto`.
