import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: 1,
  use: {
    baseURL: 'http://localhost:4200',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    // The Keycloak login happens in a real browser context
    ignoreHTTPSErrors: true,
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // Store authenticated state so tests can skip login
        storageState: 'e2e/.auth/user.json',
      },
    },
  ],
  // Auth setup runs once before all tests
  globalSetup: './e2e/global-setup.ts',
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env.CI,
  },
});
