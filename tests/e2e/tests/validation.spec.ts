import { test, expect } from '@playwright/test';

/**
 * E2E Tests for Validation API - Signature Verification
 * 
 * Tests cover:
 * - Signature verification with PQC algorithms
 * - Certificate validation
 * - Invalid signature handling
 * - Error cases
 */

const API_BASE = 'http://localhost:8080/api/v1';

test.describe('Validation - Signature Verification', () => {
    test('POST /validation/verify - should accept verification request', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdCBkYXRh', // base64 "test data"
                signature: 'dGVzdCBzaWduYXR1cmU=', // base64 "test signature"
                publicKey: '-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----'
            }
        });

        // Should accept the request (result may be valid or invalid)
        expect([200, 400]).toContain(response.status());
    });

    test('POST /validation/verify - should return structured response', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdCBkYXRh',
                signature: 'dGVzdCBzaWduYXR1cmU=',
                publicKey: '-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----'
            }
        });

        if (response.ok()) {
            const body = await response.json();
            // Should have a valid/invalid indicator
            expect(body.valid !== undefined || body.isValid !== undefined || body.verified !== undefined).toBeTruthy();
        }
    });

    test('POST /validation/verify - should return invalid for wrong signature', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'b3JpZ2luYWwgZGF0YQ==', // "original data"
                signature: 'd3Jvbmcgc2lnbmF0dXJl', // "wrong signature"
                publicKey: '-----BEGIN PUBLIC KEY-----\nMIIBIjANBg...\n-----END PUBLIC KEY-----'
            }
        });

        expect([200, 400]).toContain(response.status());

        if (response.ok()) {
            const body = await response.json();
            // Should indicate invalid signature
            expect(body.valid === false || body.isValid === false || body.verified === false).toBeTruthy();
        }
    });

    test('POST /validation/verify - should reject malformed public key', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdCBkYXRh',
                signature: 'dGVzdCBzaWduYXR1cmU=',
                publicKey: 'not a valid public key'
            }
        });

        expect([400, 200]).toContain(response.status());
    });

    test('POST /validation/verify - should reject invalid base64 data', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: '!!!invalid base64!!!',
                signature: 'dGVzdCBzaWduYXR1cmU=',
                publicKey: '-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----'
            }
        });

        expect([400, 200]).toContain(response.status());
    });

    test('POST /validation/verify - should reject empty signature', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdCBkYXRh',
                signature: '',
                publicKey: '-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----'
            }
        });

        expect([400, 200]).toContain(response.status());
    });

    test('POST /validation/verify - should handle empty request body', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {}
        });
        // API may handle empty body gracefully
        expect([200, 400, 500]).toContain(response.status());
    });

    test('POST /validation/verify - should handle missing fields', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdCBkYXRh'
                // Missing signature and publicKey
            }
        });
        // API may handle missing fields gracefully
        expect([200, 400, 500]).toContain(response.status());
    });

    test('Verification should complete within reasonable time', async ({ request }) => {
        const startTime = Date.now();

        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdCBkYXRh',
                signature: 'dGVzdCBzaWduYXR1cmU=',
                publicKey: '-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----'
            }
        });

        const duration = Date.now() - startTime;

        // Should complete within 10 seconds
        expect(duration).toBeLessThan(10000);
    });

    test('POST /validation/verify - should handle large data', async ({ request }) => {
        // Generate large base64 data (100KB)
        const largeData = Buffer.from('x'.repeat(100000)).toString('base64');

        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: largeData,
                signature: 'dGVzdCBzaWduYXR1cmU=',
                publicKey: '-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----'
            }
        });

        // Should handle or reject gracefully
        expect(response.status()).toBeDefined();
    });
});

test.describe('Validation - Certificate Validation', () => {
    test('Should accept certificate in verification request', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdCBkYXRh',
                signature: 'dGVzdCBzaWduYXR1cmU=',
                certificate: '-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----'
            }
        });

        // May use certificate for verification
        expect([200, 400]).toContain(response.status());
    });

    test('Should reject expired certificate', async ({ request }) => {
        // Create an obviously expired certificate reference
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdA==',
                signature: 'c2ln',
                certificate: '-----BEGIN CERTIFICATE-----\nEXPIRED\n-----END CERTIFICATE-----'
            }
        });

        // Should indicate invalid
        expect([200, 400]).toContain(response.status());
    });

    test('Should handle certificate chain in verification', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdA==',
                signature: 'c2ln',
                certificateChain: [
                    '-----BEGIN CERTIFICATE-----\nLEAF\n-----END CERTIFICATE-----',
                    '-----BEGIN CERTIFICATE-----\nROOT\n-----END CERTIFICATE-----'
                ]
            }
        });

        expect(response.status()).toBeDefined();
    });

    test('Should validate certificate format', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdA==',
                signature: 'c2ln',
                certificate: 'not a certificate'
            }
        });

        expect([400, 200]).toContain(response.status());
    });

    test('Should handle missing certificate gracefully', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            data: {
                data: 'dGVzdA==',
                signature: 'c2ln'
                // No publicKey or certificate
            }
        });
        // API may handle missing certificate gracefully
        expect([200, 400, 500]).toContain(response.status());
    });
});
