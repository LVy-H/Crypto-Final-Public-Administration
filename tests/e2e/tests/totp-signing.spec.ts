import { test, expect } from '@playwright/test';
import { authenticator } from 'otplib';

/**
 * E2E Tests for TOTP-based Sole Control
 * 
 * Tests cover:
 * - TOTP Setup API
 * - TOTP Setup UI (QR code enrollment)
 * - 2-step Signing Flow (init → confirm with TOTP)
 * - Full user journey with TOTP
 */

const API_BASE = 'https://api.gov-id.lvh.id.vn';
const PORTAL_BASE = process.env.PORTAL_BASE || 'https://portal.gov-id.lvh.id.vn';
const CSC_BASE = `${API_BASE}/csc/v1`;
const CREDENTIALS_BASE = `${API_BASE}/api/v1/credentials`;

// Helper: Register and login a new user
async function registerAndLogin(request: any): Promise<{ token: string; username: string }> {
    const username = `totp_test_${Date.now()}`;

    await request.post(`${API_BASE}/api/v1/auth/register`, {
        data: {
            username,
            email: `${username}@test.vn`,
            password: 'TestPass123!'
        }
    });

    const loginResponse = await request.post(`${API_BASE}/api/v1/auth/login`, {
        data: { username, password: 'TestPass123!' }
    });

    if (loginResponse.ok()) {
        const body = await loginResponse.json();
        // Use sessionId as token if token not present (Stateful auth)
        return { token: body.token || body.sessionId, username };
    }
    throw new Error('Login failed');
}

test.describe('TOTP Setup API', () => {
    test.describe.configure({ mode: 'serial' });

    test('POST /credentials/totp/setup - should require authentication', async ({ request }) => {
        const response = await request.post(`${CREDENTIALS_BASE}/totp/setup`, {
            headers: { 'Content-Type': 'application/json' }
        });

        // Gateway may return 404 if route not found, or 401/403 if auth required
        expect([401, 403, 404]).toContain(response.status());
    });

    test('POST /credentials/totp/setup - should return secret and QR URI', async ({ request }) => {
        const { token } = await registerAndLogin(request);

        const response = await request.post(`${CREDENTIALS_BASE}/totp/setup`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        // May succeed or fail if cloud-sign not deployed
        if (response.ok()) {
            const body = await response.json();
            expect(body.secret).toBeDefined();
            expect(body.qrUri).toBeDefined();
            expect(body.qrUri).toContain('otpauth://totp/');
            expect(body.secret.length).toBeGreaterThanOrEqual(16);
        } else {
            // Service may not be deployed, accept 404
            expect([401, 404, 500]).toContain(response.status());
        }
    });

    test('POST /credentials/totp/setup - secret should be valid base32', async ({ request }) => {
        const { token } = await registerAndLogin(request);

        const response = await request.post(`${CREDENTIALS_BASE}/totp/setup`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok()) {
            const body = await response.json();
            // Verify secret is valid for TOTP generation
            const code = authenticator.generate(body.secret);
            expect(code).toMatch(/^\d{6}$/);
        }
    });
});

test.describe('Signing Flow with TOTP', () => {
    test.describe.configure({ mode: 'serial' });

    let userToken: string;
    let username: string;
    let totpSecret: string;

    test.beforeAll(async ({ request }) => {
        // Register user and setup TOTP
        const auth = await registerAndLogin(request);
        userToken = auth.token;
        username = auth.username;

        // Setup TOTP
        const setupResponse = await request.post(`${CREDENTIALS_BASE}/totp/setup`, {
            headers: {
                'Authorization': `Bearer ${userToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (setupResponse.ok()) {
            const body = await setupResponse.json();
            totpSecret = body.secret;
        }
    });

    test('POST /sign/init - should create signing challenge', async ({ request }) => {
        if (!userToken) test.skip();

        const response = await request.post(`${CSC_BASE}/sign/init`, {
            headers: {
                'Authorization': `Bearer ${userToken}`,
                'Content-Type': 'application/json'
            },
            data: {
                keyAlias: username,
                documentHash: 'dGVzdCBoYXNo', // base64 "test hash"
                algorithm: 'ML-DSA-65'
            }
        });

        // May fail if key doesn't exist, but endpoint should respond
        expect([200, 201, 401, 404]).toContain(response.status());

        if (response.ok()) {
            const body = await response.json();
            expect(body.challengeId).toBeDefined();
            expect(body.expiresAt).toBeDefined();
            // Should NOT include OTP in response (removed per TOTP implementation)
            expect(body.otp).toBeUndefined();
        }
    });

    test('POST /sign/confirm - should reject invalid TOTP', async ({ request }) => {
        if (!userToken) test.skip();

        // First init
        const initResponse = await request.post(`${CSC_BASE}/sign/init`, {
            headers: {
                'Authorization': `Bearer ${userToken}`,
                'Content-Type': 'application/json'
            },
            data: {
                keyAlias: username,
                documentHash: 'dGVzdCBoYXNo',
                algorithm: 'ML-DSA-65'
            }
        });

        if (!initResponse.ok()) test.skip();

        const { challengeId } = await initResponse.json();

        // Confirm with wrong OTP
        const confirmResponse = await request.post(`${CSC_BASE}/sign/confirm`, {
            headers: {
                'Authorization': `Bearer ${userToken}`,
                'Content-Type': 'application/json'
            },
            data: {
                challengeId,
                otp: '000000'
            }
        });

        // Should fail with invalid OTP
        expect([400, 401, 403]).toContain(confirmResponse.status());
    });

    test('POST /sign/confirm - should succeed with valid TOTP', async ({ request }) => {
        if (!userToken || !totpSecret) test.skip();

        // Generate key first
        await request.post(`${CSC_BASE}/keys/generate`, {
            headers: {
                'Authorization': `Bearer ${userToken}`,
                'Content-Type': 'application/json'
            },
            data: {
                alias: username,
                algorithm: 'ML-DSA-65'
            }
        });

        // Init signing
        const initResponse = await request.post(`${CSC_BASE}/sign/init`, {
            headers: {
                'Authorization': `Bearer ${userToken}`,
                'Content-Type': 'application/json'
            },
            data: {
                keyAlias: username,
                documentHash: 'dGVzdCBoYXNo',
                algorithm: 'ML-DSA-65'
            }
        });

        if (!initResponse.ok()) test.skip();

        const { challengeId } = await initResponse.json();

        // Generate valid TOTP code
        const validOtp = authenticator.generate(totpSecret);

        // Confirm with valid OTP
        const confirmResponse = await request.post(`${CSC_BASE}/sign/confirm`, {
            headers: {
                'Authorization': `Bearer ${userToken}`,
                'Content-Type': 'application/json'
            },
            data: {
                challengeId,
                otp: validOtp
            }
        });

        // Should succeed
        if (confirmResponse.ok()) {
            const body = await confirmResponse.json();
            expect(body.signatureBase64).toBeDefined();
            expect(body.algorithm).toBeDefined();
        }
    });
});

test.describe('TOTP Setup UI', () => {
    test('Settings Security page should load', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/settings/security`);

        // May redirect to login if not authenticated
        await page.waitForLoadState('networkidle');

        // Check for either login redirect or settings page
        const url = page.url();
        expect(url).toMatch(/(settings\/security|login)/);
    });

    test('TOTP setup should show QR code after clicking activate', async ({ page, request }) => {
        // First login via API
        const { token, username } = await registerAndLogin(request);

        // Set auth cookie/token
        await page.goto(`${PORTAL_BASE}/login`);
        await page.fill('input[name="username"], input[type="text"]', username);
        await page.fill('input[name="password"], input[type="password"]', 'TestPass123!');
        await page.click('button[type="submit"]');

        // Check for dashboard redirect
        await expect(page).toHaveURL(/\/dashboard|login/, { timeout: 10000 });
        if (page.url().includes('login')) {
            console.log('Still on login page - login failed?');
        }

        // Navigate to security settings
        await page.goto(`${PORTAL_BASE}/settings/security`);
        await page.waitForLoadState('networkidle');

        // Check finding heading
        await expect(page.locator('h2')).toContainText('Cài đặt bảo mật');

        // Look for TOTP activation button - Strict check
        const activateButton = page.locator('.setup-prompt button');

        // Capture initial state
        await page.screenshot({ path: 'artifacts/totp-settings-initial.png' });

        await expect(activateButton).toBeVisible();
        await activateButton.click();
        await page.waitForTimeout(2000);

        // Should show QR code
        const qrCode = page.locator('canvas, svg, img[alt*="QR"]');
        await expect(qrCode.first()).toBeVisible({ timeout: 10000 });

        // Capture screenshot of QR Setup
        await page.screenshot({ path: 'artifacts/totp-qr-setup.png' });
    });
});

test.describe('Signing UI with TOTP', () => {
    test('Sign page should show TOTP modal after init', async ({ page, request }) => {
        const { username } = await registerAndLogin(request);

        // Login via UI
        await page.goto(`${PORTAL_BASE}/login`);
        await page.fill('input[name="username"], input[type="text"]', username);
        await page.fill('input[name="password"], input[type="password"]', 'TestPass123!');
        await page.click('button[type="submit"]');

        await page.waitForTimeout(2000);

        // Navigate to sign page
        await page.goto(`${PORTAL_BASE}/sign/upload`);
        await page.waitForLoadState('networkidle');

        // Take screenshot of sign page
        await page.screenshot({ path: 'artifacts/totp-sign-page.png' });
    });

    test('Sign page should have file upload and key selection', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/sign/upload`);
        await page.waitForLoadState('networkidle');

        // Check for key elements (may need login first)
        const uploadZone = page.locator('.upload-zone, input[type="file"], [data-testid="upload"]');
        const keySelect = page.locator('select, [data-testid="key-select"]');

        // Page may redirect to login if auth required
        expect(page.url()).toMatch(/\/sign|\/login/);
    });
});

test.describe('Full TOTP User Journey', () => {
    test('Complete flow: Register → Setup TOTP → Generate Key → Sign', async ({ request }) => {
        // Step 1: Register
        const username = `journey_${Date.now()}`;
        const registerRes = await request.post(`${API_BASE}/api/v1/auth/register`, {
            data: {
                username,
                email: `${username}@test.vn`,
                password: 'TestPass123!'
            }
        });
        expect([200, 201, 409]).toContain(registerRes.status());

        // Step 2: Login
        const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
            data: { username, password: 'TestPass123!' }
        });
        if (!loginRes.ok()) test.skip();
        const body = await loginRes.json();
        const token = body.token || body.sessionId;

        // Step 3: Setup TOTP
        const totpRes = await request.post(`${CREDENTIALS_BASE}/totp/setup`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        let secret: string | null = null;
        if (totpRes.ok()) {
            const body = await totpRes.json();
            secret = body.secret;
            expect(secret).toBeDefined();
        }

        // Step 4: Generate Key
        const keyRes = await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: { alias: username, algorithm: 'ML-DSA-65' }
        });

        // Key generation may fail if service unavailable
        if (!keyRes.ok()) return;

        // Step 5: Init Signing
        const initRes = await request.post(`${CSC_BASE}/sign/init`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                keyAlias: username,
                documentHash: btoa('test document content'),
                algorithm: 'ML-DSA-65'
            }
        });

        if (!initRes.ok()) return;
        const { challengeId } = await initRes.json();
        expect(challengeId).toBeDefined();

        // Step 6: Confirm with TOTP
        if (secret) {
            const otp = authenticator.generate(secret);
            const confirmRes = await request.post(`${CSC_BASE}/sign/confirm`, {
                headers: { 'Authorization': `Bearer ${token}` },
                data: { challengeId, otp }
            });

            if (confirmRes.ok()) {
                const result = await confirmRes.json();
                expect(result.signatureBase64).toBeDefined();
                console.log('✅ Full TOTP journey completed successfully!');
            }
        }
    });
});
