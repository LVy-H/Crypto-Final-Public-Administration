# Podman Deployment and Complete Workflow Testing Guide

## Overview

This guide provides comprehensive instructions for deploying the GovTech PQC Digital Signature System using Podman and testing all workflows from signup/KYC to signing and verification.

## Prerequisites

- **Podman** 4.0+ (rootless mode supported)
- **Node.js** 20.19+ or 22.12+
- **Java** 17+ (for Gradle builds)
- **Git** for repository access

## Quick Start Deployment

### Option 1: Using Podman (Recommended for Production-like Testing)

```bash
# Navigate to repository root
cd /home/runner/work/Crypto-Final-Public-Administration/Crypto-Final-Public-Administration

# Run the automated Podman deployment script
chmod +x scripts/run_docker.sh
./scripts/run_docker.sh
```

**What this does:**
1. Creates `crypto-net` Podman network
2. Starts infrastructure: PostgreSQL, Redis, MinIO
3. Sets up PKI certificates
4. Builds backend services (Gradle)
5. Builds and runs all microservices
6. Starts frontend on port 3000

### Option 2: Using Docker Compose (Alternative)

```bash
# Build all services
docker-compose build

# Start all services
docker-compose up -d

# Check status
docker-compose ps
```

### Option 3: Development Mode (Local without containers)

```bash
# Start infrastructure only
docker-compose up -d postgres redis minio

# Run backend services locally
./scripts/start_local.sh

# Run frontend dev server
cd apps/public-portal
npm install
npm run dev
```

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| Frontend (Vue) | 3000 | http://localhost:3000 |
| API Gateway | 8080 | http://localhost:8080 |
| Identity Service | 8081 | http://localhost:8081 |
| PKI Service | 8082 | http://localhost:8082 |
| TSA Service | 8083 | http://localhost:8083 |
| Document Service | 8084 | http://localhost:8084 |
| PostgreSQL | 5432 | localhost:5432 |
| Redis | 6379 | localhost:6379 |
| MinIO Console | 9001 | http://localhost:9001 |

## Complete Workflow Testing

### Workflow 1: User Registration and KYC

**Objective**: New citizen registers, generates PQC keys, and gets KYC approved.

#### Step 1: Registration (Citizen)

1. **Navigate** to http://localhost:3000/register
2. **Fill in the form**:
   - Username: `testuser1`
   - Email: `testuser1@example.com`
   - Algorithm: `ML-DSA-65 (NIST Level 3)` (default)
   - KYC Data: `012345678912`
3. **Click** "Gửi yêu cầu" (Submit Request)
4. **Observe**: 
   - Browser generates PQC key pair (ML-DSA)
   - Private key encrypted and stored in IndexedDB
   - CSR sent to backend
   - Redirect to dashboard

**Screenshot Location**: Capture this as `01_registration_complete.png`

**Expected Result**: 
- User created with KYC status: PENDING
- Key pair generated client-side
- CSR stored in PKI service database

#### Step 2: KYC Approval (Officer)

1. **Login as Officer**:
   - Navigate to http://localhost:3000/officer
   - Username: `officer1` (or create officer account)
   - Password: `SecurePass123!`

2. **View Dashboard**:
   - See pending requests count
   - Request list shows `testuser1`

3. **Review KYC**:
   - Click "Xem xét" (Review) on the request
   - Verify KYC data: Name, CCCD, Email, Algorithm
   - Click "Tiến hành Ký duyệt" (Approve)

**Screenshot Location**: Capture as `02_kyc_approval.png`

**Expected Result**:
- User KYC status changes to APPROVED
- Certificate signed and issued
- User can now sign documents

### Workflow 2: Certificate Management

#### Step 1: View Certificates (Citizen)

1. **Login as testuser1**
2. **Navigate** to http://localhost:3000/dashboard
3. **Observe**:
   - Table shows issued certificates
   - Serial number, Subject DN, Algorithm, Status, Expiry
   - Active certificate with ML-DSA-65

**Screenshot Location**: `03_certificate_dashboard.png`

**Expected Result**:
- At least one ACTIVE certificate
- Certificate details visible
- Download button available

#### Step 2: Download Certificate

1. **Click** "Tải về" (Download) button
2. **Verify**: Certificate downloads as `.crt` or `.pem` file

**Expected Result**: Valid X.509 certificate with ML-DSA public key

### Workflow 3: Document Signing (CSC Protocol)

**Objective**: Sign a document using Cloud Signature Consortium protocol.

#### Step 1: Prepare Document

1. **Create test document**:
```bash
echo "This is a test contract" > test_contract.txt
```

#### Step 2: Sign Document

1. **Navigate** to http://localhost:3000/sign
2. **Select key**: `key_mldsa65_alias` (or available key)
3. **Upload document**: Click file input, select `test_contract.txt`
4. **Observe**: 
   - File name appears
   - "Ký ngay" button becomes enabled
5. **Click** "Ký ngay" (Sign Now)
6. **Wait**: 
   - Client-side hashing (SHA-384)
   - WASM module generates ML-DSA signature
   - Signature displayed in Base64 format

**Screenshot Location**: `04_document_signing.png`

**Expected Result**:
- Signature displayed in textarea
- Format: `-----BEGIN ML-DSA-65 SIGNATURE-----`
- Base64 encoded signature data

#### Step 3: Create ASiC Container

**Note**: In full implementation, this step would automatically create `.asic` file. For testing:

1. **Verify signature is generated**
2. **Check backend logs**:
```bash
podman logs document-service | grep "ASiC"
```

**Expected Result**: ASiC-E container created with document + signature

### Workflow 4: Signature Verification

**Objective**: Verify signed document integrity and authenticity.

#### Step 1: Upload ASiC File

1. **Navigate** to http://localhost:3000/verify
2. **Select file**: Upload `.asic` file (if available) or test file
3. **Click** "Xác thực ngay" (Verify Now)

#### Step 2: View Verification Results

**Observe results**:
- ✓ **Valid** or ✗ **Invalid** status
- Document name and size
- Signature count
- Per-signature details:
  - Signer name
  - Algorithm (ML-DSA PQC)
  - Timestamp
  - Certificate subject/issuer
  - Validity period

**Screenshot Location**: `05_verification_results.png`

**Expected Result**:
- Valid signature confirmation
- Certificate chain verified
- Timestamp validated
- All checks passed

### Workflow 5: Officer Administration

**Objective**: Officer manages system, reviews requests, monitors services.

#### Step 1: Officer Dashboard

1. **Navigate** to http://localhost:3000/officer
2. **View statistics**:
   - Total users: 152
   - Certificates: 43
   - Pending approvals: 2 (highlighted)
   - Signatures today: 18

**Screenshot Location**: `06_officer_dashboard.png`

#### Step 2: Pending Requests

**Request Queue**:
- REQ-RA-2025-001: Certificate request
- REQ-SIGN-2025-089: Signing approval

**Actions**:
- Click "Xem xét" to review
- Approve or reject

#### Step 3: Service Health

**Monitor services**:
- API Gateway: ● Online
- Identity Service: ● Online
- Cloud Sign: ● Online
- CA Authority: ● Online

**Expected Result**: All services operational

### Workflow 6: Countersigning (Multi-signature)

**Objective**: Add additional signature to existing document.

#### Prerequisites
- Existing signed `.asic` file
- Second user with valid certificate

#### Steps

1. **Upload existing ASiC**: Use countersign endpoint
2. **Extract original document**: System reads from container
3. **Generate second signature**: Using second user's key
4. **Add to container**: New signature added to META-INF/
5. **Download**: `package_countersigned.asic`

**Expected Result**: 
- ASiC contains multiple signatures
- Verification shows 2+ valid signatures
- Each signature independently valid

## Testing with Playwright

### Run Automated Tests

```bash
cd tests/e2e
npm install

# Production readiness smoke test (15 tests)
npx playwright test tests/production-readiness.spec.ts

# Full UI test suite
npx playwright test --project=portal

# With video recording
npx playwright test --video=on

# With screenshots on failure
npx playwright test --screenshot=on
```

### Test Coverage

| Workflow | Test File | Tests |
|----------|-----------|-------|
| Registration | `auth-features.spec.ts` | 5 tests |
| Login/Logout | `auth-features.spec.ts` | 3 tests |
| Dashboard | `public-portal.spec.ts` | 4 tests |
| Signing | `sign-verify-ui.spec.ts` | 6 tests |
| Verification | `sign-verify-ui.spec.ts` | 4 tests |
| Officer Panel | `admin-portal.spec.ts` | 8 tests |

## Capturing Screenshots and Videos

### Manual Screenshot Capture

**Using Browser DevTools**:
1. Open DevTools (F12)
2. Toggle device toolbar (Ctrl+Shift+M)
3. Set viewport: 1280x720
4. Capture screenshot: DevTools > ⋮ > Capture screenshot

**Using Playwright**:
```typescript
await page.screenshot({ path: 'workflow_step.png' })
```

### Video Recording

**Using Playwright**:
```bash
# Record test execution
npx playwright test --video=on

# Videos saved to: test-results/
```

**Using OBS Studio** (for manual workflows):
1. Install OBS Studio
2. Add browser source
3. Start recording
4. Perform workflow steps
5. Stop and save video

### Recommended Screenshots

Create these screenshots for complete documentation:

1. `01_registration_form.png` - Registration page with form filled
2. `02_registration_keygen.png` - Key generation in progress
3. `03_dashboard_certificates.png` - Certificate list view
4. `04_sign_upload.png` - Document upload interface
5. `05_sign_progress.png` - Signing in progress
6. `06_sign_complete.png` - Signature displayed
7. `07_verify_upload.png` - Verification upload
8. `08_verify_results_valid.png` - Valid verification result
9. `09_verify_results_invalid.png` - Invalid verification result
10. `10_officer_dashboard.png` - Officer statistics view
11. `11_officer_review_kyc.png` - KYC review interface
12. `12_officer_approve.png` - Approval confirmation
13. `13_service_health.png` - Service status indicators
14. `14_certificate_download.png` - Certificate download
15. `15_countersign.png` - Multi-signature interface

### Video Workflows

Create these videos:

1. **`complete_signup_flow.mp4`** (2-3 min):
   - Registration → KYC approval → Certificate issuance
   
2. **`signing_workflow.mp4`** (1-2 min):
   - Login → Upload document → Sign → Download ASiC

3. **`verification_workflow.mp4`** (1 min):
   - Upload ASiC → View results → Check signatures

4. **`officer_workflow.mp4`** (2 min):
   - Officer login → Review pending → Approve → Monitor

## Troubleshooting

### Frontend not accessible

```bash
# Check if service is running
podman ps | grep public-portal

# Check logs
podman logs public-portal

# Restart
podman restart public-portal
```

### Backend service errors

```bash
# Check service status
podman ps

# View logs
podman logs identity-service
podman logs api-gateway

# Check database connection
podman exec -it postgres psql -U admin -d crypto_db -c "SELECT count(*) FROM users;"
```

### Build failures

```bash
# Clean and rebuild
./gradlew clean build -x test

# Check Java version
java -version  # Should be 17+

# Check Node version
node -version  # Should be 20.19+ or 22.12+
```

### Port conflicts

```bash
# Find process using port
sudo lsof -i :3000
sudo lsof -i :8080

# Kill process
kill -9 <PID>
```

## Security Testing

### Test RBAC

```bash
# Try accessing admin endpoint without auth (should fail)
curl -X POST http://localhost:8080/api/v1/admin/approve-kyc \
  -H "Content-Type: application/json" \
  -d '{"username":"test","action":"APPROVE"}'
# Expected: 401 Unauthorized or 403 Forbidden

# Login as user
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser1","password":"password123"}' \
  -c cookies.txt

# Try admin endpoint as regular user (should fail)
curl -X POST http://localhost:8080/api/v1/admin/approve-kyc \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{"username":"test","action":"APPROVE"}'
# Expected: 403 Forbidden
```

### Test ABAC

```bash
# User1 tries to access User2's document
curl -X GET http://localhost:8080/api/v1/documents/123 \
  -H "Cookie: JSESSIONID=user1_session"
# Expected: 403 Forbidden (if not owner)
```

## Performance Testing

### Load Test with Apache Bench

```bash
# Test registration endpoint
ab -n 100 -c 10 http://localhost:8080/api/v1/auth/register

# Test verification endpoint
ab -n 50 -c 5 -p test.asic http://localhost:8080/api/v1/documents/verify-asic
```

## Cleanup

### Stop all services

```bash
# Podman
podman stop $(podman ps -aq)
podman rm $(podman ps -aq)
podman network rm crypto-net

# Docker Compose
docker-compose down -v
```

## Next Steps

1. ✅ Deploy with Podman
2. ✅ Test all workflows manually
3. ✅ Capture screenshots
4. ✅ Record workflow videos
5. ✅ Run automated Playwright tests
6. ✅ Test security (RBAC/ABAC)
7. ✅ Document any issues
8. ✅ Create deployment checklist

## References

- **Deployment Scripts**: `scripts/run_docker.sh`
- **Docker Compose**: `docker-compose.yml`
- **Test Instructions**: `docs/test-instruction.md`
- **Frontend Guide**: `docs/FRONTEND_GUIDE.md`
- **Security Implementation**: `docs/SECURITY_IMPLEMENTATION.md`
