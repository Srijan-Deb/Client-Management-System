import { test, expect } from '@playwright/test';

/**
 * E2E: Client creation flow
 *
 * Auth state is loaded from e2e/.auth/user.json (set up in global-setup.ts).
 * This test runs against the live app at http://localhost:4200 with the real
 * backend stack (Keycloak, API Gateway, client-service, Redis).
 */

test.describe('Client Management — Create & Verify', () => {
  const uniqueEmail = `e2e-${Date.now()}@playwright-test.com`;

  test('navigates to /clients', async ({ page }) => {
    await page.goto('/clients');
    await expect(page.getByRole('heading', { name: /clients/i })).toBeVisible();
  });

  test('opens new client modal', async ({ page }) => {
    await page.goto('/clients');
    await page.getByRole('button', { name: /new client/i }).click();
    await expect(page.getByRole('heading', { name: /new client/i })).toBeVisible();
  });

  test('shows Zod validation errors on empty submit', async ({ page }) => {
    await page.goto('/clients');
    await page.getByRole('button', { name: /new client/i }).click();
    await page.getByRole('button', { name: /create client/i }).click();
    await expect(page.getByText(/first name is required/i)).toBeVisible();
    await expect(page.getByText(/last name is required/i)).toBeVisible();
  });

  test('creates a new client successfully', async ({ page }) => {
    await page.goto('/clients');
    await page.getByRole('button', { name: /new client/i }).click();

    await page.getByLabel(/first name/i).fill('Playwright');
    await page.getByLabel(/last name/i).fill('Tester');
    await page.getByLabel(/email/i).fill(uniqueEmail);
    await page.getByLabel(/phone/i).fill('+91 9000000001');

    // Select tier
    await page.selectOption('select', 'GOLD');

    await page.getByRole('button', { name: /create client/i }).click();

    // Modal should close on success
    await expect(page.getByRole('heading', { name: /new client/i })).toBeHidden({ timeout: 10_000 });

    // New row should appear in the table
    await expect(page.getByText('Playwright Tester')).toBeVisible({ timeout: 10_000 });
  });

  test('shows inline DUPLICATE_EMAIL error on re-use', async ({ page }) => {
    await page.goto('/clients');
    await page.getByRole('button', { name: /new client/i }).click();

    await page.getByLabel(/first name/i).fill('Another');
    await page.getByLabel(/last name/i).fill('Person');
    await page.getByLabel(/email/i).fill(uniqueEmail); // same email as above
    await page.getByRole('button', { name: /create client/i }).click();

    // Inline email error — not a toast
    await expect(page.getByText(/already exists/i)).toBeVisible({ timeout: 10_000 });
  });

  test('navigates to client detail page', async ({ page }) => {
    await page.goto('/clients');
    // Click the first client's name link
    const firstLink = page.locator('a.client-cell').first();
    await firstLink.click();

    await expect(page).toHaveURL(/\/clients\/\d+/);
    await expect(page.getByRole('button', { name: /billing/i })).toBeVisible();
  });
});

test.describe('Billing Page', () => {
  test('navigates to /billing and shows product catalog', async ({ page }) => {
    await page.goto('/billing');
    await expect(page.getByRole('heading', { name: /billing/i })).toBeVisible();
    await expect(page.getByText(/product catalog/i)).toBeVisible();
  });
});
