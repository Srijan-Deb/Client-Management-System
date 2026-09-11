import { useQuery } from '@tanstack/react-query';
import { 
  getClientMetrics, 
  getBillingMetrics,
  getClientGrowth,
  getTicketStatus,
  getTicketPriority,
  getInvoiceStatus
} from '../api/dashboard';

export const useClientMetrics = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: ['dashboard', 'client-metrics'],
    queryFn: getClientMetrics,
    staleTime: 60_000,
    enabled: options?.enabled ?? true,
  });
};

export const useBillingMetrics = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: ['dashboard', 'billing-metrics'],
    queryFn: getBillingMetrics,
    staleTime: 60_000,
    enabled: options?.enabled ?? true,
  });
};

export const useClientGrowth = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: ['analytics', 'client-growth'],
    queryFn: getClientGrowth,
    staleTime: 60_000,
    enabled: options?.enabled ?? true,
  });
};

export const useTicketStatus = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: ['analytics', 'ticket-status'],
    queryFn: getTicketStatus,
    staleTime: 60_000,
    enabled: options?.enabled ?? true,
  });
};

export const useTicketPriority = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: ['analytics', 'ticket-priority'],
    queryFn: getTicketPriority,
    staleTime: 60_000,
    enabled: options?.enabled ?? true,
  });
};

export const useInvoiceStatus = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: ['analytics', 'invoice-status'],
    queryFn: getInvoiceStatus,
    staleTime: 60_000,
    enabled: options?.enabled ?? true,
  });
};
