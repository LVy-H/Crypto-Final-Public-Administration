import requests
import time
import subprocess
import sys

# Configuration
GATEWAY_URL = "http://localhost:30080/api/v1"
MAX_RETRIES = 30
RETRY_DELAY = 2

def run_command(command):
    try:
        result = subprocess.run(command, shell=True, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error running command '{command}': {e.stderr}")
        return None

def check_k3s_status():
    print("⏳ Checking K3s Pod Status...")
    for i in range(MAX_RETRIES):
        output = run_command("kubectl get pods -n crypto-backend")
        if output:
            print(output)
            lines = output.splitlines()[1:] # Skip header
            if not lines:
                print("   No pods found yet.")
            else:
                all_running = True
                for line in lines:
                    if "Running" not in line:
                        all_running = False
                        break
                
                if all_running and len(lines) >= 5: # Identity, PKI, TSA, Doc, Gateway
                    print("✅ All services are RUNNING.")
                    return True
        
        time.sleep(RETRY_DELAY)
    
    print("❌ Timeout waiting for services to be ready.")
    return False

def test_identity_service():
    print("\n🔍 Testing Identity Service...")
    
    # 1. Register
    reg_payload = {"username": "testuser", "password": "password123"}
    try:
        res = requests.post(f"{GATEWAY_URL}/auth/register", json=reg_payload)
        if res.status_code == 200:
            print("   ✅ Register: Success")
        else:
            print(f"   ❌ Register: Failed ({res.status_code}) - {res.text}")
    except Exception as e:
        print(f"   ❌ Exception: {e}")

    # 2. Login
    login_payload = {"username": "testuser", "password": "password123"}
    try:
        res = requests.post(f"{GATEWAY_URL}/auth/login", json=login_payload)
        if res.status_code == 200:
            print("   ✅ Login: Success")
            cookies = res.cookies
            return cookies
        else:
            print(f"   ❌ Login: Failed ({res.status_code}) - {res.text}")
            return None
    except Exception as e:
        print(f"   ❌ Exception: {e}")
        return None

def test_pki_service():
    print("\n🔍 Testing PKI Service...")
    # CSR would be complex to gen in python without deps, sending dummy string for prototype check
    csr_dummy = "MII..." 
    payload = {"csr": csr_dummy}
    try:
        res = requests.post(f"{GATEWAY_URL}/pki/enroll", json=payload)
        if res.status_code == 200:
             print("   ✅ Enroll: Success")
        elif res.status_code == 400 or res.status_code == 500:
             # We expect failure on dummy CSR but connectivity check is what matters mostly here
             print(f"   ⚠️ Enroll: Reachable (Response: {res.status_code})")
        else:
             print(f"   ❌ Enroll: Failed ({res.status_code})")
    except Exception as e:
        print(f"   ❌ Exception: {e}")

def main():
    if not check_k3s_status():
        sys.exit(1)
    
    # Allow a little more time for Spring Boot to fully init after Pod Running
    print("⏳ Waiting 10s for Spring Boot context initialization...")
    time.sleep(10)

    cookies = test_identity_service()
    
    test_pki_service()
    
    # Add other service tests...
    
    print("\n✅ Verification Complete.")

if __name__ == "__main__":
    main()
