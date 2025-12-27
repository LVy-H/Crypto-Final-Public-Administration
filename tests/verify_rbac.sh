#!/bin/bash
set -e
HOST="api.crypto.gov.vn"
URL="http://localhost:8080/api/v1"
USER="rbac_test_$(date +%s)"

echo "Testing RBAC with USER=$USER"

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
echo "Got Token."

echo "3. Decoding Token Constraints..."
# Extract payload (2nd part), fix padding, base64 decode
PAYLOAD=$(echo $TOKEN | cut -d. -f2 | sed 's/-/+/g; s/_/\//g')
PADDED_PAYLOAD=$(printf %s "$PAYLOAD" | awk '{l=length($0);r=l%4;if(r==2)print $0"==";else if(r==3)print $0"=";else print $0}')
DECODED=$(echo "$PADDED_PAYLOAD" | base64 -d 2>/dev/null || echo "Base64 decode failed")

echo "Token Payload: $DECODED"

# Check for Role and Permissions
if echo "$DECODED" | grep -q "CITIZEN"; then
  echo "SUCCESS: Role 'CITIZEN' found in token."
else
  echo "FAILURE: Role 'CITIZEN' NOT found."
  exit 1
fi

if echo "$DECODED" | grep -q "USER_READ"; then
  echo "SUCCESS: Permission 'USER_READ' found in token."
else
  echo "FAILURE: Permission 'USER_READ' NOT found."
  exit 1
fi

echo "RBAC Verification Passed."
