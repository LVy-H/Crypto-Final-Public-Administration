import { defineConfig, devices } from '@playwright/test';

// Use remote tunnel endpoints to test ingress/WireGuard
const API_BASE = 'https://api.gov-id.lvh.id.vn';
const PORTAL_BASE = 'https://portal.gov-id.lvh.id.vn';

export default defineConfig({
    testDir: './tests',
    fullyParallel: false,  // Run serial for API tests
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    workers: 1,  // Single worker for deterministic order
    reporter: 'html',
    timeout: 60000,

    use: {
        // Use remote tunnel URL
        baseURL: API_BASE,
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
        ignoreHTTPSErrors: true,  // Allow self-signed certs
    },

    projects: [
        {
            name: 'api',
            testMatch: ['**/api.spec.ts', '**/auth.spec.ts', '**/ca-hierarchy.spec.ts', '**/cloud-signing.spec.ts', '**/validation.spec.ts', '**/security.spec.ts', '**/integration.spec.ts'],
            use: { ...devices['Desktop Chrome'] },
        },
        {
            name: 'portal',
            testMatch: ['**/public-portal.spec.ts', '**/admin-portal.spec.ts'],
            use: {
                ...devices['Desktop Chrome'],
                baseURL: PORTAL_BASE,
            },
        },
    ],

    // No webServer - using production K8s via WireGuard tunnel
});
