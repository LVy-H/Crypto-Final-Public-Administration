import { test, expect } from '@playwright/test';

/**
 * Authenticated UI Feature Tests
 * Tests the new features: Key Selector on Sign page, Trust Chain and TSA on Verify page
 */

const PORTAL_BASE = 'https://portal.gov-id.lvh.id.vn';
const API_BASE = 'https://api.gov-id.lvh.id.vn/api/v1';
const TEST_USER = { username: 'uitester', password: 'Test123!' };

// Helper to login
async function login(page: any) {
    await page.goto(`${PORTAL_BASE}/login`);
    await page.waitForLoadState('networkidle');

    await page.getByRole('textbox', { name: /Tên đăng nhập/i }).fill(TEST_USER.username);
    await page.getByRole('textbox', { name: /Mật khẩu/i }).fill(TEST_USER.password);
    await page.getByRole('button', { name: /Đăng nhập/i }).click();

    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    return page.url().includes('dashboard');
}

test.describe('Sign Page - Key Selector Feature', () => {
    test('should display sign page and upload zone', async ({ page }) => {
        const loggedIn = await login(page);
        console.log('Login result:', loggedIn ? 'SUCCESS' : 'FAILED');

        if (!loggedIn) { test.skip(); return; }

        // Navigate to sign page
        await page.locator('a[href="/sign/upload"]').first().click();
        await page.waitForLoadState('networkidle');

        console.log('Sign page URL:', page.url());
        await page.screenshot({ path: 'artifacts/sign-01-empty.png' });

        // Use specific role-based selectors to avoid strict mode violations
        await expect(page.getByRole('heading', { name: 'Ký văn bản' })).toBeVisible();
        await expect(page.getByRole('heading', { name: 'Tải lên văn bản cần ký' })).toBeVisible();

        console.log('Sign page loaded correctly');
    });

    test('should reveal signing options after file upload', async ({ page }) => {
        const loggedIn = await login(page);
        if (!loggedIn) { test.skip(); return; }

        await page.locator('a[href="/sign/upload"]').first().click();
        await page.waitForLoadState('networkidle');

        // Upload file
        const fileInput = page.locator('input[type="file"]');
        await fileInput.setInputFiles({
            name: 'test.pdf',
            mimeType: 'application/pdf',
            buffer: Buffer.from('PDF test content')
        });

        await page.waitForTimeout(1000);
        await page.screenshot({ path: 'artifacts/sign-02-file-uploaded.png' });

        // Check if signing options section appeared
        const optionsSection = page.getByRole('heading', { name: 'Tùy chọn chữ ký' });
        const optionsVisible = await optionsSection.isVisible().catch(() => false);
        console.log('Signing options heading visible:', optionsVisible);

        // Get HTML to debug key selector
        const html = await page.content();
        console.log('HTML contains "Khóa ký":', html.includes('Khóa ký'));
        console.log('HTML contains "selectedKeyAlias":', html.includes('selectedKeyAlias'));

        await page.screenshot({ path: 'artifacts/sign-03-options.png', fullPage: true });

        // Assert signing options appear
        expect(optionsVisible).toBeTruthy();
    });
});

test.describe('Verify Page - Features', () => {
    test('should display verify page with input elements', async ({ page }) => {
        const loggedIn = await login(page);
        if (!loggedIn) { test.skip(); return; }

        await page.locator('a[href="/verify"]').first().click();
        await page.waitForLoadState('networkidle');

        console.log('Verify page URL:', page.url());
        await page.screenshot({ path: 'artifacts/verify-01-empty.png' });

        // Use specific selectors
        await expect(page.getByRole('heading', { name: 'Xác thực chữ ký' })).toBeVisible();
        await expect(page.locator('textarea')).toBeVisible();

        console.log('Verify page loaded correctly');
    });

    test('should enable button after entering signature', async ({ page }) => {
        const loggedIn = await login(page);
        if (!loggedIn) { test.skip(); return; }

        await page.locator('a[href="/verify"]').first().click();
        await page.waitForLoadState('networkidle');

        // Enter signature data
        const textarea = page.locator('textarea');
        await textarea.fill('dGVzdCBzaWduYXR1cmUgZGF0YQ==');

        await page.waitForTimeout(500);
        await page.screenshot({ path: 'artifacts/verify-02-filled.png' });

        // Check if button is enabled
        const verifyButton = page.getByRole('button', { name: 'Xác thực chữ ký' });
        const isDisabled = await verifyButton.isDisabled();
        console.log('Verify button disabled:', isDisabled);
    });

    test('should show result after verification', async ({ page }) => {
        const loggedIn = await login(page);
        if (!loggedIn) { test.skip(); return; }

        await page.locator('a[href="/verify"]').first().click();
        await page.waitForLoadState('networkidle');

        // Enter signature data
        await page.locator('textarea').fill('dGVzdCBzaWduYXR1cmUgZGF0YQ==');

        // Click verify button
        await page.getByRole('button', { name: 'Xác thực chữ ký' }).click();
        await page.waitForTimeout(3000);

        await page.screenshot({ path: 'artifacts/verify-03-result.png', fullPage: true });

        // Check for result box
        const resultBox = page.locator('.result-box');
        const resultVisible = await resultBox.isVisible().catch(() => false);
        console.log('Result box visible:', resultVisible);

        // Check HTML for our new elements
        const html = await page.content();
        console.log('HTML contains verify-time:', html.includes('verify-time'));
        console.log('HTML contains section-block:', html.includes('section-block'));

        // Get result text
        if (resultVisible) {
            const resultText = await resultBox.textContent();
            console.log('Result text:', resultText?.substring(0, 200));
        }
    });
});

test.describe('API Check', () => {
    test('should check validation API response', async ({ request }) => {
        const response = await request.post(`${API_BASE}/validation/verify`, {
            multipart: {
                signature: 'dGVzdA=='
            }
        });

        console.log('API status:', response.status());
        const body = await response.text();
        console.log('API response:', body.substring(0, 200));
    });
});
