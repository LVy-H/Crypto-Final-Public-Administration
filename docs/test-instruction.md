# End-to-End Test Instructions

## Overview

Production validation suite for PQC Digital Signature System.  
**Compliance**: Decree 23/2025/ND-CP, Circular 15/2025/TT-BKHCN, eIDAS, NIST SP 800-208.

## Quick Start

```bash
cd tests/e2e
npm install
npx playwright test tests/production-readiness.spec.ts  # Quick smoke test (15 tests, ~1 min)
npx playwright test                                      # Full suite
```

## Test Suite Structure

### API Tests (`--project=api`)
| Test File | Purpose |
|-----------|---------|
| `pqc-compliance.spec.ts` | Post-quantum cryptography compliance |
| `api.spec.ts` | Core API endpoint tests |
| `auth.spec.ts` | Authentication flow tests |
| `ca-hierarchy.spec.ts` | Certificate authority chain tests |
| `cloud-signing.spec.ts` | Remote signing (CSC API) tests |
| `validation.spec.ts` | Signature validation tests |
| `security.spec.ts` | Security controls tests |
| `integration.spec.ts` | Cross-service integration |

### Portal Tests (`--project=portal`)
| Test File | Purpose |
|-----------|---------|
| `production-readiness.spec.ts` | **Production smoke test (15 tests)** |
| `public-portal.spec.ts` | Public-facing pages |
| `admin-portal.spec.ts` | Admin dashboard tests |
| `auth-features.spec.ts` | Login/Register/TOTP tests |
| `sign-verify-ui.spec.ts` | Document signing UI |
| `totp-signing.spec.ts` | TOTP-based signing flow |
| `totp-real-flow.spec.ts` | Real TOTP verification |
| `ui-user-journey.spec.ts` | Complete user journey |
| `full-journey.spec.ts` | End-to-end scenarios |
| `real-api-tests.spec.ts` | Real API integration |

## Running Specific Tests

```bash
# Production readiness only (recommended for CI)
npx playwright test tests/production-readiness.spec.ts

# API tests only
npx playwright test --project=api

# Portal tests only  
npx playwright test --project=portal

# Single test file
npx playwright test tests/auth-features.spec.ts

# With video recording
npx playwright test --video=on
```

## Test Credentials

| User | Password | Role |
|------|----------|------|
| `admin_capture` | `SecurePass123!` | ADMIN |
| `demo_user` | `SecurePass123!` | CITIZEN |

## Success Criteria

1. ✅ All 15 production readiness tests pass
2. ✅ Login/Register flows complete without errors
3. ✅ Admin can view dashboard, certificates, users
4. ✅ User can access signing page
5. ✅ API endpoints return 200 OK

## CI/CD Integration

```yaml
# GitHub Actions example
- name: Run E2E Tests
  run: |
    cd tests/e2e
    npm ci
    npx playwright install --with-deps chromium
    npx playwright test tests/production-readiness.spec.ts
```

## Archived Tests

Old/redundant tests are in `tests/archive/` for reference:
- `monitor-dashboard.spec.ts`
- `phase8-flows.spec.ts`
- `final-verification.spec.ts`
- `e2e-flows.spec.ts`
