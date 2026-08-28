
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './auth/AuthProvider';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { ThemeProvider } from './lib/ThemeProvider';
import { ErrorBoundary } from './components/ErrorBoundary';
import React, { Suspense, useMemo } from 'react';
import AppShell from './components/AppShell';

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
const LazyAccountsPage = React.lazy(() => import('./pages/PlaceholderPages').then(module => ({ default: module.AccountsPage })));
const LazyUsersPage = React.lazy(() => import('./pages/PlaceholderPages').then(module => ({ default: module.UsersPage })));


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
      throwOnError: (error: any) => {
        const status = error?.response?.status;
        return status === 429 || status >= 500;
      }
    }
  },
});

const LoginPage = () => {
  const { login, isAuthenticated, isInitialized } = useAuth();

  if (!isInitialized) {
    return (
      <div className="auth-loading">
        <div className="spinner" />
        <p>Initializing…</p>
      </div>
    );
  }

  if (isAuthenticated) return <Navigate to="/" replace />;

  return (
    <div className="login-page">
      <FloatingEmojis />
      <div className="login-card">
        <div className="login-logo">
          <div className="logo-icon login-logo-icon">C</div>
        </div>
        <h1 className="login-title">CMS Admin Portal</h1>
        <p className="login-subtitle">
          Sign in with your organizational account to continue.
        </p>
        <button onClick={login} className="login-btn">
          Sign in with Keycloak
        </button>
        <p className="login-footer">
          Client Management System · Internal use only
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
