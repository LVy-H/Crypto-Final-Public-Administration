"""
CSC Signing API Client.
"""
import base64
import hashlib
from dataclasses import dataclass
from typing import Optional

from .client import AuthenticatedClient, ApiResponse


@dataclass
class KeyGenRequest:
    """Key generation request data."""
    alias: str
    algorithm: str = "mldsa65"


@dataclass
class SigningRequest:
    """Signing request data."""
    key_alias: str
    data_hash_base64: str
    algorithm: str = "ML-DSA-65"


class SigningClient(AuthenticatedClient):
    """
    CSC Remote Signing API client.
    
    Handles:
    - Key generation
    - Signing initialization
    - Signing confirmation (with TOTP)
    - TOTP management
    """
    
    def generate_key(self, alias: str, algorithm: str = "mldsa65") -> ApiResponse:
        """Generate a new signing key."""
        return self.post(
            "/csc/v1/keys/generate",
            json={
                "alias": alias,
                "algorithm": algorithm,
            }
        )
    
    def list_keys(self) -> ApiResponse:
        """List user's signing keys."""
        return self.get("/csc/v1/keys/list")
    
    def delete_key(self, alias: str) -> ApiResponse:
        """Delete a signing key."""
        return self.delete(f"/csc/v1/keys/{alias}")
    
    def init_signing(
        self, 
        key_alias: str, 
        data_hash_base64: str,
        algorithm: str = "ML-DSA-65"
    ) -> ApiResponse:
        """Initialize a signing operation."""
        return self.post(
            "/csc/v1/sign/init",
            json={
                "keyAlias": key_alias,
                "dataHashBase64": data_hash_base64,
                "algorithm": algorithm,
            }
        )
    
    def confirm_signing(self, challenge_id: str, otp: str) -> ApiResponse:
        """Confirm signing with OTP."""
        return self.post(
            "/csc/v1/sign/confirm",
            json={
                "challengeId": challenge_id,
                "otp": otp,
            }
        )
    
    def sign_document(
        self, 
        key_alias: str, 
        document_content: bytes,
        algorithm: str = "ML-DSA-65"
    ) -> tuple[Optional[str], Optional[str]]:
        """
        Full document signing flow (without TOTP confirmation).
        
        Returns:
            Tuple of (challenge_id, error_message)
        """
        # Hash the document
        doc_hash = hashlib.sha256(document_content).digest()
        hash_b64 = base64.b64encode(doc_hash).decode()
        
        # Initialize signing
        response = self.init_signing(key_alias, hash_b64, algorithm)
        
        if response.is_ok and isinstance(response.data, dict):
            return response.data.get("challengeId"), None
        else:
            return None, f"Init failed: {response.status_code}"
    
    # TOTP management
    def get_totp_status(self) -> ApiResponse:
        """Get TOTP status for current user."""
        return self.get("/api/v1/credentials/totp/status")
    
    def setup_totp(self) -> ApiResponse:
        """Initialize TOTP setup (returns secret/QR)."""
        return self.post("/api/v1/credentials/totp/setup")
    
    def verify_totp_setup(self, otp: str) -> ApiResponse:
        """Verify TOTP setup with initial code."""
        return self.post(
            "/api/v1/credentials/totp/verify",
            json={"otp": otp}
        )
