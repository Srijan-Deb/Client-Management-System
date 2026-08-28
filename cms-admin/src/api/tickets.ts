import api from './axios';
import type { Ticket, TicketRequest, TicketCommentRequest } from '../types/ticket';

// Gateway rewrites /clients/** → client-service; service path is /api/v1/tickets
const BASE = '/clients/api/v1/tickets';

// We need a Page type here just in case, we can redefine it to be safe
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const ticketsApi = {
  list: (clientId?: number, page = 0, size = 20): Promise<PageResponse<Ticket>> =>
    api
      .get(BASE, { params: { clientId: clientId || undefined, page, size, sort: 'createdAt,desc' } })
      .then((r) => r.data),

  get: (id: number): Promise<Ticket> =>
    api.get(`${BASE}/${id}`).then((r) => r.data),

  create: (body: TicketRequest): Promise<Ticket> =>
    api.post(BASE, body).then((r) => r.data),

  assign: (id: number, agentId: number): Promise<Ticket> =>
    api.put(`${BASE}/${id}/assign`, null, { params: { agentId } }).then((r) => r.data),

  resolve: (id: number): Promise<Ticket> =>
    api.put(`${BASE}/${id}/resolve`).then((r) => r.data),

  reopen: (id: number): Promise<Ticket> =>
    api.put(`${BASE}/${id}/reopen`).then((r) => r.data),

  close: (id: number): Promise<Ticket> =>
    api.put(`${BASE}/${id}/close`).then((r) => r.data),

  addComment: (id: number, body: TicketCommentRequest): Promise<Ticket> =>
    api.post(`${BASE}/${id}/comments`, body).then((r) => r.data),
};
