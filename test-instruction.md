# End-to-End Test Instructions

## Overview
Production validation for Public Administration Crypto System.
Compliance: Decree 130/2018/ND-CP, Decree 23/2025/ND-CP, eIDAS, NIST SP 800-208, FIPS 140-3.

## Test Phases

### Phase 1: Infrastructure Bootstrap (INFRA-*)
- [ ] INFRA-001: Offline Root CA → Online Government CA
- [ ] INFRA-002: Government CA → Provincial CA
- [ ] INFRA-003: Provincial CA → District RA

### Phase 2: Citizen Onboarding (USER-*)
- [ ] USER-001: Identity verification flow
- [ ] USER-002: Certificate issuance via Cloud HSM

### Phase 3: Document Signing (PROC-*)
- [ ] PROC-001: Multi-party marriage contract (Husband + Wife + Officer)
- [ ] PROC-002: Timestamped official registration

### Phase 4: Public Validation (VAL-*)
- [ ] VAL-001: Chain validation and revocation check
- [ ] VAL-002: Revocation enforcement (block new signatures)
- [ ] VAL-003: LTV - old signatures remain valid post-revocation

## Success Criteria
1. Zero Root Key leakage
2. Full chain validation to Root CA
3. Officer signature required for legal docs
4. Revocation immediately blocks new signatures
5. LTV: timestamped signatures survive revocation
6. All events audited

## Execution
```bash
cd tests/e2e
npx playwright test
```
