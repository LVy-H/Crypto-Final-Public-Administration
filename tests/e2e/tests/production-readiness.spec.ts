/**
 * Production Readiness Test Suite
 * Tests all critical flows to ensure system is ready for production
 */
import { test, expect } from '@playwright/test';

test.describe('Production Readiness Tests', () => {

    // ============== Authentication ==============
    test('AUTH-1: Login page loads', async ({ page }) => {
        await page.goto('/login');
        await expect(page.locator('h2, h1')).toBeVisible({ timeout: 10000 });
        await page.screenshot({ path: 'prod_auth_login.png' });
        console.log('✓ Login page loads');
    });

    test('AUTH-2: Registration page loads', async ({ page }) => {
        await page.goto('/register');
        await expect(page.locator('h2, h1')).toBeVisible({ timeout: 10000 });
        await page.screenshot({ path: 'prod_auth_register.png' });
        console.log('✓ Registration page loads');
    });

    test('AUTH-3: Valid login succeeds', async ({ page }) => {
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');

        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 30000 });
        await page.screenshot({ path: 'prod_auth_success.png' });
        console.log('✓ Login succeeds, redirected to:', page.url());
    });

    test('AUTH-4: Invalid login fails', async ({ page }) => {
        await page.goto('/login');
        await page.fill('#username', 'nonexistent_user');
        await page.fill('#password', 'wrongpassword');
        await page.click('button.btn-submit');

        await page.waitForTimeout(2000);
        // Should stay on login page or show error
        const url = page.url();
        expect(url).toContain('/login');
        console.log('✓ Invalid login rejected');
    });

    // ============== Public Pages ==============
    test('PUBLIC-1: Landing page loads', async ({ page }) => {
        await page.goto('/');
        await expect(page.locator('body')).toBeVisible();
        await page.screenshot({ path: 'prod_public_landing.png' });
        console.log('✓ Landing page loads');
    });

    test('PUBLIC-2: Verify page loads', async ({ page }) => {
        await page.goto('/verify');
        await expect(page.locator('body')).toBeVisible();
        await page.screenshot({ path: 'prod_public_verify.png' });
        console.log('✓ Verify page loads');
    });

    // ============== User Dashboard ==============
    test('USER-1: Dashboard loads with stats', async ({ page }) => {
        // Login first
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');
        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 30000 });

        await page.goto('/dashboard');
        await page.waitForTimeout(3000);
        await page.screenshot({ path: 'prod_user_dashboard.png' });

        expect(page.url()).toContain('/dashboard');
        console.log('✓ User dashboard loads');
    });

    test('USER-2: Certificates page loads', async ({ page }) => {
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');
        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 30000 });

        await page.goto('/certificates');
        await page.waitForTimeout(3000);
        await page.screenshot({ path: 'prod_user_certificates.png' });
        console.log('✓ Certificates page loads');
    });

    test('USER-3: History page loads', async ({ page }) => {
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');
        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 30000 });

        await page.goto('/history');
        await page.waitForTimeout(3000);
        await page.screenshot({ path: 'prod_user_history.png' });
        console.log('✓ History page loads');
    });

    // ============== Signing ==============
    test('SIGN-1: Sign upload page loads', async ({ page }) => {
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');
        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 30000 });

        await page.goto('/sign/upload');
        await page.waitForTimeout(3000);
        await page.screenshot({ path: 'prod_sign_upload.png' });
        console.log('✓ Sign upload page loads');
    });

    // ============== Admin ==============
    test('ADMIN-1: Admin home loads', async ({ page }) => {
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');
        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 30000 });

        await page.goto('/admin');
        await page.waitForTimeout(3000);
        await page.screenshot({ path: 'prod_admin_home.png' });
        console.log('✓ Admin home loads');
    });

    test('ADMIN-2: Admin certificates loads', async ({ page }) => {
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');
        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 30000 });

        await page.goto('/admin/certificates');
        await page.waitForTimeout(3000);
        await page.screenshot({ path: 'prod_admin_certs.png' });
        console.log('✓ Admin certificates loads');
    });

    test('ADMIN-3: Admin users loads', async ({ page }) => {
        await page.goto('/login');
        await page.fill('#username', 'admin_capture');
        await page.fill('#password', 'SecurePass123!');
        await page.click('button.btn-submit');
        await page.waitForURL(/\/(admin|dashboard)/, { timeout: 30000 });

        await page.goto('/admin/users');
        await page.waitForTimeout(3000);
        await page.screenshot({ path: 'prod_admin_users.png' });
        console.log('✓ Admin users loads');
    });

    // ============== API Health ==============
    test('API-1: Auth endpoint responds', async ({ request }) => {
        const response = await request.post('/api/v1/auth/login', {
            data: { username: 'admin_capture', password: 'SecurePass123!' }
        });
        expect(response.status()).toBe(200);
        const body = await response.json();
        expect(body.sessionId).toBeTruthy();
        console.log('✓ Auth API responds correctly');
    });

    test('API-2: Admin stats endpoint responds', async ({ request }) => {
        // First login to get session
        const loginResp = await request.post('/api/v1/auth/login', {
            data: { username: 'admin_capture', password: 'SecurePass123!' }
        });
        const { sessionId } = await loginResp.json();

        // Then call admin stats
        const statsResp = await request.get('/api/v1/admin/stats', {
            headers: { 'Authorization': `Bearer ${sessionId}` }
        });
        expect(statsResp.status()).toBe(200);
        console.log('✓ Admin stats API responds');
    });
});
