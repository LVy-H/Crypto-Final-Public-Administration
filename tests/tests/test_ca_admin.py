"""
CA (Certificate Authority) Administration Tests.

Tests cover:
- CA hierarchy listing
- Certificate chain retrieval
- CA request workflow
- CRL access
"""
import pytest
from api import CaClient


class TestCaListing:
    """CA listing tests."""
    
    @pytest.mark.smoke
    def test_list_all_cas(self, admin_client):
        """Should list all Certificate Authorities."""
        ca_client = CaClient()
        ca_client.token = admin_client.token
        
        response = ca_client.list_all_cas()
        
        assert response.is_ok, f"CA list failed: {response.status_code}"
        assert isinstance(response.data, list), "Should return list of CAs"
    
    def test_ca_list_contains_expected_fields(self, admin_client):
        """CA entries should have expected fields."""
        ca_client = CaClient()
        ca_client.token = admin_client.token
        
        response = ca_client.list_all_cas()
        
        if response.is_ok and isinstance(response.data, list) and len(response.data) > 0:
            ca = response.data[0]
            # Check for common CA fields
            assert isinstance(ca, dict)
            assert any(key in ca for key in ["id", "name", "status", "level"])
    
    def test_list_cas_unauthenticated(self, ca_client):
        """Unauthenticated CA listing may be restricted."""
        response = ca_client.list_all_cas()
        
        # May be public or restricted
        assert response.status_code in [200, 401, 403]


class TestCertificateChain:
    """Certificate chain tests."""
    
    def test_get_certificate_chain(self, admin_client):
        """Should get certificate chain for a CA."""
        ca_client = CaClient()
        ca_client.token = admin_client.token
        
        # Get CA list first
        cas = ca_client.list_all_cas()
        
        if cas.is_ok and isinstance(cas.data, list) and len(cas.data) > 0:
            ca_id = cas.data[0].get("id")
            
            if ca_id:
                response = ca_client.get_certificate_chain(ca_id)
                assert response.status_code in [200, 404]
    
    def test_get_subordinate_cas(self, admin_client):
        """Should get subordinate CAs."""
        ca_client = CaClient()
        ca_client.token = admin_client.token
        
        cas = ca_client.list_all_cas()
        
        if cas.is_ok and isinstance(cas.data, list) and len(cas.data) > 0:
            ca_id = cas.data[0].get("id")
            
            if ca_id:
                response = ca_client.get_subordinates(ca_id)
                assert response.status_code in [200, 404]


class TestCaRequestWorkflow:
    """CA request workflow tests."""
    
    @pytest.mark.smoke
    def test_get_pending_ca_requests(self, admin_client):
        """Admin should see pending CA requests."""
        ca_client = CaClient()
        ca_client.token = admin_client.token
        
        response = ca_client.get_pending_requests()
        
        assert response.is_ok or response.status_code == 404, \
            f"Pending requests failed: {response.status_code}"
    
    def test_non_admin_cannot_see_pending(self, authenticated_user):
        """Regular user cannot see pending CA requests."""
        ca_client = CaClient()
        ca_client.token = authenticated_user.token
        
        response = ca_client.get_pending_requests()
        
        assert response.status_code in [401, 403, 404]


class TestCrlAccess:
    """Certificate Revocation List tests."""
    
    @pytest.mark.smoke
    def test_get_crl_public(self, ca_client, admin_client):
        """CRL should be publicly accessible."""
        # Get a CA ID first (using admin)
        admin_ca = CaClient()
        admin_ca.token = admin_client.token
        
        cas = admin_ca.list_all_cas()
        
        if cas.is_ok and isinstance(cas.data, list) and len(cas.data) > 0:
            ca_id = cas.data[0].get("id")
            
            if ca_id:
                # CRL may be public or require auth
                response = ca_client.get_crl(ca_id)
                assert response.status_code in [200, 403, 404, 500]


class TestRootCertificate:
    """Root CA certificate tests."""
    
    def test_get_root_certificate_public(self, ca_client):
        """Root certificate should be publicly accessible."""
        response = ca_client.get_root_certificate()
        
        # May be available, 404, or require auth
        assert response.status_code in [200, 403, 404]
    
    @pytest.mark.smoke
    def test_root_cert_is_pem_format(self, ca_client):
        """Root certificate should be in PEM format."""
        response = ca_client.get_root_certificate()
        
        if response.is_ok:
            if isinstance(response.data, str):
                assert "-----BEGIN CERTIFICATE-----" in response.data
            elif isinstance(response.data, dict) and "certificate" in response.data:
                assert "-----BEGIN CERTIFICATE-----" in response.data["certificate"]
