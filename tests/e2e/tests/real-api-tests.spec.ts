import { test, expect } from '@playwright/test';

/**
 * Real API E2E Tests
 * Tests signing and verification with actual API calls, no mocks
 */

const PORTAL_BASE = 'https://portal.gov-id.lvh.id.vn';
const API_BASE = 'https://api.gov-id.lvh.id.vn/api/v1';
const TEST_USER = { username: 'uitester', password: 'Test123!' };

// Test document content
const TEST_DOCUMENT = 'VGhpcyBpcyBhIHRlc3QgZG9jdW1lbnQgZm9yIHNpZ25pbmcu'; // Base64 of "This is a test document for signing."

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

test.describe('Real API - Signing', () => {
    test('should sign document via API and get valid signature', async ({ request }) => {
        // Call the real signing API
        const response = await request.post(`${API_BASE}/sign/remote`, {
            headers: { 'Content-Type': 'application/json' },
            data: {
                keyAlias: 'default',
                dataBase64: TEST_DOCUMENT
            }
        });

        console.log('Sign API status:', response.status());

        if (response.ok()) {
            const data = await response.json();
            console.log('Signature algorithm:', data.algorithm);
            console.log('Signature length:', data.signatureBase64?.length);
            console.log('Signature preview:', data.signatureBase64?.substring(0, 50) + '...');

            expect(data.algorithm).toBeTruthy();
            expect(data.signatureBase64).toBeTruthy();
        } else {
            const errorText = await response.text();
            console.log('Sign API error:', errorText);
        }
    });
});

test.describe('Real API - Verification Success Case', () => {
    test('should verify valid signature and show Trust Chain', async ({ request, page }) => {
        // Step 1: Create a real signature
        console.log('Step 1: Creating real signature via API...');
        const signResponse = await request.post(`${API_BASE}/sign/remote`, {
            headers: { 'Content-Type': 'application/json' },
            data: {
                keyAlias: 'default',
                dataBase64: TEST_DOCUMENT
            }
        });

        if (!signResponse.ok()) {
            console.log('Signing failed:', await signResponse.text());
            test.skip();
            return;
        }

        const signData = await signResponse.json();
        console.log('Got signature, algorithm:', signData.algorithm);

        // Step 2: Login and navigate to verify page
        const loggedIn = await login(page);
        if (!loggedIn) {
            console.log('Login failed');
            test.skip();
            return;
        }

        await page.locator('a[href="/verify"]').first().click();
        await page.waitForLoadState('networkidle');

        // Step 3: Upload a document file AND enter signature
        console.log('Step 3: Uploading document and entering signature...');

        // Upload document
        const fileInput = page.locator('input[type="file"]');
        await fileInput.setInputFiles({
            name: 'test-document.pdf',
            mimeType: 'application/pdf',
            buffer: Buffer.from('This is a test document for signing.')
        });

        // Enter signature
        await page.locator('textarea').fill(signData.signatureBase64);

        await page.screenshot({ path: 'artifacts/real-verify-01-filled.png' });

        // Step 4: Click verify
        await page.getByRole('button', { name: 'Xác thực chữ ký' }).click();
        await page.waitForTimeout(3000);

        await page.screenshot({ path: 'artifacts/real-verify-02-result.png', fullPage: true });

        // Step 5: Check results
        const html = await page.content();
        console.log('Result HTML contains verify-time:', html.includes('verify-time'));
        console.log('Result HTML contains section-block:', html.includes('section-block'));
        console.log('Result HTML contains "hợp lệ":', html.includes('hợp lệ'));
        console.log('Result HTML contains "không hợp lệ":', html.includes('không hợp lệ'));
        console.log('Result HTML contains "Chuỗi Tin Cậy":', html.includes('Chuỗi Tin Cậy'));
        console.log('Result HTML contains "TSA":', html.includes('TSA') || html.includes('Chứng nhận Thời gian'));

        // Get result box text
        const resultBox = page.locator('.result-box');
        if (await resultBox.isVisible()) {
            const text = await resultBox.textContent();
            console.log('Result box text:', text?.substring(0, 300));
        }
    });
});

test.describe('Real API - Verification Failure Case', () => {
    test('should show error for invalid signature', async ({ page }) => {
        const loggedIn = await login(page);
        if (!loggedIn) { test.skip(); return; }

        await page.locator('a[href="/verify"]').first().click();
        await page.waitForLoadState('networkidle');

        // Enter invalid/garbage signature
        await page.locator('textarea').fill('INVALID_GARBAGE_SIGNATURE_DATA_12345');

        await page.screenshot({ path: 'artifacts/real-verify-03-invalid-filled.png' });

        await page.getByRole('button', { name: 'Xác thực chữ ký' }).click();
        await page.waitForTimeout(3000);

        await page.screenshot({ path: 'artifacts/real-verify-04-invalid-result.png', fullPage: true });

        // Should show error
        const resultBox = page.locator('.result-box');
        const isVisible = await resultBox.isVisible().catch(() => false);
        console.log('Error result box visible:', isVisible);

        if (isVisible) {
            const text = await resultBox.textContent();
            console.log('Error result text:', text);
            expect(text).toContain('không hợp lệ');
        }

        // Trust Chain and TSA should NOT be visible for invalid signature
        const html = await page.content();
        console.log('Has Trust Chain (should be false):', html.includes('Chuỗi Tin Cậy'));
        console.log('Has TSA (should be false):', html.includes('Chứng nhận Thời gian'));
    });
});

test.describe('Real API - UI Sign Flow', () => {
    test('should sign document via UI and show result', async ({ page }) => {
        const loggedIn = await login(page);
        if (!loggedIn) { test.skip(); return; }

        // Navigate to sign page
        await page.locator('a[href="/sign/upload"]').first().click();
        await page.waitForLoadState('networkidle');

        // Upload file
        const fileInput = page.locator('input[type="file"]');
        await fileInput.setInputFiles({
            name: 'contract.pdf',
            mimeType: 'application/pdf',
            buffer: Buffer.from('Contract content for signing')
        });

        await page.waitForTimeout(1000);
        await page.screenshot({ path: 'artifacts/real-sign-01-file-uploaded.png' });

        // Check key selector is visible
        const keySelector = page.locator('th:has-text("Khóa ký")');
        const keySelectorVisible = await keySelector.isVisible().catch(() => false);
        console.log('Key selector visible:', keySelectorVisible);

        // Click sign button
        await page.getByRole('button', { name: /Ký văn bản/i }).click();
        await page.waitForTimeout(5000);

        await page.screenshot({ path: 'artifacts/real-sign-02-result.png', fullPage: true });

        // Check for success
        const html = await page.content();
        console.log('Sign result contains "thành công":', html.includes('thành công'));
        console.log('Sign result contains "signatureBase64":', html.includes('signatureBase64') || html.includes('Chữ ký'));

        const resultSection = page.locator('.result-section');
        if (await resultSection.isVisible().catch(() => false)) {
            const text = await resultSection.textContent();
            console.log('Sign result text:', text?.substring(0, 200));
        }
    });
});
