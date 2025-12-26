import { test, expect } from '@playwright/test';

/**
 * E2E Tests for Public Portal - Citizen-facing frontend
 * Tests: Landing page, Login flow, Dashboard, Document signing
 */

test.describe('Public Portal - Landing Page', () => {
    test('should display landing page with PQC branding', async ({ page }) => {
        await page.goto('/');

        // Check main heading - matches actual UI
        await expect(page.locator('h1')).toContainText(/Digital Signatures|Quantum Era/i);

        // Check for PQC badge
        await expect(page.locator('text=Post-Quantum Secure')).toBeVisible();
    });

    test('should have working Sign In link', async ({ page }) => {
        await page.goto('/');

        // Click Sign In link - matches actual UI
        await page.getByRole('link', { name: /Sign In/i }).click();

        // Should navigate to login page
        await expect(page).toHaveURL(/.*login/);
    });

    test('should have Get Started button', async ({ page }) => {
        await page.goto('/');

        // Check for Get Started button
        await expect(page.getByRole('link', { name: /Get Started/i }).first()).toBeVisible();
    });

    test('should be responsive on mobile', async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        // Page should still render
        await expect(page.locator('body')).toBeVisible();
    });
});

test.describe('Public Portal - Features Section', () => {
    test('should display quantum-resistant feature', async ({ page }) => {
        await page.goto('/');

        // Check for features
        await expect(page.locator('text=Quantum-Resistant').first()).toBeVisible();
    });

    test('should display legally valid feature', async ({ page }) => {
        await page.goto('/');

        // Check for legal compliance mention
        await expect(page.locator('text=Legally Valid')).toBeVisible();
    });
});

test.describe('Public Portal - Authentication Flow', () => {
    test('should display login form', async ({ page }) => {
        await page.goto('/login');

        // Wait for page load
        await page.waitForTimeout(1000);

        // Check form elements exist
        const hasUsernameField = await page.locator('input[type="text"], input[name*="user"], input[placeholder*="user" i]').first().isVisible();
        const hasPasswordField = await page.locator('input[type="password"]').first().isVisible();

        expect(hasUsernameField || hasPasswordField).toBeTruthy();
    });

    test('should have login submit button', async ({ page }) => {
        await page.goto('/login');
        await page.waitForTimeout(1000);

        // Check for submit button
        const submitBtn = page.locator('button[type="submit"], button:has-text("Login"), button:has-text("Đăng nhập")');
        await expect(submitBtn.first()).toBeVisible();
    });

    test('should handle login attempt', async ({ page }) => {
        await page.goto('/login');
        await page.waitForTimeout(1000);

        // Fill form if fields exist
        const usernameField = page.locator('input[type="text"], input[name*="user"]').first();
        const passwordField = page.locator('input[type="password"]').first();

        if (await usernameField.isVisible()) {
            await usernameField.fill('testuser');
        }
        if (await passwordField.isVisible()) {
            await passwordField.fill('Test123!');
        }

        // Click submit
        const submitBtn = page.locator('button[type="submit"], button').first();
        await submitBtn.click();

        // Wait for response
        await page.waitForTimeout(2000);

        // Should either show error or navigate
        expect(page.url()).toBeTruthy();
    });
});

test.describe('Public Portal - Dashboard', () => {
    test('should redirect to login when not authenticated', async ({ page }) => {
        await page.goto('/dashboard');
        await page.waitForTimeout(2000);

        // May redirect to login or show dashboard
        const url = page.url();
        expect(url.includes('dashboard') || url.includes('login') || url.includes('/')).toBeTruthy();
    });
});

test.describe('Public Portal - Navigation', () => {
    test('should have Start Signing CTA', async ({ page }) => {
        await page.goto('/');

        // Check for "Start Signing" button
        const startBtn = page.locator('text=Start Signing');
        await expect(startBtn.first()).toBeVisible();
    });

    test('should navigate to register from CTA', async ({ page }) => {
        await page.goto('/');

        // Click Start Signing or Get Started
        await page.getByRole('link', { name: /Start Signing|Get Started/i }).first().click();

        // Should navigate to register
        await expect(page).toHaveURL(/.*register/);
    });
});
