import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ticketsApi } from '../api/tickets';
import type { TicketRequest, TicketCommentRequest } from '../types/ticket';

// ─── Tickets ────────────────────────────────────────────────────────────────
export const useTickets = (clientId?: number) =>
  useQuery({
    queryKey: ['tickets', clientId],
    queryFn: () => ticketsApi.list(clientId),
    enabled: true,
  });

export const useTicket = (ticketId: number) =>
  useQuery({
    queryKey: ['tickets', 'detail', ticketId],
    queryFn: () => ticketsApi.get(ticketId),
    enabled: !!ticketId,
  });

export const useCreateTicket = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: TicketRequest) => ticketsApi.create(body),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['tickets', data.clientId] });
      qc.invalidateQueries({ queryKey: ['tickets', undefined] }); // global list
    },
  });
};

export const useAssignTicket = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, agentId }: { id: number; agentId: number }) => ticketsApi.assign(id, agentId),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['tickets', 'detail', data.ticketId] });
      qc.invalidateQueries({ queryKey: ['tickets'] });
    },
  });
};

export const useResolveTicket = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => ticketsApi.resolve(id),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['tickets', 'detail', data.ticketId] });
      qc.invalidateQueries({ queryKey: ['tickets'] });
    },
  });
};

export const useReopenTicket = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => ticketsApi.reopen(id),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['tickets', 'detail', data.ticketId] });
      qc.invalidateQueries({ queryKey: ['tickets'] });
    },
  });
};

export const useCloseTicket = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => ticketsApi.close(id),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['tickets', 'detail', data.ticketId] });
      qc.invalidateQueries({ queryKey: ['tickets'] });
    },
  });
};

export const useAddTicketComment = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: TicketCommentRequest }) => ticketsApi.addComment(id, body),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['tickets', 'detail', data.ticketId] });
    },
  });
};
