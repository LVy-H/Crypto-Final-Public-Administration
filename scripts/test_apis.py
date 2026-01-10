#!/usr/bin/env python3
"""
PQC Backend API Test Script
Tests all backend services through the API Gateway
"""

import requests
import json
from datetime import datetime

# API Gateway endpoint (NodePort)
BASE_URL = "http://localhost:30080"

def log(msg, status="INFO"):
    timestamp = datetime.now().strftime("%H:%M:%S")
    emoji = {"INFO": "ℹ️", "OK": "✅", "FAIL": "❌", "WARN": "⚠️"}.get(status, "")
    print(f"[{timestamp}] {emoji} {msg}")

def test_health_endpoints():
    """Test actuator health endpoints for each service"""
    log("Testing health endpoints...")
    
    services = {
        "api-gateway": "/actuator/health",
        "identity-service": "/api/identity/actuator/health",
        "pki-service": "/api/pki/actuator/health",
        "tsa-service": "/api/tsa/actuator/health",
        "document-service": "/api/documents/actuator/health"
    }
    
    results = {}
    for name, path in services.items():
        try:
            resp = requests.get(f"{BASE_URL}{path}", timeout=10)
            if resp.status_code == 200:
                log(f"{name}: {resp.json().get('status', 'UP')}", "OK")
                results[name] = True
            else:
                log(f"{name}: HTTP {resp.status_code}", "FAIL")
                results[name] = False
        except requests.exceptions.ConnectionError:
            log(f"{name}: Connection refused", "FAIL")
            results[name] = False
        except Exception as e:
            log(f"{name}: {str(e)}", "FAIL")
            results[name] = False
    
    return results

def test_gateway_direct():
    """Test API Gateway directly"""
    log("Testing API Gateway direct access...")
    
    try:
        # Try root endpoint
        resp = requests.get(f"{BASE_URL}/", timeout=5)
        log(f"Gateway root: HTTP {resp.status_code}", "OK" if resp.status_code < 500 else "FAIL")
        
        # Try actuator
        resp = requests.get(f"{BASE_URL}/actuator", timeout=5)
        log(f"Gateway actuator: HTTP {resp.status_code}", "OK" if resp.status_code < 500 else "FAIL")
        return True
    except requests.exceptions.ConnectionError:
        log("Cannot connect to API Gateway on port 30080", "FAIL")
        return False
    except Exception as e:
        log(f"Gateway error: {e}", "FAIL")
        return False

def test_identity_service():
    """Test identity service registration/login"""
    log("Testing Identity Service...")
    
    try:
        # Test registration endpoint
        resp = requests.post(
            f"{BASE_URL}/api/identity/auth/register",
            json={"username": "testuser", "password": "testpass123"},
            timeout=10
        )
        log(f"Register: HTTP {resp.status_code} - {resp.text[:100] if resp.text else 'empty'}", 
            "OK" if resp.status_code in [200, 201, 400, 409] else "WARN")
        return True
    except requests.exceptions.ConnectionError:
        log("Identity service not reachable", "FAIL")
        return False
    except Exception as e:
        log(f"Identity error: {e}", "FAIL")
        return False

def test_pki_service():
    """Test PKI service endpoints"""
    log("Testing PKI Service...")
    
    try:
        # Test CA info endpoint (if exists)
        resp = requests.get(f"{BASE_URL}/api/pki/ca/info", timeout=10)
        log(f"PKI CA Info: HTTP {resp.status_code}", 
            "OK" if resp.status_code in [200, 401, 403, 404] else "WARN")
        return True
    except requests.exceptions.ConnectionError:
        log("PKI service not reachable", "FAIL")
        return False
    except Exception as e:
        log(f"PKI error: {e}", "FAIL")
        return False

def test_tsa_service():
    """Test TSA service"""
    log("Testing TSA Service...")
    
    try:
        # Test timestamp endpoint (will fail without proper request but should be reachable)
        resp = requests.get(f"{BASE_URL}/api/tsa/health", timeout=10)
        log(f"TSA health: HTTP {resp.status_code}", 
            "OK" if resp.status_code in [200, 404, 405] else "WARN")
        return True
    except requests.exceptions.ConnectionError:
        log("TSA service not reachable", "FAIL")
        return False
    except Exception as e:
        log(f"TSA error: {e}", "FAIL")
        return False

def main():
    print("\n" + "="*60)
    print("  PQC Backend Services - API Test Suite")
    print("="*60 + "\n")
    
    # 1. Test gateway connectivity
    if not test_gateway_direct():
        print("\n⚠️ API Gateway not accessible. Ensure port-forward or NodePort is working.")
        print("   Try: kubectl port-forward svc/api-gateway -n crypto-backend 30080:8080")
        return
    
    print()
    
    # 2. Test health endpoints
    health_results = test_health_endpoints()
    
    print()
    
    # 3. Test individual services
    test_identity_service()
    test_pki_service()
    test_tsa_service()
    
    # Summary
    print("\n" + "="*60)
    total = len(health_results)
    passed = sum(health_results.values())
    print(f"  Health Check Summary: {passed}/{total} services responding")
    print("="*60 + "\n")

if __name__ == "__main__":
    main()
