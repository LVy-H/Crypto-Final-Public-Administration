#!/bin/sh
set -e

# Run PqcBootstrap
# Usage: pqc-bootstrap --ca-key <path> --ca-cert <path> --out-dir <path> --dns <dns> --ip <ip>

DNS_NAME=${POD_DNS_NAME:-"localhost"}
IP_ADDR=${POD_IP:-"127.0.0.1"}

echo "Bootstrapping mTLS for $DNS_NAME ($IP_ADDR)"

# Run Java with classpath
java -cp "/app/app.jar:/app/libs/*" com.gov.crypto.common.tool.PqcBootstrap \
  --ca-key /var/run/secrets/infra-ca/infra.key \
  --ca-cert /var/run/secrets/infra-ca/infra.crt \
  --out-dir /app/certs \
  --dns "$DNS_NAME" \
  --ip "$IP_ADDR"
