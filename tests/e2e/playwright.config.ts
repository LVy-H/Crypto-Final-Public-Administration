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
    reporter: 'list',  // Changed from 'html' to prevent test hanging on report server
    timeout: 60000,

    use: {
        // Use remote tunnel URL
        baseURL: API_BASE,
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
        video: 'on',  // Always record for demonstration
        ignoreHTTPSErrors: true,  // Allow self-signed certs
    },

    projects: [
        {
            name: 'api',
            testMatch: ['**/pqc-compliance.spec.ts', '**/api.spec.ts', '**/auth.spec.ts', '**/ca-hierarchy.spec.ts', '**/cloud-signing.spec.ts', '**/validation.spec.ts', '**/security.spec.ts', '**/integration.spec.ts'],
            use: { ...devices['Desktop Chrome'] },
        },
        {
            name: 'portal',
            testMatch: ['**/form-debug.spec.ts', '**/nuxt-ui-portal.spec.ts', '**/public-portal.spec.ts', '**/admin-portal.spec.ts', '**/ui-user-journey.spec.ts', '**/full-journey.spec.ts', '**/sign-verify-ui.spec.ts', '**/auth-features.spec.ts', '**/real-api-tests.spec.ts', '**/totp-signing.spec.ts', '**/totp-real-flow.spec.ts', '**/production-readiness.spec.ts', '**/kyc-totp-flows.spec.ts'],
            use: {
                ...devices['Desktop Chrome'],
                baseURL: PORTAL_BASE,
            },
        },
    ],

    // No webServer - using production K8s via WireGuard tunnel
});
