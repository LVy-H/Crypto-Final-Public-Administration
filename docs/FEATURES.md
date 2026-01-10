# 🚀 Features & Screenshots

Comprehensive visual documentation of the GovTech PQC Digital Signature Portal.

## 🔐 Role-Based Access Control

| Feature | Citizens | Officers |
|:--------|:--------:|:--------:|
| **Generate PQC Keys** | ✅ | ❌ |
| **Verify Documents** | ✅ | ✅ |
| **KYC Approval** | ❌ | ✅ |
| **TSA Management** | ❌ | ✅ |

> [!IMPORTANT]
> **Sole Control**: Only Citizens hold private keys (in Browser IndexedDB). Admins cannot sign on their behalf.

## 🎬 Signing Process (ASiC-E)

### Step 1: Upload & Hash
The user uploads a document. The browser calculates the SHA-384 hash locally.

![Client-Side Signing](screenshots/sign.png)

### Step 2: Key Access (Sole Control)
The user unlocks their Private Key with a passphrase. The key is decrypted in memory.

![Key Access](screenshots/totp_modal.png)

### Step 3: ASiC Generation
1.  **Sign**: WASM module generates ML-DSA signature.
2.  **Package**: The backend wraps the signature and document into an `.asic` container.
3.  **Download**: The user receives a `package.asic` file.

## 📱 Portal Features

### Dashboard
Overview of recent documents and certificate status.
![Dashboard](screenshots/dashboard.png)

### Registration & KeyGen
Generation of Post-Quantum Keys during onboarding.
![Key Generation](screenshots/registration_key_gen.png)

### Verification Portal
Users upload `.asic` files to verify integrity and timestamp.
![Verify](screenshots/verify_interface.png)

## 📜 Compliance Standards

- **Decree 23**: Full compliance for Digital Signatures.
- **Pure PQC**: NIST FIPS 204 (ML-DSA).
- **Format**: ETSI EN 319 162 (ASiC-E).
