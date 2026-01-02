#!/bin/bash
set -e
echo "1. Truncating certificate_authorities table..."
kubectl -n crypto-pqc exec postgres-0 -- psql -U admin -d crypto_db -c "TRUNCATE certificate_authorities CASCADE;"

echo "2. Clearing CA storage..."
POD_NAME=$(kubectl -n crypto-pqc get pods -l app=ca-authority -o jsonpath='{.items[0].metadata.name}')
if [ -n "$POD_NAME" ]; then
    kubectl -n crypto-pqc exec "$POD_NAME" -- rm -rf /secure/ca/root-ca-key.pem /secure/ca/root-ca-cert.pem /secure/ca/failed-keys /secure/ca/*-key.pem /secure/ca/*-cert.pem
else
    echo "Warning: ca-authority pod not found"
fi

echo "3. Reset Complete."
