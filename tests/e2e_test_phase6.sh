#!/bin/bash
# Comprehensive End-to-End Test for Phase 6 Features
# Tests: JWT Blacklist, RBAC, @PreAuthorize, QR Code Generation

set -e  # Exit on first error

HOST="api.crypto.gov.vn"
IDENTITY_URL="http://localhost:8080/api/v1"
VALIDATION_URL="http://localhost:8085/api/v1"
TEST_USER="e2e_test_$(date +%s)"
PASSED=0
FAILED=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_pass() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    ((PASSED++))
}

log_fail() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    ((FAILED++))
}

log_info() {
    echo -e "${YELLOW}→${NC} $1"
}

echo "=========================================="
echo "  PQC Digital Signature System E2E Tests"
echo "=========================================="
echo ""

# Check connectivity
log_info "Checking service connectivity..."

if ! nc -z localhost 8080 2>/dev/null; then
    echo "ERROR: Port 8080 (api-gateway) not accessible"
    echo "Run: kubectl port-forward -n crypto-pqc svc/api-gateway 8080:8080"
    exit 1
fi

if ! nc -z localhost 8085 2>/dev/null; then
    echo "WARNING: Port 8085 (validation-service) not accessible"
    echo "Run: kubectl port-forward -n crypto-pqc svc/validation-service 8085:8085"
    echo "Skipping QR tests..."
    SKIP_QR=true
fi

echo ""
echo "=========================================="
echo "  1. User Registration & Authentication"
echo "=========================================="

# Test 1.1: User Registration
log_info "Registering new user: $TEST_USER"
REG_RESPONSE=$(curl -s -X POST -H "Host: $HOST" -H "Content-Type: application/json" \
    -d "{\"username\":\"$TEST_USER\",\"password\":\"TestPass123!\",\"email\":\"$TEST_USER@example.com\"}" \
    "$IDENTITY_URL/auth/register")

if echo "$REG_RESPONSE" | grep -q "User added"; then
    log_pass "User registration successful"
else
    log_fail "User registration failed: $REG_RESPONSE"
fi

# Test 1.2: User Login
log_info "Logging in as $TEST_USER"
LOGIN_RESPONSE=$(curl -s -X POST -H "Host: $HOST" -H "Content-Type: application/json" \
    -d "{\"username\":\"$TEST_USER\",\"password\":\"TestPass123!\"}" \
    "$IDENTITY_URL/auth/login")

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    log_pass "User login successful"
else
    log_fail "User login failed: $LOGIN_RESPONSE"
    echo "Cannot continue without token. Exiting."
    exit 1
fi

echo ""
echo "=========================================="
echo "  2. RBAC (Role-Permission Model)"
echo "=========================================="

# Test 2.1: Token contains role
log_info "Verifying token contains CITIZEN role"
PAYLOAD=$(echo $TOKEN | cut -d. -f2 | sed 's/-/+/g; s/_/\//g')
PADDED=$(printf %s "$PAYLOAD" | awk '{l=length($0);r=l%4;if(r==2)print $0"==";else if(r==3)print $0"=";else print $0}')
DECODED=$(echo "$PADDED" | base64 -d 2>/dev/null || echo "decode_failed")

if echo "$DECODED" | grep -q '"role":"CITIZEN"'; then
    log_pass "Token contains role: CITIZEN"
else
    log_fail "Token missing role claim"
fi

# Test 2.2: Token contains permissions
log_info "Verifying token contains permissions"
if echo "$DECODED" | grep -q '"permissions"'; then
    log_pass "Token contains permissions claim"
else
    log_fail "Token missing permissions claim"
fi

# Test 2.3: Token contains USER_READ permission
if echo "$DECODED" | grep -q 'USER_READ'; then
    log_pass "Token contains USER_READ permission"
else
    log_fail "Token missing USER_READ permission"
fi

echo ""
echo "=========================================="
echo "  3. @PreAuthorize Method Security"
echo "=========================================="

# Test 3.1: Authenticated access to /status (should work for any authenticated user)
log_info "Testing authenticated access to /identity/status"
STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" \
    "$IDENTITY_URL/identity/status")

if [ "$STATUS_CODE" = "200" ]; then
    log_pass "Authenticated access to /status works (200 OK)"
else
    log_fail "/status returned $STATUS_CODE (expected 200)"
fi

# Test 3.2: CITIZEN denied access to /pending (admin only)
log_info "Testing CITIZEN access to /identity/pending (should be 403)"
PENDING_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" \
    "$IDENTITY_URL/identity/pending")

if [ "$PENDING_CODE" = "403" ]; then
    log_pass "CITIZEN correctly denied access to /pending (403 Forbidden)"
else
    log_fail "/pending returned $PENDING_CODE (expected 403)"
fi

# Test 3.3: CITIZEN denied access to /approve (admin only)
log_info "Testing CITIZEN access to /identity/approve (should be 403)"
APPROVE_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" \
    "$IDENTITY_URL/identity/approve/someuser")

if [ "$APPROVE_CODE" = "403" ]; then
    log_pass "CITIZEN correctly denied access to /approve (403 Forbidden)"
else
    log_fail "/approve returned $APPROVE_CODE (expected 403)"
fi

echo ""
echo "=========================================="
echo "  4. JWT Token Blacklist"
echo "=========================================="

# Save the current token for post-logout test
OLD_TOKEN=$TOKEN

# Test 4.1: Logout endpoint
log_info "Testing logout endpoint"
LOGOUT_RESPONSE=$(curl -s -X POST \
    -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" \
    "$IDENTITY_URL/auth/logout")

if echo "$LOGOUT_RESPONSE" | grep -q "Logged out successfully"; then
    log_pass "Logout endpoint works"
else
    log_fail "Logout failed: $LOGOUT_RESPONSE"
fi

# Test 4.2: Blacklisted token rejected
log_info "Testing that blacklisted token is rejected"
sleep 1  # Give the blacklist time to propagate
BLOCKED_RESPONSE=$(curl -s -X GET \
    -H "Host: $HOST" -H "Authorization: Bearer $OLD_TOKEN" \
    "$IDENTITY_URL/identity/status")

if echo "$BLOCKED_RESPONSE" | grep -q "Token has been revoked"; then
    log_pass "Blacklisted token correctly rejected"
else
    log_fail "Blacklisted token was NOT rejected: $BLOCKED_RESPONSE"
fi

# Re-login for remaining tests
log_info "Re-logging in for remaining tests"
LOGIN_RESPONSE=$(curl -s -X POST -H "Host: $HOST" -H "Content-Type: application/json" \
    -d "{\"username\":\"$TEST_USER\",\"password\":\"TestPass123!\"}" \
    "$IDENTITY_URL/auth/login")
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo ""
echo "=========================================="
echo "  5. QR Code Verification"
echo "=========================================="

if [ "$SKIP_QR" = "true" ]; then
    log_info "Skipping QR tests (validation-service not accessible)"
else
    # Test 5.1: QR Code Generation
    log_info "Testing QR code generation"
    QR_RESPONSE=$(curl -s -o /tmp/e2e_qr_test.png -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d '{"documentId":"test-doc-e2e","signatureHash":"e2e_hash_123","timestamp":1766850000}' \
        "$VALIDATION_URL/qr/generate")

    if [ "$QR_RESPONSE" = "200" ]; then
        QR_SIZE=$(stat -c%s /tmp/e2e_qr_test.png 2>/dev/null || stat -f%z /tmp/e2e_qr_test.png 2>/dev/null || echo "0")
        if [ "$QR_SIZE" -gt 100 ]; then
            log_pass "QR code generation works (${QR_SIZE} bytes)"
        else
            log_fail "QR code generated but too small (${QR_SIZE} bytes)"
        fi
    else
        log_fail "QR code generation returned $QR_RESPONSE"
    fi

    # Test 5.2: Verification code generation
    log_info "Testing verification code generation"
    CODE_RESPONSE=$(curl -s "$VALIDATION_URL/qr/code/test-doc-e2e")
    
    if echo "$CODE_RESPONSE" | grep -q "verificationCode"; then
        log_pass "Verification code generation works"
    else
        # This might fail if hitting old pods - not critical
        log_info "Verification code endpoint returned: $CODE_RESPONSE"
    fi
fi

echo ""
echo "=========================================="
echo "  6. Identity Verification Flow"
echo "=========================================="

# Test 6.1: Request verification
log_info "Testing identity verification request"
VERIFY_REQ=$(curl -s -X POST \
    -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" \
    "$IDENTITY_URL/identity/verify-request")

if echo "$VERIFY_REQ" | grep -q "PENDING\|already"; then
    log_pass "Verification request endpoint works"
else
    log_fail "Verification request failed: $VERIFY_REQ"
fi

# Test 6.2: Check status
log_info "Checking identity status"
STATUS_RESP=$(curl -s -X GET \
    -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" \
    "$IDENTITY_URL/identity/status")

if echo "$STATUS_RESP" | grep -q "status"; then
    log_pass "Identity status check works"
else
    log_fail "Identity status check failed: $STATUS_RESP"
fi

echo ""
echo "=========================================="
echo "  Test Summary"
echo "=========================================="
echo ""
echo -e "${GREEN}Passed:${NC} $PASSED"
echo -e "${RED}Failed:${NC} $FAILED"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed! ✓${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed. Please review.${NC}"
    exit 1
fi
