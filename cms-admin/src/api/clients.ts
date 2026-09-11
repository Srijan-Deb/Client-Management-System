import api from './axios';
import type {
  Client,
  ClientSummary,
  CreateClientRequest,
  UpdateClientRequest,
  ContactRequest,
  ContactResponse,
  AddressRequest,
  AddressResponse,
  Page,
} from '../types/client';

// Gateway rewrites /clients/** → client-service; service path is /api/v1/clients
const BASE = '/clients/api/v1/clients';

export const clientApi = {
  list: (search?: string, page = 0, size = 20): Promise<Page<ClientSummary>> =>
    api
      .get(BASE, { params: { search: search || undefined, page, size, sort: 'createdAt,desc' } })
      .then((r) => r.data),

  get: (id: number): Promise<Client> =>
    api.get(`${BASE}/${id}`).then((r) => r.data),

  getMe: (): Promise<Client> =>
    api.get(`${BASE}/me`).then((r) => r.data),

  create: (body: CreateClientRequest): Promise<Client> =>
    api.post(BASE, body).then((r) => r.data),

  update: (id: number, body: UpdateClientRequest): Promise<Client> =>
    api.put(`${BASE}/${id}`, body).then((r) => r.data),

  addContact: (clientId: number, body: ContactRequest): Promise<ContactResponse> =>
    api.post(`${BASE}/${clientId}/contacts`, body).then((r) => r.data),

  deleteContact: (clientId: number, contactId: number): Promise<void> =>
    api.delete(`${BASE}/${clientId}/contacts/${contactId}`).then((r) => r.data),

  addAddress: (clientId: number, body: AddressRequest): Promise<AddressResponse> =>
    api.post(`${BASE}/${clientId}/addresses`, body).then((r) => r.data),

  deleteAddress: (clientId: number, addressId: number): Promise<void> =>
    api.delete(`${BASE}/${clientId}/addresses/${addressId}`).then((r) => r.data),
};
