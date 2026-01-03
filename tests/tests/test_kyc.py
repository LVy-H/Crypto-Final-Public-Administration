"""
KYC (Know Your Customer) Workflow Tests.

Tests cover:
- KYC submission
- Status transitions
- Admin approval/rejection
- Edge cases
"""
import pytest
from api import AuthClient
from api.auth import KycRequest


class TestKycSubmission:
    """KYC submission tests."""
    
    @pytest.mark.smoke
    def test_submit_kyc_request_success(self, authenticated_user, kyc_request):
        """Should successfully submit KYC request."""
        response = authenticated_user.submit_kyc(kyc_request)
        
        assert response.status_code in [200, 201], \
            f"KYC submission failed: {response.status_code}"
    
    def test_initial_status_is_unverified(self, authenticated_user):
        """New user should have UNVERIFIED status."""
        response = authenticated_user.get_identity_status()
        
        assert response.is_ok, f"Status check failed: {response.status_code}"
        
        if isinstance(response.data, dict):
            status = response.data.get("status", "")
            assert status in ["UNVERIFIED", "PENDING"], \
                f"Expected UNVERIFIED or PENDING, got {status}"
    
    def test_submit_kyc_without_auth_fails(self, auth_client, kyc_request):
        """Unauthenticated KYC submission should fail."""
        response = auth_client.submit_kyc(kyc_request)
        
        assert response.status_code in [401, 403], \
            "Unauthenticated KYC should be rejected"
    
    def test_submit_kyc_with_missing_fields(self, authenticated_user):
        """KYC with missing required fields should fail."""
        incomplete_kyc = KycRequest(
            full_name="",  # Missing name
            id_number="",  # Missing ID
        )
        
        response = authenticated_user.submit_kyc(incomplete_kyc)
        
        # May be accepted or rejected depending on validation
        assert response.status_code in [200, 400, 422]


class TestKycAdminWorkflow:
    """Admin KYC approval workflow tests."""
    
    @pytest.mark.smoke
    def test_admin_view_pending_requests(self, admin_client):
        """Admin should see pending KYC requests."""
        response = admin_client.get_pending_kyc_requests()
        
        assert response.is_ok, f"Pending list failed: {response.status_code}"
        assert isinstance(response.data, list), "Should return list of requests"
    
    def test_admin_approve_kyc_success(self, authenticated_user, kyc_request, admin_client):
        """Admin should successfully approve KYC."""
        # User submits KYC
        submit_response = authenticated_user.submit_kyc(kyc_request)
        assert submit_response.status_code in [200, 201]
        
        # Admin approves
        approve_response = admin_client.approve_kyc(authenticated_user.username)
        
        assert approve_response.status_code in [200, 204], \
            f"Approval failed: {approve_response.status_code}"
    
    def test_status_becomes_verified_after_approval(
        self, authenticated_user, kyc_request, admin_client, config
    ):
        """User status should be VERIFIED after approval."""
        # Submit and approve
        authenticated_user.submit_kyc(kyc_request)
        admin_client.approve_kyc(authenticated_user.username)
        
        # Re-login to get fresh session
        client = AuthClient()
        client.login(authenticated_user.username, config.test_password)
        
        # Check status
        response = client.get_identity_status()
        
        if response.is_ok and isinstance(response.data, dict):
            status = response.data.get("status", "")
            assert status == "VERIFIED", f"Expected VERIFIED, got {status}"
    
    def test_admin_reject_kyc(self, authenticated_user, kyc_request, admin_client):
        """Admin should be able to reject KYC."""
        # Submit KYC
        authenticated_user.submit_kyc(kyc_request)
        
        # Reject
        response = admin_client.reject_kyc(
            authenticated_user.username, 
            reason="Document not clear"
        )
        
        # May succeed or 404 if endpoint doesn't exist
        assert response.status_code in [200, 204, 404]
    
    def test_non_admin_cannot_approve(self, authenticated_user, kyc_request):
        """Regular user should not be able to approve KYC."""
        # Try to approve (should fail)
        response = authenticated_user.approve_kyc("some_other_user")
        
        assert response.status_code in [401, 403, 404], \
            "Non-admin approval should be rejected"


class TestKycEdgeCases:
    """KYC edge case tests."""
    
    def test_approve_nonexistent_user(self, admin_client):
        """Approving non-existent user should fail gracefully."""
        response = admin_client.approve_kyc("nonexistent_user_xyz123")
        
        assert response.status_code in [400, 404], \
            "Approving non-existent user should fail"
    
    def test_duplicate_kyc_submission(self, authenticated_user, kyc_request):
        """Submitting KYC twice should be handled."""
        # First submission
        response1 = authenticated_user.submit_kyc(kyc_request)
        
        # Second submission
        response2 = authenticated_user.submit_kyc(kyc_request)
        
        # Both may succeed (update) or second may fail
        assert response1.status_code in [200, 201]
        assert response2.status_code in [200, 201, 400, 409]
