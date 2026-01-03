"""
Base API Client with authentication, logging, and error handling.
"""
import logging
import time
from dataclasses import dataclass
from typing import Any, Dict, Optional, Tuple
from enum import Enum

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from utils.config import get_config

# Suppress SSL warnings for self-signed certs
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

logger = logging.getLogger(__name__)


class HttpMethod(Enum):
    GET = "GET"
    POST = "POST"
    PUT = "PUT"
    PATCH = "PATCH"
    DELETE = "DELETE"


@dataclass
class ApiResponse:
    """Structured API response container."""
    status_code: int
    data: Optional[Any]
    headers: Dict[str, str]
    elapsed_ms: float
    success: bool
    error: Optional[str] = None
    
    @property
    def is_ok(self) -> bool:
        return 200 <= self.status_code < 300
    
    def json(self) -> Any:
        return self.data


class ApiClient:
    """
    Base API client with authentication and request handling.
    
    Features:
    - Automatic token management
    - Request/response logging
    - Retry logic with exponential backoff
    - Structured response objects
    """
    
    def __init__(self, base_url: Optional[str] = None):
        self.config = get_config()
        self.base_url = base_url or self.config.base_url
        self._token: Optional[str] = None
        self._session = self._create_session()
        
    def _create_session(self) -> requests.Session:
        """Create a requests session with retry logic."""
        session = requests.Session()
        
        retry_strategy = Retry(
            total=self.config.max_retries,
            backoff_factor=self.config.retry_delay,
            status_forcelist=[502, 503, 504],
            allowed_methods=["GET", "POST", "PUT", "DELETE"]
        )
        
        adapter = HTTPAdapter(max_retries=retry_strategy)
        session.mount("https://", adapter)
        session.mount("http://", adapter)
        
        return session
    
    @property
    def token(self) -> Optional[str]:
        return self._token
    
    @token.setter
    def token(self, value: Optional[str]):
        self._token = value
        
    def _build_headers(self, extra_headers: Optional[Dict] = None) -> Dict[str, str]:
        """Build request headers with optional auth token."""
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        
        if self._token:
            headers["Authorization"] = f"Bearer {self._token}"
            headers["X-Auth-Token"] = self._token
            
        if extra_headers:
            headers.update(extra_headers)
            
        return headers
    
    def request(
        self,
        method: HttpMethod,
        endpoint: str,
        *,
        json: Optional[Dict] = None,
        params: Optional[Dict] = None,
        headers: Optional[Dict] = None,
        expected_status: Optional[int] = None,
        allow_statuses: Optional[list] = None,
    ) -> ApiResponse:
        """
        Make an API request.
        
        Args:
            method: HTTP method
            endpoint: API endpoint (will be joined with base_url)
            json: JSON body
            params: Query parameters
            headers: Additional headers
            expected_status: Expected status code (for validation)
            allow_statuses: List of acceptable status codes
            
        Returns:
            ApiResponse object with structured response data
        """
        url = f"{self.base_url}{endpoint}" if not endpoint.startswith("http") else endpoint
        request_headers = self._build_headers(headers)
        
        if self.config.verbose_logging:
            logger.info(f"→ {method.value} {endpoint}")
            if json:
                logger.debug(f"  Body: {json}")
        
        start_time = time.time()
        
        try:
            response = self._session.request(
                method=method.value,
                url=url,
                json=json,
                params=params,
                headers=request_headers,
                timeout=self.config.timeout,
                verify=self.config.verify_ssl,
            )
            
            elapsed_ms = (time.time() - start_time) * 1000
            
            # Parse response body
            try:
                data = response.json()
            except (ValueError, requests.exceptions.JSONDecodeError):
                data = response.text if response.text else None
            
            # Determine success
            if allow_statuses:
                success = response.status_code in allow_statuses
            elif expected_status:
                success = response.status_code == expected_status
            else:
                success = response.ok
                
            api_response = ApiResponse(
                status_code=response.status_code,
                data=data,
                headers=dict(response.headers),
                elapsed_ms=elapsed_ms,
                success=success,
            )
            
            if self.config.verbose_logging:
                logger.info(f"← {response.status_code} ({elapsed_ms:.0f}ms)")
                
            return api_response
            
        except requests.exceptions.RequestException as e:
            elapsed_ms = (time.time() - start_time) * 1000
            logger.error(f"Request failed: {e}")
            
            return ApiResponse(
                status_code=0,
                data=None,
                headers={},
                elapsed_ms=elapsed_ms,
                success=False,
                error=str(e),
            )
    
    # Convenience methods
    def get(self, endpoint: str, **kwargs) -> ApiResponse:
        return self.request(HttpMethod.GET, endpoint, **kwargs)
    
    def post(self, endpoint: str, **kwargs) -> ApiResponse:
        return self.request(HttpMethod.POST, endpoint, **kwargs)
    
    def put(self, endpoint: str, **kwargs) -> ApiResponse:
        return self.request(HttpMethod.PUT, endpoint, **kwargs)
    
    def delete(self, endpoint: str, **kwargs) -> ApiResponse:
        return self.request(HttpMethod.DELETE, endpoint, **kwargs)


class AuthenticatedClient(ApiClient):
    """API client that requires authentication."""
    
    def __init__(self, base_url: Optional[str] = None):
        super().__init__(base_url)
        self._username: Optional[str] = None
        
    def login(self, username: str, password: str) -> Tuple[bool, Optional[str]]:
        """
        Login and store authentication token.
        
        Returns:
            Tuple of (success, error_message)
        """
        response = self.post(
            "/api/v1/auth/login",
            json={"username": username, "password": password}
        )
        
        if response.is_ok:
            # Token from header
            token = response.headers.get("X-Auth-Token")
            if not token:
                auth = response.headers.get("Authorization", "")
                if auth.startswith("Bearer "):
                    token = auth[7:]
            
            if token:
                self._token = token
                self._username = username
                return True, None
            else:
                return False, "No token in response"
        else:
            error = response.data if isinstance(response.data, str) else str(response.data)
            return False, f"Login failed: {response.status_code} - {error}"
    
    def logout(self) -> bool:
        """Logout and clear token."""
        response = self.post("/api/v1/auth/logout")
        self._token = None
        self._username = None
        return response.is_ok
    
    @property
    def username(self) -> Optional[str]:
        return self._username
    
    @property
    def is_authenticated(self) -> bool:
        return self._token is not None
