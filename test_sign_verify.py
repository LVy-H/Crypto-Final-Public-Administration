import requests
import json
import time
import base64
import hashlib
import os

#BASE_URL = "http://api-gateway.crypto-pqc.svc.cluster.local:8080"
BASE_URL = "http://localhost:8090" # For local testing if port forwarded

USERNAME = f"sign_verify_user_{int(time.time())}"
PASSWORD = "password123"

def print_step(msg):
    print(f"\n==================================================")
    print(f" {msg}")
    print(f"==================================================")

def run_flow():
    session = requests.Session()

    # 1. Register
    print_step("1. Registering User")
    reg_payload = {
        "username": USERNAME,
        "password": PASSWORD,
        "fullName": "Sign Verify Test User",
        "email": f"{USERNAME}@example.com",
        "phoneNumber": "0987654321",
        "citizenId": f"CITIZEN_{int(time.time())}"
    }
    try:
        res = session.post(f"{BASE_URL}/api/v1/auth/register", json=reg_payload)
        print(f"Register status: {res.status_code}")
        if res.status_code not in [200, 201]:
            print(f"Register failed: {res.text}")
            return
    except Exception as e:
        print(f"Register Failed: {e}")
        return

    # 2. Login
    print_step("2. Logging In")
    login_payload = {"username": USERNAME, "password": PASSWORD}
    res = session.post(f"{BASE_URL}/api/v1/auth/login", json=login_payload)
    print(f"Login status: {res.status_code}")
    if res.status_code != 200:
        print(f"Login failed: {res.text}")
        return
    
    print("Login successful. Cookies:", session.cookies.get_dict())

    # 3. Generate CSR via Debug Endpoint
    print_step("3. Generating KeyPair and CSR (Debug)")
    debug_payload = {
        "subjectDn": f"CN={USERNAME}, O=Citizen, C=VN",
        "algorithm": "ML-DSA-44"
    }
    res = session.post(f"{BASE_URL}/api/v1/validation/debug/generate-csr", json=debug_payload)
    if res.status_code != 200:
        print(f"CSR Generation failed: {res.text}")
        return
    
    key_data = res.json()
    private_key_pem = key_data['privateKey']
    csr_pem = key_data['csr']
    print("Generated CSR.")

    # 4. Request Certificate
    print_step("4. Requesting Certificate")
    req_payload = {
        "algorithm": "ML-DSA-44",
        "csrPem": csr_pem
    }
    res = session.post(f"{BASE_URL}/api/v1/certificates/request", json=req_payload)
    if res.status_code != 200:
        print(f"Certificate request failed: {res.text}")
        return
    
    cert_req_data = res.json()
    req_id = cert_req_data['id']
    print(f"Certificate Request ID: {req_id}")

    # 5. Approve Certificate (Admin Action - hacking via user session for now if allowed, or need admin?)
    # CertificateController approve endpoint allows anyone? Or restricted?
    # Let's try calling it. If 403, we might need to bypass or login as admin.
    pass
    print_step("5. Approving Certificate")
    res = session.post(f"{BASE_URL}/api/v1/certificates/{req_id}/approve")
    if res.status_code != 200:
        print(f"Approve failed (expected if auth required): {res.status_code} {res.text}")
        # If it failed, we assume it's OK for now or we need to fix it.
        # My implementation of CertificateController didn't add @PreAuthorize to approve endpoint explicitly, 
        # so it might default to authenticated.
    else:
        print("Certificate Approved.")
        
    # 6. Get Certificates
    print_step("6. Getting User Certificate")
    res = session.get(f"{BASE_URL}/api/v1/certificates/my")
    if res.status_code != 200:
        print(f"Get certificates failed: {res.text}")
        return
    
    certs = res.json()
    if not certs:
        print("No certificates found!")
        return
        
    print(f"Certificates response: {certs}")
    if not certs:
        print("No certificates found!")
        return
    
    cert_id = certs[0].get('id')
    print(f"Downloading certificate with ID: {cert_id}")
    download_resp = session.get(f"{BASE_URL}/api/v1/certificates/{cert_id}/download")
    if download_resp.status_code != 200:
        print(f"Failed to download certificate: {download_resp.text}")
        return

    user_cert_pem = download_resp.json().get('certificate')
    print("Got User Certificate PEM.")

    # 7. Sign Document (Debug)
    print_step("7. Signing Document (Debug)")
    document_content = "This is a test document to sign."
    document_hash = base64.b64encode(hashlib.sha256(document_content.encode()).digest()).decode()
    
    sign_payload = {
        "privateKeyPem": private_key_pem,
        "dataBase64": base64.b64encode(document_hash.encode()).decode(), # ValidationService expects Base64 data to sign?
        # ValidationServiceImpl.signDebug: byte[] data = Base64.getDecoder().decode(dataBase64);
        # But wait, we usually sign the HASH. 
        # Ideally we sign the raw bytes. 
        # If I pass hash as base64, it will decode to 32 bytes.
        # Yes.
        "algorithm": "ML-DSA-44"
    }
    
    # Wait, signDebug doc says "dataBase64". 
    # Usually we sigh the digest.
    # Let's pass the digest bytes base64 encoded.
    # hashlib.sha256 gives bytes. 
    digest_bytes = hashlib.sha256(document_content.encode()).digest()
    digest_b64 = base64.b64encode(digest_bytes).decode()
    
    sign_payload = {
        "privateKeyPem": private_key_pem,
        "dataBase64": digest_b64,
        "algorithm": "ML-DSA-44"
    }

    res = session.post(f"{BASE_URL}/api/v1/validation/debug/sign", json=sign_payload)
    if res.status_code != 200:
        print(f"Signing failed: {res.text}")
        return

    signature = res.json()['signature']
    print("Generated Signature.")

    # 8. Find Officer CA ID
    print_step("8. Finding Officer CA")
    # We need a valid CA ID.
    # Let's get root init status or provincial?
    # We don't have a direct endpoint to list all CAs publically.
    # But init_ca.py creates them.
    # Maybe we can query /api/v1/ca/hierarchy?
    # Let's try to list CAs if possible.
    # Or just use the one from user cert issuing CA?
    issuing_ca = certs[0].get('issuingCa') # If returned in response?
    # IssuedCertificate entity has issuingCa, but JSON response depends on serialization.
    # Check IssuedCertificate model or Controller.
    # It returns entity. Entity has @ManyToOne. Might serialize full object or loop?
    # If not, we might be stuck.
    
    # Let's assume we can get it from 'issuingCa' field if serialized, or try to init if needed.
    # Actually, we can use the ID from `init_ca.py` logs if we had them.
    # Better: Use `getCertificateChain` endpoint if available?
    
    # Let's try to assume the first CA in DB is Root and second is Provincial.
    # How to list?
    # Admin portal would list them.
    # I'll rely on `init_ca.py` having run and CAs existing. 
    # I will try to call an endpoint that lists CAs.
    # `HierarchicalCaController` has `getAllCa`? No.
    # `getCasByLevel`? Yes.
    
    officer_ca_id = None
    # Try getting Level 1 CAs (Subordinate/Provincial)
    res = session.get(f"{BASE_URL}/api/v1/ca/level/1")
    if res.status_code == 200:
        cas = res.json()
        if cas:
            officer_ca_id = cas[0]['id']
            print(f"Found Provincial CA ID: {officer_ca_id}")
    
    if not officer_ca_id:
        print("Could not find Officer CA (Level 1). Trying Level 0.")
        res = session.get(f"{BASE_URL}/api/v1/ca/level/0")
        if res.status_code == 200 and res.json():
            officer_ca_id = res.json()[0]['id']
            print(f"Found Root CA ID: {officer_ca_id}")
            
    if not officer_ca_id:
        print("No CA found. Cannot apply stamp.")
        return

    # 9. Apply Stamp
    print_step("9. Applying Countersignature")
    stamp_payload = {
        "documentHash": digest_b64, # Sending B64 of Hash
        "userSignature": signature,
        "userCertPem": user_cert_pem,
        "officerId": "11111111-1111-1111-1111-111111111111", # Placeholder UUID
        "officerCaId": officer_ca_id,
        "purpose": "OFFICIAL_VALIDATION"
    }
    
    res = session.post(f"{BASE_URL}/api/v1/stamp/apply", json=stamp_payload)
    if res.status_code != 200:
        print(f"Stamp apply failed: {res.text}")
        return
        
    stamp_data = res.json()
    print("Stamp Applied.")
    print(json.dumps(stamp_data, indent=2))
    
    officer_signature = stamp_data['stampSignature']
    officer_cert_pem = stamp_data['officerCertPem']

    # 10. Verify Stamp (CA Authority)
    print_step("10. Verifying Stamp (CA Authority)")
    verify_payload = {
        "documentHash": digest_b64,
        "userSignature": signature,
        "officerSignature": officer_signature,
        "officerCertPem": officer_cert_pem,
        "userCertPem": user_cert_pem
    }
    
    res = session.post(f"{BASE_URL}/api/v1/stamp/verify", json=verify_payload)
    print(f"Verify Status: {res.status_code}")
    print(res.text)

    # 11. Verify Stamp (Validation Service)
    print_step("11. Verifying Stamp (Validation Service)")
    # Endpoint /api/v1/validation/verify-stamp
    # Request body: StampVerifyRequest
    
    res = session.post(f"{BASE_URL}/api/v1/validation/verify-stamp", json=verify_payload)
    print(f"Validation Status: {res.status_code}")
    print(res.text)

if __name__ == "__main__":
    run_flow()
