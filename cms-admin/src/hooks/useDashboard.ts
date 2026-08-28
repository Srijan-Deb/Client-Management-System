import { useQuery } from '@tanstack/react-query';
import { getClientMetrics, getBillingMetrics } from '../api/dashboard';

export const useClientMetrics = () => {
  return useQuery({
    queryKey: ['dashboard', 'client-metrics'],
    queryFn: getClientMetrics,
    staleTime: 60_000, // 1 minute
  });
};

export const useBillingMetrics = () => {
  return useQuery({
    queryKey: ['dashboard', 'billing-metrics'],
    queryFn: getBillingMetrics,
    staleTime: 60_000, // 1 minute
  });
};
