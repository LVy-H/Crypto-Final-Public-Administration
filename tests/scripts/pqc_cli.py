#!/usr/bin/env python3
"""
PQC Digital Signature Platform - Command Line Interface
========================================================
A complete CLI for interacting with the Government PQC Digital Signature Platform.

Usage:
    pqc-cli auth login <username> <password>
    pqc-cli auth register <username> <password> [--role ROLE]
    pqc-cli admin approve-kyc <username>
    pqc-cli doc upload <file>
    pqc-cli doc sign <doc-id> --signer <name>
    pqc-cli doc countersign <asic-file> --signer <name>
    pqc-cli doc verify <asic-file>
    pqc-cli tsa info
"""

import click
import requests
import base64
import json
import os
import sys
import datetime
from pathlib import Path
from typing import Optional

# Optional cryptography for certificate generation
try:
    from cryptography import x509
    from cryptography.hazmat.backends import default_backend
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import rsa
    HAS_CRYPTO = True
except ImportError:
    HAS_CRYPTO = False

# Configuration
DEFAULT_BASE_URL = "http://localhost:8080/api/v1"
CONFIG_FILE = Path.home() / ".pqc-cli" / "config.json"
SESSION_FILE = Path.home() / ".pqc-cli" / "session.json"


class PQCClient:
    """HTTP client for the PQC platform."""
    
    def __init__(self, base_url: str = DEFAULT_BASE_URL):
        self.base_url = base_url
        self.session = requests.Session()
        self._load_session()
    
    def _load_session(self):
        """Load saved session if exists."""
        if SESSION_FILE.exists():
            try:
                data = json.loads(SESSION_FILE.read_text())
                self.session.cookies.update(data.get('cookies', {}))
            except:
                pass
    
    def _save_session(self):
        """Save session cookies."""
        SESSION_FILE.parent.mkdir(parents=True, exist_ok=True)
        SESSION_FILE.write_text(json.dumps({
            'cookies': dict(self.session.cookies)
        }))
    
    def register(self, username: str, password: str, role: str = "USER") -> dict:
        resp = self.session.post(
            f"{self.base_url}/auth/register",
            json={"username": username, "password": password, "role": role}
        )
        return {"success": resp.status_code == 200, "message": resp.text, "status": resp.status_code}
    
    def login(self, username: str, password: str) -> dict:
        resp = self.session.post(
            f"{self.base_url}/auth/login",
            json={"username": username, "password": password}
        )
        if resp.status_code == 200:
            self._save_session()
        return {"success": resp.status_code == 200, "message": resp.text, "status": resp.status_code}
    
    def approve_kyc(self, username: str) -> dict:
        resp = self.session.post(
            f"{self.base_url}/admin/approve-kyc",
            json={"username": username, "action": "APPROVE"}
        )
        return {"success": resp.status_code == 200, "message": resp.text, "status": resp.status_code}
    
    def upload_document(self, file_path: Path) -> dict:
        with open(file_path, 'rb') as f:
            files = {'file': (file_path.name, f, 'application/octet-stream')}
            resp = self.session.post(f"{self.base_url}/documents/upload", files=files)
        if resp.status_code == 200:
            return {"success": True, "docId": resp.json().get('docId')}
        return {"success": False, "message": resp.text}
    
    def sign_document(self, doc_id: str, signature: bytes, certificate: bytes) -> Optional[bytes]:
        payload = {
            "docId": doc_id,
            "signature": base64.b64encode(signature).decode(),
            "certificate": base64.b64encode(certificate).decode()
        }
        resp = self.session.post(f"{self.base_url}/documents/finalize-asic", json=payload)
        if resp.status_code == 200:
            return resp.content
        return None
    
    def countersign(self, asic_bytes: bytes, signature: bytes, certificate: bytes) -> Optional[bytes]:
        files = {'file': ('package.asic', asic_bytes, 'application/vnd.etsi.asic-e+zip')}
        data = {
            'signature': base64.b64encode(signature).decode(),
            'certificate': base64.b64encode(certificate).decode()
        }
        resp = self.session.post(f"{self.base_url}/documents/countersign", files=files, data=data)
        if resp.status_code == 200:
            return resp.content
        return None
    
    def verify(self, asic_bytes: bytes) -> dict:
        files = {'file': ('package.asic', asic_bytes, 'application/vnd.etsi.asic-e+zip')}
        resp = self.session.post(f"{self.base_url}/documents/verify-asic", files=files)
        if resp.status_code == 200:
            return resp.json()
        return {"valid": False, "error": resp.text}
    
    def tsa_info(self) -> dict:
        resp = self.session.get(f"{self.base_url}/tsa/info")
        if resp.status_code == 200:
            return resp.json()
        return {"error": resp.text}


def generate_test_cert(common_name: str) -> tuple:
    """Generate a test certificate and private key."""
    if not HAS_CRYPTO:
        raise click.ClickException("cryptography library required: pip install cryptography")
    
    key = rsa.generate_private_key(65537, 2048, default_backend())
    cert = x509.CertificateBuilder().subject_name(
        x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME, common_name)])
    ).issuer_name(
        x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME, common_name)])
    ).public_key(key.public_key()).serial_number(
        x509.random_serial_number()
    ).not_valid_before(
        datetime.datetime.now(datetime.UTC)
    ).not_valid_after(
        datetime.datetime.now(datetime.UTC) + datetime.timedelta(days=365)
    ).sign(key, hashes.SHA256(), default_backend())
    
    return (
        cert.public_bytes(serialization.Encoding.DER),
        key.private_bytes(
            serialization.Encoding.PEM,
            serialization.PrivateFormat.PKCS8,
            serialization.NoEncryption()
        )
    )


# CLI Definition
@click.group()
@click.option('--url', default=DEFAULT_BASE_URL, envvar='PQC_API_URL', help='API base URL')
@click.pass_context
def cli(ctx, url):
    """PQC Digital Signature Platform CLI."""
    ctx.ensure_object(dict)
    ctx.obj['client'] = PQCClient(url)


# Auth commands
@cli.group()
def auth():
    """Authentication commands."""
    pass


@auth.command()
@click.argument('username')
@click.argument('password')
@click.option('--role', default='USER', type=click.Choice(['USER', 'ADMIN', 'OFFICER', 'CA_OPERATOR']))
@click.pass_context
def register(ctx, username, password, role):
    """Register a new user."""
    client = ctx.obj['client']
    result = client.register(username, password, role)
    if result['success']:
        click.secho(f"✅ User '{username}' registered with role '{role}'", fg='green')
    else:
        click.secho(f"❌ Registration failed: {result['message']}", fg='red')


@auth.command()
@click.argument('username')
@click.argument('password')
@click.pass_context
def login(ctx, username, password):
    """Login to the platform."""
    client = ctx.obj['client']
    result = client.login(username, password)
    if result['success']:
        click.secho(f"✅ Logged in as '{username}'", fg='green')
    else:
        click.secho(f"❌ Login failed: {result['message']}", fg='red')


# Admin commands
@cli.group()
def admin():
    """Admin commands."""
    pass


@admin.command('approve-kyc')
@click.argument('username')
@click.pass_context
def approve_kyc(ctx, username):
    """Approve KYC for a user."""
    client = ctx.obj['client']
    result = client.approve_kyc(username)
    if result['success']:
        click.secho(f"✅ KYC approved for '{username}'", fg='green')
    else:
        click.secho(f"❌ KYC approval failed: {result['message']}", fg='red')


# Document commands
@cli.group()
def doc():
    """Document operations."""
    pass


@doc.command()
@click.argument('file', type=click.Path(exists=True))
@click.pass_context
def upload(ctx, file):
    """Upload a document."""
    client = ctx.obj['client']
    result = client.upload_document(Path(file))
    if result['success']:
        click.secho(f"✅ Uploaded: docId={result['docId']}", fg='green')
    else:
        click.secho(f"❌ Upload failed: {result.get('message', 'Unknown error')}", fg='red')


@doc.command()
@click.argument('doc_id')
@click.option('--signer', required=True, help='Signer common name for certificate')
@click.option('--output', '-o', default='signed.asic', help='Output ASiC filename')
@click.pass_context
def sign(ctx, doc_id, signer, output):
    """Sign a document and create ASiC container."""
    client = ctx.obj['client']
    
    # Generate certificate and signature
    cert_der, _ = generate_test_cert(signer)
    signature = f"ML-DSA-SIG-{signer}".encode() * 10  # Simulated signature
    
    click.echo(f"🔑 Generated certificate for: {signer}")
    
    asic_bytes = client.sign_document(doc_id, signature, cert_der)
    if asic_bytes:
        Path(output).write_bytes(asic_bytes)
        click.secho(f"✅ Signed ASiC saved to: {output} ({len(asic_bytes)} bytes)", fg='green')
    else:
        click.secho("❌ Signing failed", fg='red')


@doc.command()
@click.argument('asic_file', type=click.Path(exists=True))
@click.option('--signer', required=True, help='Countersigner common name')
@click.option('--output', '-o', help='Output filename (default: adds -countersigned)')
@click.pass_context
def countersign(ctx, asic_file, signer, output):
    """Add a countersignature to an ASiC container."""
    client = ctx.obj['client']
    
    asic_bytes = Path(asic_file).read_bytes()
    cert_der, _ = generate_test_cert(signer)
    signature = f"ML-DSA-COUNTERSIG-{signer}".encode() * 10
    
    click.echo(f"🔑 Generated certificate for: {signer}")
    
    new_asic = client.countersign(asic_bytes, signature, cert_der)
    if new_asic:
        out_file = output or asic_file.replace('.asic', '-countersigned.asic')
        Path(out_file).write_bytes(new_asic)
        click.secho(f"✅ Countersigned ASiC saved to: {out_file} ({len(new_asic)} bytes)", fg='green')
    else:
        click.secho("❌ Countersigning failed", fg='red')


@doc.command()
@click.argument('asic_file', type=click.Path(exists=True))
@click.option('--json', 'as_json', is_flag=True, help='Output as JSON')
@click.pass_context
def verify(ctx, asic_file, as_json):
    """Verify signatures in an ASiC container."""
    client = ctx.obj['client']
    
    asic_bytes = Path(asic_file).read_bytes()
    result = client.verify(asic_bytes)
    
    if as_json:
        click.echo(json.dumps(result, indent=2))
        return
    
    click.echo(f"\n📄 Verification Report for: {asic_file}")
    click.echo("=" * 50)
    
    if result.get('valid'):
        click.secho("✅ VALID", fg='green', bold=True)
    else:
        click.secho("❌ INVALID", fg='red', bold=True)
        if result.get('errorMessage'):
            click.echo(f"   Error: {result['errorMessage']}")
    
    click.echo(f"\n📊 Summary:")
    click.echo(f"   Document: {result.get('documentName', 'unknown')}")
    click.echo(f"   Size: {result.get('documentSize', 0)} bytes")
    click.echo(f"   Signatures: {result.get('signatureCount', 0)}")
    
    for i, sig in enumerate(result.get('signatures', []), 1):
        click.echo(f"\n🔏 Signature {i}:")
        if sig.get('valid'):
            click.secho(f"   Status: VALID", fg='green')
        else:
            click.secho(f"   Status: INVALID", fg='red')
        click.echo(f"   Message: {sig.get('message', '')}")
        click.echo(f"   Signer: {sig.get('signerName', 'unknown')}")
        click.echo(f"   Timestamp: {sig.get('timestamp', 'N/A')}")
        click.echo(f"   Cert Valid: {sig.get('certificateNotBefore', '')} → {sig.get('certificateNotAfter', '')}")


# TSA commands
@cli.group()
def tsa():
    """Timestamp Authority commands."""
    pass


@tsa.command()
@click.pass_context
def info(ctx):
    """Get TSA information."""
    client = ctx.obj['client']
    result = client.tsa_info()
    
    if 'error' in result:
        click.secho(f"❌ Error: {result['error']}", fg='red')
        return
    
    click.echo("\n🕐 Timestamp Authority Information")
    click.echo("=" * 40)
    click.echo(f"   Name: {result.get('name', 'Unknown')}")
    click.echo(f"   Version: {result.get('version', 'Unknown')}")
    click.echo(f"   Protocol: {result.get('protocol', 'Unknown')}")
    click.echo(f"   Signature Algorithm: {result.get('signatureAlgorithm', 'Unknown')}")
    click.echo(f"   Accepted Digests: {', '.join(result.get('acceptedDigestAlgorithms', []))}")


# Demo command
@cli.command()
@click.option('--admin', 'admin_user', default='cli_admin', help='Admin username')
@click.option('--citizen', 'citizen_user', default='cli_citizen', help='Citizen username')
@click.option('--officer', 'officer_user', default='cli_officer', help='Officer username')
@click.pass_context
def demo(ctx, admin_user, citizen_user, officer_user):
    """Run a full workflow demo."""
    client = ctx.obj['client']
    
    click.echo("\n" + "=" * 60)
    click.echo("🚀 PQC DIGITAL SIGNATURE PLATFORM - FULL DEMO")
    click.echo("=" * 60)
    
    # Register users
    click.echo("\n📋 Phase 1: User Registration")
    client.register(admin_user, "admin123", "ADMIN")
    client.register(citizen_user, "citizen123", "USER")
    client.register(officer_user, "officer123", "OFFICER")
    click.secho("   ✅ Users registered", fg='green')
    
    # KYC
    click.echo("\n📋 Phase 2: KYC Approval")
    client.approve_kyc(citizen_user)
    client.approve_kyc(officer_user)
    click.secho("   ✅ KYC approved", fg='green')
    
    # Upload
    click.echo("\n📄 Phase 3: Document Upload")
    test_file = Path('/tmp/demo_contract.txt')
    test_file.write_text("OFFICIAL CONTRACT\nDate: 2026-01-10\nTerms apply...")
    result = client.upload_document(test_file)
    doc_id = result.get('docId')
    click.secho(f"   ✅ Uploaded: docId={doc_id}", fg='green')
    
    # Sign
    click.echo("\n🔏 Phase 4: Citizen Signs")
    cert_der, _ = generate_test_cert(f"{citizen_user}-ML-DSA")
    signature = f"ML-DSA-SIG-citizen".encode() * 10
    asic = client.sign_document(str(doc_id), signature, cert_der)
    click.secho(f"   ✅ Signed ({len(asic)} bytes)", fg='green')
    
    # Countersign
    click.echo("\n👮 Phase 5: Officer Countersigns")
    cert_der2, _ = generate_test_cert(f"{officer_user}-ML-DSA")
    signature2 = f"ML-DSA-SIG-officer".encode() * 10
    final_asic = client.countersign(asic, signature2, cert_der2)
    click.secho(f"   ✅ Countersigned ({len(final_asic)} bytes)", fg='green')
    
    # Verify
    click.echo("\n🔍 Phase 6: Verification")
    result = client.verify(final_asic)
    click.echo(f"   Valid: {result.get('valid')}")
    click.echo(f"   Signatures: {result.get('signatureCount')}")
    for sig in result.get('signatures', []):
        click.echo(f"   - {sig.get('signerName')}: {sig.get('message')}")
    
    # Save
    Path('demo_output.asic').write_bytes(final_asic)
    click.echo("\n💾 Saved: demo_output.asic")
    
    click.echo("\n" + "=" * 60)
    click.secho("🎉 DEMO COMPLETE!", fg='green', bold=True)
    click.echo("=" * 60)


if __name__ == '__main__':
    cli()
