#!/usr/bin/env python3
"""
Offline Root CA Setup Script
Uses standard library only (no requests dependency)
"""

import urllib.request
import urllib.error
import json
import subprocess
import os
import ssl

API_BASE = "https://api.gov-id.lvh.id.vn/api/v1"
OFFLINE_DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "offline_data")

# Disable SSL verification for local dev
ssl_context = ssl.create_default_context()
ssl_context.check_hostname = False
ssl_context.verify_mode = ssl.CERT_NONE

def http_request(method, url, data=None, headers=None):
    """Make HTTP request using urllib"""
    if headers is None:
        headers = {}
    headers["Content-Type"] = "application/json"
    
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    
    try:
        with urllib.request.urlopen(req, timeout=30, context=ssl_context) as resp:
            # Return status, body, and headers
            response_headers = dict(resp.headers)
            return resp.status, json.loads(resp.read().decode()), response_headers
    except urllib.error.HTTPError as e:
        try:
            body = json.loads(e.read().decode())
        except:
            body = str(e)
        return e.code, body, {}
    except urllib.error.URLError as e:
        return 0, {"error": str(e.reason)}, {}

def login(username, password):
    print(f"Logging in as {username}...")
    status, resp, headers = http_request("POST", f"{API_BASE}/auth/login", {"username": username, "password": password})
    if status != 200:
        raise Exception(f"Login failed ({status}): {resp}")
    
    # Token is in X-Auth-Token header
    token = headers.get("X-Auth-Token")
    if not token and "Authorization" in headers:
        token = headers["Authorization"].replace("Bearer ", "")
        
    if not token:
        # Fallback to body just in case
        token = resp.get("token")
        
    if not token:
        print("Headers received:", headers.keys())
        raise Exception(f"No token in response headers or body: {resp}")
        
    print("Login successful")
    return token

def generate_csr(token, name, algorithm):
    print(f"Requesting CSR for {name} ({algorithm})...")
    headers = {"Authorization": f"Bearer {token}", "X-Auth-Token": token, "X-TOTP-Code": "000000"}
    status, resp, _ = http_request("POST", f"{API_BASE}/ca/init-csr", {"name": name, "algorithm": algorithm}, headers)
    if status != 200:
        raise Exception(f"CSR Generation failed ({status}): {resp}")
    
    print(f"CSR Generated. Pending ID: {resp.get('pendingCaId')}")
    return resp

def run_offline_signer():
    print("Running Offline Root Signer (Java Test)...")
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
    cmd = [
        "./gradlew", 
        ":core:ca-authority:test", 
        "--tests", "com.gov.crypto.caauthority.service.OfflineRootSignerTest",
        "--info"
    ]
    
    process = subprocess.run(cmd, cwd=root_dir, capture_output=True, text=True)
    
    if process.returncode != 0:
        print("Signer Failed:")
        print(process.stdout[-2000:] if len(process.stdout) > 2000 else process.stdout)
        print(process.stderr[-1000:] if len(process.stderr) > 1000 else process.stderr)
        raise Exception("Offline Signer failed")
    
    print("Signer finished successfully.")

def upload_cert(token, pending_id, cert_pem, root_cert_pem):
    print(f"Uploading Signed Cert for {pending_id}...")
    headers = {"Authorization": f"Bearer {token}", "X-TOTP-Code": "000000"}
    payload = {
        "pendingCaId": pending_id,
        "certificatePem": cert_pem,
        "nationalRootCertPem": root_cert_pem
    }
    status, resp, _ = http_request("POST", f"{API_BASE}/ca/upload-cert", payload, headers)
    if status != 200:
        raise Exception(f"Upload failed ({status}): {resp}")
    
    print("Upload Successful. CA Activated:")
    print(json.dumps(resp, indent=2))

def main():
    if not os.path.exists(OFFLINE_DATA_DIR):
        os.makedirs(OFFLINE_DATA_DIR)

    token = login("admin_capture", "SecurePass123!")
    
    # 1. Get CSR
    csr_data = generate_csr(token, "National Issuing CA 1", "mldsa65")
    csr_pem = csr_data["csrPem"]
    pending_id = csr_data["pendingCaId"]
    
    # Save CSR
    csr_path = os.path.join(OFFLINE_DATA_DIR, "csr.pem")
    with open(csr_path, "w") as f:
        f.write(csr_pem)
    print(f"CSR saved to {csr_path}")
        
    # 2. Sign (Java)
    run_offline_signer()
    
    # 3. Read Certs
    with open(os.path.join(OFFLINE_DATA_DIR, "issuing_cert.pem"), "r") as f:
        cert_pem = f.read()
        
    with open(os.path.join(OFFLINE_DATA_DIR, "root_cert.pem"), "r") as f:
        root_cert_pem = f.read()

    # 4. Upload
    upload_cert(token, pending_id, cert_pem, root_cert_pem)
    print("\n✅ Offline CA Setup Complete!")

if __name__ == "__main__":
    main()
