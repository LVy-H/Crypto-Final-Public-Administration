---
description: Verification workflow for PQC Document Signing (End-to-End).
---
# Verify PQC Signing Flow

1. **Run Playwright E2E Test**
   ```bash
   cd tests/e2e
   # Runs the specific signing spec
   npx playwright test tests/signing.spec.ts
   ```

2. **Manual Backend Check**
   ```bash
   # Check logs for successful signature verification
   kubectl logs -l app=document-service --tail=50 | grep "Signature valid"
   ```
