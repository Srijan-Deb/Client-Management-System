import { useQuery } from '@tanstack/react-query';
import { accountApi } from '../api/accounts';

export const useAccounts = (search?: string, page = 0, size = 20) =>
  useQuery({
    queryKey: ['accounts', search, page, size],
    queryFn: () => accountApi.list(search, page, size),
  });

export const useAccount = (id: number) =>
  useQuery({
    queryKey: ['accounts', id],
    queryFn: () => accountApi.get(id),
    enabled: !!id,
  });
