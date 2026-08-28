import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

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

vi.mock('../hooks/useClients', () => ({
  useClient: vi.fn(),
}));

vi.mock('../hooks/useBilling', () => ({
  useInvoices: vi.fn(),
}));

vi.mock('../api/axios', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: [] }),
  },
}));

import ClientDetailPage from './ClientDetailPage';
import { useClient } from '../hooks/useClients';
import { useInvoices } from '../hooks/useBilling';

const wrapper = (clientId = '1') =>
  ({ children }: { children: React.ReactNode }) => {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return (
      <QueryClientProvider client={qc}>
        <MemoryRouter initialEntries={[`/clients/${clientId}`]}>
          <Routes>
            <Route path="/clients/:id" element={children} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

const mockClient = {
  clientId: 1,
  firstName: 'John',
  lastName: 'Smith',
  email: 'john@example.com',
  phone: '+91 9876543210',
  companyName: 'Smith Ltd',
  tier: 'PLATINUM' as const,
  status: 'ACTIVE' as const,
  createdAt: '2024-01-01T00:00:00Z',
  contacts: [],
  addresses: [],
};

describe('ClientDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (useInvoices as any).mockReturnValue({ data: [] });
  });

  it('renders loading skeleton while fetching', () => {
    (useClient as any).mockReturnValue({ data: undefined, isLoading: true, isError: false });
    render(<ClientDetailPage />, { wrapper: wrapper() });
    // Skeleton renders — no client name visible
    expect(screen.queryByText('John Smith')).toBeNull();
  });

  it('renders client name and email in header', () => {
    (useClient as any).mockReturnValue({ data: mockClient, isLoading: false, isError: false });
    render(<ClientDetailPage />, { wrapper: wrapper() });
    expect(screen.getByText('John Smith')).toBeInTheDocument();
    expect(screen.getAllByText(/john@example.com/)[0]).toBeInTheDocument();
  });

  it('shows profile tab content by default', () => {
    (useClient as any).mockReturnValue({ data: mockClient, isLoading: false, isError: false });
    render(<ClientDetailPage />, { wrapper: wrapper() });
    expect(screen.getByText('Client Profile')).toBeInTheDocument();
    expect(screen.getByText('PLATINUM')).toBeInTheDocument();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
  });

  it('shows error panel on fetch failure', () => {
    (useClient as any).mockReturnValue({ data: undefined, isLoading: false, isError: true });
    render(<ClientDetailPage />, { wrapper: wrapper() });
    expect(screen.getByText(/failed to load client details/i)).toBeInTheDocument();
  });
});
