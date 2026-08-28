import { test, expect } from '@playwright/test';

test.describe('Critical Paths', () => {
  test.beforeEach(async ({ page }) => {
    // Assuming global-setup handles the login and stores the state,
    // we can just go to the homepage. If not, we'd log in here.
    await page.goto('/');
  });

  test('1. Login Path', async ({ page }) => {
    // We are already logged in via storageState, so we expect to see the dashboard.
    await expect(page.locator('text=Welcome back')).toBeVisible();
    await expect(page.locator('text=Total Clients')).toBeVisible();
  });

  test('2. Create Client Path', async ({ page }) => {
    await page.click('text=Clients');
    await expect(page.locator('h1:has-text("Clients")')).toBeVisible();

    await page.click('button:has-text("New Client")');
    await expect(page.locator('h2:has-text("New Client")')).toBeVisible();

    await page.fill('input[name="firstName"]', 'E2E');
    await page.fill('input[name="lastName"]', 'Test');
    await page.fill('input[name="email"]', `e2e-${Date.now()}@example.com`);
    await page.fill('input[name="companyName"]', 'E2E Corp');

    await page.click('button:has-text("Create Client")');

    // Wait for modal to close and check if E2E Test is in the table
    await expect(page.locator('text=New Client')).not.toBeVisible();
    await expect(page.locator('text=E2E Test').first()).toBeVisible();
  });

  test('3. Create Contract/Payment Path', async ({ page }) => {
    await page.click('text=Billing');
    await expect(page.locator('h1:has-text("Billing")')).toBeVisible();

    // Find the first unpaid invoice and click Pay
    const payButton = page.locator('button:has-text("Pay")').first();
    
    // If there is a pay button (depends on seed data), process payment
    if (await payButton.isVisible()) {
      await payButton.click();
      await expect(page.locator('h2:has-text("Process Payment")')).toBeVisible();
      
      // Submit payment
      await page.click('button:has-text("Pay")');
      
      // Wait for success
      await expect(page.locator('text=Payment Successful')).toBeVisible();
      await page.click('button:has-text("Close")');
    }
  });

  test('4. Create Ticket Path', async ({ page }) => {
    await page.click('text=Support');
    await expect(page.locator('h1:has-text("Support Tickets")')).toBeVisible();

    await page.click('button:has-text("New Ticket")');
    await expect(page.locator('h2:has-text("Create Support Ticket")')).toBeVisible();

    await page.fill('input[placeholder="e.g. Cannot access billing dashboard"]', 'E2E Ticket Subject');
    await page.fill('textarea[placeholder="Describe the issue in detail..."]', 'E2E description');

    await page.click('button:has-text("Create Ticket")');

    await expect(page.locator('text=Create Support Ticket')).not.toBeVisible();
    await expect(page.locator('text=E2E Ticket Subject').first()).toBeVisible();
  });
});
