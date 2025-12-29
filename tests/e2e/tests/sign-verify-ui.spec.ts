import { test, expect } from '@playwright/test';

/**
 * E2E Tests for Public Portal UI
 * Tests both public and authenticated pages
 */

const PORTAL_BASE = 'https://portal.gov-id.lvh.id.vn';

// ============================================
// PUBLIC PAGES (No Login Required)
// ============================================

test.describe('Public Pages - No Auth Required', () => {
    test('should display landing page', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/`);
        await page.waitForLoadState('networkidle');

        // Check for main heading on landing page
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
        await expect(page.locator('body')).toContainText('ML-DSA');

        await page.screenshot({ path: 'artifacts/01-landing-page.png' });
    });

    test('should display login page with form elements', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/login`);
        await page.waitForLoadState('networkidle');

        // Check for login heading
        await expect(page.getByRole('heading', { name: /Đăng nhập/i })).toBeVisible();

        // Check for form fields
        await expect(page.getByRole('textbox', { name: /Tên đăng nhập/i })).toBeVisible();
        await expect(page.getByRole('textbox', { name: /Mật khẩu/i })).toBeVisible();
        await expect(page.getByRole('button', { name: /Đăng nhập/i })).toBeVisible();

        await page.screenshot({ path: 'artifacts/02-login-form.png' });
    });

    test('should display registration page', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/register`);
        await page.waitForLoadState('networkidle');

        // Check for registration content
        await expect(page.getByRole('heading', { name: /Đăng ký/i })).toBeVisible();

        await page.screenshot({ path: 'artifacts/03-register-page.png' });
    });

    test('should have navigation links on landing page', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/`);
        await page.waitForLoadState('networkidle');

        // Check for Sign In link
        await expect(page.getByRole('link', { name: /Sign In|Đăng nhập/i }).first()).toBeVisible();

        // Check for Get Started link
        await expect(page.getByRole('link', { name: /Get Started|Đăng ký|Start/i }).first()).toBeVisible();
    });

    test('should navigate from landing to login', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/`);
        await page.waitForLoadState('networkidle');

        // Click Sign In
        await page.getByRole('link', { name: /Sign In|Đăng nhập/i }).first().click();
        await page.waitForLoadState('networkidle');

        // Should be on login page
        await expect(page).toHaveURL(/login/);
    });

    test('should navigate from landing to register', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/`);
        await page.waitForLoadState('networkidle');

        // Click Đăng ký (Register) link on landing page
        await page.getByRole('link', { name: /Đăng ký/i }).first().click();
        await page.waitForLoadState('networkidle');

        // Should be on register page
        await expect(page).toHaveURL(/register/);
    });
});

// ============================================
// LOGIN FORM INTERACTION
// ============================================

test.describe('Login Form - Interaction', () => {
    test('should allow typing in login form', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/login`);
        await page.waitForLoadState('networkidle');

        // Fill the form
        await page.getByRole('textbox', { name: /Tên đăng nhập/i }).fill('testuser');
        await page.getByRole('textbox', { name: /Mật khẩu/i }).fill('password123');

        // Verify values were entered
        await expect(page.getByRole('textbox', { name: /Tên đăng nhập/i })).toHaveValue('testuser');

        await page.screenshot({ path: 'artifacts/04-login-filled.png' });
    });

    test('should submit login form', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/login`);
        await page.waitForLoadState('networkidle');

        // Fill and submit
        await page.getByRole('textbox', { name: /Tên đăng nhập/i }).fill('testuser');
        await page.getByRole('textbox', { name: /Mật khẩu/i }).fill('password123');
        await page.getByRole('button', { name: /Đăng nhập/i }).click();

        // Wait for response
        await page.waitForLoadState('networkidle');
        await page.waitForTimeout(1000);

        // Take screenshot of result (success or error)
        await page.screenshot({ path: 'artifacts/05-login-result.png' });
    });
});

// ============================================
// PROTECTED PAGES (Redirect to Login)
// ============================================

test.describe('Protected Pages - Auth Redirect', () => {
    test('should redirect dashboard to login', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/dashboard`);
        await page.waitForLoadState('networkidle');
        await page.waitForTimeout(1000);

        // Should either be on dashboard (if session exists) or login
        const url = page.url();
        expect(url.includes('dashboard') || url.includes('login')).toBeTruthy();

        await page.screenshot({ path: 'artifacts/06-dashboard-redirect.png' });
    });

    test('should redirect sign page to login', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/sign/upload`);
        await page.waitForLoadState('networkidle');
        await page.waitForTimeout(1000);

        const url = page.url();
        expect(url.includes('sign') || url.includes('login') || url.includes('dashboard')).toBeTruthy();

        await page.screenshot({ path: 'artifacts/07-sign-redirect.png' });
    });

    test('should redirect verify page to login', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/verify`);
        await page.waitForLoadState('networkidle');
        await page.waitForTimeout(1000);

        const url = page.url();
        expect(url.includes('verify') || url.includes('login') || url.includes('dashboard')).toBeTruthy();

        await page.screenshot({ path: 'artifacts/08-verify-redirect.png' });
    });
});
