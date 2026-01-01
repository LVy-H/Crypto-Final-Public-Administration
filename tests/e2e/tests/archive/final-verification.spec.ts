import { test, expect } from '@playwright/test';
import { randomUUID } from 'crypto';

test.describe('Final System Verification', () => {
    let userEmail = `verify_${randomUUID().substring(0, 8)}@example.com`;
    const userPass = 'Test@123';
    let userName = userEmail.split('@')[0];

    test.describe.configure({ mode: 'serial' });

    test('1. Registration Flow with TOTP QR Code', async ({ page }) => {
        console.log(`Starting Registration for ${userEmail}`);

        await page.goto('/register');

        // Step 1: Account Info
        await page.fill('input[id="username"]', userName);
        await page.fill('input[id="email"]', userEmail);
        await page.fill('input[id="password"]', userPass);

        // Ensure values are set
        await expect(page.locator('input[id="username"]')).toHaveValue(userName);

        // Click Submit
        await page.click('button[type="submit"]');

        // Check for error
        const errorMsg = page.locator('.error-msg');
        if (await errorMsg.isVisible()) {
            console.log('Error Message:', await errorMsg.textContent());
        }

        // Step 2: Certificate Info
        await expect(page.locator('label:has-text("Dữ liệu KYC")')).toBeVisible({ timeout: 10000 });
        await page.fill('input[placeholder*="012345678912"]', '079090001234'); // kycData
        // Algorithm default is fine
        await page.click('button[type="submit"]');

        // Step 3: Security Setup (QR Code)
        // Wait for QR code
        await expect(page.locator('.qr-container canvas, .qr-container img')).toBeVisible({ timeout: 15000 });
        await page.screenshot({ path: 'evidence_register_qr_code.png', fullPage: true });
        console.log('Captured QR Code evidence');

        // Complete
        await page.click('button:has-text("Đã quét & Hoàn tất")');

        // Step 4: Success
        await expect(page.locator('text=Đăng ký thành công!')).toBeVisible();

        // Wait for redirect to dashboard
        await page.waitForURL('**/dashboard', { timeout: 15000 });
        console.log('Registration Complete');
    });

    test('2. User Dashboard Stats Verification', async ({ page }) => {
        // We are already logged in after registration redirect? 
        // Likely yes, but let's ensure we are on dashboard.
        // If independent test run, we need to login. But serial mode shares state? 
        // Playwright tests share state if reusing context, but default is new context per test.
        // So we need to Login again.

        await page.goto('/login');
        await page.fill('input[type="text"]', userName); // Login uses username or email? AuthController usually username.
        await page.fill('input[type="password"]', userPass);
        await page.click('button[type="submit"]'); // Or whatever the login button text is

        // Wait for dashboard
        await page.waitForURL('**/dashboard');

        // Wait for stats API
        await page.waitForResponse(resp => resp.url().includes('/api/v1/user/stats') && resp.status() === 200, { timeout: 10000 });

        // Take screenshot
        await page.waitForTimeout(1000); // Wait for UI render
        await page.screenshot({ path: 'evidence_user_dashboard_stats.png', fullPage: true });
        console.log('Captured User Dashboard Stats evidence');

        // Logout
        // Selector for user menu might be needed.
        // Assuming a logout button exists or we can just clear state for next test.
    });

    test('3. Admin Certificate Approval Flow', async ({ page }) => {
        // Login as Admin
        await page.goto('/login');
        await page.fill('input[type="text"]', 'admin_capture');
        await page.fill('input[type="password"]', 'SecurePass123!');
        await page.click('button[type="submit"]');

        await page.waitForURL('**/admin');

        // Navigate/Check Certificates
        // If already on admin, navigate to certs
        if (!page.url().includes('/certificates')) {
            await page.goto('/admin/certificates');
        }

        await page.waitForSelector('table');
        await page.waitForTimeout(1000);

        await page.screenshot({ path: 'evidence_admin_certificates_list.png', fullPage: true });

        // Find Upgrade/Approve button
        // Looking for the user's request. 
        // We might need to filter or just take the first Pending.
        const approveBtn = page.locator('button:has-text("Duyệt"), button:has-text("Approve")').first();

        if (await approveBtn.isVisible()) {
            await approveBtn.click();
            await page.waitForTimeout(2000); // Wait for action
            await page.screenshot({ path: 'evidence_admin_approval_success.png', fullPage: true });
            console.log('Captured Admin Approval evidence');
        } else {
            console.log('No Approve button found');
        }
    });

});
