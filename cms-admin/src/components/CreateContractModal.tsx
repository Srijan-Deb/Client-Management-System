import React, { useState } from 'react';
import { useProducts, useCreateContract } from '../hooks/useBilling';
import type { ContractResponse } from '../types/billing';

interface Props {
  clientId: number;
  accountId: number;
  clientEmail: string;
  onClose: () => void;
  onSuccess?: (result: ContractResponse) => void;
}

export default function CreateContractModal({ clientId, accountId, clientEmail, onClose, onSuccess }: Props) {
  const { data: products = [], isLoading: productsLoading } = useProducts();
  const createContractMutation = useCreateContract();

  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [recipientEmail, setRecipientEmail] = useState(clientEmail);
  const [errorMsg, setErrorMsg] = useState('');
  const [successResult, setSuccessResult] = useState<ContractResponse | null>(null);

  const toggle = (id: number) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const subtotal = products
    .filter(p => selectedIds.has(p.id))
    .reduce((sum, p) => sum + Number(p.price), 0);
  const tax = subtotal * 0.18;
  const total = subtotal + tax;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    if (selectedIds.size === 0) {
      setErrorMsg('Please select at least one product.');
      return;
    }
    if (!recipientEmail) {
      setErrorMsg('Recipient email is required.');
      return;
    }

    createContractMutation.mutate(
      {
        clientId,
        accountId,
        recipientEmail,
        productIds: Array.from(selectedIds),
      },
      {
        onSuccess: (result) => {
          setSuccessResult(result);
          onSuccess?.(result);
        },
        onError: (err: any) => {
          setErrorMsg(
            err?.response?.data?.message || err?.message || 'Failed to create contract.'
          );
        },
      }
    );
  };

  const currency = products[0]?.currency ?? 'INR';

  return (
    <div className="modal-backdrop">
      <div className="modal-content" style={{ maxWidth: '560px' }}>
        <div className="modal-header">
          <h3>📄 New Contract</h3>
          <button className="btn btn-ghost" onClick={onClose} disabled={createContractMutation.isPending}>
            ✕
          </button>
        </div>

        {/* ── Success screen ── */}
        {successResult && (
          <div style={{ padding: '8px 0' }}>
            <div style={{
              background: 'var(--status-active-bg, #d1fae5)',
              color: 'var(--status-active, #059669)',
              padding: '16px',
              borderRadius: '8px',
              marginBottom: '20px',
            }}>
              <div style={{ fontWeight: 700, fontSize: '15px', marginBottom: '6px' }}>
                ✅ Contract created successfully!
              </div>
              <p style={{ fontSize: '13px', margin: 0 }}>
                Invoice <strong>{successResult.invoiceNumber}</strong> has been generated (total:{' '}
                <strong>{currency} {Number(successResult.totalValue).toFixed(2)}</strong>) and emailed to{' '}
                <strong>{recipientEmail}</strong>.
              </p>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button className="btn btn-primary" onClick={onClose}>
                Done
              </button>
            </div>
          </div>
        )}

        {/* ── Form ── */}
        {!successResult && (
          <form onSubmit={handleSubmit}>
            {errorMsg && (
              <div style={{
                background: 'var(--status-suspended-bg, #fee2e2)',
                color: 'var(--status-suspended, #dc2626)',
                padding: '12px',
                borderRadius: '6px',
                marginBottom: '16px',
                fontSize: '13px',
              }}>
                {errorMsg}
              </div>
            )}

            {/* Product selection */}
            <div style={{ marginBottom: '20px' }}>
              <label className="form-label" style={{ marginBottom: '10px', display: 'block' }}>
                Select Products <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(one or more)</span>
              </label>

              {productsLoading && (
                <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Loading products…</p>
              )}

              {!productsLoading && products.length === 0 && (
                <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                  No products available. Add products in the billing service database.
                </p>
              )}

              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '240px', overflowY: 'auto' }}>
                {products.map(p => (
                  <label
                    key={p.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      padding: '12px 14px',
                      borderRadius: '8px',
                      border: `1px solid ${selectedIds.has(p.id) ? 'var(--primary)' : 'var(--border)'}`,
                      background: selectedIds.has(p.id) ? 'var(--primary-muted, rgba(99,102,241,0.08))' : 'var(--surface)',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={selectedIds.has(p.id)}
                      onChange={() => toggle(p.id)}
                      disabled={createContractMutation.isPending}
                      style={{ width: '16px', height: '16px', accentColor: 'var(--primary)', flexShrink: 0 }}
                    />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 600, fontSize: '14px', color: 'var(--text-primary)' }}>
                        {p.name}
                      </div>
                      {p.description && (
                        <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {p.description}
                        </div>
                      )}
                    </div>
                    <div style={{ textAlign: 'right', flexShrink: 0 }}>
                      <div style={{ fontWeight: 700, fontSize: '14px', color: 'var(--text-primary)' }}>
                        {p.currency} {Number(p.price).toFixed(2)}
                      </div>
                      <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                        / {p.billingCycle.toLowerCase()}
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            {/* Price summary */}
            {selectedIds.size > 0 && (
              <div style={{
                background: 'var(--surface-alt, rgba(0,0,0,0.04))',
                borderRadius: '8px',
                padding: '14px 16px',
                marginBottom: '20px',
                fontSize: '13px',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px', color: 'var(--text-secondary)' }}>
                  <span>Subtotal</span><span>{currency} {subtotal.toFixed(2)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', color: 'var(--text-secondary)' }}>
                  <span>Tax (18% GST)</span><span>{currency} {tax.toFixed(2)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 700, color: 'var(--text-primary)', borderTop: '1px solid var(--border)', paddingTop: '8px' }}>
                  <span>Total Due</span><span>{currency} {total.toFixed(2)}</span>
                </div>
              </div>
            )}

            {/* Recipient email */}
            <div style={{ marginBottom: '24px' }}>
              <label className="form-label">Invoice Recipient Email</label>
              <input
                type="email"
                className="form-input"
                value={recipientEmail}
                onChange={e => setRecipientEmail(e.target.value)}
                disabled={createContractMutation.isPending}
                placeholder="billing@client.com"
              />
              <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: '4px 0 0 0' }}>
                The invoice PDF will be automatically emailed to this address.
              </p>
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={onClose}
                disabled={createContractMutation.isPending}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={createContractMutation.isPending || selectedIds.size === 0}
              >
                {createContractMutation.isPending ? '⏳ Creating…' : '✅ Create Contract & Generate Invoice'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
