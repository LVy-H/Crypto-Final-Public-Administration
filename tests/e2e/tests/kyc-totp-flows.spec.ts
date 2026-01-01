import { test, expect } from '@playwright/test';

test.describe('KYC and TOTP Integration Tests', () => {

    test('KYC-1: Cert Request WITHOUT KYC fails', async ({ request }) => {
        // 1. Login
        const loginResp = await request.post('/api/v1/auth/login', {
            data: { username: 'admin_capture', password: 'SecurePass123!' }
        });
        const { sessionId } = await loginResp.json();
        const headers = { 'Authorization': `Bearer ${sessionId}` };
        console.log(`[KYC-1] Session ID: ${sessionId}`);

        // 2. Call RA Request (Missing KYC)
        const resp = await request.post('/api/v1/ra/request', {
            headers,
            data: {
                username: 'admin_capture',
                email: 'admin@test.com',
                algorithm: 'ML-DSA-44'
            }
        });
        console.log(`[KYC-1] Status: ${resp.status()}`);
        if (resp.status() !== 200) {
            try { console.log(`[KYC-1] Body: ${await resp.text()}`); } catch (e) { }
        }

        // Validation failure should be 400 or 500, NOT 403
        expect(resp.status()).not.toBe(200);
        expect(resp.status()).not.toBe(403); // Critical check for session/auth
    });

    test('KYC-2: Cert Request WITH Valid KYC succeeds', async ({ request, baseURL }) => {
        // 1. Login as admin_capture (RA Officer)
        const loginResp = await request.post(`${baseURL}/api/v1/auth/login`, {
            data: {
                username: 'admin_capture',
                password: 'SecurePass123!'
            }
        });
        expect(loginResp.ok()).toBeTruthy();
        const { sessionId } = await loginResp.json();
        console.log(`[KYC-2] Extracted Session ID (Body): ${sessionId}`);

        // Check if it needs decoding (if it looks like Base64)
        // If it's pure UUID (dashed), it's likely raw.
        let tokenToSend = sessionId;
        if (!sessionId.includes('-') && sessionId.length > 36) {
            tokenToSend = Buffer.from(sessionId, 'base64').toString('ascii');
            console.log(`[KYC-2] Decoded Session ID: ${tokenToSend}`);
        }



        // 2. Submit Request
        const resp = await request.post(`${baseURL}/api/v1/ra/request`, {
            headers: {
                'X-Auth-Token': tokenToSend,
                'Authorization': `Bearer ${tokenToSend}`
            },
            data: {
                username: 'test_user_kyc_success',
                email: 'success@gov.vn',
                // Use ECDSA for dev environment as SoftwareKeyStorageService only supports EC
                algorithm: 'SHA384withECDSA',
                kycData: {
                    cccdNumber: '012345678901',
                    fullName: 'Nguyen Van Success',
                    email: 'success@gov.vn',
                    province: 'Hanoi',
                    district: 'Hoan Kiem',
                    organization: 'Gov',
                    country: 'VN'
                }
            }
        });

        if (resp.status() !== 200) {
            try { console.log(`[KYC-2] Body: ${await resp.text()}`); } catch (e) { }
        }
        expect(resp.status()).toBe(200);
    });

    test('TOTP-1: Admin Approval requires TOTP', async ({ request }) => {
        const loginResp = await request.post('/api/v1/auth/login', {
            data: { username: 'admin_capture', password: 'SecurePass123!' }
        });
        const { sessionId } = await loginResp.json();
        const headers = { 'Authorization': `Bearer ${sessionId}` };

        const randomId = '123e4567-e89b-12d3-a456-426614174000';
        const approveResp = await request.post(`/api/v1/certificates/${randomId}/approve`, {
            headers,
            data: {} // Missing otpCode
        });

        console.log(`[TOTP-1] Status: ${approveResp.status()}`);
        if (approveResp.status() !== 200) {
            try { console.log(`[TOTP-1] Body: ${await approveResp.text()}`); } catch (e) { }
        }

        expect(approveResp.status()).toBe(400);
        const body = await approveResp.json();
        expect(body.error).toContain('Invalid or missing TOTP');
    });

});
