import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../mocks/server';

vi.mock('../auth/AuthProvider', () => ({
  useAuth: () => ({
    user: { firstName: 'Admin', lastName: 'User' },
    roles: ['admin'],
  }),
}));

import DashboardPage from './DashboardPage';

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter>{children}</MemoryRouter>
    </QueryClientProvider>
  );
};

const url = (path: string) => `http://localhost:8085${path}`;

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders metrics from API', async () => {
    server.use(
      http.get(url('/clients/api/v1/dashboard/metrics'), () => {
        return HttpResponse.json({
          totalClients: 10,
          openTickets: 3,
        });
      }),
      http.get(url('/billing/api/v1/dashboard/metrics'), () => {
        return HttpResponse.json({
          activeSubscriptions: 5,
          outstandingInvoices: 2,
        });
      })
    );

    render(<DashboardPage />, { wrapper });

    await waitFor(() => {
      expect(screen.getByText('10')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
    });
  });

  it('handles API error gracefully', async () => {
    server.use(
      http.get(url('/clients/api/v1/dashboard/metrics'), () => HttpResponse.error()),
      http.get(url('/billing/api/v1/dashboard/metrics'), () => HttpResponse.error())
    );

    render(<DashboardPage />, { wrapper });
    
    // Test that it doesn't crash, instead shows skeleton or fallback
    await waitFor(() => {
      expect(screen.getByText('Total Clients')).toBeInTheDocument();
    });
  });
});
