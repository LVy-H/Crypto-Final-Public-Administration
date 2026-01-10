---
description: Verification workflow for Certificate Issuance (CSR -> Cert).
---
# Verify PKI Issuance Flow

1. **Check CA Status**
   ```bash
   curl -s http://localhost:8082/actuator/health | grep UP
   ```

2. **Run Enrollment Test**
   ```bash
   # This script simulates a client sending a CSR
   python tests/scripts/test_pki_enrollment.py
   ```

3. **Verify Certificate Usage**
   ```bash
   # Checks if the issued cert is valid for signing
   openssl x509 -in tests/artifacts/user.crt -text -noout | grep "Key Usage"
   ```
