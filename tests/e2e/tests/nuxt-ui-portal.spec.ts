import { test, expect } from '@playwright/test';

/**
 * E2E Tests for Updated Public Portal UI (Nuxt UI 4)
 * Tests the modernized UI with Nuxt UI components.
 * Runs against the actual deployed environment.
 */

const PORTAL_BASE = 'https://portal.gov-id.lvh.id.vn';

test.describe('Public Portal - Login Flow', () => {
    // Serial mode removed to allow all tests to run independently (where possible)
    // test.describe.configure({ mode: 'serial' });

    test.beforeEach(async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/login`);
    });

    test('should display login page with Nuxt UI components', async ({ page }) => {
        // Check page title
        await expect(page.locator('h1')).toContainText('Đăng nhập');

        // Check form fields exist with Nuxt UI styling
        // Use more specific selectors for Nuxt UI inputs
        await expect(page.locator('input[name="username"]')).toBeVisible();
        await expect(page.locator('input[name="password"]')).toBeVisible();

        // Check submit button
        await expect(page.getByRole('button', { name: /Đăng nhập/i })).toBeVisible();
    });

    test('should show validation errors for empty form', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/login`, { waitUntil: 'networkidle' });

        // Wait for Vue hydration
        await page.waitForSelector('input[name="username"]', { state: 'visible' });
        await page.waitForTimeout(500);

        // Click submit without filling
        await page.getByRole('button', { name: /Đăng nhập/i }).click();

        // Wait for validation to trigger
        await page.waitForTimeout(1000);
        console.log('Form text content:', await page.locator('form').textContent());

        await expect(page.getByText('Tên đăng nhập phải có ít nhất 3 ký tự')).toBeVisible({ timeout: 5000 });
        await expect(page.getByText('Mật khẩu phải có ít nhất 6 ký tự')).toBeVisible({ timeout: 5000 });
    });

    test('should successfully login and redirect to dashboard', async ({ page }) => {
        // Note: effective testing requires a valid user. 
        // We will try to register a new user for every test run to ensure a clean state.
        const username = `e2e_${Date.now()}`;

        // 1. Register - wait for page to be fully loaded and hydrated
        await page.goto(`${PORTAL_BASE}/register`, { waitUntil: 'networkidle' });

        // Wait for Vue hydration by checking for interactive elements
        await page.waitForSelector('input[name="username"]', { state: 'visible' });
        await page.waitForTimeout(500); // Small delay for Vue hydration

        await page.fill('input[name="username"]', username);
        await page.fill('input[name="email"]', `${username}@test.vn`);
        await page.fill('input[name="password"]', 'SecurePass123!');
        await page.fill('input[name="confirmPassword"]', 'SecurePass123!');

        // Click and wait for navigation
        await page.getByRole('button', { name: /Đăng ký/i }).click();

        // 2. Wait for either dashboard or login redirect (longer timeout)
        try {
            await page.waitForURL(/.*\/(dashboard|login)/, { timeout: 15000 });
        } catch {
            // If we're still on register, something went wrong
            console.log('Current URL after registration:', page.url());
        }

        const url = page.url();
        if (url.includes('/login')) {
            // Login manually if not auto-logged in
            await page.waitForSelector('input[name="username"]', { state: 'visible' });
            await page.fill('input[name="username"]', username);
            await page.fill('input[name="password"]', 'SecurePass123!');
            await page.getByRole('button', { name: /Đăng nhập/i }).click();
            await page.waitForURL(/.*\/dashboard/, { timeout: 10000 });
        }

        // 3. Verify Dashboard
        await expect(page).toHaveURL(/.*\/dashboard/, { timeout: 10000 });
        // Look for greeting or dashboard specific element
        // Adjust text expectation based on actual UI "Xin chào" or similar
        // We check for the username being present in the body (e.g. in header or welcome msg)
        await expect(page.locator('body')).toContainText(username, { timeout: 5000 });
    });
});

test.describe('Public Portal - Registration Flow', () => {
    test('should display registration form', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/register`);

        await expect(page.locator('h1')).toContainText('Đăng ký');
        await expect(page.locator('input[name="username"]')).toBeVisible();
        await expect(page.locator('input[name="email"]')).toBeVisible();
    });

    test('should validate password confirmation', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/register`, { waitUntil: 'networkidle' });

        // Wait for Vue hydration
        await page.waitForSelector('input[name="username"]', { state: 'visible' });
        await page.waitForTimeout(500);

        await page.fill('input[name="username"]', 'testmismatch');
        await page.fill('input[name="email"]', 'mismatch@test.vn');
        await page.fill('input[name="password"]', 'Password123!');
        await page.fill('input[name="confirmPassword"]', 'DifferentPassword');

        await page.getByRole('button', { name: /Đăng ký/i }).click();

        // Wait for validation to trigger
        await page.waitForTimeout(1500);
        console.log('Register Form text:', await page.locator('form').textContent());

        // Note: refine() validation requires all other validations to pass first
        await expect(page.getByText('Mật khẩu xác nhận không khớp')).toBeVisible({ timeout: 5000 });
    });
});

test.describe('Public Portal - Dashboard Features', () => {
    // Re-use session if possible, or login again
    test.beforeEach(async ({ page }) => {
        const username = `dash_${Date.now()}`;
        await page.goto(`${PORTAL_BASE}/register`, { waitUntil: 'networkidle' });

        // Wait for Vue hydration
        await page.waitForSelector('input[name="username"]', { state: 'visible' });
        await page.waitForTimeout(500);

        await page.fill('input[name="username"]', username);
        await page.fill('input[name="email"]', `${username}@test.vn`);
        await page.fill('input[name="password"]', 'SecurePass123!');
        await page.fill('input[name="confirmPassword"]', 'SecurePass123!');
        await page.getByRole('button', { name: /Đăng ký/i }).click();

        // Wait for redirect to dashboard or login
        try {
            await page.waitForURL(/.*\/(dashboard|login)/, { timeout: 15000 });
        } catch {
            console.log('Registration redirect timeout, URL:', page.url());
        }

        const url = page.url();
        if (url.includes('/login')) {
            await page.waitForSelector('input[name="username"]', { state: 'visible' });
            await page.fill('input[name="username"]', username);
            await page.fill('input[name="password"]', 'SecurePass123!');
            await page.getByRole('button', { name: /Đăng nhập/i }).click();
            await page.waitForURL(/.*\/dashboard/, { timeout: 10000 });
        }
    });

    test('should display dashboard with stats cards', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/dashboard`);
        // We might have 0 documents initially, checking for labels
        // Use .first() on the specific text locator, not on the expect
        await expect(page.getByText('Chứng chỉ').first()).toBeVisible();
        await expect(page.getByText('Văn bản đã ký').first()).toBeVisible();
    });

    test('should display quick action links', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/dashboard`);
        // Check for action links text
        await expect(page.locator('text=Ký văn bản')).toBeVisible();
        await expect(page.locator('text=Xác thực')).toBeVisible();
        await expect(page.locator('text=Chứng chỉ')).toBeVisible();
    });
});

test.describe('Public Portal - Sign Page', () => {
    test.beforeEach(async ({ page }) => {
        // Login first
        const username = `sign_${Date.now()}`;
        await page.goto(`${PORTAL_BASE}/register`, { waitUntil: 'networkidle' });

        // Wait for Vue hydration
        await page.waitForSelector('input[name="username"]', { state: 'visible' });
        await page.waitForTimeout(500);

        await page.fill('input[name="username"]', username);
        await page.fill('input[name="email"]', `${username}@test.vn`);
        await page.fill('input[name="password"]', 'SecurePass123!');
        await page.fill('input[name="confirmPassword"]', 'SecurePass123!');
        await page.getByRole('button', { name: /Đăng ký/i }).click();

        // Wait for redirect to dashboard or login
        try {
            await page.waitForURL(/.*\/(dashboard|login)/, { timeout: 15000 });
        } catch {
            console.log('Registration redirect timeout, URL:', page.url());
        }

        const url = page.url();
        if (url && url.includes('/login')) {
            await page.waitForSelector('input[name="username"]', { state: 'visible' });
            await page.fill('input[name="username"]', username);
            await page.fill('input[name="password"]', 'SecurePass123!');
            await page.getByRole('button', { name: /Đăng nhập/i }).click();
            await page.waitForURL(/.*\/dashboard/, { timeout: 10000 });
        }
    });

    test('should display sign page with key status', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/sign`);
        await page.waitForTimeout(2000);

        // Page header
        await expect(page.locator('h1')).toContainText('Ký tài liệu');

        // Key status card
        await expect(page.locator('text=Khóa ký của bạn')).toBeVisible();

        // TOTP status card
        await expect(page.locator('text=Xác thực 2 bước')).toBeVisible();
    });

    test('should show file upload area', async ({ page }) => {
        await page.goto(`${PORTAL_BASE}/sign/upload`, { waitUntil: 'networkidle' });
        await page.waitForTimeout(1000);

        // Check for file upload area - look for drag and drop text or upload button
        await expect(page.getByText(/kéo thả|chọn tệp|nhấp để chọn/i).first()).toBeVisible({ timeout: 5000 });
    });
});
