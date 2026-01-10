# WF-01: KYC & User Onboarding Strategy

**Goal**: Establish a Verified Identity (`CITIZEN` or `OFFICER`) bound to a PQC KeyPair.

## 1. Actors
- **User (Unverified)**: Any public internet user.
- **Identity Service**: `backend/identity-service` (Keycloak + PostgreSQL).
- **Admin (Officer)**: Government official using Admin Portal.

## 2. Process Flow

### Phase A: Registration (Self-Service)
1.  **User** visits Public Portal (`/register`).
2.  **User** enters PII (Full Name, Citizen ID, Email, Phone).
3.  **User** sets a Password.
4.  **System** creates account with status `UNVERIFIED`.
    - *API*: `POST /api/v1/auth/register`

### Phase B: eKYC Submission (Document Upload)
1.  **User** logs in (Status: `UNVERIFIED`).
2.  **User** navigates to "Verify Identity".
3.  **User** uploads photos of Citizen ID (Front/Back) and/or Face Scan.
    - *Note*: In this MVP, we mock the AI verification but store the artifacts.
4.  **System** stores eKYC data in `secure-storage` (MinIO).
5.  **System** updates status to `PENDING_APPROVAL`.
    - *API*: `POST /api/v1/kyc/submit`

### Phase C: Admin Verification (Manual/Mock)
1.  **Officer** logs into Admin Portal.
2.  **Officer** views "Pending KYC Requests".
3.  **Officer** reviews uploaded documents vs submitted PII.
4.  **Officer** clicks "Approve".
5.  **System** updates status to `VERIFIED`.
    - *API*: `POST /api/v1/admin/kyc/{userId}/approve`

### Phase D: Post-Condition
- User now has `ROLE_CITIZEN`.
- User can access **Certificate Enrollment** (WF-02).

## 3. Data Model (Identity)
```json
{
  "userId": "uuid",
  "username": "citizen_id",
  "status": "VERIFIED",
  "roles": ["CITIZEN"],
  "attributes": {
    "province": "Hanoi",
    "district": "Ba Dinh"
  }
}
```
