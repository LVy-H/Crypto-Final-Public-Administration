"""
Pytest fixtures and configuration for API tests.
"""
import pytest
import time
import sys
from pathlib import Path
from typing import Generator

# Add tests directory to path for imports
sys.path.insert(0, str(Path(__file__).parent))

from api import AuthClient, CertificateClient, CaClient, SigningClient, ValidationClient
from api.auth import UserRegistration, KycRequest
from utils.config import get_config


# ============================================================
# Configuration Fixtures
# ============================================================

@pytest.fixture(scope="session")
def config():
    """Get test configuration."""
    return get_config()


@pytest.fixture(scope="session")
def base_url(config):
    """Get API base URL."""
    return config.base_url


# ============================================================
# Client Fixtures
# ============================================================

@pytest.fixture
def auth_client() -> AuthClient:
    """Fresh auth client for each test."""
    return AuthClient()


@pytest.fixture
def cert_client() -> CertificateClient:
    """Fresh certificate client for each test."""
    return CertificateClient()


@pytest.fixture
def ca_client() -> CaClient:
    """Fresh CA client for each test."""
    return CaClient()


@pytest.fixture
def signing_client() -> SigningClient:
    """Fresh signing client for each test."""
    return SigningClient()


@pytest.fixture
def validation_client() -> ValidationClient:
    """Fresh validation client for each test."""
    return ValidationClient()


# ============================================================
# Authenticated Session Fixtures
# ============================================================

@pytest.fixture(scope="session")
def admin_session(config) -> Generator[AuthClient, None, None]:
    """Authenticated admin client (session-scoped for efficiency)."""
    client = AuthClient()
    success, error = client.login(config.admin_username, config.admin_password)
    
    if not success:
        pytest.skip(f"Admin login failed: {error}")
    
    yield client
    
    client.logout()


@pytest.fixture
def admin_client(admin_session) -> AuthClient:
    """Alias for admin_session with cleaner name."""
    return admin_session


# ============================================================
# Test User Fixtures
# ============================================================

@pytest.fixture
def unique_username(config) -> str:
    """Generate a unique username for testing."""
    timestamp = int(time.time() * 1000)
    return f"{config.test_user_prefix}_{timestamp}"


@pytest.fixture
def test_user_data(unique_username, config) -> UserRegistration:
    """Generate test user registration data."""
    return UserRegistration(
        username=unique_username,
        email=f"{unique_username}@test.gov.vn",
        password=config.test_password,
    )


@pytest.fixture
def registered_user(auth_client, test_user_data) -> tuple[AuthClient, UserRegistration]:
    """Register a new test user and return client with user data."""
    response = auth_client.register(test_user_data)
    
    if not response.is_ok:
        pytest.fail(f"Failed to register test user: {response.status_code}")
    
    return auth_client, test_user_data


@pytest.fixture
def authenticated_user(registered_user, config) -> AuthClient:
    """Registered and logged-in test user."""
    client, user_data = registered_user
    
    success, error = client.login(user_data.username, user_data.password)
    if not success:
        pytest.fail(f"Failed to login test user: {error}")
    
    return client


@pytest.fixture
def verified_user(authenticated_user, admin_client) -> AuthClient:
    """Registered, logged-in, and KYC-verified test user."""
    user_client = authenticated_user
    
    # Submit KYC
    kyc = KycRequest(
        full_name=f"Test User {int(time.time())}",
        id_number=f"ID{int(time.time())}",
    )
    kyc_response = user_client.submit_kyc(kyc)
    
    if not kyc_response.is_ok:
        pytest.skip(f"KYC submission failed: {kyc_response.status_code}")
    
    # Admin approves KYC
    approve_response = admin_client.approve_kyc(user_client.username)
    
    if not approve_response.is_ok:
        pytest.skip(f"KYC approval failed: {approve_response.status_code}")
    
    return user_client


# ============================================================
# KYC Test Data Fixtures
# ============================================================

@pytest.fixture
def kyc_request(unique_username) -> KycRequest:
    """Generate KYC request data."""
    return KycRequest(
        full_name=f"Test Citizen {unique_username}",
        id_number=f"ID{int(time.time())}",
        id_type="NATIONAL_ID",
        date_of_birth="1990-01-15",
        address="123 Test Street, Test City",
    )


# ============================================================
# Utility Fixtures
# ============================================================

@pytest.fixture
def timestamp() -> str:
    """Current timestamp string for unique identifiers."""
    return str(int(time.time() * 1000))
