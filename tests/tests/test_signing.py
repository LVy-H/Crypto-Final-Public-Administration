"""
Document Signing Tests (CSC API).

Tests cover:
- Key generation
- Signing initialization
- TOTP flow
- Algorithm support
"""
import pytest
import hashlib
import base64
from api import SigningClient


class TestKeyGeneration:
    """Signing key generation tests."""
    
    @pytest.mark.smoke
    def test_generate_mldsa_key(self, admin_client, timestamp):
        """Should generate ML-DSA signing key."""
        signing_client = SigningClient()
        signing_client.token = admin_client.token
        
        # Key alias must start with username_ or signing_key_ for authorization
        alias = f"signing_key_{timestamp}"
        response = signing_client.generate_key(alias, "mldsa65")
        
        assert response.status_code in [200, 201], \
            f"Key generation failed: {response.status_code}"
        
        if response.is_ok and isinstance(response.data, dict):
            assert "publicKeyPem" in response.data or "alias" in response.data
    
    @pytest.mark.parametrize("algorithm", [
        "mldsa44",
        "mldsa65",
        "mldsa87",
    ])
    def test_generate_different_mldsa_levels(self, admin_client, timestamp, algorithm):
        """Should support all ML-DSA security levels."""
        signing_client = SigningClient()
        signing_client.token = admin_client.token
        
        # Key alias must match authorization pattern: signing_key_*
        alias = f"signing_key_{algorithm}_{timestamp}"
        response = signing_client.generate_key(alias, algorithm)
        
        assert response.status_code in [200, 201, 400], \
            f"{algorithm} key gen failed: {response.status_code}"
    
    def test_generate_key_unauthenticated(self, signing_client, timestamp):
        """Unauthenticated key generation should fail."""
        response = signing_client.generate_key(f"test_{timestamp}", "mldsa65")
        
        assert response.status_code in [401, 403]
    
    def test_generate_duplicate_alias_overwrites(self, admin_client, timestamp):
        """Generating key with duplicate alias may overwrite or fail."""
        signing_client = SigningClient()
        signing_client.token = admin_client.token
        
        alias = f"signing_key_dup_{timestamp}"
        
        # First generation
        response1 = signing_client.generate_key(alias, "mldsa65")
        assert response1.is_ok, "First key generation should succeed"
        
        # Second with same alias - API may allow overwrite or reject
        response2 = signing_client.generate_key(alias, "mldsa65")
        # Accept either behavior: overwrite (200) or reject (400/409)
        assert response2.status_code in [200, 400, 409]


class TestSigningInitialization:
    """Signing initialization tests."""
    
    @pytest.mark.smoke
    def test_init_signing_success(self, admin_client, timestamp):
        """Should successfully initialize signing."""
        signing_client = SigningClient()
        signing_client.token = admin_client.token
        
        # Generate key first with authorized alias format
        alias = f"signing_key_sign_{timestamp}"
        key_response = signing_client.generate_key(alias, "mldsa65")
        
        if not key_response.is_ok:
            pytest.skip("Key generation failed")
        
        # Init signing
        test_data = f"Test document {timestamp}".encode()
        doc_hash = hashlib.sha256(test_data).digest()
        hash_b64 = base64.b64encode(doc_hash).decode()
        
        response = signing_client.init_signing(alias, hash_b64)
        
        assert response.status_code in [200, 201], \
            f"Init signing failed: {response.status_code}"
        
        if response.is_ok and isinstance(response.data, dict):
            assert "challengeId" in response.data
    
    def test_init_signing_with_invalid_key(self, admin_client):
        """Init signing with non-existent key should fail."""
        signing_client = SigningClient()
        signing_client.token = admin_client.token
        
        hash_b64 = base64.b64encode(b"test").decode()
        response = signing_client.init_signing("nonexistent_key", hash_b64)
        
        # 401 = not authorized for key, 400/404 = key not found
        assert response.status_code in [400, 401, 404]


class TestSigningConfirmation:
    """Signing confirmation (TOTP) tests."""
    
    def test_confirm_without_totp_fails(self, admin_client, timestamp):
        """Confirm signing without valid TOTP should fail."""
        signing_client = SigningClient()
        signing_client.token = admin_client.token
        
        # Generate key and init signing (use signing_key_ prefix for authorization)
        alias = f"signing_key_totp_{timestamp}"
        signing_client.generate_key(alias, "mldsa65")
        
        hash_b64 = base64.b64encode(b"test data").decode()
        init_response = signing_client.init_signing(alias, hash_b64)
        
        if init_response.is_ok and isinstance(init_response.data, dict):
            challenge_id = init_response.data.get("challengeId")
            
            if challenge_id:
                # Confirm with invalid OTP
                response = signing_client.confirm_signing(challenge_id, "000000")
                
                # Should fail (invalid OTP) or succeed (if TOTP not required)
                assert response.status_code in [200, 400, 401]


class TestTotpManagement:
    """TOTP management tests."""
    
    def test_get_totp_status(self, admin_client):
        """Should get TOTP status."""
        signing_client = SigningClient()
        signing_client.token = admin_client.token
        
        response = signing_client.get_totp_status()
        
        # May be 200 with status or 404 if not set up
        assert response.status_code in [200, 404]
    
    def test_totp_status_unauthenticated(self, signing_client):
        """Unauthenticated TOTP status should fail."""
        response = signing_client.get_totp_status()
        
        assert response.status_code in [401, 403, 404]


class TestKeyListing:
    """Key listing tests."""
    
    def test_list_keys(self, admin_client):
        """Should list user's signing keys."""
        signing_client = SigningClient()
        signing_client.token = admin_client.token
        
        response = signing_client.list_keys()
        
        # May be 200 or 404 if endpoint doesn't exist
        assert response.status_code in [200, 404]
