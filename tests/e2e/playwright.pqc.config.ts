import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
    testDir: './tests',
    timeout: 60000,
    use: {
        baseURL: 'http://localhost:3000',
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
    },
    projects: [
        {
            name: 'pqc-local',
            testMatch: /pqc-client\.spec\.ts/,
            use: { ...devices['Desktop Chrome'] },
        },
    ],
});
