import { test, expect } from '@playwright/test';

test.describe('PQC Client-Side Key Generation Flow', () => {
    const TEST_USER = `testuser_${Date.now()}`;
    const TEST_EMAIL = `${TEST_USER}@example.com`;

    test.beforeEach(async ({ page }) => {
        // Navigate to registration page
        await page.goto('/register');
    });

    test('should generate ML-DSA keys and submit CSR', async ({ page }) => {
        // Fill form
        await page.fill('input[placeholder="nguyenvana"]', TEST_USER);
        await page.fill('input[placeholder="email@example.com"]', TEST_EMAIL);
        await page.selectOption('select', 'ML-DSA-65');
        await page.fill('input[placeholder="012345678912"]', '123456789012');

        // Wait for console logs to verify PQC generation
        const consoleLogs: string[] = [];
        page.on('console', msg => consoleLogs.push(msg.text()));

        // Submit
        await page.click('button[type="submit"]');

        // Expect loading state
        await expect(page.locator('button')).toBeDisabled();
        await expect(page.locator('button')).toHaveText('Đang xử lý...');

        // Wait for success message
        await expect(page.locator('.success')).toBeVisible({ timeout: 10000 });
        await expect(page.locator('.success')).toHaveText('Gửi yêu cầu thành công!');

        // Verify console logs for PQC steps
        const generationLog = consoleLogs.find(l => l.includes('Generating ML-DSA-65 key pair'));
        const csrLog = consoleLogs.find(l => l.includes('Generating CSR for'));
        const successLog = consoleLogs.find(l => l.includes('Enrollment success'));

        expect(generationLog).toBeTruthy();
        expect(csrLog).toBeTruthy();
        expect(successLog).toBeTruthy();
    });
});
