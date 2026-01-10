#!/bin/bash
set -e

CLI_JAR="backend/offline-ca-cli/build/libs/offline-ca-cli-0.0.1-SNAPSHOT.jar"
SECRETS_DIR="infra/k3s/secrets"
mkdir -p "$SECRETS_DIR"

echo "🔐 Generating PKI Infrastructure..."

# 1. Build CLI if needed
if [ ! -f "$CLI_JAR" ]; then
    echo "🔨 Building CA CLI..."
    ./gradlew :offline-ca-cli:build -x test
fi

# 2. Init Root CA (if missing)
if [ ! -f "root-ca.key" ]; then
    echo "🌱 Generating Offline Root CA..."
    java -jar "$CLI_JAR" init-root
else
    echo "✅ Root CA exists."
fi

# 3. Issue Certificates

# A. PKI Service (Intermediate CA)
if [ ! -f "$SECRETS_DIR/pki-ca.key" ]; then
    echo "📜 Issuing PKI Service Intermediate CA..."
    java -jar "$CLI_JAR" issue-cert \
        --subject-dn "CN=PKI Service CA, O=Gov Crypto, C=VN" \
        --output-prefix "$SECRETS_DIR/pki-ca" \
        --is-ca
fi

# B. TSA Service (Leaf / Timestamping)
if [ ! -f "$SECRETS_DIR/tsa-service.key" ]; then
    echo "📜 Issuing TSA Service Key..."
    java -jar "$CLI_JAR" issue-cert \
        --subject-dn "CN=TSA Service, O=Gov Crypto, C=VN" \
        --output-prefix "$SECRETS_DIR/tsa-service"
fi

# C. API Gateway (TLS - For future use, currently placeholder or JWT signing?)
# Assuming Gateway uses Standard TLS (RSA/ECDSA) for Browser compatibility? 
# Or if this is PQC Gateway... 
# For now, let's just generate a key for it if needed, or skip.
# Let's skip Gateway PQC cert for now unless strictly required, as Browsers don't support it yet.

# 4. Generate Kubernetes Secrets YAML
echo "📦 Generating K3s Secrets Manifest..."

# Helper to base64 encode
b64() {
    base64 -w 0 < "$1"
}

cat > "$SECRETS_DIR/pki-secrets.yaml" <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: pki-ca-keypair
  namespace: crypto-backend
type: Opaque
data:
  tls.key: $(b64 "$SECRETS_DIR/pki-ca.key")
  tls.crt: $(b64 "$SECRETS_DIR/pki-ca.crt")
  root.crt: $(b64 "root-ca.crt")
---
apiVersion: v1
kind: Secret
metadata:
  name: tsa-keypair
  namespace: crypto-backend
type: Opaque
data:
  tls.key: $(b64 "$SECRETS_DIR/tsa-service.key")
  tls.crt: $(b64 "$SECRETS_DIR/tsa-service.crt")
  root.crt: $(b64 "root-ca.crt")
EOF

echo "✅ PKI Setup Complete. Secrets at $SECRETS_DIR/pki-secrets.yaml"
