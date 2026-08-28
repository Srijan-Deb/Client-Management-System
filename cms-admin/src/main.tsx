import { createRoot } from 'react-dom/client';
import * as Sentry from '@sentry/react';
import './index.css';
import App from './App.tsx';

Sentry.init({
  dsn: import.meta.env.VITE_SENTRY_DSN || "https://dummy@o0.ingest.sentry.io/0",
  integrations: [
    Sentry.browserTracingIntegration(),
    Sentry.replayIntegration(),
  ],
  tracesSampleRate: 1.0,
  replaysSessionSampleRate: 0.1,
  replaysOnErrorSampleRate: 1.0,
});

// StrictMode is intentionally omitted: keycloak-js init() cannot be called
// twice (which StrictMode triggers in dev). The useRef guard in AuthProvider
// is the correct alternative guard for double-invocation protection.
createRoot(document.getElementById('root')!).render(<App />);
