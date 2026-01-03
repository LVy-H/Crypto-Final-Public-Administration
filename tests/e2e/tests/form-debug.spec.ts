import { test, expect } from '@playwright/test';

/**
 * Diagnostic test to debug form submission issues
 */

const PORTAL_BASE = 'https://portal.gov-id.lvh.id.vn';

test.describe('Form Submission Debug', () => {
    test('should capture console errors on register page', async ({ page }) => {
        const consoleErrors: string[] = [];
        const consoleWarnings: string[] = [];
        const networkErrors: string[] = [];

        // Capture console messages
        page.on('console', msg => {
            if (msg.type() === 'error') {
                consoleErrors.push(msg.text());
            }
            if (msg.type() === 'warning') {
                consoleWarnings.push(msg.text());
            }
        });

        // Capture network errors
        page.on('requestfailed', request => {
            networkErrors.push(`${request.url()} - ${request.failure()?.errorText}`);
        });

        // Navigate to register page
        await page.goto(`${PORTAL_BASE}/register`);
        await page.waitForLoadState('networkidle');

        // Wait for hydration
        await page.waitForTimeout(2000);

        // Log captured errors
        console.log('=== Console Errors ===');
        consoleErrors.forEach(e => console.log(e));
        console.log('=== Console Warnings ===');
        consoleWarnings.forEach(w => console.log(w));
        console.log('=== Network Errors ===');
        networkErrors.forEach(n => console.log(n));

        // Check if form exists
        const form = page.locator('form');
        await expect(form).toBeVisible();

        // Check form method attribute
        const formMethod = await form.getAttribute('method');
        console.log(`Form method: ${formMethod}`);

        // Check if form has action attribute
        const formAction = await form.getAttribute('action');
        console.log(`Form action: ${formAction}`);

        // Fill form with test data
        await page.fill('input[name="username"]', 'debugtest');
        await page.fill('input[name="email"]', 'debug@test.vn');
        await page.fill('input[name="password"]', 'SecurePass123!');
        await page.fill('input[name="confirmPassword"]', 'SecurePass123!');

        // Get current URL before submit
        const urlBefore = page.url();
        console.log(`URL before submit: ${urlBefore}`);

        // Click submit button
        await page.getByRole('button', { name: /Đăng ký/i }).click();

        // Wait a bit for any async operations
        await page.waitForTimeout(3000);

        // Get URL after submit
        const urlAfter = page.url();
        console.log(`URL after submit: ${urlAfter}`);

        // Check if URL contains query parameters (indicates GET submission)
        const hasQueryParams = urlAfter.includes('?username=') || urlAfter.includes('?email=');

        // Log more errors that might have occurred during submit
        console.log('=== Errors after submit ===');
        consoleErrors.forEach(e => console.log(e));

        // Fail the test if GET submission happened
        expect(hasQueryParams, 'Form should NOT submit as GET with query parameters').toBe(false);
    });

    test('should check hydration status', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/register`);

        // Wait for initial load
        await page.waitForLoadState('domcontentloaded');

        // Check if Vue/Nuxt has hydrated by looking for data-v- attributes
        // or by checking if event handlers are attached

        // Get the form element
        const form = page.locator('form');
        await expect(form).toBeVisible();

        // Check for Vue internal attributes that indicate hydration
        const formHtml = await form.evaluate(el => el.outerHTML);
        console.log('Form HTML (first 500 chars):');
        console.log(formHtml.substring(0, 500));

        // Try to trigger validation by submitting empty form
        // Clear any pre-filled values
        await page.fill('input[name="username"]', '');
        await page.fill('input[name="email"]', '');
        await page.fill('input[name="password"]', '');
        await page.fill('input[name="confirmPassword"]', '');

        const urlBefore = page.url();
        await page.getByRole('button', { name: /Đăng ký/i }).click();

        await page.waitForTimeout(1000);
        const urlAfter = page.url();

        // If validation works, URL should NOT change (no GET submission)
        const urlChanged = urlAfter !== urlBefore && urlAfter.includes('?');
        console.log(`URL changed to GET params: ${urlChanged}`);

        // Look for validation error messages
        const errorMessages = await page.locator('[class*="error"], [class*="Error"], [aria-invalid="true"]').count();
        console.log(`Error indicators found: ${errorMessages}`);
    });
});
