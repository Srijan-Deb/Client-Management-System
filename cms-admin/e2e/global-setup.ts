import { chromium } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Global setup — authenticates once with Keycloak and saves
 * storage state (cookies + localStorage) so individual test
 * files can skip the login step.
 *
 * Credentials come from env vars so nothing is hard-coded.
 *   PLAYWRIGHT_USER    (default: admin)
 *   PLAYWRIGHT_PASS    (default: admin)
 */
async function globalSetup() {
  const browser = await chromium.launch();
  const page = await browser.newPage();

  const baseURL = process.env.BASE_URL ?? 'http://localhost:4200';
  const username = process.env.PLAYWRIGHT_USER ?? 'admin';
  const password = process.env.PLAYWRIGHT_PASS ?? 'admin';

  await page.goto(`${baseURL}/login`);

  // Click the Keycloak login button
  await page.getByRole('button', { name: /sign in with keycloak/i }).click();

  // Fill in Keycloak login form
  await page.fill('#username', username);
  await page.fill('#password', password);
  await page.click('#kc-login');

  // Wait until redirected back to the app
  await page.waitForURL(`${baseURL}/`);

  // Save auth state
  const authDir = path.join(__dirname, '.auth');
  if (!fs.existsSync(authDir)) fs.mkdirSync(authDir, { recursive: true });
  await page.context().storageState({ path: path.join(authDir, 'user.json') });

  await browser.close();
}

export default globalSetup;
