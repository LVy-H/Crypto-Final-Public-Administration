
import urllib.request
import urllib.error
import json
import ssl
import sys

API_BASE = "https://api.gov-id.lvh.id.vn"

# Disable SSL verification for local testing
ssl_context = ssl.create_default_context()
ssl_context.check_hostname = False
ssl_context.verify_mode = ssl.CERT_NONE

def request(method, endpoint, data=None):
    url = f"{API_BASE}{endpoint}"
    headers = {"Content-Type": "application/json"}
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    
    try:
        with urllib.request.urlopen(req, timeout=30, context=ssl_context) as response:
            return json.loads(response.read().decode())
    except urllib.error.HTTPError as e:
        print(f"Error calling {endpoint}: {e.code}")
        print(e.read().decode())
        sys.exit(1)
    except Exception as e:
        print(f"Exception calling {endpoint}: {e}")
        sys.exit(1)

def main():
    print("Initializing CA Hierarchy...")
    
    # 1. Initialize Root CA
    print("1. Initializing Root CA...")
    root_resp = request("POST", "/api/v1/ca/root/init", {"name": "National Root CA"})
    root_id = root_resp.get("id")
    print(f"   Root CA Initialized: {root_id}")
    
    # 2. Create Provincial CA (Issuing CA)
    print("2. Creating Provincial CA...")
    prov_resp = request("POST", "/api/v1/ca/provincial", {
        "parentCaId": root_id,
        "name": "Dong Nai Provincial CA"
    })
    prov_id = prov_resp.get("id")
    print(f"   Provincial CA Created: {prov_id}")
    
    print("CA Hierarchy Initialization Complete.")

if __name__ == "__main__":
    main()
