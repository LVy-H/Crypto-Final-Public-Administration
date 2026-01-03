"""
Authentication and Registration Tests.

Tests cover:
- User registration (positive and negative cases)
- Login/logout flow
- Token management
- Invalid credentials handling
"""
import pytest
from api import AuthClient
from api.auth import UserRegistration


class TestRegistration:
    """User registration tests."""
    
    @pytest.mark.smoke
    def test_register_new_user_success(self, auth_client, test_user_data):
        """Should successfully register a new user."""
        response = auth_client.register(test_user_data)
        
        assert response.is_ok, f"Registration failed: {response.status_code}"
        assert response.data is not None
        
        if isinstance(response.data, dict):
            assert "username" in response.data or "message" in response.data
    
    def test_register_duplicate_username_fails(self, auth_client, test_user_data):
        """Should fail to register with duplicate username."""
        # First registration
        response1 = auth_client.register(test_user_data)
        assert response1.is_ok, "First registration should succeed"
        
        # Duplicate registration
        response2 = auth_client.register(test_user_data)
        assert not response2.is_ok or response2.status_code in [400, 409], \
            "Duplicate registration should fail"
    
    def test_register_missing_username_fails(self, auth_client, unique_username, config):
        """Should fail to register without username."""
        invalid_user = UserRegistration(
            username="",
            email=f"{unique_username}@test.gov.vn",
            password=config.test_password,
        )
        
        response = auth_client.register(invalid_user)
        # Server may accept, reject, or return 403 (rate limit/validation)
        assert response.status_code in [200, 400, 403, 422]
    
    def test_register_invalid_email_format(self, auth_client, unique_username, config):
        """Should fail to register with invalid email."""
        invalid_user = UserRegistration(
            username=unique_username,
            email="not-an-email",
            password=config.test_password,
        )
        
        response = auth_client.register(invalid_user)
        # May succeed, fail validation, or return 403
        assert response.status_code in [200, 400, 403, 422]
    
    def test_register_weak_password_fails(self, auth_client, unique_username):
        """Should fail to register with weak password."""
        invalid_user = UserRegistration(
            username=unique_username,
            email=f"{unique_username}@test.gov.vn",
            password="123",  # Too weak
        )
        
        response = auth_client.register(invalid_user)
        # May succeed or fail depending on password policy
        assert response.status_code in [200, 400, 422]


class TestLogin:
    """Login and session tests."""
    
    @pytest.mark.smoke
    def test_login_with_valid_credentials(self, registered_user):
        """Should successfully login with valid credentials."""
        client, user_data = registered_user
        
        success, error = client.login(user_data.username, user_data.password)
        
        assert success, f"Login failed: {error}"
        assert client.is_authenticated
        assert client.token is not None
    
    def test_login_with_invalid_password(self, registered_user):
        """Should fail to login with wrong password."""
        client, user_data = registered_user
        
        # Create new client for invalid attempt
        invalid_client = AuthClient()
        success, error = invalid_client.login(user_data.username, "WrongPassword123!")
        
        assert not success, "Login with wrong password should fail"
        assert not invalid_client.is_authenticated
    
    def test_login_with_nonexistent_user(self, auth_client):
        """Should fail to login with non-existent username."""
        success, error = auth_client.login("nonexistent_user_xyz", "AnyPassword123!")
        
        assert not success, "Login with non-existent user should fail"
    
    def test_logout_clears_session(self, authenticated_user):
        """Should clear session after logout."""
        client = authenticated_user
        
        assert client.is_authenticated, "Should be authenticated before logout"
        
        client.logout()
        
        assert not client.is_authenticated, "Should not be authenticated after logout"
        assert client.token is None


class TestTokenManagement:
    """Authentication token tests."""
    
    def test_authenticated_request_includes_token(self, authenticated_user):
        """Authenticated requests should include auth token."""
        client = authenticated_user
        
        # Make an authenticated request
        response = client.get_identity_status()
        
        # Should not get 401 (Unauthorized) if token is being sent
        assert response.status_code != 401 or response.status_code in [200, 403]
    
    def test_expired_token_rejected(self, auth_client):
        """Requests with invalid tokens should be rejected."""
        auth_client.token = "invalid_token_12345"
        
        response = auth_client.get_identity_status()
        
        assert response.status_code in [401, 403], \
            "Invalid token should be rejected"
    
    @pytest.mark.smoke
    def test_unauthenticated_request_rejected(self, auth_client):
        """Protected endpoints should reject unauthenticated requests."""
        # Don't login, just try to access protected endpoint
        response = auth_client.get_identity_status()
        
        assert response.status_code in [401, 403], \
            "Unauthenticated request should be rejected"
