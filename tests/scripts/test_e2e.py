#!/usr/bin/env python3
"""
Comprehensive E2E Test Suite for PQC Digital Signature Platform
================================================================
Tests the complete workflow from registration to document verification.
Run with: python tests/scripts/test_e2e.py
"""

import requests
import base64
import datetime
import sys
import json
from typing import Optional

# Test configuration
API_BASE = 'http://localhost:8080/api/v1'
PORTAL_BASE = 'http://localhost:5173'

# Try importing cryptography for real cert generation
try:
    from cryptography import x509
    from cryptography.hazmat.backends import default_backend
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import rsa
    HAS_CRYPTO = True
except ImportError:
    HAS_CRYPTO = False
    print("⚠ cryptography not installed, using mock certificates")


class TestResult:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.tests = []
    
    def add(self, name: str, success: bool, message: str = ""):
        self.tests.append((name, success, message))
        if success:
            self.passed += 1
            print(f"  ✅ {name}")
        else:
            self.failed += 1
            print(f"  ❌ {name}: {message}")
    
    def summary(self):
        total = self.passed + self.failed
        print(f"\n{'='*60}")
        print(f"TEST SUMMARY: {self.passed}/{total} passed")
        if self.failed > 0:
            print(f"FAILED TESTS:")
            for name, success, message in self.tests:
                if not success:
                    print(f"  - {name}: {message}")
        print(f"{'='*60}")
        return self.failed == 0


def generate_test_cert(common_name: str) -> tuple:
    """Generate a test certificate and private key."""
    if HAS_CRYPTO:
        key = rsa.generate_private_key(65537, 2048, default_backend())
        cert = x509.CertificateBuilder().subject_name(
            x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME, common_name)])
        ).issuer_name(
            x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME, common_name)])
        ).public_key(key.public_key()).serial_number(
            x509.random_serial_number()
        ).not_valid_before(
            datetime.datetime.now(datetime.UTC)
        ).not_valid_after(
            datetime.datetime.now(datetime.UTC) + datetime.timedelta(days=365)
        ).sign(key, hashes.SHA256(), default_backend())
        return cert.public_bytes(serialization.Encoding.DER)
    else:
        # Mock DER cert header
        return bytes([0x30, 0x82, 0x01, 0x00] + [0x00] * 256)


class E2ETestSuite:
    def __init__(self):
        self.session = requests.Session()
        self.results = TestResult()
    
    def test_api_health(self):
        """Test that backend API is reachable."""
        print("\n🔧 Testing API Health...")
        try:
            resp = self.session.get(f"{API_BASE}/health", timeout=5)
            self.results.add("API reachable", True)
        except requests.RequestException as e:
            self.results.add("API reachable", False, str(e))
            return False
        return True
    
    def test_portal_health(self):
        """Test that Vue portal is reachable."""
        print("\n🌐 Testing Portal Health...")
        try:
            resp = self.session.get(PORTAL_BASE, timeout=5)
            has_vue = '<div id="app">' in resp.text
            self.results.add("Portal reachable", resp.ok)
            self.results.add("Vue app mounted", has_vue)
        except requests.RequestException as e:
            self.results.add("Portal reachable", False, str(e))
            return False
        return True
    
    def test_user_registration(self):
        """Test user registration flow."""
        print("\n👤 Testing User Registration...")
        
        # Generate unique usernames
        ts = datetime.datetime.now().strftime("%H%M%S")
        users = [
            (f"test_citizen_{ts}", "pass123", "USER"),
            (f"test_admin_{ts}", "pass123", "ADMIN"),
            (f"test_officer_{ts}", "pass123", "OFFICER"),
        ]
        
        for username, password, role in users:
            try:
                resp = self.session.post(
                    f"{API_BASE}/auth/register",
                    json={"username": username, "password": password, "role": role}
                )
                self.results.add(f"Register {role}", resp.ok or resp.status_code == 409)
            except Exception as e:
                self.results.add(f"Register {role}", False, str(e))
        
        return True
    
    def test_document_upload(self):
        """Test document upload."""
        print("\n📄 Testing Document Upload...")
        
        content = f"Test document content - {datetime.datetime.now().isoformat()}"
        files = {'file': ('test.txt', content.encode(), 'application/octet-stream')}
        
        try:
            resp = self.session.post(f"{API_BASE}/documents/upload", files=files)
            if resp.ok:
                data = resp.json()
                self.doc_id = data.get('docId')
                self.results.add("Document upload", True)
                self.results.add("Got docId", self.doc_id is not None)
                self.content = content.encode()
            else:
                self.results.add("Document upload", False, resp.text[:100])
        except Exception as e:
            self.results.add("Document upload", False, str(e))
        
        return hasattr(self, 'doc_id')
    
    def test_document_signing(self):
        """Test document signing and ASiC creation."""
        print("\n🔏 Testing Document Signing...")
        
        if not hasattr(self, 'doc_id'):
            self.results.add("Document signing", False, "No document uploaded")
            return False
        
        cert = generate_test_cert("E2E-Test-Signer")
        signature = b"ML-DSA-TEST-SIGNATURE" * 10
        
        try:
            resp = self.session.post(
                f"{API_BASE}/documents/finalize-asic",
                json={
                    "docId": str(self.doc_id),
                    "signature": base64.b64encode(signature).decode(),
                    "certificate": base64.b64encode(cert).decode()
                }
            )
            if resp.ok:
                self.asic_bytes = resp.content
                self.results.add("Document signing", True)
                self.results.add(f"ASiC size OK ({len(self.asic_bytes)} bytes)", len(self.asic_bytes) > 100)
            else:
                self.results.add("Document signing", False, resp.text[:100])
        except Exception as e:
            self.results.add("Document signing", False, str(e))
        
        return hasattr(self, 'asic_bytes')
    
    def test_document_verification(self):
        """Test ASiC verification."""
        print("\n🔍 Testing Document Verification...")
        
        if not hasattr(self, 'asic_bytes'):
            self.results.add("Document verification", False, "No ASiC created")
            return False
        
        try:
            files = {'file': ('test.asic', self.asic_bytes, 'application/vnd.etsi.asic-e+zip')}
            resp = self.session.post(f"{API_BASE}/documents/verify-asic", files=files)
            
            if resp.ok:
                data = resp.json()
                self.results.add("Verification request", True)
                self.results.add("Verification passed", data.get('valid', False))
                self.results.add("Signature count > 0", data.get('signatureCount', 0) > 0)
                
                # Check signature details
                sigs = data.get('signatures', [])
                if sigs:
                    sig = sigs[0]
                    self.results.add("Signature has message", 'message' in sig)
                    self.results.add("Signature has timestamp", 'timestamp' in sig)
            else:
                self.results.add("Verification request", False, resp.text[:100])
        except Exception as e:
            self.results.add("Document verification", False, str(e))
        
        return True
    
    def test_countersigning(self):
        """Test adding a countersignature."""
        print("\n👥 Testing Countersigning...")
        
        if not hasattr(self, 'asic_bytes'):
            self.results.add("Countersigning", False, "No ASiC to countersign")
            return False
        
        cert = generate_test_cert("E2E-Countersigner")
        signature = b"ML-DSA-COUNTERSIG" * 10
        
        try:
            files = {'file': ('test.asic', self.asic_bytes, 'application/vnd.etsi.asic-e+zip')}
            data = {
                'signature': base64.b64encode(signature).decode(),
                'certificate': base64.b64encode(cert).decode()
            }
            resp = self.session.post(f"{API_BASE}/documents/countersign", files=files, data=data)
            
            if resp.ok:
                self.countersigned_asic = resp.content
                self.results.add("Countersigning", True)
                self.results.add("New ASiC larger", len(self.countersigned_asic) > len(self.asic_bytes))
            else:
                self.results.add("Countersigning", False, resp.text[:100])
        except Exception as e:
            self.results.add("Countersigning", False, str(e))
        
        return hasattr(self, 'countersigned_asic')
    
    def test_multi_signature_verification(self):
        """Test verification of multi-signature ASiC."""
        print("\n🔗 Testing Multi-Signature Verification...")
        
        if not hasattr(self, 'countersigned_asic'):
            self.results.add("Multi-sig verification", False, "No countersigned ASiC")
            return False
        
        try:
            files = {'file': ('multi.asic', self.countersigned_asic, 'application/vnd.etsi.asic-e+zip')}
            resp = self.session.post(f"{API_BASE}/documents/verify-asic", files=files)
            
            if resp.ok:
                data = resp.json()
                sig_count = data.get('signatureCount', 0)
                self.results.add("Multi-sig verification", data.get('valid', False))
                self.results.add("Has 2 signatures", sig_count == 2)
                
                # Verify both signatures are valid
                sigs = data.get('signatures', [])
                all_valid = all(s.get('valid', False) for s in sigs)
                self.results.add("All signatures valid", all_valid)
            else:
                self.results.add("Multi-sig verification", False, resp.text[:100])
        except Exception as e:
            self.results.add("Multi-sig verification", False, str(e))
        
        return True
    
    def test_tamper_detection(self):
        """Test that tampered documents are detected."""
        print("\n🛡️ Testing Tamper Detection...")
        
        if not hasattr(self, 'asic_bytes'):
            self.results.add("Tamper detection", False, "No ASiC to tamper")
            return False
        
        import zipfile
        import io
        
        try:
            # Tamper with the content
            zf_in = zipfile.ZipFile(io.BytesIO(self.asic_bytes), 'r')
            zf_out_buffer = io.BytesIO()
            zf_out = zipfile.ZipFile(zf_out_buffer, 'w')
            
            for item in zf_in.namelist():
                data = zf_in.read(item)
                if not item.startswith('META-INF/'):
                    data = b"TAMPERED CONTENT!"  # Modify document
                zf_out.writestr(item, data)
            
            zf_out.close()
            tampered_asic = zf_out_buffer.getvalue()
            
            # Verify tampered document
            files = {'file': ('tampered.asic', tampered_asic, 'application/vnd.etsi.asic-e+zip')}
            resp = self.session.post(f"{API_BASE}/documents/verify-asic", files=files)
            
            if resp.ok:
                data = resp.json()
                is_invalid = not data.get('valid', True)
                self.results.add("Tamper detected", is_invalid)
                
                # Check for mismatch message
                sigs = data.get('signatures', [])
                if sigs:
                    msg = sigs[0].get('message', '').lower()
                    has_mismatch = 'mismatch' in msg or 'integrity' in msg
                    self.results.add("Error mentions hash mismatch", has_mismatch)
            else:
                self.results.add("Tamper detection request", False, resp.text[:100])
        except Exception as e:
            self.results.add("Tamper detection", False, str(e))
        
        return True
    
    def run_all(self):
        """Run all tests."""
        print("=" * 60)
        print("🚀 PQC DIGITAL SIGNATURE PLATFORM - E2E TEST SUITE")
        print("=" * 60)
        
        # Run tests in order
        if not self.test_api_health():
            print("\n⛔ Backend not available. Aborting tests.")
            return False
        
        self.test_portal_health()
        self.test_user_registration()
        
        if self.test_document_upload():
            if self.test_document_signing():
                self.test_document_verification()
                if self.test_countersigning():
                    self.test_multi_signature_verification()
                self.test_tamper_detection()
        
        return self.results.summary()


if __name__ == '__main__':
    suite = E2ETestSuite()
    success = suite.run_all()
    sys.exit(0 if success else 1)
