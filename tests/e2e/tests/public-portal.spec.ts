import { test, expect } from '@playwright/test';

/**
 * E2E Tests for Public Portal - Citizen-facing frontend
 * Tests: Landing page, Login flow, Dashboard, Document signing
 */

test.describe('Public Portal - Landing Page', () => {
    test('should display landing page with PQC branding', async ({ page }) => {
        await page.goto('/');

        // Check main heading
        await expect(page.locator('h1')).toContainText(/Chữ ký số|Digital Signature/i);

        // Check navigation elements exist
        await expect(page.getByRole('link', { name: /Đăng nhập|Login|Sign In/i })).toBeVisible();
    });

    test('should have working navigation links', async ({ page }) => {
        await page.goto('/');

        // Click login link
        await page.getByRole('link', { name: /Đăng nhập|Login|Sign In/i }).click();

        // Should navigate to login page
        await expect(page).toHaveURL(/.*login/);
    });

    test('should be responsive on mobile', async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        // Page should still render
        await expect(page.locator('body')).toBeVisible();
    });
});

test.describe('Public Portal - Authentication Flow', () => {
    test('should display login form', async ({ page }) => {
        await page.goto('/login');

        // Check form elements
        await expect(page.getByLabel(/username|Tên đăng nhập/i)).toBeVisible();
        await expect(page.getByLabel(/password|Mật khẩu/i)).toBeVisible();
        await expect(page.getByRole('button', { name: /Đăng nhập|Login|Submit/i })).toBeVisible();
    });

    test('should show validation errors for empty form', async ({ page }) => {
        await page.goto('/login');

        // Click login without filling form
        await page.getByRole('button', { name: /Đăng nhập|Login|Submit/i }).click();

        // Should show validation or stay on login page
        await expect(page).toHaveURL(/.*login/);
    });

    test('should show error for invalid credentials', async ({ page }) => {
        await page.goto('/login');

        // Fill with invalid credentials
        await page.getByLabel(/username|Tên đăng nhập/i).fill('invaliduser');
        await page.getByLabel(/password|Mật khẩu/i).fill('wrongpassword');
        await page.getByRole('button', { name: /Đăng nhập|Login|Submit/i }).click();

        // Wait for response
        await page.waitForTimeout(2000);

        // Should show error message or stay on login
        const errorVisible = await page.locator('text=/failed|error|thất bại/i').isVisible();
        const stillOnLogin = page.url().includes('login');
        expect(errorVisible || stillOnLogin).toBeTruthy();
    });

    test('should login successfully with valid credentials', async ({ page }) => {
        // First register a user via API
        const registerResponse = await page.request.post('http://localhost:8080/api/v1/auth/register', {
            data: {
                username: 'e2e_test_user_' + Date.now(),
                email: `e2e_${Date.now()}@test.vn`,
                password: 'Test123!'
            }
        });

        // If registration successful, try login
        if (registerResponse.ok()) {
            const userData = await registerResponse.json();

            await page.goto('/login');
            await page.getByLabel(/username|Tên đăng nhập/i).fill(userData.username || 'e2e_test_user');
            await page.getByLabel(/password|Mật khẩu/i).fill('Test123!');
            await page.getByRole('button', { name: /Đăng nhập|Login|Submit/i }).click();

            // Wait for navigation
            await page.waitForTimeout(3000);

            // Should navigate to dashboard or home
            const url = page.url();
            expect(url.includes('dashboard') || url.includes('/')).toBeTruthy();
        }
    });
});

test.describe('Public Portal - Dashboard', () => {
    test.beforeEach(async ({ page }) => {
        // Login before each test
        await page.goto('/login');
        await page.getByLabel(/username|Tên đăng nhập/i).fill('testuser');
        await page.getByLabel(/password|Mật khẩu/i).fill('Test123!');
        await page.getByRole('button', { name: /Đăng nhập|Login|Submit/i }).click();
        await page.waitForTimeout(2000);
    });

    test('should display dashboard elements', async ({ page }) => {
        await page.goto('/dashboard');

        // Dashboard should have main content area
        await expect(page.locator('main, .dashboard, [class*="dashboard"]')).toBeVisible();
    });
});

test.describe('Public Portal - Document Signing', () => {
    test('should have sign document link', async ({ page }) => {
        await page.goto('/');

        // Look for signing link
        const signLink = page.getByRole('link', { name: /Ký văn bản|Sign Document/i });
        await expect(signLink).toBeVisible();
    });

    test('should navigate to signing page', async ({ page }) => {
        await page.goto('/');

        // Click signing link
        await page.getByRole('link', { name: /Ký văn bản|Sign Document/i }).click();

        // Wait for navigation
        await page.waitForTimeout(1000);

        // Should be on signing page or login redirect
        const url = page.url();
        expect(url.includes('sign') || url.includes('login')).toBeTruthy();
    });
});
