import { test, expect } from '@playwright/test';

test.describe('Dashboard Monitoring', () => {
    test('monitor_admin_dashboard_stats', async ({ page }) => {
        // Intercept and log stats requests
        await page.route('**/api/v1/user/stats', async route => {
            const request = route.request();
            const headers = request.headers();
            console.log(`[REQUEST] ${request.method()} ${request.url()}`);
            console.log(`[HEADERS] Authorization: ${headers['authorization'] || 'MISSING'}`);

            // Continue request
            const response = await route.fetch();
            console.log(`[RESPONSE] Status: ${response.status()}`);
            console.log(`[RESPONSE] Body: ${await response.text()}`);

            await route.fulfill({ response });
        });

        await page.route('**/api/v1/admin/stats', async route => {
            const request = route.request();
            const headers = request.headers();
            console.log(`[REQUEST] ${request.method()} ${request.url()}`);
            console.log(`[HEADERS] Authorization: ${headers['authorization'] || 'MISSING'}`);

            const response = await route.fetch();
            console.log(`[RESPONSE] Status: ${response.status()}`);

            await route.fulfill({ response });
        });

        // 1. Login
        await page.goto('https://portal.gov-id.lvh.id.vn/login');
        await page.fill('input[type="text"]', 'admin_capture');
        await page.fill('input[type="password"]', 'SecurePass123!');
        await page.click('button[type="submit"]');

        // 2. Wait for Admin Dashboard (redirects from login)
        await page.waitForURL('**/admin');
        console.log('Navigated to Admin Dashboard');

        // 3. Wait for stats response
        try {
            const statsResponse = await page.waitForResponse(resp => resp.url().includes('/api/v1/admin/stats') && resp.status() === 200, { timeout: 10000 });
            console.log('Stats loaded successfully');
        } catch (e) {
            console.log('Timed out waiting for stats 200 OK');
        }

        // Take screenshot
        await page.screenshot({ path: 'monitor_dashboard_stats.png' });

        // Check logs in console output
    });
});
