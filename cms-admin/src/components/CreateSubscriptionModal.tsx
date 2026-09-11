import React, { useState } from 'react';
import { useProducts, useCreateSubscription } from '../hooks/useBilling';

interface CreateSubscriptionModalProps {
  clientId: number;
  clientEmail: string;
  onClose: () => void;
}

export default function CreateSubscriptionModal({ clientId, clientEmail, onClose }: CreateSubscriptionModalProps) {
  const { data: products = [], isLoading: productsLoading } = useProducts();
  const createSubMutation = useCreateSubscription();

  const [productId, setProductId] = useState<number | ''>('');
  const [recipientEmail, setRecipientEmail] = useState(clientEmail);
  const [errorMsg, setErrorMsg] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!productId) {
      setErrorMsg('Please select a plan.');
      return;
    }
    if (!recipientEmail) {
      setErrorMsg('Recipient email is required.');
      return;
    }

    createSubMutation.mutate(
      { clientId, productId: Number(productId), recipientEmail },
      {
        onSuccess: () => {
          onClose();
        },
        onError: (err: any) => {
          setErrorMsg(err?.response?.data?.message || 'Failed to create subscription.');
        },
      }
    );
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-content" style={{ maxWidth: '500px' }}>
        <div className="modal-header">
          <h3>Create Subscription</h3>
          <button className="btn btn-ghost" onClick={onClose} disabled={createSubMutation.isPending}>
            ✕
          </button>
        </div>
        
        {errorMsg && (
          <div style={{ background: 'var(--status-suspended-bg)', color: 'var(--status-suspended)', padding: '12px', borderRadius: '6px', marginBottom: '16px', fontSize: '13px' }}>
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="form-row">
          <div style={{ gridColumn: '1 / -1' }}>
            <label className="form-label">Select Plan</label>
            {productsLoading ? (
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Loading plans...</p>
            ) : (
              <select 
                className="form-input" 
                value={productId} 
                onChange={e => setProductId(e.target.value === '' ? '' : Number(e.target.value))}
                disabled={createSubMutation.isPending}
              >
                <option value="">-- Choose a plan --</option>
                {products.map(p => (
                  <option key={p.id} value={p.id}>
                    {p.name} - {p.currency} {p.price} / {p.billingCycle.toLowerCase()}
                  </option>
                ))}
              </select>
            )}
          </div>

          <div style={{ gridColumn: '1 / -1' }}>
            <label className="form-label">Recipient Email</label>
            <input 
              type="email" 
              className="form-input" 
              value={recipientEmail}
              onChange={e => setRecipientEmail(e.target.value)}
              disabled={createSubMutation.isPending}
              placeholder="billing@client.com"
            />
            <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: '4px 0 0 0' }}>
              Invoices will be sent to this email address.
            </p>
          </div>

          <div style={{ gridColumn: '1 / -1', display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
            <button 
              type="button" 
              className="btn btn-ghost" 
              onClick={onClose}
              disabled={createSubMutation.isPending}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={createSubMutation.isPending || !productId}
            >
              {createSubMutation.isPending ? 'Subscribing...' : 'Start Subscription'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
