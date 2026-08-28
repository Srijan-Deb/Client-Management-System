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
