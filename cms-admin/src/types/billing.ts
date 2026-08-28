// ─── Billing domain types mirroring backend DTOs ────────────────────────────

export interface Product {
  id: number;
  name: string;
  description: string | null;
  price: number; // BigDecimal serialised as number by Jackson
  currency: string; // "USD"
  billingCycle: string; // "MONTHLY" | "ANNUAL" | etc.
  category: { id: number; name: string } | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

// ─── ContractRequest → POST /api/v1/billing/contracts ────────────────────────
export interface ContractRequest {
  clientId: number;
  accountId: number;
  recipientEmail: string;
  productIds: number[];
}

// ─── ContractResponse ← POST /api/v1/billing/contracts ───────────────────────
export interface ContractResponse {
  contractId: number;
  invoiceId: number;
  invoiceNumber: string;
  totalValue: number;
  pdfUrl: string | null;
  startDate: string; // ISO LocalDate
}

// ─── SubscriptionRequest → POST /api/v1/billing/subscriptions ────────────────
export interface SubscriptionRequest {
  clientId: number;
  productId: number;
  recipientEmail: string;
}

// ─── Invoice (derived from Invoice entity fields returned as JSON) ────────────
export interface Invoice {
  id: number;
  clientId: number;
  accountId: number;
  invoiceNumber: string;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  totalAmount: number;
  currency: string;
  dueDate: string; // ISO LocalDate
  status: InvoiceStatus;
  pdfObjectKey: string | null;
  createdAt: string;
  updatedAt: string;
}

export type InvoiceStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED';

// ─── PaymentRequest → POST /api/v1/billing/payments ─────────────────────────
export interface PaymentRequest {
  invoiceId: number;
  amount: number;
  recipientEmail: string;
  paymentMethodToken: string; // Stripe test token e.g. "tok_visa"
  idempotencyKey: string; // UUID generated client-side on modal open
}

// ─── PaymentResponse ← POST /api/v1/billing/payments ────────────────────────
export interface PaymentResponse {
  paymentId: number;
  status: 'PAID' | 'FAILED' | 'PENDING';
  stripePaymentId: string | null;
}
