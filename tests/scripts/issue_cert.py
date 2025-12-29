import urllib.request
import json
import ssl

# Configuration
RA_ID = "4ad9ac82-75a2-482b-a249-0a9dca96819d"
URL = "http://localhost:8081/api/v1/ca/issue"
SUBJECT_DN = "CN=test_browser_user,O=Citizen,C=VN"

# Read CSR
with open("core/ca-authority/test_user.csr", "r") as f:
    csr_content = f.read()

payload = {
    "issuingRaId": RA_ID,
    "csr": csr_content,
    "subjectDn": SUBJECT_DN
}

data = json.dumps(payload).encode('utf-8')
req = urllib.request.Request(URL, data=data, headers={'Content-Type': 'application/json'})

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

try:
    print(f"Submitting CSR to {URL} with RA ID {RA_ID}...")
    with urllib.request.urlopen(req, context=ctx, timeout=10) as response:
        print("Status:", response.status)
        if response.status == 200:
            resp_body = response.read().decode('utf-8')
            data = json.loads(resp_body)
            cert_pem = data.get("certificate")
            if cert_pem:
                with open("core/ca-authority/test_user_cert.pem", "w") as f:
                    f.write(cert_pem)
                print("Saved certificate to core/ca-authority/test_user_cert.pem")
            else:
                print("No certificate field in response:", data)
        else:
             print("Error status:", response.status)
except Exception as e:
    print("Request failed:", e)
