#!/bin/bash
# Initialize TSA (Timestamp Authority) for development

set -e

TSA_DIR="/tsa"
CERTS_DIR="$TSA_DIR/certs"
DATA_DIR="$TSA_DIR/data"

echo "=== TSA Initialization ==="

# Create required files if not exist
mkdir -p "$CERTS_DIR" "$DATA_DIR"
touch "$DATA_DIR/index.txt"
echo "01" > "$DATA_DIR/serial"
echo "01" > "$DATA_DIR/tsa_serial"

# Generate TSA key and certificate if not exist
if [ ! -f "$CERTS_DIR/tsa.key" ]; then
    echo "Generating TSA key pair..."
    openssl genrsa -out "$CERTS_DIR/tsa.key" 4096
    
    echo "Generating TSA certificate..."
    openssl req -new -x509 -days 365 \
        -key "$CERTS_DIR/tsa.key" \
        -out "$CERTS_DIR/tsa.crt" \
        -config "$TSA_DIR/tsa.cnf" \
        -extensions tsa_section
    
    echo "TSA certificate generated"
fi

echo ""
echo "=== TSA Configuration ==="
echo "Certificate: $CERTS_DIR/tsa.crt"
echo "Private Key: $CERTS_DIR/tsa.key"
echo "Port: 8318"
echo ""
echo "Usage: Send timestamp requests to http://localhost:8318/tsa"
echo ""
echo "TSA ready for timestamp operations"

# Execute the main command
exec "$@"
