#!/usr/bin/env python3
"""
Full User Flow API Test
Tests the complete workflow: register → login → request certificate
"""

import urllib.request
import urllib.error
import json
import time
import ssl

API_BASE = "https://api.gov-id.lvh.id.vn"

# Disable SSL verification for local testing
ssl_context = ssl.create_default_context()
ssl_context.check_hostname = False
ssl_context.verify_mode = ssl.CERT_NONE

class APIClient:
    def __init__(self):
        self.session_cookie = None
    
    def request(self, method, endpoint, data=None):
        """Make HTTP request and return response"""
        url = f"{API_BASE}{endpoint}"
        headers = {"Content-Type": "application/json"}
        if self.session_cookie:
            headers["Cookie"] = self.session_cookie
        
        body = json.dumps(data).encode() if data else None
        req = urllib.request.Request(url, data=body, headers=headers, method=method)
        
        try:
            with urllib.request.urlopen(req, timeout=30, context=ssl_context) as response:
                resp_headers = dict(response.headers)
                body = response.read().decode()
                
                # Extract session cookie
                set_cookie = resp_headers.get("Set-Cookie", "")
                if set_cookie and "SESSION=" in set_cookie:
                    self.session_cookie = set_cookie.split(";")[0]
                
                return {
                    "status": response.status,
                    "headers": resp_headers,
                    "body": json.loads(body) if body else None
                }
        except urllib.error.HTTPError as e:
            body_content = e.read().decode() if e.fp else None
            try:
                body_json = json.loads(body_content) if body_content else None
            except:
                body_json = body_content
            return {
                "status": e.code,
                "headers": dict(e.headers) if e.headers else {},
                "body": body_json,
                "error": str(e)
            }
        except Exception as e:
            return {"status": 0, "error": str(e)}

def main():
    print("=" * 60)
    print("FULL USER FLOW API TEST")
    print("=" * 60)
    print(f"Target: {API_BASE}")
    print()
    
    client = APIClient()
    timestamp = int(time.time())
    
    # ========================================
    # STEP 1: REGISTER
    # ========================================
    print("STEP 1: REGISTER NEW USER")
    print("-" * 40)
    
    username = f"flowtest_{timestamp}"
    password = "TestPass123!"
    email = f"flowtest_{timestamp}@example.com"
    
    register_data = {
        "username": username,
        "password": password,
        "email": email,
        "fullName": "Flow Test User",
        "nationalId": f"ID{timestamp}"
    }
    
    print(f"  Registering: {username}")
    resp = client.request("POST", "/api/v1/auth/register", register_data)
    
    if resp.get("status") in [200, 201]:
        print(f"  ✅ Registration successful")
        print(f"     Response: {json.dumps(resp.get('body'), indent=6)[:200]}...")
    else:
        print(f"  ❌ Registration failed: {resp.get('status')}")
        print(f"     Error: {resp.get('body')}")
        return False
    
    print()
    
    # ========================================
    # STEP 2: LOGIN
    # ========================================
    print("STEP 2: LOGIN")
    print("-" * 40)
    
    login_data = {"username": username, "password": password}
    print(f"  Logging in as: {username}")
    resp = client.request("POST", "/api/v1/auth/login", login_data)
    
    if resp.get("status") == 200:
        print(f"  ✅ Login successful")
        print(f"     Session: {client.session_cookie[:50]}..." if client.session_cookie else "     No session")
        if resp.get("body"):
            user_info = resp.get("body")
            print(f"     User: {user_info.get('username', 'N/A')}")
    else:
        print(f"  ❌ Login failed: {resp.get('status')}")
        print(f"     Error: {resp.get('body')}")
        return False
    
    print()
    
    # ========================================
    # STEP 3: GET USER PROFILE
    # ========================================
    print("STEP 3: GET USER PROFILE")
    print("-" * 40)
    
    resp = client.request("GET", "/api/v1/identity/me")
    
    if resp.get("status") == 200:
        print(f"  ✅ Profile retrieved")
        if resp.get("body"):
            profile = resp.get("body")
            print(f"     Username: {profile.get('username', 'N/A')}")
            print(f"     Email: {profile.get('email', 'N/A')}")
            print(f"     Verified: {profile.get('identityVerified', 'N/A')}")
    elif resp.get("status") == 404:
        print(f"  ⚠️  Profile endpoint not available (404)")
    else:
        print(f"  ⚠️  Profile request returned: {resp.get('status')}")
    
    print()
    
    # ========================================
    # STEP 4: LIST CERTIFICATES (should be empty)
    # ========================================
    print("STEP 4: LIST CERTIFICATES")
    print("-" * 40)
    
    resp = client.request("GET", "/api/v1/certificates")
    
    if resp.get("status") == 200:
        certs = resp.get("body", [])
        print(f"  ✅ Certificate list retrieved")
        print(f"     Count: {len(certs)}")
    elif resp.get("status") == 404:
        print(f"  ⚠️  Certificates endpoint not available (404)")
    else:
        print(f"  ⚠️  Certificates request returned: {resp.get('status')}")
        print(f"     Body: {resp.get('body')}")
    
    print()
    
    # ========================================
    # STEP 5: REQUEST CERTIFICATE
    # ========================================
    print("STEP 5: REQUEST CERTIFICATE")
    print("-" * 40)
    
    cert_request = {
        "subjectCn": "Flow Test User",
        "subjectOrg": "Test Organization",
        "keyAlgorithm": "ML-DSA-65"
    }
    
    print(f"  Requesting certificate with ML-DSA-65...")
    resp = client.request("POST", "/api/v1/certificates/request", cert_request)
    
    if resp.get("status") in [200, 201, 202]:
        print(f"  ✅ Certificate request submitted")
        if resp.get("body"):
            print(f"     Response: {json.dumps(resp.get('body'), indent=6)[:300]}...")
    elif resp.get("status") == 404:
        print(f"  ⚠️  Certificate request endpoint not available (404)")
    else:
        print(f"  ⚠️  Certificate request returned: {resp.get('status')}")
        print(f"     Body: {resp.get('body')}")
    
    print()
    
    # ========================================
    # STEP 6: TEST CA CSR GENERATION
    # ========================================
    print("STEP 6: TEST CA CSR GENERATION")
    print("-" * 40)
    
    csr_request = {
        "name": f"TestCA_{timestamp}",
        "algorithm": "mldsa87"
    }
    
    print(f"  Generating CSR for subordinate CA...")
    resp = client.request("POST", "/api/v1/ca/init-csr", csr_request)
    
    if resp.get("status") == 200:
        body = resp.get("body", {})
        if isinstance(body, dict):
            csr = body.get("csr", "")
            if "BEGIN CERTIFICATE REQUEST" in csr:
                print(f"  ✅ CSR generated successfully")
                print(f"     CSR length: {len(csr)} bytes")
                print(f"     Starts with: {csr[:60]}...")
            else:
                print(f"  ⚠️  Response doesn't contain valid CSR")
        else:
            print(f"  ⚠️  Unexpected response format")
    else:
        print(f"  ⚠️  CSR generation returned: {resp.get('status')}")
        print(f"     Body: {str(resp.get('body'))[:200]}")
    
    print()
    
    # ========================================
    # STEP 7: LOGOUT
    # ========================================
    print("STEP 7: LOGOUT")
    print("-" * 40)
    
    resp = client.request("POST", "/api/v1/auth/logout")
    
    if resp.get("status") in [200, 204]:
        print(f"  ✅ Logout successful")
    elif resp.get("status") == 404:
        print(f"  ⚠️  Logout endpoint not available (404)")
    else:
        print(f"  ⚠️  Logout returned: {resp.get('status')}")
    
    print()
    
    # ========================================
    # SUMMARY
    # ========================================
    print("=" * 60)
    print("FLOW TEST COMPLETE")
    print("=" * 60)
    print(f"User created: {username}")
    print(f"Password: {password}")
    print()
    
    return True

if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)
