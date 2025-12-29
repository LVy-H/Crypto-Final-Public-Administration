#!/usr/bin/env python3
"""
API Test Script for PQC Digital Signature Platform
Tests deployed Kubernetes services via port-forwarded API gateway
"""

import urllib.request
import urllib.error
import json
import time

API_BASE = "http://localhost:8080"
SESSION_COOKIE = None

def make_request(method, endpoint, data=None, headers=None):
    """Make HTTP request and return response"""
    url = f"{API_BASE}{endpoint}"
    if headers is None:
        headers = {"Content-Type": "application/json"}
    if SESSION_COOKIE:
        headers["Cookie"] = SESSION_COOKIE
    
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            resp_headers = dict(response.headers)
            body = response.read().decode()
            return {
                "status": response.status,
                "headers": resp_headers,
                "body": json.loads(body) if body else None
            }
    except urllib.error.HTTPError as e:
        return {
            "status": e.code,
            "headers": dict(e.headers) if e.headers else {},
            "body": e.read().decode() if e.fp else None,
            "error": str(e)
        }
    except Exception as e:
        return {"status": 0, "error": str(e)}

def test_health():
    """Test health endpoints"""
    print("\n=== Testing Health Endpoints ===")
    
    endpoints = [
        "/api/v1/auth/health",
        "/api/v1/ca/health", 
        "/api/v1/validation/health"
    ]
    
    results = []
    for endpoint in endpoints:
        resp = make_request("GET", endpoint)
        status = "✅" if resp.get("status") == 200 else "❌"
        print(f"  {status} {endpoint}: {resp.get('status')}")
        results.append(resp.get("status") == 200)
    
    return all(results)

def test_registration():
    """Test user registration"""
    print("\n=== Testing User Registration ===")
    
    user_data = {
        "username": f"testuser_{int(time.time())}",
        "password": "TestPass123!",
        "email": f"test_{int(time.time())}@example.com",
        "fullName": "Test User",
        "nationalId": f"ID{int(time.time())}"
    }
    
    resp = make_request("POST", "/api/v1/auth/register", user_data)
    if resp.get("status") in [200, 201]:
        print(f"  ✅ Registration successful: {user_data['username']}")
        return user_data
    else:
        print(f"  ❌ Registration failed: {resp.get('status')} - {resp.get('body')}")
        return None

def test_login(username, password):
    """Test user login"""
    global SESSION_COOKIE
    print("\n=== Testing Login ===")
    
    login_data = {"username": username, "password": password}
    resp = make_request("POST", "/api/v1/auth/login", login_data)
    
    if resp.get("status") == 200:
        # Extract session cookie
        set_cookie = resp.get("headers", {}).get("Set-Cookie", "")
        if set_cookie:
            SESSION_COOKIE = set_cookie.split(";")[0]
        print(f"  ✅ Login successful for {username}")
        print(f"     Session: {SESSION_COOKIE[:50]}..." if SESSION_COOKIE else "     No session cookie")
        return True
    else:
        print(f"  ❌ Login failed: {resp.get('status')} - {resp.get('body')}")
        return False

def test_ca_authority():
    """Test CA Authority endpoints"""
    print("\n=== Testing CA Authority ===")
    
    # List CAs
    resp = make_request("GET", "/api/v1/ca/authority/list")
    if resp.get("status") == 200:
        cas = resp.get("body", [])
        print(f"  ✅ CA list retrieved: {len(cas)} CAs found")
        return True
    else:
        print(f"  ❌ CA list failed: {resp.get('status')} - {resp.get('body')}")
        return False

def test_certificate_request():
    """Test certificate request (requires authentication)"""
    print("\n=== Testing Certificate Request ===")
    
    if not SESSION_COOKIE:
        print("  ⚠️  Skipping - not authenticated")
        return False
    
    cert_data = {
        "subjectCn": "Test User",
        "subjectOrg": "Test Organization",
        "keyAlgorithm": "ML-DSA-65"
    }
    
    resp = make_request("POST", "/api/v1/certificates/request", cert_data)
    if resp.get("status") in [200, 201, 202]:
        print(f"  ✅ Certificate request submitted")
        return resp.get("body")
    else:
        print(f"  ❌ Certificate request failed: {resp.get('status')} - {resp.get('body')}")
        return None

def test_validation_service():
    """Test validation service"""
    print("\n=== Testing Validation Service ===")
    
    # Test verify endpoint format
    sample_verify = {
        "documentHash": "test_hash_placeholder",
        "signature": "test_sig_placeholder",
        "certificatePem": "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----"
    }
    
    resp = make_request("POST", "/api/v1/validation/verify", sample_verify)
    # We expect this to fail with 400 (bad input) not 500 (server error)
    if resp.get("status") in [200, 400]:
        print(f"  ✅ Validation endpoint responding: {resp.get('status')}")
        return True
    else:
        print(f"  ❌ Validation endpoint error: {resp.get('status')} - {resp.get('body')}")
        return False

def main():
    print("=" * 50)
    print("PQC Digital Signature Platform - API Test Suite")
    print("=" * 50)
    print(f"Target: {API_BASE}")
    
    # Wait for port-forward to be ready
    print("\nWaiting for API gateway...")
    time.sleep(2)
    
    results = {}
    
    # Health checks
    results["health"] = test_health()
    
    # Registration
    user = test_registration()
    results["registration"] = user is not None
    
    # Login
    if user:
        results["login"] = test_login(user["username"], user["password"])
    else:
        results["login"] = False
    
    # CA Authority
    results["ca_authority"] = test_ca_authority()
    
    # Certificate request
    results["cert_request"] = test_certificate_request() is not None
    
    # Validation
    results["validation"] = test_validation_service()
    
    # Summary
    print("\n" + "=" * 50)
    print("TEST SUMMARY")
    print("=" * 50)
    passed = sum(1 for v in results.values() if v)
    total = len(results)
    print(f"Passed: {passed}/{total}")
    for test, result in results.items():
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"  {status}: {test}")
    
    return passed == total

if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)
