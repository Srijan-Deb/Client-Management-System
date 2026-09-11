import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { clientApi } from '../api/clients';
import type { CreateClientRequest, UpdateClientRequest, ContactRequest, AddressRequest } from '../types/client';

export const useClients = (search?: string, page = 0, size = 20, options?: { enabled?: boolean }) =>
  useQuery({
    queryKey: ['clients', search, page, size],
    queryFn: () => clientApi.list(search, page, size),
    enabled: options?.enabled ?? true,
  });

export const useMyClient = (options?: { enabled?: boolean }) =>
  useQuery({
    queryKey: ['clients', 'me'],
    queryFn: () => clientApi.getMe(),
    enabled: options?.enabled ?? true,
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

export const useAddContact = (clientId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: ContactRequest) => clientApi.addContact(clientId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clients', clientId] }),
  });
};

export const useDeleteContact = (clientId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (contactId: number) => clientApi.deleteContact(clientId, contactId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clients', clientId] }),
  });
};

export const useAddAddress = (clientId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: AddressRequest) => clientApi.addAddress(clientId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clients', clientId] }),
  });
};

export const useDeleteAddress = (clientId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (addressId: number) => clientApi.deleteAddress(clientId, addressId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clients', clientId] }),
  });
};
