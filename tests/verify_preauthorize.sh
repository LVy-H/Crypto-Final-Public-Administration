#!/bin/bash
set -e
HOST="api.crypto.gov.vn"
URL="http://localhost:8080/api/v1"
USER="preauth_test_$(date +%s)"

echo "Testing @PreAuthorize with USER=$USER"

# Ensure port-forward is running
if ! nc -z localhost 8080 2>/dev/null; then
  echo "Port-forward not active. Please run: kubectl port-forward -n crypto-pqc svc/api-gateway 8080:8080"
  exit 1
fi

echo "1. Registering CITIZEN user..."
REG_RESPONSE=$(curl -s -X POST -H "Host: $HOST" -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"password\",\"email\":\"$USER@example.com\"}" \
  "$URL/auth/register")
echo "$REG_RESPONSE" | jq .

echo "2. Logging in..."
TOKEN=$(curl -s -X POST -H "Host: $HOST" -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"password\"}" \
  "$URL/auth/login" | jq -r .token)

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "Failed to get token"
  exit 1
fi
echo "Got Token."

echo "3. Testing /pending endpoint (should return 403 Forbidden for CITIZEN)..."
PENDING_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
  -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" \
  "$URL/identity/pending")

echo "Pending Endpoint Status: $PENDING_STATUS"

if [ "$PENDING_STATUS" = "403" ]; then
  echo "SUCCESS: CITIZEN correctly denied access to /pending (403 Forbidden)"
else
  echo "FAILURE: Expected 403 Forbidden, got $PENDING_STATUS"
  exit 1
fi

echo "4. Testing /approve/{username} endpoint (should return 403 Forbidden for CITIZEN)..."
APPROVE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" \
  "$URL/identity/approve/someuser")

echo "Approve Endpoint Status: $APPROVE_STATUS"

if [ "$APPROVE_STATUS" = "403" ]; then
  echo "SUCCESS: CITIZEN correctly denied access to /approve (403 Forbidden)"
else
  echo "FAILURE: Expected 403 Forbidden, got $APPROVE_STATUS"
  exit 1
fi

echo ""
echo "@PreAuthorize Verification Passed! CITIZEN users are correctly blocked from admin endpoints."
