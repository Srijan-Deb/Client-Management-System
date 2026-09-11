import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { billingApi } from '../api/billing';
import type { ContractRequest, SubscriptionRequest, PaymentRequest } from '../types/billing';

// ─── Products (cached — rarely changes) ────────────────────────────────────
export const useProducts = () =>
  useQuery({
    queryKey: ['billing', 'products'],
    queryFn: billingApi.listProducts,
    staleTime: 5 * 60 * 1000, // 5 min — products list is @Cacheable on backend
  });

// ─── Invoices ───────────────────────────────────────────────────────────────
export const useInvoices = (clientId?: number, options?: { enabled?: boolean }) =>
  useQuery({
    queryKey: ['billing', 'invoices', clientId],
    queryFn: () => billingApi.listInvoices(clientId),
    enabled: options?.enabled ?? true,
    throwOnError: false, // Handle inline — don't crash the ErrorBoundary
  });


// ─── Contracts ──────────────────────────────────────────────────────────────
export const useContracts = (clientId?: number) =>
  useQuery({
    queryKey: ['billing', 'contracts', clientId],
    queryFn: () => billingApi.listContracts(clientId),
    enabled: true,
    throwOnError: false,
  });

export const useCreateContract = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: ContractRequest) => billingApi.createContract(body),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['billing', 'invoices'] });
      qc.invalidateQueries({ queryKey: ['billing', 'contracts', variables.clientId] });
    },
  });
};

// ─── Subscriptions ──────────────────────────────────────────────────────────
export const useCreateSubscription = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: SubscriptionRequest) => billingApi.createSubscription(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['billing'] });
    },
  });
};

// ─── Payments ───────────────────────────────────────────────────────────────
export const useProcessPayment = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: PaymentRequest) => billingApi.processPayment(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['billing', 'invoices'] });
    },
  });
};
