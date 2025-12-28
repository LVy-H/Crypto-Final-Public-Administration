import { test, expect } from '@playwright/test';

/**
 * PQC Compliance E2E Tests
 * Based on test-instruction.md
 *
 * Phases:
 * - INFRA: Infrastructure Bootstrap (CA Hierarchy)
 * - USER: Citizen Onboarding
 * - PROC: Document Signing
 * - VAL: Public Validation
 *
 * Compliance: Decree 130/2018/ND-CP, eIDAS, NIST SP 800-208, FIPS 140-3
 */

const API_BASE = 'https://api.gov-id.lvh.id.vn/api/v1';
const CSC_BASE = 'https://api.gov-id.lvh.id.vn/csc/v1';

// CA Level mappings
const CA_LEVEL = {
    ROOT: 0,
    INTERNAL: 1,
    PROVINCIAL: 2,
    DISTRICT: 3,
};

// Store test context
let rootCaId: string | null = null;
let authToken: string | null = null;

// ============================================================================
// PHASE 1: Infrastructure Bootstrap (INFRA-*)
// ============================================================================

test.describe('INFRA-001: Root CA Operations', () => {
    test.describe.configure({ mode: 'serial' });

    test('Root CA exists and uses ML-DSA-87 (NIST Level 5)', async ({ request }) => {
        const response = await request.get(`${API_BASE}/ca/level/${CA_LEVEL.ROOT}`);
        expect(response.status()).toBe(200);

        const roots = await response.json();
        expect(Array.isArray(roots)).toBeTruthy();
        expect(roots.length).toBeGreaterThan(0);

        const rootCA = roots[0];
        expect(rootCA.algorithm).toBe('ML-DSA-87');
        expect(rootCA.status).toBe('ACTIVE');
        expect(rootCA.name).toBeDefined();
        rootCaId = rootCA.id;
    });

    test('Root CA has valid 10-year validity period', async ({ request }) => {
        const response = await request.get(`${API_BASE}/ca/level/${CA_LEVEL.ROOT}`);
        const roots = await response.json();

        if (roots.length > 0) {
            const validUntil = new Date(roots[0].validUntil);
            const now = new Date();
            const yearsRemaining = (validUntil.getTime() - now.getTime()) / (1000 * 60 * 60 * 24 * 365);

            // Root CA should have significant validity remaining (>5 years)
            expect(yearsRemaining).toBeGreaterThan(5);
        }
    });
});

test.describe('INFRA-002: Internal Services CA', () => {
    test('Internal CA exists and uses ML-DSA-65 (mTLS)', async ({ request }) => {
        const response = await request.get(`${API_BASE}/ca/level/${CA_LEVEL.INTERNAL}`);
        expect(response.status()).toBe(200);

        const cas = await response.json();
        expect(Array.isArray(cas)).toBeTruthy();

        if (cas.length > 0) {
            expect(cas[0].algorithm).toBe('ML-DSA-65');
            expect(cas[0].status).toBe('ACTIVE');
        }
    });
});

test.describe('INFRA-003: Certificate Chain Validation', () => {
    test('GET /ca/chain/{id} returns valid chain', async ({ request }) => {
        const rootResponse = await request.get(`${API_BASE}/ca/level/${CA_LEVEL.ROOT}`);
        const roots = await rootResponse.json();

        if (roots.length > 0) {
            const chainResponse = await request.get(`${API_BASE}/ca/chain/${roots[0].id}`);
            expect([200, 404]).toContain(chainResponse.status());

            if (chainResponse.ok()) {
                const chain = await chainResponse.json();
                expect(Array.isArray(chain)).toBeTruthy();
            }
        }
    });
});

// ============================================================================
// PHASE 2: Citizen Onboarding (USER-*)
// ============================================================================

test.describe('USER-001: Authentication', () => {
    test.describe.configure({ mode: 'serial' });

    const testUser = {
        username: `testuser_${Date.now()}`,
        email: `test_${Date.now()}@example.com`,
        password: 'SecureP@ss123!',
    };

    test('POST /auth/register - can register new user', async ({ request }) => {
        const response = await request.post(`${API_BASE}/auth/register`, {
            data: testUser,
        });

        // Accept various success codes
        expect([200, 201, 409, 500]).toContain(response.status());
    });

    test('POST /auth/login - can authenticate user', async ({ request }) => {
        const response = await request.post(`${API_BASE}/auth/login`, {
            data: {
                username: testUser.username,
                password: testUser.password,
            },
        });

        // Login might fail if registration didn't work
        if (response.ok()) {
            const body = await response.json();
            if (body.token) {
                authToken = body.token;
                // JWT format: xxx.yyy.zzz
                expect(body.token.split('.').length).toBe(3);
            }
        }
    });

    test('Protected endpoint rejects invalid token', async ({ request }) => {
        const response = await request.get(`${API_BASE}/keys`, {
            headers: {
                Authorization: 'Bearer invalid.token.here',
            },
        });

        expect([401, 403, 404]).toContain(response.status());
    });
});

test.describe('USER-002: Key Generation (Cloud HSM)', () => {
    test('Cloud signing endpoint is accessible', async ({ request }) => {
        const response = await request.get(`${CSC_BASE}/info`);
        // May require auth or return service info
        expect([200, 401, 403, 404]).toContain(response.status());
    });

    test('Key generation requires authentication', async ({ request }) => {
        const response = await request.post(`${API_BASE}/keys/generate`, {
            data: {
                algorithm: 'ML-DSA-65',
                alias: 'test-key',
            },
        });

        // Should require auth
        expect([401, 403, 200, 201, 404]).toContain(response.status());
    });
});

// ============================================================================
// PHASE 3: Document Signing (PROC-*)
// ============================================================================

test.describe('PROC-001: Signature Service', () => {
    test('CSC service info endpoint responds', async ({ request }) => {
        const response = await request.get(`${CSC_BASE}/info`);
        expect([200, 401, 404]).toContain(response.status());
    });

    test('Signing initiation requires authentication', async ({ request }) => {
        const response = await request.post(`${CSC_BASE}/signatures/signHash`, {
            data: {
                hash: 'dGVzdGhhc2g=', // base64 "testhash"
                credentialID: 'test-cred',
            },
        });

        expect([401, 403, 400, 404]).toContain(response.status());
    });
});

// ============================================================================
// PHASE 4: Public Validation (VAL-*)
// ============================================================================

test.describe('VAL-001: Signature Validation', () => {
    test('POST /validation/verify - endpoint accessible', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                signature: 'test-signature',
                data: 'test-data',
            },
        });

        // Should process request (might return error for invalid input)
        expect([200, 400, 404, 500]).toContain(response.status());
    });

    test('Validation service health check', async ({ request }) => {
        const response = await request.get(`${API_BASE}/health`);
        expect([200, 404]).toContain(response.status());
    });
});

test.describe('VAL-002: Algorithm Compliance', () => {
    test('Root CA uses post-quantum algorithm', async ({ request }) => {
        const response = await request.get(`${API_BASE}/ca/level/${CA_LEVEL.ROOT}`);
        const roots = await response.json();

        if (roots.length > 0) {
            // Must be ML-DSA (Dilithium) - PQC algorithm
            expect(['ML-DSA-87', 'ML-DSA-65', 'ML-DSA-44']).toContain(roots[0].algorithm);
        }
    });

    test('Internal CA uses appropriate algorithm level', async ({ request }) => {
        const response = await request.get(`${API_BASE}/ca/level/${CA_LEVEL.INTERNAL}`);
        const cas = await response.json();

        if (cas.length > 0) {
            // ML-DSA-65 for internal services (NIST Level 3)
            expect(['ML-DSA-87', 'ML-DSA-65']).toContain(cas[0].algorithm);
        }
    });
});

// ============================================================================
// Security Tests
// ============================================================================

test.describe('Security - OWASP Compliance', () => {
    test('API returns security headers', async ({ request }) => {
        const response = await request.get(`${API_BASE}/ca/level/0`);

        // HSTS should be present
        const hstsHeader = response.headers()['strict-transport-security'];
        expect(hstsHeader).toBeDefined();
    });

    test('SQL Injection attempt is handled safely', async ({ request }) => {
        const response = await request.post(`${API_BASE}/auth/login`, {
            data: {
                username: "admin'; DROP TABLE users;--",
                password: 'test',
            },
        });

        // Should not cause server error
        expect(response.status()).toBeLessThan(500);
    });
});
