#!/bin/bash
# Generate ML-DSA-65 mTLS Certificates for Internal Services
# Requires: OpenSSL 3.5+ with ML-DSA support
# Run this inside the ca_authority container or on a system with OpenSSL 3.5+

set -e

CERT_DIR="${1:-./certs/internal}"
mkdir -p "$CERT_DIR"

echo "=== Generating Internal Services CA (ML-DSA-65) ==="
# Generate ML-DSA-65 private key for Internal CA
openssl genpkey -algorithm mldsa65 -out "$CERT_DIR/internal-ca-key.pem"

# Create self-signed Internal CA certificate (5 years)
openssl req -new -x509 \
  -key "$CERT_DIR/internal-ca-key.pem" \
  -out "$CERT_DIR/internal-ca.pem" \
  -days 1825 \
  -subj "/CN=Internal Services CA/O=PQC Digital Signature System/C=VN"

echo "=== Generating Service Certificates (ML-DSA-65) ==="

SERVICES=("api-gateway" "identity-service" "cloud-sign" "ca-authority" "validation-service" "ra-service" "signature-core")

for svc in "${SERVICES[@]}"; do
  echo "Generating certificate for: $svc"
  
  # Generate ML-DSA-65 key pair
  openssl genpkey -algorithm mldsa65 -out "$CERT_DIR/${svc}-key.pem"
  
  # Generate CSR
  openssl req -new \
    -key "$CERT_DIR/${svc}-key.pem" \
    -out "$CERT_DIR/${svc}.csr" \
    -subj "/CN=${svc}.internal/O=PQC System"
  
  # Sign with Internal CA
  openssl x509 -req \
    -in "$CERT_DIR/${svc}.csr" \
    -CA "$CERT_DIR/internal-ca.pem" \
    -CAkey "$CERT_DIR/internal-ca-key.pem" \
    -out "$CERT_DIR/${svc}.pem" \
    -days 365 \
    -CAcreateserial
  
  # Create combined chain file (cert + CA)
  cat "$CERT_DIR/${svc}.pem" "$CERT_DIR/internal-ca.pem" > "$CERT_DIR/${svc}-chain.pem"
  
  # Clean up CSR
  rm -f "$CERT_DIR/${svc}.csr"
  
  echo "Done: $svc"
done

echo ""
echo "=== Certificate Generation Complete ==="
echo "Certificates generated in: $CERT_DIR"
echo ""
echo "Files generated:"
ls -la "$CERT_DIR"
echo ""
echo "To use with Spring Boot, configure application.yml:"
echo "  server.ssl.key-store: /app/certs/${svc}-key.pem"
echo "  server.ssl.certificate: /app/certs/${svc}.pem"
echo "  server.ssl.trust-certificate: /app/certs/internal-ca.pem"
