#!/bin/bash
set -e
HOST="api.crypto.gov.vn"
URL="http://localhost:8080/api/v1"
USER="test_bl_$(date +%s)"

echo "Testing with USER=$USER"

echo "1. Registering..."
curl -s -X POST -H "Host: $HOST" -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"password\",\"email\":\"$USER@example.com\"}" \
  "$URL/auth/register" | jq .

echo "2. Logging in..."
TOKEN=$(curl -s -X POST -H "Host: $HOST" -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"password\"}" \
  "$URL/auth/login" | jq -r .token)

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "Failed to get token"
  exit 1
fi
echo "Got Token: ${TOKEN:0:10}..."

echo "3. Verifying Access (Expect 200)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" "$URL/identity/status")
echo "Code: $HTTP_CODE"
if [ "$HTTP_CODE" != "200" ]; then
  echo "Access failed before logout!"
  exit 1
fi

echo "4. Logging out..."
LOGOUT_RESP=$(curl -s -X POST -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" "$URL/auth/logout")
echo "Logout Resp: $LOGOUT_RESP"

echo "5. Verifying Blacklist (Expect 401)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" "$URL/identity/status")
echo "Code: $HTTP_CODE"

# Also check response body for error message
RESP=$(curl -s -H "Host: $HOST" -H "Authorization: Bearer $TOKEN" "$URL/identity/status")
echo "Response: $RESP"

if [ "$HTTP_CODE" == "401" ]; then
  echo "SUCCESS: Token blacklisted."
else
  echo "FAILURE: Token still valid (Code $HTTP_CODE)."
  exit 1
fi
