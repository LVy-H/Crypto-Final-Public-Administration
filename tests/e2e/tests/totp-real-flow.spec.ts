import { test, expect } from '@playwright/test';
import { authenticator } from 'otplib';

test.use({
    baseURL: 'https://portal.gov-id.lvh.id.vn',
    ignoreHTTPSErrors: true,
    video: 'on'
});

test.describe('Real Portal TOTP Flow', () => {

    test('Full User Journey: Register -> Setup -> Sign', async ({ page }) => {
        const username = `u_${Date.now()}`;
        const email = `${username}@test.vn`;
        const password = 'Test@1234';

        console.log(`Starting Real Journey for ${username} on ${test.info().project.use.baseURL}`);

        // 1. Register (UI) - Multi-step form
        await page.goto('/register');
        await page.waitForLoadState('networkidle'); // Wait for hydration

        // Wait for Step 1 form to be ready
        await page.waitForSelector('#username', { state: 'visible', timeout: 10000 });
        console.log('Step 1 form visible, filling...');

        // Step 1: Account Info
        await page.fill('#username', username);
        await page.fill('#email', email);
        await page.fill('#password', password);
        console.log('Step 1 filled, clicking submit');
        await page.click('button[type="submit"]'); // Click "Tiếp tục" -> Step 2

        // Step 2: Certificate Info (KYC) - Wait for Step 2 form to appear
        console.log('Waiting for Step 2 (KYC form)...');
        const kycInput = page.locator('input[placeholder*="012345"]');
        await expect(kycInput).toBeVisible({ timeout: 5000 });
        await kycInput.fill('012345678901');
        console.log('KYC filled, clicking submit');
        await page.click('button[type="submit"]'); // Click "Đăng ký & Gửi yêu cầu" -> Step 3

        // Wait for navigation to dashboard or login (registration redirects after 2s)
        console.log('Waiting for redirect from registration...');
        try {
            // Wait for URL to change from /register
            await page.waitForURL(url => !url.includes('/register'), { timeout: 10000 });
        } catch {
            console.log('Timeout waiting for redirect, current URL:', page.url());
        }

        let currentUrl = page.url();
        console.log('After registration, current URL:', currentUrl);

        // Helper to login
        const doLogin = async () => {
            console.log('Performing login with:', username);
            await page.fill('#username', username);
            await page.fill('#password', password);
            await page.click('button[type="submit"]');
            await page.waitForURL('**/dashboard', { timeout: 15000 });
        };

        // Handle various outcomes
        if (currentUrl.includes('/dashboard')) {
            console.log('Already on dashboard');
        } else if (currentUrl.includes('/login')) {
            await doLogin();
        } else {
            // Still on register - manual navigation via link
            console.log('Still on register, clicking dashboard link...');
            await page.click('a[href="/dashboard"]');
            await page.waitForURL('**/dashboard', { timeout: 10000 });
        }
        console.log('Now on dashboard');

        // Debug: Check localStorage state
        const lsState = await page.evaluate(() => {
            return {
                sessionId: localStorage.getItem('sessionId'),
                user: localStorage.getItem('user'),
                token: localStorage.getItem('token')
            };
        });
        console.log('localStorage state:', JSON.stringify(lsState));

        // 3. Setup TOTP - Click the security link on dashboard
        console.log('Navigating to Security Settings');
        await page.click('a[href="/settings/security"]');
        await page.waitForURL('**/settings/security', { timeout: 10000 });
        console.log('Security page URL:', page.url());

        // Verify Page Loaded
        const heading = page.locator('h2');
        await expect(heading).toContainText('Cài đặt bảo mật', { timeout: 10000 });

        // Activations - click TOTP setup button
        console.log('Clicking TOTP setup button');
        await page.click('.setup-prompt button');
        await page.waitForTimeout(3000); // Wait for API call
        console.log('After TOTP button click, URL:', page.url());

        // Check if there's an error message
        const errorMsg = page.locator('.error');
        if (await errorMsg.count() > 0) {
            console.log('Error message:', await errorMsg.textContent());
        }

        const qrContainer = page.locator('canvas, svg, img[alt*="QR"]');
        await expect(qrContainer.first()).toBeVisible({ timeout: 10000 });

        // Get TOTP secret from page
        const secretEl = page.locator('code, .secret-text');
        const secretText = await secretEl.textContent();
        expect(secretText).toBeTruthy();
        const secret = secretText ?? '';

        // Generate Token
        const cleanSecret = secret.replace(/\s/g, '');
        const token = authenticator.generate(cleanSecret);

        // Verify TOTP
        await page.fill('.totp-input', token);
        await page.click('button:has-text("Xác nhận")');
        await expect(page.locator('.success, .badge-active')).toBeVisible();
        console.log('TOTP Activated');

        // 4. Sign Document
        await page.goto('/sign/upload');
        // Upload
        await page.setInputFiles('input[type="file"]', {
            name: 'real-doc.pdf',
            mimeType: 'application/pdf',
            buffer: Buffer.from('Real Content') as unknown as Buffer
        });

        await page.click('button:has-text("Ký văn bản")');

        // Modal
        const modal = page.locator('.modal');
        await expect(modal).toBeVisible();

        // Generate Sign Token
        const signToken = authenticator.generate(cleanSecret);
        await modal.locator('input.totp-input').fill(signToken);
        await modal.locator('button:has-text("Xác nhận")').click();

        // Result
        await expect(page.locator('.result-section')).toBeVisible();
        console.log('Signing Complete');
    });
});
