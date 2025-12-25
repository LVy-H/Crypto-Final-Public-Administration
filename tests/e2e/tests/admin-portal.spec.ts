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
        await page.waitForTimeout(2000);

        // Check for admin portal elements
        await expect(page.locator('body')).toBeVisible();
    });

    test('should show PKI hierarchy stats', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(2000);

        // Look for CA-related content
        const hasCAContent = await page.locator('text=/CA|Root|Provincial|District/i').first().isVisible();
        expect(hasCAContent).toBeTruthy();
    });

    test('should display sidebar navigation', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(2000);

        // Check for navigation menu
        const sidebar = page.locator('aside, nav, .sidebar, [class*="sidebar"]');
        await expect(sidebar.first()).toBeVisible();
    });
});

test.describe('Admin Portal - CA Management', () => {
    test('should have CA management section', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(2000);

        // Look for CA management link or section
        const caSection = page.locator('text=/CA Management|Quản lý CA|Certificate/i');
        await expect(caSection.first()).toBeVisible();
    });

    test('should display Root CA information', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(2000);

        // Look for Root CA reference
        const rootCa = page.locator('text=/Root CA|ML-DSA-87|NIST/i');
        await expect(rootCa.first()).toBeVisible();
    });

    test('should show mTLS status', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(2000);

        // Look for mTLS or Internal CA reference
        const mtls = page.locator('text=/mTLS|Internal|TLS/i');
        await expect(mtls.first()).toBeVisible();
    });
});

test.describe('Admin Portal - Quick Actions', () => {
    test('should have action buttons', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(2000);

        // Look for action buttons
        const buttons = page.locator('button');
        const buttonCount = await buttons.count();
        expect(buttonCount).toBeGreaterThan(0);
    });

    test('should trigger action on button click', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(2000);

        // Set up dialog handler
        page.on('dialog', async dialog => {
            expect(dialog.message()).toBeTruthy();
            await dialog.dismiss();
        });

        // Click first action button
        const actionBtn = page.locator('button').first();
        if (await actionBtn.isVisible()) {
            await actionBtn.click();
        }
    });
});

test.describe('Admin Portal - System Information', () => {
    test('should display algorithm information', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(2000);

        // Look for algorithm info
        const algoInfo = page.locator('text=/ML-DSA|ML-KEM|Quantum|PQC/i');
        await expect(algoInfo.first()).toBeVisible();
    });

    test('should show security level', async ({ page }) => {
        await page.goto(ADMIN_URL);
        await page.waitForTimeout(2000);

        // Look for security level
        const secLevel = page.locator('text=/NIST|Level|Security/i');
        await expect(secLevel.first()).toBeVisible();
    });
});
