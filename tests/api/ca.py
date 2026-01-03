"""
CA Management API Client.
"""
from dataclasses import dataclass
from typing import Optional

from .client import AuthenticatedClient, ApiResponse


@dataclass
class CaRequest:
    """CA creation request data."""
    name: str
    parent_id: Optional[str] = None
    key_algorithm: str = "ML-DSA-65"
    validity_years: int = 10


class CaClient(AuthenticatedClient):
    """
    Certificate Authority management API client.
    
    Handles:
    - CA hierarchy listing
    - CA creation/approval
    - Certificate chain retrieval
    - CRL management
    """
    
    def list_all_cas(self) -> ApiResponse:
        """List all Certificate Authorities."""
        return self.get("/api/v1/ca/all")
    
    def get_ca(self, ca_id: str) -> ApiResponse:
        """Get a specific CA by ID."""
        return self.get(f"/api/v1/ca/{ca_id}")
    
    def get_certificate_chain(self, ca_id: str) -> ApiResponse:
        """Get the certificate chain for a CA."""
        return self.get(f"/api/v1/ca/{ca_id}/chain")
    
    def get_subordinates(self, ca_id: str) -> ApiResponse:
        """Get subordinate CAs for a given CA."""
        return self.get(f"/api/v1/ca/subordinates/{ca_id}")
    
    def get_pending_requests(self) -> ApiResponse:
        """Get pending CA requests (admin only)."""
        return self.get("/api/v1/ca/requests/pending")
    
    def approve_ca_request(self, request_id: str) -> ApiResponse:
        """Approve a CA request (admin only)."""
        return self.post(f"/api/v1/ca/requests/{request_id}/approve")
    
    def reject_ca_request(self, request_id: str, reason: str = "Test rejection") -> ApiResponse:
        """Reject a CA request (admin only)."""
        return self.post(
            f"/api/v1/ca/requests/{request_id}/reject",
            json={"reason": reason}
        )
    
    # Public endpoints
    def get_crl(self, ca_id: str) -> ApiResponse:
        """Get the CRL for a CA (public endpoint)."""
        return self.get(f"/api/v1/ca/crl/{ca_id}")
    
    def get_root_certificate(self) -> ApiResponse:
        """Get the root CA certificate (public endpoint)."""
        return self.get("/api/v1/ca/root/certificate")
