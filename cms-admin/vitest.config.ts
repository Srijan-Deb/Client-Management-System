// @ts-ignore
import { defineConfig } from 'vitest/config';
// @ts-ignore
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Redirect Vite's internal cache to D drive — C: has no space
  cacheDir: 'D:/vite-cache/cms-admin',
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
    globals: true,
    exclude: ['node_modules', 'e2e/**'],
    env: {
      VITE_API_BASE_URL: 'http://localhost:8085',
    },
  },
});
