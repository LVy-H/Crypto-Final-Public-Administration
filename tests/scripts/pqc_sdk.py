"""
PQC Digital Signature Platform - Python SDK
============================================
A comprehensive client SDK that simulates frontend interactions with the backend APIs.
This can be used for integration testing, automation, and as a reference implementation.
"""

import requests
import base64
import json
import hashlib
import os
import datetime
from dataclasses import dataclass
from typing import Optional, Dict, Any, List, Tuple
from enum import Enum

# Optional: Use liboqs for real PQC if available
try:
    import oqs
    HAS_LIBOQS = True
except ImportError:
    HAS_LIBOQS = False

# Use cryptography for certificate generation
from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa


class Role(Enum):
    USER = "USER"
    ADMIN = "ADMIN"
    OFFICER = "OFFICER"
    CA_OPERATOR = "CA_OPERATOR"


class KycStatus(Enum):
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"


class Algorithm(Enum):
    ML_DSA_44 = "ML-DSA-44"
    ML_DSA_65 = "ML-DSA-65"
    ML_DSA_87 = "ML-DSA-87"
    SLH_DSA_SHAKE_128F = "SLH-DSA-SHAKE-128f"


@dataclass
class User:
    username: str
    role: Role
    kyc_status: Optional[KycStatus] = None
    session_token: Optional[str] = None


@dataclass
class Document:
    doc_id: str
    filename: str
    owner_id: str
    status: str


@dataclass
class SignedPackage:
    asic_bytes: bytes
    signature_count: int
    signatures: List[str]


class PQCClient:
    """
    A comprehensive Python client for the PQC Digital Signature Platform.
    Simulates frontend interactions with all backend services.
    """

    def __init__(self, base_url: str = "http://localhost:8080/api/v1"):
        self.base_url = base_url
        self.session = requests.Session()
        self.current_user: Optional[User] = None
        self.algorithm = Algorithm.ML_DSA_44

    # =========================================================================
    # Authentication & User Management
    # =========================================================================

    def register(self, username: str, password: str, role: Role = Role.USER) -> Dict[str, Any]:
        """Register a new user with the platform."""
        response = self.session.post(
            f"{self.base_url}/auth/register",
            json={"username": username, "password": password, "role": role.value}
        )
        return {"success": response.status_code == 200, "message": response.text}

    def login(self, username: str, password: str) -> bool:
        """Authenticate and establish a session."""
        response = self.session.post(
            f"{self.base_url}/auth/login",
            json={"username": username, "password": password}
        )
        if response.status_code == 200:
            self.current_user = User(username=username, role=Role.USER)
            return True
        return False

    def logout(self) -> None:
        """End the current session."""
        self.session.post(f"{self.base_url}/auth/logout")
        self.current_user = None

    # =========================================================================
    # KYC Management (Admin Only)
    # =========================================================================

    def approve_kyc(self, username: str) -> Dict[str, Any]:
        """Approve a user's KYC status (requires ADMIN role)."""
        response = self.session.post(
            f"{self.base_url}/admin/approve-kyc",
            json={"username": username, "action": "APPROVE"}
        )
        return {
            "success": response.status_code == 200,
            "message": response.text,
            "status_code": response.status_code
        }

    def reject_kyc(self, username: str) -> Dict[str, Any]:
        """Reject a user's KYC status (requires ADMIN role)."""
        response = self.session.post(
            f"{self.base_url}/admin/approve-kyc",
            json={"username": username, "action": "REJECT"}
        )
        return {"success": response.status_code == 200, "message": response.text}

    # =========================================================================
    # Document Management
    # =========================================================================

    def upload_document(self, filename: str, content: bytes, owner_id: Optional[str] = None) -> Optional[str]:
        """Upload a document and return its ID."""
        files = {"file": (filename, content, "application/octet-stream")}
        data = {}
        if owner_id:
            data["ownerId"] = owner_id

        response = self.session.post(
            f"{self.base_url}/documents/upload",
            files=files,
            data=data
        )

        if response.status_code == 200:
            return response.json().get("docId")
        return None

    def get_document_hash(self, doc_id: str) -> Optional[str]:
        """Get the hash of a document for signing."""
        response = self.session.get(f"{self.base_url}/documents/{doc_id}/hash")
        if response.status_code == 200:
            return response.json().get("hash")
        return None

    # =========================================================================
    # Cryptographic Operations
    # =========================================================================

    def generate_keypair(self, algorithm: Algorithm = None) -> Tuple[bytes, bytes]:
        """
        Generate a PQC keypair for signing.
        Returns (public_key, private_key) tuple.
        """
        algo = algorithm or self.algorithm

        if HAS_LIBOQS:
            # Use real PQC with liboqs
            sig = oqs.Signature(algo.value)
            public_key = sig.generate_keypair()
            private_key = sig.export_secret_key()
            return (public_key, private_key)
        else:
            # Simulate with placeholder
            print(f"[SIM] Generating {algo.value} keypair (simulated)")
            fake_pk = f"PK_{algo.value}_{os.urandom(16).hex()}".encode()
            fake_sk = f"SK_{algo.value}_{os.urandom(32).hex()}".encode()
            return (fake_pk, fake_sk)

    def sign_data(self, data: bytes, private_key: bytes, algorithm: Algorithm = None) -> bytes:
        """
        Sign data using the private key.
        Returns the signature bytes.
        """
        algo = algorithm or self.algorithm

        if HAS_LIBOQS:
            sig = oqs.Signature(algo.value)
            sig.import_secret_key(private_key)
            return sig.sign(data)
        else:
            # Simulate signature
            print(f"[SIM] Signing with {algo.value} (simulated)")
            sig_content = f"{algo.value}_SIG_{hashlib.sha256(data).hexdigest()}"
            return sig_content.encode() * 10  # Make it realistic size

    def generate_certificate(self, common_name: str) -> bytes:
        """
        Generate a self-signed X.509 certificate for testing.
        In production, this would be issued by the CA.
        """
        key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048,
            backend=default_backend()
        )
        subject = issuer = x509.Name([
            x509.NameAttribute(x509.NameOID.COMMON_NAME, common_name),
            x509.NameAttribute(x509.NameOID.ORGANIZATION_NAME, "PQC Test Org"),
        ])
        cert = (
            x509.CertificateBuilder()
            .subject_name(subject)
            .issuer_name(issuer)
            .public_key(key.public_key())
            .serial_number(x509.random_serial_number())
            .not_valid_before(datetime.datetime.now(datetime.UTC))
            .not_valid_after(datetime.datetime.now(datetime.UTC) + datetime.timedelta(days=365))
            .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
            .sign(key, hashes.SHA256(), default_backend())
        )
        return cert.public_bytes(serialization.Encoding.DER)

    # =========================================================================
    # Digital Signature Workflow
    # =========================================================================

    def sign_document(self, doc_id: str, signature: bytes, certificate: bytes) -> Optional[bytes]:
        """
        Finalize a document signature and create an ASiC container.
        Returns the ASiC container bytes.
        """
        payload = {
            "docId": doc_id,
            "signature": base64.b64encode(signature).decode("utf-8"),
            "certificate": base64.b64encode(certificate).decode("utf-8")
        }
        response = self.session.post(
            f"{self.base_url}/documents/finalize-asic",
            json=payload
        )

        if response.status_code == 200:
            return response.content
        print(f"Sign document failed: {response.status_code} - {response.text}")
        return None

    def countersign_document(self, asic_bytes: bytes, signature: bytes, certificate: bytes) -> Optional[bytes]:
        """
        Add a countersignature to an existing ASiC container.
        Returns the updated ASiC container bytes.
        """
        files = {"file": ("package.asic", asic_bytes, "application/vnd.etsi.asic-e+zip")}
        data = {
            "signature": base64.b64encode(signature).decode("utf-8"),
            "certificate": base64.b64encode(certificate).decode("utf-8")
        }
        response = self.session.post(
            f"{self.base_url}/documents/countersign",
            files=files,
            data=data
        )

        if response.status_code == 200:
            return response.content
        print(f"Countersign failed: {response.status_code} - {response.text}")
        return None

    def verify_signatures(self, asic_bytes: bytes) -> Dict[str, Any]:
        """
        Verify all signatures in an ASiC container.
        Returns verification results including signature count and validity.
        """
        files = {"file": ("package.asic", asic_bytes, "application/vnd.etsi.asic-e+zip")}
        response = self.session.post(
            f"{self.base_url}/documents/verify-asic",
            files=files
        )

        if response.status_code == 200:
            return response.json()
        return {"valid": False, "error": response.text}

    # =========================================================================
    # PKI Operations
    # =========================================================================

    def get_ca_certificate(self) -> Optional[bytes]:
        """Retrieve the CA's root certificate."""
        response = self.session.get(f"{self.base_url}/pki/ca/certificate")
        if response.status_code == 200:
            return response.content
        return None

    def request_certificate(self, csr: bytes) -> Optional[bytes]:
        """Submit a CSR and receive a signed certificate."""
        response = self.session.post(
            f"{self.base_url}/pki/ca/sign",
            data=csr,
            headers={"Content-Type": "application/pkcs10"}
        )
        if response.status_code == 200:
            return response.content
        return None

    # =========================================================================
    # Timestamp Authority
    # =========================================================================

    def get_timestamp(self, data_hash: bytes) -> Optional[bytes]:
        """Request an RFC 3161 timestamp for a hash."""
        response = self.session.post(
            f"{self.base_url}/tsa/timestamp",
            data=data_hash,
            headers={"Content-Type": "application/timestamp-query"}
        )
        if response.status_code == 200:
            return response.content
        return None

    # =========================================================================
    # High-Level Workflow Helpers
    # =========================================================================

    def complete_document_signing(
        self,
        filename: str,
        content: bytes,
        signer_name: str
    ) -> Optional[SignedPackage]:
        """
        Complete workflow: Upload -> Sign -> Return ASiC package.
        """
        # Step 1: Upload
        doc_id = self.upload_document(filename, content)
        if not doc_id:
            print("Failed to upload document")
            return None

        # Step 2: Generate crypto materials
        _, private_key = self.generate_keypair()
        signature = self.sign_data(content, private_key)
        certificate = self.generate_certificate(signer_name)

        # Step 3: Create ASiC
        asic_bytes = self.sign_document(doc_id, signature, certificate)
        if not asic_bytes:
            print("Failed to create signed package")
            return None

        return SignedPackage(asic_bytes=asic_bytes, signature_count=1, signatures=[])

    def add_countersignature(
        self,
        package: SignedPackage,
        signer_name: str
    ) -> Optional[SignedPackage]:
        """
        Add a countersignature to an existing package.
        """
        # Generate crypto materials for countersigner
        _, private_key = self.generate_keypair()

        # Sign the original content (extracted from container in backend)
        signature = self.sign_data(package.asic_bytes, private_key)
        certificate = self.generate_certificate(signer_name)

        # Add to container
        new_asic = self.countersign_document(package.asic_bytes, signature, certificate)
        if not new_asic:
            return None

        return SignedPackage(
            asic_bytes=new_asic,
            signature_count=package.signature_count + 1,
            signatures=[]
        )


# =============================================================================
# Demo / Test Script
# =============================================================================

def run_full_workflow_demo():
    """Demonstrate a complete frontend workflow using the SDK."""
    print("=" * 60)
    print("PQC Digital Signature Platform - Full Workflow Demo")
    print("=" * 60)

    client = PQCClient()

    # === Phase 1: User Setup ===
    print("\n📋 Phase 1: User Registration & KYC")
    print("-" * 40)

    # Register users
    client.register("demo_admin", "admin123", Role.ADMIN)
    client.register("demo_citizen", "citizen123", Role.USER)
    client.register("demo_officer", "officer123", Role.OFFICER)
    print("✅ Users registered")

    # Admin approves KYC
    client.login("demo_admin", "admin123")
    result = client.approve_kyc("demo_citizen")
    print(f"   Citizen KYC: {result['status_code']}")
    result = client.approve_kyc("demo_officer")
    print(f"   Officer KYC: {result['status_code']}")

    # === Phase 2: Document Signing ===
    print("\n📝 Phase 2: Citizen Signs Document")
    print("-" * 40)

    client.login("demo_citizen", "citizen123")
    contract = b"OFFICIAL CONTRACT\nDate: 2026-01-10\nParties: Government & Citizen\n\nTerms and conditions apply..."

    package = client.complete_document_signing(
        filename="contract.txt",
        content=contract,
        signer_name="Demo Citizen (ML-DSA)"
    )

    if package:
        print(f"✅ Document signed. ASiC size: {len(package.asic_bytes)} bytes")

        # === Phase 3: Officer Countersigns ===
        print("\n👮 Phase 3: Officer Countersigns")
        print("-" * 40)

        client.login("demo_officer", "officer123")
        final_package = client.add_countersignature(package, "Demo Officer (ML-DSA)")

        if final_package:
            print(f"✅ Countersigned. ASiC size: {len(final_package.asic_bytes)} bytes")

            # === Phase 4: Verification ===
            print("\n🔍 Phase 4: Verify Chain of Trust")
            print("-" * 40)

            result = client.verify_signatures(final_package.asic_bytes)
            print(f"   Valid: {result.get('valid', False)}")
            print(f"   Signature Count: {result.get('signatureCount', 0)}")
            print(f"   Signatures: {result.get('signatures', [])}")

            if result.get('signatureCount', 0) >= 2:
                print("\n🎉 SUCCESS: Multi-party signing workflow complete!")

            # Save for inspection
            with open("demo_output.asic", "wb") as f:
                f.write(final_package.asic_bytes)
            print("💾 Saved to demo_output.asic")


if __name__ == "__main__":
    run_full_workflow_demo()
