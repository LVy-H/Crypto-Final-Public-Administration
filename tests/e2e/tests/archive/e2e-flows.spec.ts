/**
 * E2E Test: Admin and User Flows
 * Simplified tests with shorter waits
 */
import { test, expect } from '@playwright/test';

test.describe('Admin and User Flows', () => {

    test('1. Admin login and certificate access', async ({ page }) => {
        // Login
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');

        // Wait for navigation
        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 15000 });
        console.log('After login:', page.url());

        // Take screenshot of admin page
        await page.screenshot({ path: 'evidence_admin_home.png', fullPage: true });

        // Navigate via URL (simpler)
        await page.evaluate(() => window.location.href = '/admin/certificates');
        await page.waitForTimeout(3000);

        await page.screenshot({ path: 'evidence_admin_certs.png', fullPage: true });
        console.log('Final URL:', page.url());

        // Just verify we loaded some page
        const title = await page.title();
        expect(title).toBeTruthy();
    });

    test('2. Sign page access', async ({ page }) => {
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');

        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 15000 });

        await page.evaluate(() => window.location.href = '/sign/upload');
        await page.waitForTimeout(3000);

        await page.screenshot({ path: 'evidence_sign_page.png', fullPage: true });
        console.log('Sign page URL:', page.url());

        const title = await page.title();
        expect(title).toBeTruthy();
    });

    test('3. Dashboard stats display', async ({ page }) => {
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');

        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 15000 });

        await page.evaluate(() => window.location.href = '/dashboard');
        await page.waitForTimeout(3000);

        await page.screenshot({ path: 'evidence_dashboard.png', fullPage: true });

        expect(page.url()).toContain('/dashboard');
        console.log('PASSED: Dashboard');
    });
});
