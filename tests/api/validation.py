"""
Validation API Client.
"""
import base64
from dataclasses import dataclass
from typing import Optional

from .client import ApiClient, ApiResponse


@dataclass
class VerificationRequest:
    """Signature verification request."""
    original_doc_hash: str  # Base64 encoded
    signature_base64: str
    cert_pem: str


class ValidationClient(ApiClient):
    """
    Signature Validation API client.
    
    Handles:
    - Signature verification
    - Certificate validation
    - Revocation checks
    """
    
    def verify_signature(
        self, 
        doc_hash_b64: str, 
        signature_b64: str, 
        cert_pem: str
    ) -> ApiResponse:
        """Verify a document signature."""
        return self.post(
            "/api/v1/validation/verify",
            json={
                "originalDocHash": doc_hash_b64,
                "signatureBase64": signature_b64,
                "certPem": cert_pem,
            }
        )
    
    def verify_document(
        self, 
        document_content: bytes,
        signature_b64: str,
        cert_pem: str
    ) -> ApiResponse:
        """Verify a document signature (hashes document internally)."""
        import hashlib
        doc_hash = hashlib.sha256(document_content).digest()
        hash_b64 = base64.b64encode(doc_hash).decode()
        
        return self.verify_signature(hash_b64, signature_b64, cert_pem)
    
    def validate_certificate(self, cert_pem: str) -> ApiResponse:
        """Validate a certificate chain."""
        return self.post(
            "/api/v1/validation/certificate",
            json={"certPem": cert_pem}
        )
    
    def check_revocation(self, cert_pem: str) -> ApiResponse:
        """Check if a certificate is revoked."""
        return self.post(
            "/api/v1/validation/revocation",
            json={"certPem": cert_pem}
        )
