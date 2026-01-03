"""
Authentication API Client.
"""
from dataclasses import dataclass
from typing import Optional, Tuple

from .client import AuthenticatedClient, ApiResponse


@dataclass
class UserRegistration:
    """User registration data."""
    username: str
    email: str
    password: str


@dataclass  
class KycRequest:
    """KYC verification request data."""
    full_name: str
    id_number: str
    id_type: str = "NATIONAL_ID"
    date_of_birth: str = "1990-01-15"
    address: str = "123 Test Street, Test City"


class AuthClient(AuthenticatedClient):
    """
    Authentication and Identity API client.
    
    Handles:
    - User registration
    - Login/logout
    - KYC verification workflow
    - Identity status checks
    """
    
    def register(self, user: UserRegistration) -> ApiResponse:
        """Register a new user."""
        return self.post(
            "/api/v1/auth/register",
            json={
                "username": user.username,
                "email": user.email,
                "password": user.password,
            }
        )
    
    def register_and_login(
        self, 
        username: str, 
        email: str, 
        password: str
    ) -> Tuple[bool, Optional[str]]:
        """Register a new user and immediately login."""
        user = UserRegistration(username=username, email=email, password=password)
        reg_response = self.register(user)
        
        if not reg_response.is_ok:
            return False, f"Registration failed: {reg_response.status_code}"
        
        return self.login(username, password)
    
    def get_identity_status(self) -> ApiResponse:
        """Get current user's identity verification status."""
        return self.get("/api/v1/identity/status")
    
    def submit_kyc(self, kyc: KycRequest) -> ApiResponse:
        """Submit KYC verification request."""
        return self.post(
            "/api/v1/identity/verify-request",
            json={
                "fullName": kyc.full_name,
                "idNumber": kyc.id_number,
                "idType": kyc.id_type,
                "dateOfBirth": kyc.date_of_birth,
                "address": kyc.address,
            }
        )
    
    def get_pending_kyc_requests(self) -> ApiResponse:
        """Get pending KYC requests (admin only)."""
        return self.get("/api/v1/identity/pending")
    
    def approve_kyc(self, username: str) -> ApiResponse:
        """Approve a user's KYC request (admin only)."""
        return self.post(f"/api/v1/identity/approve/{username}")
    
    def reject_kyc(self, username: str, reason: str = "Test rejection") -> ApiResponse:
        """Reject a user's KYC request (admin only)."""
        return self.post(
            f"/api/v1/identity/reject/{username}",
            json={"reason": reason}
        )
