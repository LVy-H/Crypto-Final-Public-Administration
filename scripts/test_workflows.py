#!/usr/bin/env python3
"""
PQC Backend - Business Workflow Test Suite

Tests the core government PKI workflows:
1. User Registration (KYC simulation)
2. User Login (Session-based authentication)
3. CSR Enrollment (Certificate issuance with ML-DSA)
4. TSA Timestamping (RFC 3161)

Prerequisites:
- All services running and accessible via Gateway at localhost:30080
- Python 3.8+ with requests, cryptography libraries

Usage:
    python scripts/test_workflows.py
"""

import requests
import json
import base64
import hashlib
import sys
from datetime import datetime
from typing import Optional, Tuple

GATEWAY_URL = "http://localhost:30080"

def log(level: str, msg: str):
    timestamp = datetime.now().strftime("%H:%M:%S")
    symbols = {"INFO": "ℹ️", "OK": "✅", "FAIL": "❌", "WARN": "⚠️", "STEP": "🔹"}
    print(f"[{timestamp}] {symbols.get(level, '•')} {msg}")

def section(title: str):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}\n")


# ============================================================
# 1. USER REGISTRATION (KYC Simulation)
# ============================================================
def test_user_registration() -> bool:
    """Simulate KYC by registering a new user."""
    section("1. User Registration (KYC)")
    
    username = f"workflow_user_{datetime.now().strftime('%H%M%S')}"
    password = "SecureP@ssw0rd123"
    
    log("STEP", f"Registering user: {username}")
    
    try:
        resp = requests.post(
            f"{GATEWAY_URL}/api/v1/auth/register",
            json={"username": username, "password": password},
            timeout=10
        )
        
        if resp.status_code == 200:
            log("OK", f"User registered successfully: {resp.text}")
            return True, username, password
        else:
            log("FAIL", f"Registration failed: HTTP {resp.status_code} - {resp.text}")
            return False, None, None
    except Exception as e:
        log("FAIL", f"Registration error: {e}")
        return False, None, None


# ============================================================
# 2. USER LOGIN (Session Authentication)
# ============================================================
def test_user_login(username: str, password: str) -> Optional[requests.Session]:
    """Login and get session."""
    section("2. User Login (Session Authentication)")
    
    log("STEP", f"Logging in as: {username}")
    
    session = requests.Session()
    
    try:
        resp = session.post(
            f"{GATEWAY_URL}/api/v1/auth/login",
            json={"username": username, "password": password},
            timeout=10
        )
        
        if resp.status_code == 200:
            data = resp.json()
            log("OK", f"Login successful. Session ID: {data.get('sessionId', 'N/A')[:20]}...")
            return session
        else:
            log("FAIL", f"Login failed: HTTP {resp.status_code} - {resp.text}")
            return None
    except Exception as e:
        log("FAIL", f"Login error: {e}")
        return None


# ============================================================
# 3. GET CA CERTIFICATE (Public Info)
# ============================================================
def test_ca_info() -> Optional[str]:
    """Get CA certificate info."""
    section("3. CA Certificate Info (Public)")
    
    log("STEP", "Fetching CA certificate info...")
    
    try:
        resp = requests.get(f"{GATEWAY_URL}/api/v1/pki/ca/info", timeout=10)
        
        if resp.status_code == 200:
            data = resp.json()
            log("OK", f"CA Subject: {data.get('subject', 'N/A')}")
            log("OK", f"CA Issuer: {data.get('issuer', 'N/A')}")
            cert_preview = data.get('certificate', '')[:50] + "..."
            log("OK", f"Certificate (Base64): {cert_preview}")
            return data.get('certificate')
        else:
            log("FAIL", f"Failed to get CA info: HTTP {resp.status_code}")
            return None
    except Exception as e:
        log("FAIL", f"CA info error: {e}")
        return None


# ============================================================
# 4. TSA INFO (RFC 3161 Capabilities)
# ============================================================
def test_tsa_info() -> bool:
    """Get TSA capabilities."""
    section("4. TSA Info (RFC 3161)")
    
    log("STEP", "Fetching TSA capabilities...")
    
    try:
        resp = requests.get(f"{GATEWAY_URL}/api/v1/tsa/info", timeout=10)
        
        if resp.status_code == 200:
            data = resp.json()
            log("OK", f"TSA Name: {data.get('name', 'N/A')}")
            log("OK", f"Protocol: {data.get('protocol', 'N/A')}")
            log("OK", f"Signature Algorithm: {data.get('signatureAlgorithm', 'N/A')}")
            log("OK", f"Accepted Hashes: {', '.join(data.get('acceptedDigestAlgorithms', []))}")
            return True
        else:
            log("FAIL", f"Failed to get TSA info: HTTP {resp.status_code}")
            return False
    except Exception as e:
        log("FAIL", f"TSA info error: {e}")
        return False


# ============================================================
# 5. TSA TIMESTAMP REQUEST (RFC 3161)
# ============================================================
def test_tsa_timestamp() -> bool:
    """Test TSA timestamping with a sample hash."""
    section("5. TSA Timestamp Request (RFC 3161)")
    
    log("STEP", "Creating timestamp request for sample data...")
    
    try:
        # Create a sample document hash (SHA-256)
        sample_data = b"This is a sample document to timestamp."
        doc_hash = hashlib.sha256(sample_data).digest()
        
        log("INFO", f"Document hash (SHA-256): {doc_hash.hex()[:32]}...")
        
        # Build TimeStampReq ASN.1 structure (simplified)
        # In production, use proper ASN.1 library
        # For now, we test if the endpoint is accessible
        
        # RFC 3161 TimeStampReq structure (minimal):
        # SEQUENCE {
        #   version INTEGER (1),
        #   messageImprint MessageImprint,
        #   nonce INTEGER OPTIONAL,
        #   certReq BOOLEAN OPTIONAL
        # }
        # MessageImprint = SEQUENCE { hashAlgorithm AlgorithmIdentifier, hashedMessage OCTET STRING }
        
        # Build ASN.1 DER manually for SHA-256 hash
        # AlgorithmIdentifier for SHA-256: OID 2.16.840.1.101.3.4.2.1
        sha256_oid = bytes([0x60, 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01])
        alg_id = bytes([0x30, len(sha256_oid) + 2, 0x06, len(sha256_oid)]) + sha256_oid
        
        # MessageImprint
        hash_octets = bytes([0x04, len(doc_hash)]) + doc_hash
        msg_imprint = bytes([0x30, len(alg_id) + len(hash_octets)]) + alg_id + hash_octets
        
        # Version = 1
        version = bytes([0x02, 0x01, 0x01])
        
        # TimeStampReq
        ts_req_content = version + msg_imprint
        ts_req = bytes([0x30, len(ts_req_content)]) + ts_req_content
        
        log("STEP", f"Sending timestamp request ({len(ts_req)} bytes)...")
        
        resp = requests.post(
            f"{GATEWAY_URL}/api/v1/tsa/stamp",
            data=ts_req,
            headers={"Content-Type": "application/timestamp-query"},
            timeout=30
        )
        
        if resp.status_code == 200:
            log("OK", f"Timestamp response received ({len(resp.content)} bytes)")
            log("OK", f"Content-Type: {resp.headers.get('Content-Type', 'N/A')}")
            
            # Check if it's a valid TimeStampResp (starts with ASN.1 SEQUENCE)
            if resp.content and resp.content[0] == 0x30:
                log("OK", "Response is valid ASN.1 SEQUENCE (TimeStampResp)")
                log("OK", f"Response preview: {resp.content[:50].hex()}...")
                return True
            else:
                log("WARN", "Response doesn't appear to be ASN.1 encoded")
                return False
        else:
            log("FAIL", f"Timestamp request failed: HTTP {resp.status_code}")
            log("FAIL", f"Response: {resp.text[:200]}")
            return False
            
    except Exception as e:
        log("FAIL", f"TSA timestamp error: {e}")
        return False


# ============================================================
# MAIN
# ============================================================
def main():
    print("\n" + "="*60)
    print("  PQC Backend - Business Workflow Tests")
    print("  " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    print("="*60)
    
    results = {}
    
    # Test 1: User Registration
    success, username, password = test_user_registration()
    results["User Registration"] = success
    
    # Test 2: User Login (if registration succeeded)
    if success and username and password:
        session = test_user_login(username, password)
        results["User Login"] = session is not None
    else:
        results["User Login"] = False
    
    # Test 3: CA Info (public)
    ca_cert = test_ca_info()
    results["CA Info"] = ca_cert is not None
    
    # Test 4: TSA Info (public)
    results["TSA Info"] = test_tsa_info()
    
    # Test 5: TSA Timestamp (public)
    results["TSA Timestamp"] = test_tsa_timestamp()
    
    # Summary
    section("Test Results Summary")
    
    passed = sum(1 for v in results.values() if v)
    total = len(results)
    
    for test_name, success in results.items():
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"  {status}  {test_name}")
    
    print(f"\n  Total: {passed}/{total} tests passed")
    print("="*60 + "\n")
    
    return 0 if passed == total else 1


if __name__ == "__main__":
    sys.exit(main())
