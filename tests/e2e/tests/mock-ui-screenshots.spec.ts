import { test, expect } from '@playwright/test';

test.describe('Mock UI Processes', () => {
    test.use({ baseURL: 'http://localhost:5173' });

    test('1. Registration Process (RA)', async ({ page }) => {
        await page.goto('/register');
        await expect(page.locator('h2')).toContainText('Đăng ký CKS');

        // Snapshot 1.1: Empty Form
        await page.screenshot({ path: 'artifacts/1_register_empty.png', fullPage: true });

        await page.fill('input[type="text"]', 'nguyenvana');
        await page.fill('input[type="email"]', 'nguyenvana@gov.vn');
        await page.fill('input[placeholder="012345678912"]', '001088012345');
        await page.selectOption('select', 'ML-DSA-65');

        await page.screenshot({ path: 'artifacts/1_register_form.png', fullPage: true });

        await page.click('button[type="submit"]');
        await expect(page.locator('.success')).toBeVisible();

        await page.screenshot({ path: 'artifacts/1_register_success.png', fullPage: true });
    });

    test('2. Dashboard & Certificates', async ({ page }) => {
        await page.goto('/dashboard');
        await expect(page.locator('h2')).toContainText('Quản lý Chứng thư số');
        await expect(page.locator('table')).toBeVisible();
        await page.screenshot({ path: 'artifacts/2_dashboard_list.png', fullPage: true });
    });

    test('3. Cloud Signing (CSC)', async ({ page }) => {
        await page.goto('/sign');
        await expect(page.locator('h2')).toContainText('Ký số từ xa');
        await page.screenshot({ path: 'artifacts/3_sign_initial.png', fullPage: true });

        await page.selectOption('select', 'key_mldsa65_alias');
        await page.setInputFiles('input[type="file"]', {
            name: 'contract.pdf',
            mimeType: 'application/pdf',
            buffer: Buffer.from('dummy content')
        });

        await page.screenshot({ path: 'artifacts/3_sign_ready.png', fullPage: true });

        await page.click('.btn-sign');
        await expect(page.locator('.result')).toBeVisible();
        await page.screenshot({ path: 'artifacts/3_sign_result.png', fullPage: true });
    });

    test('4. Validation (Verify)', async ({ page }) => {
        await page.goto('/verify');
        await expect(page.locator('h2')).toContainText('Kiểm tra Chữ ký');

        // Snapshot 4.1: Initial Empty State
        await page.screenshot({ path: 'artifacts/4_verify_initial.png', fullPage: true });

        await page.setInputFiles('input[type="file"]', {
            name: 'signed_doc.pdf',
            mimeType: 'application/pdf',
            buffer: Buffer.from('signed content')
        });

        // 4. Fill verification form with the mock signature
        await page.fill('textarea', `-----BEGIN ML-DSA-65 SIGNATURE-----
MIIFvzCCA6egAwIBAgIUM/WpHYxjDkX/xYrw0+hC7LUjfdswDQYJKoZIhvcNAQELBQAwbzELMAkGA1UEBhMCVk4xDjAMBgNVBAgMBUhhbm9pMQ8wDQYDVQQHDAZCYURpbmgxDDAKBgNVBAoMA0dvdjEPMA0GA1UECwwGUFFDLUNBMSAwHgYDVQQDDBdNb2NrIFBRQyBTaWduYXR1cmUgRGF0YTAeFw0yNTEyMjkwMTQ5MzlaFw0yNjEyMjkwMTQ5MzlaMG8xCzAJBgNVBAYTAlZOMQ4wDAYDVQQIDAVIYW5vaTEPMA0GA1UEBwwGQmFEaW5oMQwwCgYDVQQKDANHb3YxDzANBgNVBAsMBlBRQy1DQTEgMB4GA1UEAwwXTW9jayBQUUMgU2lnbmF0dXJlIERhdGEwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCbQvIznFp0G0ymvQpwGP3V2oCmR3q+XU3fcLWHx3nJJqQ5lT4niv1/psVBn5dEwphFuNfW6BcxULvbLoNLQ6IK/6qf86SaySbYiZeIFtF6aBekPONKsTWRU5zBkQNSrrL1h03agwwaVFmBxgJ9mDlmZ54t1S7LVXt0c/HZ3oWbrjP15DmK24HB/33dCzVt+Gmbo+u4qtaocHvcDTxpqRnKOl9rDNphcS9SK/u6C1/qW8KYG2jvl0aMuyLHKRuoALkYcLrXlxoA+MDxl0dq2gMPimy1vKltoTIpjhxNsV+0+oJxlVab24Yyuv+NA3tL9LVTglxuk6vUbLBGMZiPL4KODp+QqosUWSeJ5zcaztnyJ4Yw6s1WEgKuhBjzPYLsJ0ffSsT1rzrdivBcAWfBRcuTNQ7fZOxIbpeQqBpEi9S1k6ypdb31CgCdeWhH6BdjsWsTlAynAIqpqwFMLUljdJr5S4nsc4yZewhk1Lcz3fMkX3UESQFy1jW7Uxg1M2bRoHHGehH2z7KDcV6fJrL4LBg6A2W7vdYocCVmAqmbo43HbXvwVRG6IkK+AG7hkosPZLbP81fXBlGzMECVYO2atu4FmvPxzmAhfyEzEoH7svO3Wy31711Tb4Xl4SLlqVYUtDIjT7jOssvgxA50y4ABNO4QnWhNRniw3m8hpaVGMRzOxwIDAQABo1MwUTAdBgNVHQ4EFgQU+6WgqjJHs0UOCYotQ2QMXPz8AiswHwYDVR0jBBgwFoAU+6WgqjJHs0UOCYotQ2QMXPz8AiswDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAgEAhDwo0tlNFWo8qWKarAabwq6soklyS6rOYVbfSjZiGC2qQobBb9xi5rbvu7XCxxXo9jmm3Bg7YsDCRFiN6uuCdCPq3mn0wvLWIhsPIJ5mp4KSQCSzaK6do/jDzn4V5bSi5FRxw5gcD2gZNmRvhZX1xs8mF4g76T15R2aP75gMZnoLv2b+oKPSXkubR6iCm3p2FSY7W7kVEC+/oE/KkbFxtuhehtuBqd++nnqb4IhAxLKJw/5myeqGV9u4M0TZ7J/4nsuuKmm38/UFYfjrCNOWzVUDHG9szv/gddTKf6rMY8wrF5FSSPBO3TMfZxub7s5J7/2/d3RESRe36+lI8DasPONgswz7rfr9DvHBgNfpByFN2WQ1twbGEI2UhkUZccKh45kDm2arQCWVADw7Spxi2W+vwXplyfuhdJA5uZao+Yb02FcJm0+4f9I5bdMbxRfMttSrq5CUaByIoy7Sk7SuxTu2SKavgNYk2uEjyCUgsAm4RqcNGG6WDHa6CJp10J/4lvkiD9Ohlhd/pR/m3Epw61UYYVgO5utS8iNP8xhlw8piwesCO2HZT/k/Qz5dNtg9iLgr+W1ozOcMxESacppC+utpQkosctOocxhd81Mraiul5sTt3m79eHDVzKZr/eAGonxDqwQbv4wob69nOwMNZ7DMZWYAAZniY9/d1rJbl94=
-----END ML-DSA-65 SIGNATURE-----`);

        // Snapshot 4.2: Filled State
        await page.screenshot({ path: 'artifacts/4_verify_filled.png', fullPage: true });

        await page.click('.btn-verify');
        await expect(page.locator('.result-box')).toBeVisible();

        // Assert Timestamp
        await expect(page.locator('.verify-time')).toContainText('Được xác thực lúc:');

        // Check Trust Chain is visible (default open)
        // Note: Now we have two .chain-list elements (one for TSA, one for Chain).
        // Using .first() or specific locators to avoid strict mode violation.

        // TSA Section Check
        await expect(page.locator('h4', { hasText: 'Chứng nhận Thời gian (TSA)' })).toBeVisible();

        // Trust Chain Check
        await expect(page.locator('h4', { hasText: 'Chuỗi Tin Cậy (Trust Chain)' })).toBeVisible();

        // Snapshot 4.3: Result Full Page
        await page.screenshot({ path: 'artifacts/4_verify_result.png', fullPage: true });

        // Snapshot 4.4: Chain Detail (Trust Chain)
        // Scoping to the Trust Chain section specifically
        const trustChainSection = page.locator('.section-block').filter({ hasText: 'Chuỗi Tin Cậy' }).locator('.chain-list');
        await expect(trustChainSection).toBeVisible();
        await trustChainSection.screenshot({ path: 'artifacts/4_verify_chain_detail.png' });

        // Snapshot 4.5: TSA Detail
        const tsaSection = page.locator('.section-block').filter({ hasText: 'Chứng nhận Thời gian' }).locator('.chain-list');
        await expect(tsaSection).toBeVisible();
        await tsaSection.screenshot({ path: 'artifacts/4_verify_tsa_detail.png' });
    });

    test('5. Officer Workflow', async ({ page }) => {
        await page.goto('/officer');
        await expect(page.locator('h2')).toContainText('Quản trị hệ thống');

        // Snapshot 5.1: Officer Dashboard
        await page.screenshot({ path: 'artifacts/5_officer_dashboard.png', fullPage: true });

        // Click first review button
        await page.click('.btn-review');
        await expect(page.locator('h2')).toContainText('Phê duyệt Yêu cầu');

        // Snapshot 5.2: Review Details
        await page.screenshot({ path: 'artifacts/5_officer_review.png', fullPage: true });

        // Click "Proceed to Sign"
        await page.click('.btn-next');
        await expect(page.locator('.signing-ui')).toBeVisible();

        // Snapshot 5.3: Signing UI
        await page.screenshot({ path: 'artifacts/5_officer_signer.png', fullPage: true });

        // Perform Sign
        await page.click('.btn-sign');
        await expect(page.locator('.success-box')).toBeVisible();

        // Snapshot 5.4: Sign Success
        await page.screenshot({ path: 'artifacts/5_officer_success.png', fullPage: true });
    });
});
