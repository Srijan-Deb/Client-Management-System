import api from './axios';
import type {
  Product,
  ContractRequest,
  ContractResponse,
  SubscriptionRequest,
  Invoice,
  PaymentRequest,
  PaymentResponse,
} from '../types/billing';

const BASE = '/billing/api/v1/billing';

export const billingApi = {
  // ─── Products ────────────────────────────────────────────────────────────
  listProducts: (): Promise<Product[]> =>
    api.get(`${BASE}/products`).then((r) => r.data),

  // ─── Contracts ───────────────────────────────────────────────────────────
  createContract: (body: ContractRequest): Promise<ContractResponse> =>
    api.post(`${BASE}/contracts`, body).then((r) => r.data),

  // ─── Subscriptions ───────────────────────────────────────────────────────
  createSubscription: (body: SubscriptionRequest) =>
    api.post(`${BASE}/subscriptions`, body).then((r) => r.data),

  // ─── Invoices ────────────────────────────────────────────────────────────
  // Backend doesn't yet expose a list endpoint — we'll add one below.
  // When it does, the hook will just work.
  listInvoices: (clientId?: number): Promise<Invoice[]> =>
    api
      .get(`${BASE}/invoices`, { params: clientId ? { clientId } : undefined })
      .then((r) => r.data),

  // ─── PDF Download URL ─────────────────────────────────────────────────────
  // Calls the backend to exchange the raw S3 object key for a pre-signed URL.
  getPdfDownloadUrl: (objectKey: string): Promise<string> =>
    api
      .get(`${BASE}/invoices/pdf-url`, { params: { objectKey } })
      .then((r) => r.data?.url ?? r.data),

  // ─── Payments ────────────────────────────────────────────────────────────
  processPayment: (body: PaymentRequest): Promise<PaymentResponse> =>
    api.post(`${BASE}/payments`, body).then((r) => r.data),
};
