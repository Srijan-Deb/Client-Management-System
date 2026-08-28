import { useState, useRef, useEffect } from 'react';
import { useProcessPayment } from '../hooks/useBilling';
import type { Invoice, PaymentResponse } from '../types/billing';

// Generate a UUID v4 client-side (no dependency needed)
function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

interface PaymentModalProps {
  invoice: Invoice;
  clientEmail: string;
  onClose: () => void;
}

const PaymentModal = ({ invoice, clientEmail, onClose }: PaymentModalProps) => {
  const mutation = useProcessPayment();

  // UUID is generated once when the modal mounts — reused on retry
  // so that if the first request succeeded but timed out, the backend
  // deduplicates it and does NOT double-charge.
  const idempotencyKey = useRef(generateUUID());

  const [token, setToken] = useState('tok_visa'); // Stripe test token
  const [result, setResult] = useState<PaymentResponse | null>(null);
  const tokenInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    tokenInputRef.current?.focus();
  }, []);

  const [submitted, setSubmitted] = useState(false);

  const handlePay = async () => {
    if (submitted && result?.status === 'PAID') return; // guard
    setSubmitted(true);
    try {
      const res = await mutation.mutateAsync({
        invoiceId: invoice.id,
        amount: invoice.totalAmount,
        recipientEmail: clientEmail,
        paymentMethodToken: token,
        idempotencyKey: idempotencyKey.current,
      });
      setResult(res);
    } catch {
      setSubmitted(false); // Allow retry on network error
    }
  };

  const handleRetry = () => {
    // Retry uses the SAME idempotency key — backend deduplicates
    setResult(null);
    setSubmitted(false);
    mutation.reset();
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px' }}>
        <div className="modal-header">
          <h2 className="modal-title">Process Payment</h2>
          <button className="modal-close" onClick={onClose} aria-label="Close modal">✕</button>
        </div>

        <div className="modal-form">
          {/* Invoice Summary */}
          <div style={{
            background: 'var(--bg-elevated)',
            borderRadius: 'var(--radius)',
            padding: '16px',
            marginBottom: '20px',
            border: '1px solid var(--border)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>Invoice</span>
              <span style={{ fontWeight: 600 }}>{invoice.invoiceNumber}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>Subtotal</span>
              <span>{invoice.currency} {invoice.subtotal.toFixed(2)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>Tax ({(invoice.taxRate * 100).toFixed(0)}%)</span>
              <span>{invoice.currency} {invoice.taxAmount.toFixed(2)}</span>
            </div>
            <div style={{
              display: 'flex', justifyContent: 'space-between',
              paddingTop: '8px', borderTop: '1px solid var(--border)',
              fontWeight: 700, fontSize: '16px'
            }}>
              <span>Total</span>
              <span style={{ color: 'var(--primary)' }}>{invoice.currency} {invoice.totalAmount.toFixed(2)}</span>
            </div>
          </div>

          {/* Success State */}
          {result?.status === 'PAID' && (
            <div style={{
              textAlign: 'center', padding: '24px',
              background: 'rgba(16,185,129,0.08)',
              borderRadius: 'var(--radius)',
              border: '1px solid rgba(16,185,129,0.3)'
            }}>
              <div style={{ fontSize: '40px', marginBottom: '12px' }}>✅</div>
              <p style={{ fontWeight: 600, color: '#10b981', marginBottom: '4px' }}>Payment Successful</p>
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                Stripe ID: {result.stripePaymentId ?? 'N/A'}
              </p>
            </div>
          )}

          {/* Failed State */}
          {result?.status === 'FAILED' && (
            <div style={{
              textAlign: 'center', padding: '24px',
              background: 'rgba(239,68,68,0.08)',
              borderRadius: 'var(--radius)',
              border: '1px solid rgba(239,68,68,0.3)'
            }}>
              <div style={{ fontSize: '40px', marginBottom: '12px' }}>❌</div>
              <p style={{ fontWeight: 600, color: '#ef4444', marginBottom: '8px' }}>Payment Failed</p>
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
                Your payment could not be processed. Please check your card details and try again.
              </p>
              <button className="btn btn-primary" onClick={handleRetry}>
                🔄 Retry Payment
              </button>
            </div>
          )}

          {/* Input & Submit State */}
          {!result && (
            <>
              <div className="form-group">
                <label className="form-label">Payment Method Token</label>
                <input
                  ref={tokenInputRef}
                  className="form-input"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  placeholder="tok_visa"
                  disabled={submitted}
                />
                <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Use Stripe test tokens: tok_visa, tok_mastercard, tok_chargeDeclined
                </p>
              </div>

              <div className="form-group">
                <label className="form-label">Recipient Email</label>
                <input className="form-input" value={clientEmail} readOnly style={{ opacity: 0.7 }} />
              </div>

              <div style={{
                fontSize: '12px', color: 'var(--text-muted)',
                background: 'var(--bg-elevated)',
                padding: '8px 12px', borderRadius: 'var(--radius-sm)',
                marginBottom: '8px'
              }}>
                🔑 Idempotency Key: <code style={{ fontSize: '11px' }}>{idempotencyKey.current}</code>
              </div>

              {mutation.isError && (
                <p className="form-error" style={{ marginBottom: '12px' }}>
                  {(mutation.error as any)?.response?.data?.message ?? 'Network error — please retry.'}
                </p>
              )}

              <div className="modal-actions">
                <button className="btn btn-ghost" onClick={onClose} disabled={mutation.isPending}>
                  Cancel
                </button>
                <button
                  className="btn btn-primary"
                  onClick={handlePay}
                  disabled={mutation.isPending || submitted}
                >
                  {mutation.isPending ? 'Processing…' : `Pay ${invoice.currency} ${invoice.totalAmount.toFixed(2)}`}
                </button>
              </div>
            </>
          )}

          {result?.status === 'PAID' && (
            <div className="modal-actions">
              <button className="btn btn-primary" onClick={onClose}>Close</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default PaymentModal;
