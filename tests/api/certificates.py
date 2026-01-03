"""
Certificate API Client.
"""
from dataclasses import dataclass
from typing import Optional, List

from .client import AuthenticatedClient, ApiResponse


@dataclass
class CertificateRequest:
    """Certificate request data."""
    certificate_type: str = "SIGNATURE"
    key_algorithm: str = "ML-DSA-65"


class CertificateClient(AuthenticatedClient):
    """
    Certificate management API client.
    
    Handles:
    - Certificate requests
    - Certificate listing
    - Admin approval/rejection
    - Certificate download
    """
    
    def request_certificate(self, req: CertificateRequest) -> ApiResponse:
        """Request a new certificate."""
        return self.post(
            "/api/v1/certificates/request",
            json={
                "certificateType": req.certificate_type,
                "keyAlgorithm": req.key_algorithm,
            }
        )
    
    def list_my_certificates(self) -> ApiResponse:
        """List current user's certificates."""
        return self.get("/api/v1/certificates/my")
    
    def get_certificate(self, cert_id: str) -> ApiResponse:
        """Get a specific certificate by ID."""
        return self.get(f"/api/v1/certificates/{cert_id}")
    
    def download_certificate(self, cert_id: str) -> ApiResponse:
        """Download certificate PEM."""
        return self.get(f"/api/v1/certificates/{cert_id}/download")
    
    # Admin endpoints
    def get_pending_requests(self) -> ApiResponse:
        """Get pending certificate requests (admin only)."""
        return self.get("/api/v1/admin/certificates/requests/pending")
    
    def approve_request(self, request_id: str) -> ApiResponse:
        """Approve a certificate request (admin only)."""
        return self.post(f"/api/v1/admin/certificates/requests/{request_id}/approve")
    
    def reject_request(self, request_id: str, reason: str = "Test rejection") -> ApiResponse:
        """Reject a certificate request (admin only)."""
        return self.post(
            f"/api/v1/admin/certificates/requests/{request_id}/reject",
            json={"reason": reason}
        )
    
    def revoke_certificate(self, cert_id: str, reason: str = "Test revocation") -> ApiResponse:
        """Revoke a certificate (admin only)."""
        return self.post(
            f"/api/v1/admin/certificates/{cert_id}/revoke",
            json={"reason": reason}
        )
