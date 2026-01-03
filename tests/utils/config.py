"""
Configuration management for API tests.
Supports environment-based configuration with .env file overrides.
"""
import os
from dataclasses import dataclass, field
from typing import Optional
from pathlib import Path

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass


@dataclass
class TestConfig:
    """Test configuration with environment variable support."""
    
    # API Base URLs
    base_url: str = field(default_factory=lambda: os.getenv(
        "API_BASE_URL", "https://api.gov-id.lvh.id.vn"))
    portal_url: str = field(default_factory=lambda: os.getenv(
        "PORTAL_URL", "https://portal.gov-id.lvh.id.vn"))
    
    # Default test credentials
    admin_username: str = field(default_factory=lambda: os.getenv(
        "ADMIN_USERNAME", "admin_capture"))
    admin_password: str = field(default_factory=lambda: os.getenv(
        "ADMIN_PASSWORD", "SecurePass123!"))
    
    # Test user settings
    test_user_prefix: str = "pytest_user"
    test_password: str = "SecureTestPass123!"
    
    # Request settings
    timeout: int = 30
    verify_ssl: bool = False
    max_retries: int = 3
    retry_delay: float = 1.0
    
    # Test behavior
    cleanup_after_tests: bool = field(default_factory=lambda: 
        os.getenv("CLEANUP_AFTER_TESTS", "false").lower() == "true")
    verbose_logging: bool = field(default_factory=lambda:
        os.getenv("VERBOSE_LOGGING", "false").lower() == "true")


# Global config instance
config = TestConfig()


def get_config() -> TestConfig:
    """Get the current test configuration."""
    return config
