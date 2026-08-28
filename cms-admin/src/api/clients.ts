import api from './axios';
import type {
  Client,
  ClientSummary,
  CreateClientRequest,
  UpdateClientRequest,
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

  create: (body: CreateClientRequest): Promise<Client> =>
    api.post(BASE, body).then((r) => r.data),

  update: (id: number, body: UpdateClientRequest): Promise<Client> =>
    api.put(`${BASE}/${id}`, body).then((r) => r.data),
};
