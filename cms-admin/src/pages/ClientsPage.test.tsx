import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../mocks/server';

// ─── Mock keycloak so tests don't need a live Keycloak server ────────────────
vi.mock('../lib/keycloak', () => ({
  default: {
    token: 'test-token',
    realmAccess: { roles: ['admin'] },
    init: vi.fn().mockResolvedValue(true),
    login: vi.fn(),
    logout: vi.fn(),
    updateToken: vi.fn().mockResolvedValue(true),
    loadUserProfile: vi.fn().mockResolvedValue({ firstName: 'Test', lastName: 'Admin' }),
    onTokenExpired: undefined,
  },
}));

// ─── Mock RoleGate ────────────────────────────────────────────────────────────
vi.mock('../auth/RoleGate', () => ({
  useHasRole: () => true, // admin can always write
}));

import ClientsPage from './ClientsPage';

const url = (path: string) => `http://localhost:8085${path}`;

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter>{children}</MemoryRouter>
    </QueryClientProvider>
  );
};

describe('ClientsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders client rows from API data', async () => {
    server.use(
      http.get(url('/clients/api/v1/clients'), () => {
        return HttpResponse.json({
          content: [
            {
              clientId: 1,
              firstName: 'Jane',
              lastName: 'Doe',
              email: 'jane@example.com',
              companyName: 'Acme Corp',
              tier: 'GOLD',
              status: 'ACTIVE',
              createdAt: '2024-01-15T10:00:00Z',
            },
          ],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 20,
        });
      })
    );

    render(<ClientsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.getByText('Jane Doe')).toBeInTheDocument();
      expect(screen.getByText('jane@example.com')).toBeInTheDocument();
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    });
  });

  it('shows error banner on API failure', async () => {
    server.use(
      http.get(url('/clients/api/v1/clients'), () => {
        return HttpResponse.error();
      })
    );

    render(<ClientsPage />, { wrapper });
    
    await waitFor(() => {
      expect(screen.getByText(/failed to load clients/i)).toBeInTheDocument();
    });
  });

  it('opens the New Client modal and validates input', async () => {
    server.use(
      http.get(url('/clients/api/v1/clients'), () => {
        return HttpResponse.json({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
      })
    );

    render(<ClientsPage />, { wrapper });
    await waitFor(() => expect(screen.getByText(/no clients found/i)).toBeInTheDocument());

    await userEvent.click(screen.getAllByRole('button', { name: /new client/i })[0]);
    expect(screen.getByRole('heading', { name: /new client/i })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /create client/i }));
    await waitFor(() => {
      expect(screen.getByText(/first name is required/i)).toBeInTheDocument();
      expect(screen.getByText(/last name is required/i)).toBeInTheDocument();
    });
  });
});

