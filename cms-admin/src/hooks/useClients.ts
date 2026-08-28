import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { clientApi } from '../api/clients';
import type { CreateClientRequest, UpdateClientRequest } from '../types/client';

export const useClients = (search?: string, page = 0, size = 20) =>
  useQuery({
    queryKey: ['clients', search, page, size],
    queryFn: () => clientApi.list(search, page, size),
  });

export const useClient = (id: number) =>
  useQuery({
    queryKey: ['clients', id],
    queryFn: () => clientApi.get(id),
    enabled: !!id,
  });

export const useCreateClient = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateClientRequest) => clientApi.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clients'] }),
  });
};

export const useUpdateClient = (id: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateClientRequest) => clientApi.update(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['clients'] });
      qc.invalidateQueries({ queryKey: ['clients', id] });
    },
  });
};
