---
description: Verification workflow for User Registration and KYC flow.
---
# Verify Identity & KYC Flow

// turbo
1. **Reset Database** (Optional)
   ```bash
   # Caution: This wipes data
   # ./scripts/reset_db.sh
   echo "Skipping reset for safety"
   ```

2. **Run API Registration Test**
   ```bash
   python tests/scripts/test_api.py --test-registration
   ```

3. **Verify Admin KYC Approval**
   ```bash
   # Simulates Admin approving the user
   python tests/scripts/kyc_auto_approve.py --user-email "test@citizen.gov.vn"
   ```
