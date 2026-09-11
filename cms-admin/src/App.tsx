
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './auth/AuthProvider';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { ThemeProvider } from './lib/ThemeProvider';
import { ErrorBoundary } from './components/ErrorBoundary';
import React, { Suspense, useMemo, useState } from 'react';
import AppShell from './components/AppShell';
import keycloak from './lib/keycloak';

const CMS_EMOJIS = ['👥','📄','💳','🎫','🏢','📊','📈','💼','🔔','📧','🤝','📋','🗂️','💰','🔑','📱','⭐','🏆','📝','🔒'];

const FloatingEmojis = () => {
  const particles = useMemo(() =>
    Array.from({ length: 30 }, (_, i) => ({
      id: i,
      emoji: CMS_EMOJIS[i % CMS_EMOJIS.length],
      left: `${((i * 3.33) + Math.sin(i * 1.7) * 10 + 50) % 100}%`,
      size: 16 + (i % 5) * 7,
      duration: 14 + (i % 8) * 2.5,
      delay: -(i * 1.55),
      opacity: 0.07 + (i % 4) * 0.04,
      rotate: (i % 2 === 0 ? 1 : -1) * (8 + (i % 4) * 12),
    })), []
  );
  return (
    <div className="login-emoji-bg" aria-hidden="true">
      {particles.map((p) => (
        <span
          key={p.id}
          className="login-emoji-particle"
          style={{
            left: p.left,
            fontSize: `${p.size}px`,
            animationDuration: `${p.duration}s`,
            animationDelay: `${p.delay}s`,
            opacity: p.opacity,
            '--rotate-end': `${p.rotate}deg`,
          } as React.CSSProperties}
        >
          {p.emoji}
        </span>
      ))}
    </div>
  );
};

const DashboardPage = React.lazy(() => import('./pages/DashboardPage'));
const ClientsPage = React.lazy(() => import('./pages/ClientsPage'));
const ClientDetailPage = React.lazy(() => import('./pages/ClientDetailPage'));
const BillingPage = React.lazy(() => import('./pages/BillingPage'));
const SupportTicketsPage = React.lazy(() => import('./pages/SupportTicketsPage'));
const SupportTicketDetailPage = React.lazy(() => import('./pages/SupportTicketDetailPage'));
const LazyAccountsPage = React.lazy(() => import('./pages/AccountsPage'));
const LazyUsersPage = React.lazy(() => import('./pages/UsersPage'));


const queryClient = new QueryClient({
  defaultOptions: { 
    queries: { 
      retry: 1, 
      staleTime: 30_000,
      throwOnError: (error: any) => {
        const status = error?.response?.status;
        return status === 429 || status >= 500;
      }
    },
    mutations: {
      throwOnError: false
    }
  },
});

import { evaluatePasswordStrength } from './utils/passwordValidation';
import { PasswordStrengthMeter } from './components/PasswordStrengthMeter';
import axios from 'axios';

const LoginPage = () => {
  const { login, isAuthenticated, isInitialized } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isInitialized) {
    return (
      <div className="auth-loading">
        <div className="spinner" />
        <p>Initializing…</p>
      </div>
    );
  }

  if (isAuthenticated) return <Navigate to="/" replace />;

  const passwordStrength = evaluatePasswordStrength(password);

  const handleDirectLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');

    // Username validation
    if (!username.trim()) {
      setErrorMsg('Please enter your username or email address.');
      return;
    }

    // Strong password validation
    if (!password) {
      setErrorMsg('Password is required.');
      return;
    }

    if (!passwordStrength.isStrong) {
      setErrorMsg(
        'Strong password required: Password must contain at least 8 characters, 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special character.'
      );
      return;
    }

    setIsSubmitting(true);
    try {
      const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080';
      const realm = import.meta.env.VITE_KEYCLOAK_REALM || 'cms';
      const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'cms-admin';

      const tokenEndpoint = `${keycloakUrl}/realms/${realm}/protocol/openid-connect/token`;
      const body = new URLSearchParams({
        grant_type: 'password',
        client_id: clientId,
        username: username.trim(),
        password: password,
        scope: 'openid profile email',
      });

      const response = await axios.post(tokenEndpoint, body, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      });

      if (response.data?.access_token) {
        // Successfully validated credentials — redirect into Keycloak session
        keycloak.login({ loginHint: username.trim() });
      } else {
        setErrorMsg('Authentication failed: No token received.');
      }
    } catch (err: any) {
      const serverDesc = err?.response?.data?.error_description;
      if (serverDesc) {
        setErrorMsg(`Authentication Error: ${serverDesc}`);
      } else if (err?.response?.status === 401 || err?.response?.status === 400) {
        setErrorMsg('Invalid username or password. Please check your credentials.');
      } else {
        // Fallback to standard Keycloak SSO redirect
        keycloak.login({ loginHint: username.trim() });
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      <FloatingEmojis />
      <div className="login-card" style={{ maxWidth: '440px', padding: '36px 32px' }}>
        <div className="login-logo" style={{ marginBottom: '16px' }}>
          <div className="logo-icon login-logo-icon">C</div>
        </div>
        <h1 className="login-title" style={{ fontSize: '20px', marginBottom: '4px' }}>CMS Portal Login</h1>
        <p className="login-subtitle" style={{ fontSize: '13px', marginBottom: '20px' }}>
          Sign in with your organizational account and verified credentials.
        </p>

        {errorMsg && (
          <div
            style={{
              background: 'rgba(239, 68, 68, 0.12)',
              border: '1px solid var(--danger)',
              color: 'var(--danger)',
              padding: '10px 14px',
              borderRadius: 'var(--radius-sm)',
              fontSize: '12.5px',
              lineHeight: 1.4,
              textAlign: 'left',
              marginBottom: '16px',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '8px',
            }}
          >
            <span>⚠️</span>
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleDirectLogin} style={{ textAlign: 'left' }}>
          {/* Username / Email */}
          <div className="form-group" style={{ marginBottom: '14px' }}>
            <label className="form-label" htmlFor="login-username">
              Username or Email *
            </label>
            <input
              id="login-username"
              type="text"
              className="form-input"
              placeholder="e.g. admin or client@example.com"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={isSubmitting}
              autoComplete="username"
              required
            />
          </div>

          {/* Password with Show/Hide toggle */}
          <div className="form-group" style={{ marginBottom: '6px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
              <label className="form-label" htmlFor="login-password" style={{ margin: 0 }}>
                Password *
              </label>
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--text-muted)',
                  fontSize: '11px',
                  cursor: 'pointer',
                  padding: 0,
                }}
              >
                {showPassword ? '🙈 Hide' : '👁️ Show'}
              </button>
            </div>
            <div style={{ position: 'relative' }}>
              <input
                id="login-password"
                type={showPassword ? 'text' : 'password'}
                className="form-input"
                placeholder="Enter your strong password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isSubmitting}
                autoComplete="current-password"
                required
              />
            </div>
          </div>

          {/* Live Strong Password Strength Meter */}
          <PasswordStrengthMeter password={password} showRules={true} />

          {/* Sign In Button */}
          <button
            type="submit"
            className="login-btn"
            disabled={isSubmitting}
            style={{
              marginTop: '12px',
              marginBottom: '14px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
            }}
          >
            {isSubmitting ? (
              <>
                <div className="spinner" style={{ width: '14px', height: '14px', borderWidth: '2px' }} />
                <span>Verifying Credentials…</span>
              </>
            ) : (
              <span>Sign In with Validated Credentials</span>
            )}
          </button>
        </form>

        {/* Divider */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            margin: '12px 0 16px',
            color: 'var(--text-muted)',
            fontSize: '12px',
          }}
        >
          <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
          <span>or</span>
          <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
        </div>

        {/* Keycloak SSO Button */}
        <button
          type="button"
          onClick={() => login()}
          className="btn btn-ghost"
          style={{
            width: '100%',
            padding: '10px',
            borderRadius: 'var(--radius-sm)',
            border: '1px solid var(--border)',
            fontSize: '13px',
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            marginBottom: '16px',
          }}
        >
          <span>🔑</span>
          <span>Sign In with Keycloak SSO</span>
        </button>

        <p className="login-footer">
          Client Management System · Enterprise OAuth2 &amp; RBAC Protected
        </p>
      </div>
    </div>
  );
};

function App() {
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <ErrorBoundary>
                      <Suspense fallback={<div className="auth-loading"><div className="spinner" /></div>}>
                        <AppShell />
                      </Suspense>
                    </ErrorBoundary>
                  </ProtectedRoute>
                }
              >
                <Route index element={<DashboardPage />} />
                <Route path="clients" element={<ClientsPage />} />
                <Route path="clients/:id" element={<ClientDetailPage />} />
                <Route path="accounts" element={<LazyAccountsPage />} />
                <Route path="billing" element={<BillingPage />} />
                <Route path="support" element={<SupportTicketsPage />} />
                <Route path="support/:id" element={<SupportTicketDetailPage />} />
                <Route path="users" element={<LazyUsersPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}

export default App;
