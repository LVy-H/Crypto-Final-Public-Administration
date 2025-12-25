import { test, expect } from '@playwright/test';

/**
 * E2E Tests for Admin Portal - PKI Management Dashboard
 * Tests: Admin dashboard, CA management, certificate operations
 */

const ADMIN_URL = 'http://localhost:3001';

test.describe('Admin Portal - Dashboard', () => {
    test('should display admin dashboard', async ({ page }) => {
        await page.goto(ADMIN_URL);

        // Wait for page load
        await page.waitForTimeout(3000);

        // Check for admin portal elements - matches actual UI
        await expect(page.locator('body')).toBeVisible();

        // Look for Admin Portal header
        const hasAdminContent = await page.locator('text=/Admin Portal|PKI Dashboard|PKI Management/i').first().isVisible();
        expect(hasAdminContent).toBeTruthy();
    });

    test('should show PKI Dashboard title', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        // Look for PKI Dashboard heading - matches actual UI h2
        const dashboardTitle = page.locator('h2:has-text("PKI Dashboard")');
        await expect(dashboardTitle).toBeVisible();
    });

    test('should display sidebar navigation', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        // Check for sidebar - matches actual UI
        const sidebar = page.locator('aside.sidebar, .sidebar');
        await expect(sidebar.first()).toBeVisible();
    });

    test('should show System Active status', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        // Check for status badge
        await expect(page.locator('text=System Active')).toBeVisible();
    });
});

test.describe('Admin Portal - CA Stats', () => {
    test('should display Root CAs stat', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        // Look for Root CAs stat card - matches actual UI h3
        await expect(page.locator('h3:has-text("Root CAs")')).toBeVisible();
    });

    test('should display Provincial CAs stat', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        // Look for Provincial CAs stat
        await expect(page.locator('h3:has-text("Provincial CAs")')).toBeVisible();
    });

    test('should display ML-DSA-87 algorithm', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        // Check for ML-DSA-87 label
        await expect(page.locator('text=ML-DSA-87')).toBeVisible();
    });

    test('should display mTLS Enabled label', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        // Check for mTLS reference
        await expect(page.locator('text=mTLS Enabled')).toBeVisible();
    });
});

test.describe('Admin Portal - Quick Actions', () => {
    test('should have Initialize Root CA button', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        // Look for action button - matches actual UI
        await expect(page.locator('button:has-text("Initialize Root CA")')).toBeVisible();
    });

    test('should have Create Provincial CA button', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        await expect(page.locator('button:has-text("Create Provincial CA")')).toBeVisible();
    });

    test('should trigger alert on button click', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        // Set up dialog handler
        let alertMessage = '';
        page.on('dialog', async dialog => {
            alertMessage = dialog.message();
            await dialog.dismiss();
        });

        // Click Initialize Root CA button
        await page.locator('button:has-text("Initialize Root CA")').click();

        // Wait for dialog
        await page.waitForTimeout(500);

        expect(alertMessage).toContain('Root CA');
    });
});

test.describe('Admin Portal - System Information', () => {
    test('should display PKI Algorithm info', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        // Look for algorithm info section
        await expect(page.locator('text=PKI Algorithm')).toBeVisible();
    });

    test('should display TLS Version info', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        await expect(page.locator('text=TLS 1.3')).toBeVisible();
    });

    test('should display NIST Level info', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        await expect(page.locator('text=NIST Level')).toBeVisible();
    });

    test('should display Quantum-Safe label', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        await expect(page.locator('text=Quantum-Safe')).toBeVisible();
    });
});

test.describe('Admin Portal - Navigation', () => {
    test('should have Dashboard nav link', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        await expect(page.locator('a:has-text("Dashboard")')).toBeVisible();
    });

    test('should have CA Management nav link', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        await expect(page.locator('a:has-text("CA Management")')).toBeVisible();
    });

    test('should have Certificates nav link', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(3000);

        await expect(page.locator('a:has-text("Certificates")')).toBeVisible();
    });
});
