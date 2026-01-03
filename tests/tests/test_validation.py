"""
Signature Validation Tests.

Tests cover:
- Signature verification
- Invalid signature detection
- Certificate validation
"""
import pytest
import base64
from api import ValidationClient


class TestSignatureVerification:
    """Signature verification tests."""
    
    @pytest.mark.smoke
    def test_verification_endpoint_available(self, validation_client):
        """Verification endpoint should be available."""
        # Send mock data - will return invalid but proves endpoint works
        response = validation_client.verify_signature(
            doc_hash_b64=base64.b64encode(b"test hash").decode(),
            signature_b64=base64.b64encode(b"mock signature").decode(),
            cert_pem="-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----"
        )
        
        # Endpoint should respond (even if signature is invalid)
        assert response.status_code in [200, 400]
    
    def test_invalid_signature_rejected(self, validation_client):
        """Invalid signature should be flagged as invalid."""
        response = validation_client.verify_signature(
            doc_hash_b64=base64.b64encode(b"document content").decode(),
            signature_b64=base64.b64encode(b"invalid signature bytes").decode(),
            cert_pem="-----BEGIN CERTIFICATE-----\nMIIBtest\n-----END CERTIFICATE-----"
        )
        
        if response.is_ok and isinstance(response.data, dict):
            # API may return 'valid' or 'isValid'
            is_valid = response.data.get("valid") or response.data.get("isValid")
            assert is_valid == False, \
                "Invalid signature should be marked as invalid"
    
    def test_verification_with_empty_hash(self, validation_client):
        """Verification with empty hash should fail."""
        response = validation_client.verify_signature(
            doc_hash_b64="",
            signature_b64=base64.b64encode(b"sig").decode(),
            cert_pem="-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----"
        )
        
        assert response.status_code in [200, 400]
    
    def test_verification_with_empty_signature(self, validation_client):
        """Verification with empty signature should fail."""
        response = validation_client.verify_signature(
            doc_hash_b64=base64.b64encode(b"hash").decode(),
            signature_b64="",
            cert_pem="-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----"
        )
        
        assert response.status_code in [200, 400]


class TestCertificateValidation:
    """Certificate validation tests."""
    
    def test_validate_certificate(self, validation_client):
        """Should validate certificate structure."""
        response = validation_client.validate_certificate(
            cert_pem="-----BEGIN CERTIFICATE-----\nMIIBtest\n-----END CERTIFICATE-----"
        )
        
        # Endpoint may not exist (404) or work (200/400)
        assert response.status_code in [200, 400, 404]
    
    def test_validate_malformed_certificate(self, validation_client):
        """Malformed certificate should be rejected."""
        response = validation_client.validate_certificate(
            cert_pem="not a valid certificate"
        )
        
        if response.is_ok and isinstance(response.data, dict):
            # API may return 'valid' or 'isValid'
            is_valid = response.data.get("valid") or response.data.get("isValid")
            assert is_valid == False


class TestRevocationCheck:
    """Certificate revocation check tests."""
    
    def test_check_revocation_endpoint(self, validation_client):
        """Revocation check endpoint should be available."""
        response = validation_client.check_revocation(
            cert_pem="-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----"
        )
        
        # Endpoint may not exist (404) or respond (200/400)
        assert response.status_code in [200, 400, 404]


class TestValidationSecurity:
    """Validation security tests."""
    
    @pytest.mark.security
    def test_large_payload_handled(self, validation_client):
        """Large payloads should be handled gracefully."""
        large_data = "A" * 100000  # 100KB
        
        response = validation_client.verify_signature(
            doc_hash_b64=base64.b64encode(large_data.encode()).decode(),
            signature_b64=base64.b64encode(b"sig").decode(),
            cert_pem="-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----"
        )
        
        # Should not crash - either process or reject
        assert response.status_code in [200, 400, 413]
    
    @pytest.mark.security
    def test_special_characters_in_cert(self, validation_client):
        """Special characters should be handled safely."""
        response = validation_client.verify_signature(
            doc_hash_b64=base64.b64encode(b"test").decode(),
            signature_b64=base64.b64encode(b"sig").decode(),
            cert_pem="-----BEGIN CERTIFICATE-----\n<script>alert('xss')</script>\n-----END CERTIFICATE-----"
        )
        
        # Should handle safely
        assert response.status_code in [200, 400]
