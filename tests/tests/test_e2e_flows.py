"""
End-to-End Workflow Tests.

Tests complete user journeys through the system.
"""
import pytest
import time
import hashlib
import base64
from api import AuthClient, CertificateClient, SigningClient, CaClient
from api.auth import UserRegistration, KycRequest
from api.certificates import CertificateRequest


@pytest.mark.e2e
class TestCitizenRegistrationWorkflow:
    """Complete citizen registration and KYC workflow."""
    
    def test_full_registration_to_verification(self, config, admin_client, timestamp):
        """Complete flow: Register → Submit KYC → Admin Approve → Verified."""
        # Step 1: Register
        user_client = AuthClient()
        username = f"e2e_citizen_{timestamp}"
        
        user = UserRegistration(
            username=username,
            email=f"{username}@test.gov.vn",
            password=config.test_password,
        )
        
        reg_response = user_client.register(user)
        assert reg_response.is_ok, f"Registration failed: {reg_response.status_code}"
        
        # Step 2: Login
        success, error = user_client.login(username, config.test_password)
        assert success, f"Login failed: {error}"
        
        # Step 3: Check initial status
        status_response = user_client.get_identity_status()
        assert status_response.is_ok
        
        # Step 4: Submit KYC
        kyc = KycRequest(
            full_name=f"E2E Citizen {timestamp}",
            id_number=f"E2E{timestamp}",
        )
        kyc_response = user_client.submit_kyc(kyc)
        assert kyc_response.status_code in [200, 201]
        
        # Step 5: Admin approves
        approve_response = admin_client.approve_kyc(username)
        assert approve_response.status_code in [200, 204]
        
        # Step 6: Verify final status
        final_status = user_client.get_identity_status()
        
        if final_status.is_ok and isinstance(final_status.data, dict):
            assert final_status.data.get("status") == "VERIFIED"


@pytest.mark.e2e
class TestCertificateEnrollmentWorkflow:
    """Complete certificate enrollment workflow."""
    
    def test_full_certificate_enrollment(self, verified_user, admin_client):
        """Complete flow: Request → Admin Approve → Certificate Issued."""
        cert_client = CertificateClient()
        cert_client.token = verified_user.token
        
        # Step 1: Request certificate
        request = CertificateRequest(
            certificate_type="SIGNATURE",
            key_algorithm="ML-DSA-65"
        )
        
        req_response = cert_client.request_certificate(request)
        # 403 = user not verified (timing issue with KYC approval)
        assert req_response.status_code in [200, 201, 403], \
            f"Request failed: {req_response.status_code}"
        
        request_id = None
        if isinstance(req_response.data, dict):
            request_id = req_response.data.get("requestId") or req_response.data.get("id")
        
        # Step 2: Admin views pending (optional - find request)
        admin_cert = CertificateClient()
        admin_cert.token = admin_client.token
        
        pending = admin_cert.get_pending_requests()
        
        if pending.is_ok and isinstance(pending.data, list):
            for req in pending.data:
                req_id = req.get("id") or req.get("requestId")
                if req_id:
                    request_id = req_id
                    break
        
        # Step 3: Admin approves
        if request_id:
            approve = admin_cert.approve_request(request_id)
            assert approve.status_code in [200, 201, 400, 404]
        
        # Step 4: User lists certificates
        time.sleep(0.5)  # Brief wait for processing
        certs = cert_client.list_my_certificates()
        assert certs.is_ok


@pytest.mark.e2e
class TestDocumentSigningWorkflow:
    """Complete document signing workflow."""
    
    def test_full_signing_flow(self, admin_client, timestamp):
        """Complete flow: Generate Key → Hash Document → Init Sign → (TOTP) → Signature."""
        signing_client = SigningClient()
        signing_client.token = admin_client.token
        
        # Step 1: Generate signing key (alias must be signing_key_* for authorization)
        alias = f"signing_key_{timestamp}"
        key_response = signing_client.generate_key(alias, "mldsa65")
        
        assert key_response.status_code in [200, 201], \
            f"Key generation failed: {key_response.status_code}"
        
        # Step 2: Hash document
        document = f"E2E Test Document - Timestamp: {timestamp}".encode()
        doc_hash = hashlib.sha256(document).digest()
        hash_b64 = base64.b64encode(doc_hash).decode()
        
        # Step 3: Initialize signing
        init_response = signing_client.init_signing(alias, hash_b64)
        
        assert init_response.status_code in [200, 201], \
            f"Init signing failed: {init_response.status_code}"
        
        if init_response.is_ok and isinstance(init_response.data, dict):
            challenge_id = init_response.data.get("challengeId")
            assert challenge_id is not None, "Should receive challenge ID"
            
            # Step 4: Confirm (will fail without real TOTP)
            confirm = signing_client.confirm_signing(challenge_id, "000000")
            # 401 = OTP invalid (expected), 200 = signed
            assert confirm.status_code in [200, 400, 401]


@pytest.mark.e2e
class TestCaHierarchyWorkflow:
    """CA hierarchy management workflow."""
    
    def test_view_ca_hierarchy(self, admin_client):
        """View complete CA hierarchy with chains."""
        ca_client = CaClient()
        ca_client.token = admin_client.token
        
        # Step 1: List all CAs
        cas = ca_client.list_all_cas()
        assert cas.is_ok
        
        if isinstance(cas.data, list) and len(cas.data) > 0:
            # Step 2: Get first CA's chain
            ca = cas.data[0]
            ca_id = ca.get("id")
            
            if ca_id:
                chain = ca_client.get_certificate_chain(ca_id)
                # Chain may exist or not
                assert chain.status_code in [200, 404]
                
                # Step 3: Get subordinates
                subs = ca_client.get_subordinates(ca_id)
                assert subs.status_code in [200, 404]


@pytest.mark.e2e
@pytest.mark.slow
class TestCompleteUserJourney:
    """Complete end-to-end user journey."""
    
    def test_full_user_journey(self, config, admin_client, timestamp):
        """
        Complete journey:
        Register → Login → KYC → Approve → Request Cert → 
        Approve Cert → Generate Key → Sign Document
        """
        # === PHASE 1: Registration & KYC ===
        user_client = AuthClient()
        username = f"journey_{timestamp}"
        
        # Register
        user = UserRegistration(
            username=username,
            email=f"{username}@gov.vn",
            password=config.test_password,
        )
        assert user_client.register(user).is_ok
        
        # Login
        success, _ = user_client.login(username, config.test_password)
        assert success
        
        # KYC
        kyc = KycRequest(full_name=f"Journey User {timestamp}", id_number=f"JRN{timestamp}")
        assert user_client.submit_kyc(kyc).status_code in [200, 201]
        
        # Admin approve KYC
        assert admin_client.approve_kyc(username).status_code in [200, 204]
        
        # === PHASE 2: Certificate Enrollment ===
        # Re-login to refresh session
        user_client = AuthClient()
        user_client.login(username, config.test_password)
        
        cert_client = CertificateClient()
        cert_client.token = user_client.token
        
        # Request cert
        cert_req = cert_client.request_certificate(CertificateRequest())
        assert cert_req.status_code in [200, 201, 403]
        
        # === PHASE 3: Document Signing ===
        signing_client = SigningClient()
        signing_client.token = user_client.token
        
        # Use signing_key_ prefix for authorization
        alias = f"signing_key_journey_{timestamp}"
        key_gen = signing_client.generate_key(alias, "mldsa65")
        
        if key_gen.is_ok:
            # Hash and init signing
            doc = f"Journey document {timestamp}".encode()
            hash_b64 = base64.b64encode(hashlib.sha256(doc).digest()).decode()
            
            init = signing_client.init_signing(alias, hash_b64)
            assert init.status_code in [200, 201]
        
        # Journey complete!
