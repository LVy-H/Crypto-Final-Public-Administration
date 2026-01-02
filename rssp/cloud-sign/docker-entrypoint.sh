#!/bin/bash
set -e

TOKEN_LABEL="${HSM_TOKEN_LABEL:-user-signing-token}"
SO_PIN="${HSM_SO_PIN:-12345678}"
USER_PIN="${HSM_USER_PIN:-87654321}"

echo "=== SoftHSM Initialization ==="
echo "Token Label: $TOKEN_LABEL"

# Check if token already exists
if softhsm2-util --show-slots 2>/dev/null | grep -q "Label: $TOKEN_LABEL"; then
    echo "Token '$TOKEN_LABEL' already exists"
else
    echo "Initializing new token: $TOKEN_LABEL"
    softhsm2-util --init-token --slot 0 --label "$TOKEN_LABEL" --so-pin "$SO_PIN" --pin "$USER_PIN"
    echo "Token initialized successfully"
fi

# Show slot information for debugging
echo ""
echo "=== Available Slots ==="
softhsm2-util --show-slots

echo ""
echo "=== Starting Cloud Sign Service ==="
exec java -jar /app/app.jar "$@"
