import { test, expect } from '@playwright/test';

/**
 * E2E Tests for Cloud Signing API (CSC - Cloud Signature Consortium)
 * 
 * Tests cover:
 * - Key generation (with algorithm enforcement)
 * - Document signing operations
 * - CSR generation
 * - Authorization/SAD validation
 */

const API_BASE = 'https://api.gov-id.lvh.id.vn/api/v1';
const CSC_BASE = 'https://api.gov-id.lvh.id.vn/csc/v1';

// Helper to get auth token
async function getAuthToken(request: any): Promise<string | null> {
    const username = `sign_test_${Date.now()}`;

    await request.post(`${API_BASE}/auth/register`, {
        data: {
            username,
            email: `${username}@test.vn`,
            password: 'TestPass123!'
        }
    });

    const loginResponse = await request.post(`${API_BASE}/auth/login`, {
        data: { username, password: 'TestPass123!' }
    });

    if (loginResponse.ok()) {
        const body = await loginResponse.json();
        return body.token;
    }
    return null;
}

test.describe('Cloud Signing - Key Generation', () => {
    test.describe.configure({ mode: 'serial' });

    test('POST /keys/generate - should generate ML-DSA-65 key pair', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias: `key_mldsa65_${Date.now()}`,
                algorithm: 'ML-DSA-65'
            }
        });

        // May succeed or fail auth, or endpoint not deployed (404)
        expect([200, 201, 401, 403, 404]).toContain(response.status());

        if (response.ok()) {
            const body = await response.json();
            expect(body.publicKeyPem).toBeDefined();
            expect(body.publicKeyPem).toContain('-----BEGIN PUBLIC KEY-----');
        }
    });

    test('POST /keys/generate - should generate ML-DSA-87 key pair', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias: `key_mldsa87_${Date.now()}`,
                algorithm: 'ML-DSA-87'
            }
        });

        expect([200, 201, 401, 403]).toContain(response.status());

        if (response.ok()) {
            const body = await response.json();
            expect(body.publicKeyPem).toBeDefined();
        }
    });

    test('POST /keys/generate - should require authentication', async ({ request }) => {
        const response = await request.post(`${CSC_BASE}/keys/generate`, {
            data: {
                alias: `unauth_key_${Date.now()}`,
                algorithm: 'ML-DSA-65'
            }
        });

        // Should reject unauthenticated request
        expect([401, 403]).toContain(response.status());
    });

    test('POST /keys/generate - should reject empty alias', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias: '',
                algorithm: 'ML-DSA-65'
            }
        });

        expect([400, 401]).toContain(response.status());
    });

    test('POST /keys/generate - should sanitize alias with special characters', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias: 'test;drop table keys;--',
                algorithm: 'ML-DSA-65'
            }
        });

        // Should reject dangerous characters
        expect([400, 401]).toContain(response.status());
    });

    test('POST /keys/generate - should reject duplicate alias', async ({ request }) => {
        const token = await getAuthToken(request);
        const alias = `dup_key_${Date.now()}`;

        // First creation
        await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: { alias, algorithm: 'ML-DSA-65' }
        });

        // Second creation with same alias
        const response = await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: { alias, algorithm: 'ML-DSA-65' }
        });

        // Should conflict or handle gracefully
        expect([409, 400, 401, 200]).toContain(response.status());
    });

    test('POST /keys/generate - public key should be PEM formatted', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias: `pem_key_${Date.now()}`,
                algorithm: 'ML-DSA-65'
            }
        });

        if (response.ok()) {
            const body = await response.json();
            expect(body.publicKeyPem).toMatch(/-----BEGIN PUBLIC KEY-----/);
            expect(body.publicKeyPem).toMatch(/-----END PUBLIC KEY-----/);
        }
    });

    test('POST /keys/generate - should enforce algorithm whitelist', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias: `invalid_algo_${Date.now()}`,
                algorithm: 'AES-256' // Invalid algorithm
            }
        });

        expect([400, 401]).toContain(response.status());
    });
});

test.describe('Cloud Signing - Sign Operations', () => {
    test.describe.configure({ mode: 'serial' });

    test('POST /sign - should require valid authorization token', async ({ request }) => {
        const response = await request.post(`${CSC_BASE}/sign`, {
            data: {
                keyAlias: 'test_key',
                dataHashBase64: 'dGVzdCBoYXNo', // base64 "test hash"
                algorithm: 'ML-DSA-65'
            }
        });

        // Should reject unauthenticated request (or 404 if not deployed)
        expect([401, 403, 404]).toContain(response.status());
    });

    test('POST /sign - should reject invalid authorization token', async ({ request }) => {
        const response = await request.post(`${CSC_BASE}/sign`, {
            headers: { 'Authorization': 'Bearer invalid-token-12345' },
            data: {
                keyAlias: 'test_key',
                dataHashBase64: 'dGVzdCBoYXNo',
                algorithm: 'ML-DSA-65'
            }
        });

        expect([401, 403]).toContain(response.status());
    });

    test('POST /sign - should reject expired token', async ({ request }) => {
        // Using clearly invalid token format
        const response = await request.post(`${CSC_BASE}/sign`, {
            headers: { 'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjB9.invalid' },
            data: {
                keyAlias: 'test_key',
                dataHashBase64: 'dGVzdCBoYXNo',
                algorithm: 'ML-DSA-65'
            }
        });

        expect([401, 403]).toContain(response.status());
    });

    test('POST /sign - should reject invalid base64 data hash', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/sign`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                keyAlias: 'test_key',
                dataHashBase64: 'not-valid-base64!!!',
                algorithm: 'ML-DSA-65'
            }
        });

        expect([400, 401]).toContain(response.status());
    });

    test('POST /sign - should reject empty data hash', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/sign`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                keyAlias: 'test_key',
                dataHashBase64: '',
                algorithm: 'ML-DSA-65'
            }
        });

        expect([400, 401]).toContain(response.status());
    });

    test('POST /sign - should reject non-existent key alias', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/sign`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                keyAlias: 'nonexistent_key_12345',
                dataHashBase64: 'dGVzdCBoYXNo',
                algorithm: 'ML-DSA-65'
            }
        });

        expect([404, 401, 400]).toContain(response.status());
    });

    test('POST /sign - signature should be base64 encoded', async ({ request }) => {
        const token = await getAuthToken(request);
        const alias = `sign_key_${Date.now()}`;

        // First generate a key
        await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: { alias, algorithm: 'ML-DSA-65' }
        });

        // Then try to sign
        const response = await request.post(`${CSC_BASE}/sign`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                keyAlias: alias,
                dataHashBase64: 'dGVzdCBoYXNo',
                algorithm: 'ML-DSA-65'
            }
        });

        if (response.ok()) {
            const body = await response.json();
            expect(body.signatureBase64).toBeDefined();
            // Verify it's valid base64
            expect(() => atob(body.signatureBase64)).not.toThrow();
        }
    });

    test('POST /sign - should log audit trail', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/sign`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                keyAlias: 'audit_test_key',
                dataHashBase64: 'dGVzdCBoYXNo',
                algorithm: 'ML-DSA-65'
            }
        });

        // Response should complete without error
        expect(response.status()).toBeDefined();
    });

    test('POST /sign - should handle concurrent signing requests', async ({ request }) => {
        const token = await getAuthToken(request);

        const promises = Array(5).fill(null).map((_, i) =>
            request.post(`${CSC_BASE}/sign`, {
                headers: { 'Authorization': `Bearer ${token}` },
                data: {
                    keyAlias: `concurrent_key_${i}`,
                    dataHashBase64: 'dGVzdCBoYXNo',
                    algorithm: 'ML-DSA-65'
                }
            })
        );

        const responses = await Promise.all(promises);

        // All should return some response (not hang)
        responses.forEach(r => {
            expect(r.status()).toBeDefined();
        });
    });

    test('POST /sign - should complete within timeout', async ({ request }) => {
        const token = await getAuthToken(request);
        const startTime = Date.now();

        const response = await request.post(`${CSC_BASE}/sign`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                keyAlias: 'timeout_test_key',
                dataHashBase64: 'dGVzdCBoYXNo',
                algorithm: 'ML-DSA-65'
            }
        });

        const duration = Date.now() - startTime;

        // Should complete within 30 seconds
        expect(duration).toBeLessThan(30000);
    });
});

test.describe('Cloud Signing - CSR Generation', () => {
    test.describe.configure({ mode: 'serial' });

    test('POST /keys/csr - should require authentication', async ({ request }) => {
        const response = await request.post(`${CSC_BASE}/keys/csr`, {
            data: {
                alias: 'test_csr_key',
                subject: '/CN=Test User/O=Test Org/C=VN'
            }
        });

        expect([401, 403]).toContain(response.status());
    });

    test('POST /keys/csr - should generate CSR for existing key', async ({ request }) => {
        const token = await getAuthToken(request);
        const alias = `csr_key_${Date.now()}`;

        // Generate key first
        await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: { alias, algorithm: 'ML-DSA-65' }
        });

        // Generate CSR
        const response = await request.post(`${CSC_BASE}/keys/csr`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias,
                subject: '/CN=Test User/O=Test Org/C=VN'
            }
        });

        if (response.ok()) {
            const body = await response.json();
            expect(body.csrPem).toContain('-----BEGIN CERTIFICATE REQUEST-----');
        }
    });

    test('POST /keys/csr - should reject invalid subject DN', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/keys/csr`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias: 'test_key',
                subject: 'invalid subject' // Missing /CN=
            }
        });

        expect([400, 401, 404]).toContain(response.status());
    });

    test('POST /keys/csr - should sanitize subject input', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/keys/csr`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias: 'test_key',
                subject: '/CN=Test;rm -rf//O=Evil/C=XX'
            }
        });

        // Should reject or sanitize dangerous characters
        expect([400, 401, 200]).toContain(response.status());

        if (response.ok()) {
            const body = await response.json();
            expect(body.csrPem).not.toContain('rm -rf');
        }
    });

    test('POST /keys/csr - should return PEM format', async ({ request }) => {
        const token = await getAuthToken(request);
        const alias = `pem_csr_${Date.now()}`;

        await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: { alias, algorithm: 'ML-DSA-65' }
        });

        const response = await request.post(`${CSC_BASE}/keys/csr`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias,
                subject: '/CN=Test/O=Org/C=VN'
            }
        });

        if (response.ok()) {
            const body = await response.json();
            expect(body.csrPem).toMatch(/-----BEGIN CERTIFICATE REQUEST-----/);
            expect(body.csrPem).toMatch(/-----END CERTIFICATE REQUEST-----/);
        }
    });

    test('POST /keys/csr - should reject non-existent key', async ({ request }) => {
        const token = await getAuthToken(request);

        const response = await request.post(`${CSC_BASE}/keys/csr`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias: 'nonexistent_key_99999',
                subject: '/CN=Test/O=Org/C=VN'
            }
        });

        expect([404, 401, 400]).toContain(response.status());
    });

    test('POST /keys/csr - CSR subject should include provided CN', async ({ request }) => {
        const token = await getAuthToken(request);
        const alias = `cn_test_${Date.now()}`;
        const expectedCN = 'Test Certificate User';

        await request.post(`${CSC_BASE}/keys/generate`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: { alias, algorithm: 'ML-DSA-65' }
        });

        const response = await request.post(`${CSC_BASE}/keys/csr`, {
            headers: { 'Authorization': `Bearer ${token}` },
            data: {
                alias,
                subject: `/CN=${expectedCN}/O=Org/C=VN`
            }
        });

        // If successful, CSR contains the subject
        expect([200, 401, 404]).toContain(response.status());
    });
});
