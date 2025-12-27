#!/bin/bash
# Initialize SoftHSM token for cloud-sign service

set -e

TOKEN_LABEL="${HSM_TOKEN_LABEL:-gov-signing-token}"
SO_PIN="${HSM_SO_PIN:-12345678}"
USER_PIN="${HSM_USER_PIN:-87654321}"

echo "=== SoftHSM Initialization ==="
echo "Token Label: $TOKEN_LABEL"

# Check if token already exists
if softhsm2-util --show-slots 2>/dev/null | grep -q "Label: $TOKEN_LABEL"; then
    echo "Token '$TOKEN_LABEL' already exists, skipping initialization"
else
    echo "Initializing new token: $TOKEN_LABEL"
    softhsm2-util --init-token --slot 0 --label "$TOKEN_LABEL" --so-pin "$SO_PIN" --pin "$USER_PIN"
    echo "Token initialized successfully"
fi

# Show slot information
echo ""
echo "=== Available Slots ==="
softhsm2-util --show-slots

echo ""
echo "=== PKCS#11 Configuration ==="
echo "Library: /usr/lib/softhsm/libsofthsm2.so"
echo "Token Label: $TOKEN_LABEL"
echo "User PIN: [set via HSM_USER_PIN env var]"
echo ""
echo "SoftHSM ready for PKCS#11 operations"

# Execute the main command
exec "$@"
