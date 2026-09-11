import api from './axios';

export interface ClientMetrics {
  totalClients: number;
  openTickets: number;
}

export interface BillingMetrics {
  activeSubscriptions: number;
  outstandingInvoices: number;
}

export const getClientMetrics = async (): Promise<ClientMetrics> => {
  const response = await api.get('/clients/api/v1/dashboard/metrics');
  return response.data;
};

export const getBillingMetrics = async (): Promise<BillingMetrics> => {
  const response = await api.get('/billing/api/v1/dashboard/metrics');
  return response.data;
};

export const getClientGrowth = async (): Promise<any[]> => {
  const response = await api.get('/analytics/api/v1/analytics/client-growth');
  return response.data;
};

export const getTicketStatus = async (): Promise<any[]> => {
  const response = await api.get('/analytics/api/v1/analytics/ticket-status');
  return response.data;
};

export const getTicketPriority = async (): Promise<any[]> => {
  const response = await api.get('/analytics/api/v1/analytics/ticket-priority');
  return response.data;
};

export const getInvoiceStatus = async (): Promise<any[]> => {
  const response = await api.get('/analytics/api/v1/analytics/invoice-status');
  return response.data;
};
