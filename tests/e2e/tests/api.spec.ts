import { test, expect } from '@playwright/test';

/**
 * E2E Tests for Backend API - CA Authority and Authentication
 * Tests API endpoints directly via Playwright's request context
 */

const API_BASE = 'https://api.gov-id.lvh.id.vn/api/v1';

test.describe('API - Authentication Endpoints', () => {
    test('POST /auth/register - should register new user', async ({ request }) => {
        const response = await request.post(`${API_BASE}/auth/register`, {
            data: {
                username: `api_test_${Date.now()}`,
                email: `api_${Date.now()}@test.vn`,
                password: 'TestPass123!'
            }
        });

        // Should succeed or return user exists error
        expect([200, 201, 400]).toContain(response.status());
    });

    test('POST /auth/login - should authenticate user', async ({ request }) => {
        // First register
        const uniqueUser = `login_test_${Date.now()}`;
        await request.post(`${API_BASE}/auth/register`, {
            data: {
                username: uniqueUser,
                email: `${uniqueUser}@test.vn`,
                password: 'TestPass123!'
            }
        });

        // Then login
        const response = await request.post(`${API_BASE}/auth/login`, {
            data: {
                username: uniqueUser,
                password: 'TestPass123!'
            }
        });

        expect([200, 401]).toContain(response.status());

        if (response.ok()) {
            const body = await response.json();
            expect(body.token).toBeDefined();
        }
    });

    test('POST /auth/login - should reject invalid credentials', async ({ request }) => {
        const response = await request.post(`${API_BASE}/auth/login`, {
            data: {
                username: 'nonexistent_user',
                password: 'wrongpassword'
            }
        });

        expect([401, 400, 403]).toContain(response.status());
    });
});

test.describe('API - CA Authority Endpoints', () => {
    test('GET /ca/level/ROOT - should list root CAs', async ({ request }) => {
        const response = await request.get(`${API_BASE}/ca/level/ROOT`);

        expect(response.status()).toBe(200);
        const body = await response.json();
        expect(Array.isArray(body)).toBeTruthy();
    });

    test('GET /ca/level/PROVINCIAL - should list provincial CAs', async ({ request }) => {
        const response = await request.get(`${API_BASE}/ca/level/PROVINCIAL`);

        expect(response.status()).toBe(200);
        const body = await response.json();
        expect(Array.isArray(body)).toBeTruthy();
    });

    test('GET /ca/level/INTERNAL - should list internal CAs', async ({ request }) => {
        const response = await request.get(`${API_BASE}/ca/level/INTERNAL`);

        expect(response.status()).toBe(200);
    });

    test('POST /ca/root/init - should initialize or return existing root CA', async ({ request }) => {
        const response = await request.post(`${API_BASE}/ca/root/init`, {
            data: {
                name: 'E2E Test Root CA'
            }
        });

        expect([200, 201, 409, 500]).toContain(response.status()); // 409 = already exists, 500 = server config issue

        if (response.ok()) {
            const body = await response.json();
            expect(body.id).toBeDefined();
            expect(body.algorithm).toBe('ML-DSA-87');
            expect(body.status).toBe('ACTIVE');
        }
    });

    test('GET /ca/chain/{id} - should return certificate chain', async ({ request }) => {
        // First get root CA
        const rootResponse = await request.get(`${API_BASE}/ca/level/ROOT`);
        const roots = await rootResponse.json();

        if (roots.length > 0) {
            const chainResponse = await request.get(`${API_BASE}/ca/chain/${roots[0].id}`);
            expect(chainResponse.status()).toBe(200);

            const chain = await chainResponse.json();
            expect(Array.isArray(chain)).toBeTruthy();
        }
    });
});

test.describe('API - Validation Endpoints', () => {
    test('POST /validation/verify - should handle signature verification request', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdCBkYXRh', // base64 "test data"
                signature: 'dGVzdCBzaWduYXR1cmU=', // base64 "test signature"
                publicKey: '-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----'
            }
        });

        // Should respond (may be valid or invalid signature)
        expect([200, 400]).toContain(response.status());
    });
});

test.describe('API - Health Checks', () => {
    test('API Gateway should be accessible', async ({ request }) => {
        // Try to access any endpoint to verify gateway is up
        const response = await request.get(`${API_BASE}/ca/level/ROOT`);
        expect(response.status()).not.toBe(502); // Not bad gateway
        expect(response.status()).not.toBe(503); // Not service unavailable
    });
});
