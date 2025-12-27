#!/usr/bin/env python3
"""mTLS Certificate Initialization Script

Initializes Internal CA and issues service certificates via ca-authority API.
Saves certificates as PEM files for use as Kubernetes Secrets.
"""

import json
import os
import sys
import time
import urllib.request
import urllib.error

CA_URL = os.getenv("CA_URL", "http://ca-authority:8082/api/v1/ca")
CERT_DIR = os.getenv("CERT_DIR", "/certs")
SERVICES = [
    "api-gateway",
    "identity-service", 
    "org-service",
    "doc-service",
    "signature-core",
    "validation-service",
    "ca-authority"
]

def wait_for_ca(max_retries=30, delay=5):
    """Wait for ca-authority to be ready."""
    health_url = CA_URL.replace("/api/v1/ca", "/actuator/health")
    print(f"Waiting for ca-authority at {health_url}...")
    
    for i in range(max_retries):
        try:
            req = urllib.request.urlopen(health_url, timeout=5)
            if req.status == 200:
                print("ca-authority is ready!")
                return True
        except Exception:
            pass
        print(f"  Attempt {i+1}/{max_retries} - waiting {delay}s...")
        time.sleep(delay)
    
    print("ERROR: ca-authority not ready after max retries")
    return False

def post_json(url, data=None):
    """POST JSON to URL and return response as dict."""
    req = urllib.request.Request(
        url,
        data=json.dumps(data or {}).encode('utf-8'),
        headers={'Content-Type': 'application/json'},
        method='POST'
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode('utf-8'))

def save_pem(content, filepath):
    """Save PEM content to file, handling escaped newlines."""
    if content:
        # Replace escaped newlines with actual newlines
        pem = content.replace('\\n', '\n')
        with open(filepath, 'w') as f:
            f.write(pem)
        return os.path.getsize(filepath)
    return 0

def main():
    os.makedirs(CERT_DIR, exist_ok=True)
    
    if not wait_for_ca():
        sys.exit(1)
    
    # Step 1: Initialize Internal CA
    print("\n=== Step 1: Initialize Internal CA ===")
    try:
        result = post_json(f"{CA_URL}/internal/init")
        print(f"Internal CA: {result.get('name')} (ID: {result.get('id')})")
    except Exception as e:
        print(f"Internal CA initialization: {e}")
    
    # Step 2: Issue Service Certificates  
    print("\n=== Step 2: Issue Service Certificates ===")
    success, failed = 0, 0
    
    for svc in SERVICES:
        dns_names = [svc, f"{svc}.crypto-pqc.svc.cluster.local"]
        try:
            resp = post_json(f"{CA_URL}/internal/issue", {
                "serviceName": svc,
                "dnsNames": dns_names,
                "validDays": 365
            })
            
            cert_size = save_pem(resp.get('certificate'), f"{CERT_DIR}/{svc}.crt")
            key_size = save_pem(resp.get('privateKey'), f"{CERT_DIR}/{svc}.key")
            save_pem(resp.get('caCertificate'), f"{CERT_DIR}/ca.crt")
            
            if cert_size > 1000:
                print(f"  ✓ {svc} (cert: {cert_size}B, key: {key_size}B)")
                success += 1
            else:
                print(f"  ✗ {svc} (cert too small: {cert_size}B)")
                failed += 1
        except Exception as e:
            print(f"  ✗ {svc}: {e}")
            failed += 1
    
    print(f"\n=== Complete: {success} succeeded, {failed} failed ===")
    
    # List generated files
    print("\nGenerated files:")
    for f in sorted(os.listdir(CERT_DIR)):
        path = os.path.join(CERT_DIR, f)
        if os.path.isfile(path):
            print(f"  {f}: {os.path.getsize(path)} bytes")
    
    sys.exit(0 if failed == 0 else 1)

if __name__ == "__main__":
    main()
