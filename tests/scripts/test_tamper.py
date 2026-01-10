#!/usr/bin/env python3
"""Test tamper detection in ASiC container verification."""

import requests
import base64
import datetime
from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
import zipfile
import io

BASE_URL = 'http://localhost:8080/api/v1'

print('=' * 60)
print('TAMPER DETECTION TEST')
print('=' * 60)

# Step 1: Create and sign a document
print('\n[1] Creating and signing original document...')
content = b'ORIGINAL CONTRACT CONTENT - Do not modify'

# Upload
files = {'file': ('contract.txt', content, 'application/octet-stream')}
resp = requests.post(f'{BASE_URL}/documents/upload', files=files)
doc_id = resp.json().get('docId')
print(f'    Uploaded: docId={doc_id}')

# Generate cert and signature
key = rsa.generate_private_key(65537, 2048, default_backend())
cert = x509.CertificateBuilder().subject_name(
    x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME, 'Test Signer')])
).issuer_name(
    x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME, 'Test Signer')])
).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(
    datetime.datetime.now(datetime.UTC)
).not_valid_after(
    datetime.datetime.now(datetime.UTC) + datetime.timedelta(days=365)
).sign(key, hashes.SHA256(), default_backend())

cert_der = cert.public_bytes(serialization.Encoding.DER)
fake_sig = b'ML-DSA-SIGNATURE-PLACEHOLDER' * 10

# Sign
payload = {
    'docId': doc_id,
    'signature': base64.b64encode(fake_sig).decode(),
    'certificate': base64.b64encode(cert_der).decode()
}
resp = requests.post(f'{BASE_URL}/documents/finalize-asic', json=payload)
asic_bytes = resp.content
print(f'    Original ASiC size: {len(asic_bytes)} bytes')

# Step 2: Verify original (should pass)
print('\n[2] Verifying ORIGINAL document...')
files = {'file': ('test.asic', asic_bytes, 'application/vnd.etsi.asic-e+zip')}
resp = requests.post(f'{BASE_URL}/documents/verify-asic', files=files)
result = resp.json()
print(f'    Valid: {result["valid"]}')
print(f'    Message: {result["signatures"][0]["message"]}')
print(f'    Timestamp: {result["signatures"][0]["timestamp"]}')

# Step 3: Tamper with the document inside ASiC
print('\n[3] TAMPERING with document content...')
zf_in = zipfile.ZipFile(io.BytesIO(asic_bytes), 'r')
zf_out_bytes = io.BytesIO()
zf_out = zipfile.ZipFile(zf_out_bytes, 'w')

for item in zf_in.namelist():
    data = zf_in.read(item)
    if item == 'contract.txt':
        # TAMPER: Change the content
        data = b'MODIFIED CONTRACT - THIS IS FRAUD!'
        print(f'    Modified: {item}')
    zf_out.writestr(item, data)

zf_out.close()
tampered_asic = zf_out_bytes.getvalue()
print(f'    Tampered ASiC size: {len(tampered_asic)} bytes')

# Step 4: Verify tampered document (should FAIL)
print('\n[4] Verifying TAMPERED document...')
files = {'file': ('tampered.asic', tampered_asic, 'application/vnd.etsi.asic-e+zip')}
resp = requests.post(f'{BASE_URL}/documents/verify-asic', files=files)
result = resp.json()
print(f'    Valid: {result["valid"]}')
print(f'    Message: {result["signatures"][0]["message"]}')

if not result['valid'] and 'mismatch' in result['signatures'][0]['message'].lower():
    print('\n✅ SUCCESS: Tamper was correctly detected!')
    print('   The signature verification caught the modified content.')
else:
    print('\n❌ FAILURE: Tamper was NOT detected!')
