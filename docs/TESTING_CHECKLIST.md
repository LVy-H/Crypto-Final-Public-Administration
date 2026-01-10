# Complete Workflow Testing Checklist

## Pre-Deployment Checklist

- [ ] Java 17+ installed
- [ ] Node.js 20.19+ or 22.12+ installed
- [ ] Podman 4.0+ installed (or Docker as alternative)
- [ ] Git repository cloned
- [ ] Ports 3000, 8080-8084, 5432, 6379, 9000-9001 available

## Deployment Checklist

### Infrastructure

- [ ] PostgreSQL running (port 5432)
- [ ] Redis running (port 6379)
- [ ] MinIO running (ports 9000, 9001)
- [ ] Database initialized (`crypto_db`)
- [ ] MinIO buckets created

### Backend Services

- [ ] Gradle build successful (`./gradlew build`)
- [ ] PKI certificates generated (`./scripts/setup_pki.sh`)
- [ ] Identity Service running (port 8081)
- [ ] PKI Service running (port 8082)
- [ ] TSA Service running (port 8083)
- [ ] Document Service running (port 8084)
- [ ] API Gateway running (port 8080)

### Frontend

- [ ] Dependencies installed (`npm install`)
- [ ] Build successful (`npm run build`)
- [ ] Dev server running (port 3000) OR
- [ ] Production container running (port 3000)

### Health Checks

- [ ] Frontend accessible: http://localhost:3000
- [ ] API Gateway responding: http://localhost:8080/actuator/health
- [ ] Services registered and healthy

## Workflow 1: User Registration & KYC

### Registration (Citizen Role)

- [ ] Navigate to http://localhost:3000/register
- [ ] Form loads correctly with all fields
- [ ] Fill username: `testuser1`
- [ ] Fill email: `testuser1@example.com`
- [ ] Select algorithm: `ML-DSA-65`
- [ ] Fill KYC data: `012345678912`
- [ ] Click "Gửi yêu cầu" button
- [ ] Key generation starts (WASM loading)
- [ ] Progress indicator visible
- [ ] Private key stored in IndexedDB
- [ ] CSR sent to backend successfully
- [ ] Redirect to dashboard
- [ ] Success message displayed

**Screenshot**: `✅ 01_registration_complete.png`

**Validation**:
```bash
# Check user created
curl http://localhost:8081/admin/users | grep testuser1

# Check CSR in database
podman exec -it postgres psql -U admin -d crypto_db \
  -c "SELECT * FROM csr_requests WHERE user_id='testuser1';"
```

### KYC Approval (Officer Role)

- [ ] Navigate to http://localhost:3000/officer
- [ ] Login as officer (create if needed)
- [ ] Dashboard loads with statistics
- [ ] Pending approvals count shows > 0
- [ ] Request table visible
- [ ] testuser1 request visible in list
- [ ] Click "Xem xét" (Review) button
- [ ] Review page loads with KYC details
- [ ] User information correct:
  - [ ] Name: testuser1
  - [ ] Email: testuser1@example.com
  - [ ] CCCD: 012345678912
  - [ ] Algorithm: ML-DSA-65
- [ ] Click "Tiến hành Ký duyệt" (Approve)
- [ ] Confirmation message appears
- [ ] Certificate issuance starts
- [ ] Return to dashboard
- [ ] Pending count decreases by 1

**Screenshot**: `✅ 02_kyc_approval.png`

**Validation**:
```bash
# Check KYC status updated
podman exec -it postgres psql -U admin -d crypto_db \
  -c "SELECT username, kyc_status FROM users WHERE username='testuser1';"
# Expected: kyc_status = APPROVED

# Check certificate issued
curl http://localhost:8082/pki/admin/csr/1 | grep SIGNED
```

## Workflow 2: Certificate Management

### View Certificates

- [ ] Login as testuser1
- [ ] Navigate to http://localhost:3000/dashboard
- [ ] Page title: "Quản lý Chứng thư số"
- [ ] Certificate table visible
- [ ] At least one certificate row displayed
- [ ] Certificate details visible:
  - [ ] Serial number (e.g., 547823901238)
  - [ ] Subject DN (CN=testuser1, O=Gov, C=VN)
  - [ ] Algorithm (ML-DSA-65 or ML-DSA-44)
  - [ ] Status (ACTIVE or REVOKED)
  - [ ] Expiry date (future date)
  - [ ] Download button present

**Screenshot**: `✅ 03_certificate_dashboard.png`

### Download Certificate

- [ ] Click "Tải về" (Download) button
- [ ] File download dialog appears
- [ ] Certificate downloads as `.crt` or `.pem`
- [ ] File size > 0 bytes
- [ ] File contains "BEGIN CERTIFICATE"

**Validation**:
```bash
# View certificate
openssl x509 -in certificate.crt -text -noout

# Check ML-DSA algorithm OID
grep "dilithium" certificate.crt || grep "1.3.6.1.4.1" certificate.crt
```

## Workflow 3: Document Signing

### Prepare Document

- [ ] Create test document:
```bash
echo "This is a test government contract" > test_contract.txt
```

### Sign Document

- [ ] Navigate to http://localhost:3000/sign
- [ ] Page title: "Ký số từ xa (Cloud Signing - CSC)"
- [ ] Key selection dropdown visible
- [ ] Select key: `key_mldsa65_alias`
- [ ] Upload section visible
- [ ] Click file input
- [ ] Select `test_contract.txt`
- [ ] File name appears: "Đã chọn: test_contract.txt"
- [ ] "Ký ngay" button enabled
- [ ] Click "Ký ngay"
- [ ] Loading state: "Đang ký (Signing)..."
- [ ] Wait 1-2 seconds
- [ ] Signature appears in result textarea
- [ ] Signature format correct:
  - [ ] Starts with `-----BEGIN ML-DSA-65 SIGNATURE-----`
  - [ ] Contains base64 data
  - [ ] Ends with `-----END ML-DSA-65 SIGNATURE-----`

**Screenshot**: `✅ 04_document_signing.png`

**Validation**:
```bash
# Check signature length (should be ~3000+ chars for ML-DSA-65)
wc -c signature.txt
```

### ASiC Container Creation

- [ ] Backend receives signature
- [ ] ASiC-E container created
- [ ] Document + signature packaged
- [ ] Download available (if implemented)

**Validation**:
```bash
# Check document service logs
podman logs document-service | tail -20

# If .asic file available:
unzip -l package.asic
# Should show: mimetype, META-INF/, document, signature
```

## Workflow 4: Signature Verification

### Upload ASiC File

- [ ] Navigate to http://localhost:3000/verify
- [ ] Page title: "Kiểm tra Chữ ký (Verification Service)"
- [ ] File input visible: "Chọn file ASiC để xác thực"
- [ ] Click file input
- [ ] Select `.asic` file (if available, or mock file)
- [ ] File selected confirmation
- [ ] "Xác thực ngay" button enabled
- [ ] Click "Xác thực ngay"
- [ ] Loading state visible

### View Results (Valid Signature)

- [ ] Results section appears
- [ ] Status: "✓ Tài liệu Hợp lệ (Valid)"
- [ ] Background color: green
- [ ] Verification timestamp displayed
- [ ] Document details:
  - [ ] Document name correct
  - [ ] Document size displayed (bytes)
  - [ ] Status badge: "Verified (X signatures)"
- [ ] Signature list visible
- [ ] Per-signature details:
  - [ ] Signature index (#1, #2, etc.)
  - [ ] Timestamp
  - [ ] Status badge: "✓ Valid"
  - [ ] Signer name
  - [ ] Algorithm: "ML-DSA (PQC)"
  - [ ] Certificate subject
  - [ ] Certificate issuer
  - [ ] Validity period (from → to)

**Screenshot**: `✅ 05_verification_valid.png`

### View Results (Invalid Signature)

- [ ] Upload tampered or invalid file
- [ ] Status: "✗ Tài liệu Không hợp lệ (Invalid)"
- [ ] Background color: red
- [ ] Error message displayed

**Screenshot**: `✅ 06_verification_invalid.png`

**Validation**:
```bash
# Test verification endpoint directly
curl -X POST http://localhost:8080/api/v1/documents/verify-asic \
  -F "file=@package.asic" | jq .

# Expected response:
# {
#   "valid": true,
#   "signatureCount": 1,
#   "documentName": "test_contract.txt",
#   "signatures": [...]
# }
```

## Workflow 5: Officer Administration

### Dashboard Overview

- [ ] Navigate to http://localhost:3000/officer
- [ ] Page title: "Quản trị hệ thống (Officer Portal)"
- [ ] Statistics cards visible:
  - [ ] Users count (e.g., 152)
  - [ ] Certificates count (e.g., 43)
  - [ ] Pending approvals (highlighted, e.g., 2)
  - [ ] Signatures today (e.g., 18)
- [ ] Pending requests section visible
- [ ] Service health section visible

**Screenshot**: `✅ 07_officer_dashboard.png`

### Pending Requests

- [ ] Request table headers correct:
  - Mã YC (Request ID)
  - Người gửi (Sender)
  - Loại yêu cầu (Request Type)
  - Thời gian (Time)
  - Thao tác (Action)
- [ ] At least one request row visible
- [ ] Request IDs format: REQ-RA-YYYY-NNN or REQ-SIGN-YYYY-NNN
- [ ] "Xem xét" button present on each row

### Review Request

- [ ] Click "Xem xét" on first request
- [ ] Review page loads
- [ ] Back button visible: "← Quay lại"
- [ ] Request ID in title
- [ ] KYC information section:
  - [ ] Full name
  - [ ] CCCD number
  - [ ] Email
  - [ ] Algorithm requested
- [ ] Action buttons visible:
  - [ ] "Từ chối" (Reject)
  - [ ] "Tiến hành Ký duyệt" (Approve)

**Screenshot**: `✅ 08_officer_review.png`

### Service Health

- [ ] Service status list visible:
  - [ ] API Gateway
  - [ ] Identity Service
  - [ ] Cloud Sign
  - [ ] CA Authority
- [ ] Each service shows status:
  - [ ] Green dot (●) for online
  - [ ] Red dot (●) for offline (if any)

**Screenshot**: `✅ 09_service_health.png`

## Workflow 6: Countersigning (Multi-signature)

### Add Second Signature

- [ ] Existing `.asic` file with 1 signature
- [ ] Second user with valid certificate
- [ ] Navigate to countersign endpoint
- [ ] Upload existing ASiC
- [ ] Select second user's key
- [ ] Click countersign button
- [ ] Second signature added to container
- [ ] Download updated `.asic`
- [ ] File contains 2 signatures

### Verify Multi-signature

- [ ] Upload countersigned `.asic` to verify page
- [ ] Results show: "Verified (2 signatures)"
- [ ] Signature list shows 2 entries
- [ ] Both signatures show "✓ Valid"
- [ ] Each signature has different signer

**Screenshot**: `✅ 10_countersign_result.png`

## Security Testing Checklist

### RBAC Tests

- [ ] **Test 1**: Anonymous access to admin endpoint
  ```bash
  curl -X POST http://localhost:8080/api/v1/admin/approve-kyc
  # Expected: 401 Unauthorized or 403 Forbidden
  ```

- [ ] **Test 2**: Regular user access to admin endpoint
  ```bash
  curl -X POST http://localhost:8080/api/v1/admin/approve-kyc \
    -H "Cookie: JSESSIONID=user_session"
  # Expected: 403 Forbidden
  ```

- [ ] **Test 3**: Officer access to admin endpoint
  ```bash
  curl -X POST http://localhost:8080/api/v1/admin/approve-kyc \
    -H "Cookie: JSESSIONID=officer_session" \
    -d '{"username":"test","action":"APPROVE"}'
  # Expected: 200 OK
  ```

- [ ] **Test 4**: Regular user access to CA admin
  ```bash
  curl http://localhost:8080/api/v1/pki/admin/csr/pending \
    -H "Cookie: JSESSIONID=user_session"
  # Expected: 403 Forbidden
  ```

- [ ] **Test 5**: CA_OPERATOR access to CA admin
  ```bash
  curl http://localhost:8080/api/v1/pki/admin/csr/pending \
    -H "Cookie: JSESSIONID=ca_operator_session"
  # Expected: 200 OK with CSR list
  ```

### ABAC Tests

- [ ] **Test 1**: User1 access to own document
  ```bash
  curl http://localhost:8080/api/v1/documents/123 \
    -H "Cookie: JSESSIONID=user1_session"
  # Expected: 200 OK (if owner)
  ```

- [ ] **Test 2**: User2 access to User1's document
  ```bash
  curl http://localhost:8080/api/v1/documents/123 \
    -H "Cookie: JSESSIONID=user2_session"
  # Expected: 403 Forbidden (not owner)
  ```

- [ ] **Test 3**: Officer access to any document
  ```bash
  curl http://localhost:8080/api/v1/documents/123 \
    -H "Cookie: JSESSIONID=officer_session"
  # Expected: 200 OK (officer can read)
  ```

- [ ] **Test 4**: User modify own document
  ```bash
  curl -X PUT http://localhost:8080/api/v1/documents/123 \
    -H "Cookie: JSESSIONID=owner_session"
  # Expected: 200 OK
  ```

- [ ] **Test 5**: User modify other's document
  ```bash
  curl -X PUT http://localhost:8080/api/v1/documents/123 \
    -H "Cookie: JSESSIONID=other_user_session"
  # Expected: 403 Forbidden
  ```

## Performance Testing Checklist

- [ ] Load test registration: 100 concurrent
- [ ] Load test login: 100 concurrent
- [ ] Load test signing: 50 concurrent
- [ ] Load test verification: 50 concurrent
- [ ] Response time < 1s for 95% requests
- [ ] No memory leaks after 1 hour
- [ ] Database connections properly pooled

## Automated Testing Checklist

### Playwright Tests

- [ ] Install dependencies: `npm install`
- [ ] Run production readiness: `npx playwright test tests/production-readiness.spec.ts`
- [ ] All 15 tests pass
- [ ] Run full portal suite: `npx playwright test --project=portal`
- [ ] Run API tests: `npx playwright test --project=api`
- [ ] Generate HTML report: `npx playwright show-report`
- [ ] Check test artifacts: screenshots, videos, traces

### Test Results

- [ ] Production readiness: 15/15 ✅
- [ ] Portal tests: XX/XX ✅
- [ ] API tests: XX/XX ✅
- [ ] Total pass rate: >95%

## Documentation Checklist

### Screenshots Captured

- [ ] 01_registration_complete.png
- [ ] 02_kyc_approval.png
- [ ] 03_certificate_dashboard.png
- [ ] 04_document_signing.png
- [ ] 05_verification_valid.png
- [ ] 06_verification_invalid.png
- [ ] 07_officer_dashboard.png
- [ ] 08_officer_review.png
- [ ] 09_service_health.png
- [ ] 10_countersign_result.png
- [ ] 11_certificate_download.png
- [ ] 12_registration_keygen.png
- [ ] 13_sign_upload.png
- [ ] 14_sign_progress.png
- [ ] 15_verify_upload.png

### Videos Captured

- [ ] complete_signup_flow.mp4 (2-3 min)
- [ ] signing_workflow.mp4 (1-2 min)
- [ ] verification_workflow.mp4 (1 min)
- [ ] officer_workflow.mp4 (2 min)
- [ ] countersign_workflow.mp4 (1 min)

### Documentation Updated

- [ ] PODMAN_DEPLOYMENT_TESTING.md created
- [ ] FRONTEND_GUIDE.md has all workflows
- [ ] SECURITY_IMPLEMENTATION.md complete
- [ ] README.md updated with deployment info
- [ ] Screenshots added to docs/screenshots/
- [ ] Videos added to docs/videos/

## Sign-off Checklist

- [ ] All workflows tested manually
- [ ] All automated tests passing
- [ ] All screenshots captured
- [ ] All videos recorded
- [ ] Security testing complete
- [ ] Performance acceptable
- [ ] Documentation complete
- [ ] No critical bugs
- [ ] Ready for review

## Issues Found

Document any issues discovered during testing:

| Issue | Severity | Description | Status |
|-------|----------|-------------|--------|
| | | | |

## Notes

Add any additional notes or observations:

---

**Tested by**: _____________
**Date**: _____________
**Environment**: Podman / Docker Compose / Local
**Version**: _____________
