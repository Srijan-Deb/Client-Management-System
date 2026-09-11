import React, { useState } from 'react';
import { useAddAddress } from '../hooks/useClients';
import type { AddressRequest, AddressType } from '../types/client';

interface AddAddressModalProps {
  clientId: number;
  onClose: () => void;
}

const ADDRESS_TYPES: AddressType[] = ['BILLING', 'SHIPPING', 'BRANCH', 'REGISTERED'];

export default function AddAddressModal({ clientId, onClose }: AddAddressModalProps) {
  const addAddressMutation = useAddAddress(clientId);

  const [addressType, setAddressType] = useState<AddressType>('BILLING');
  const [line1, setLine1] = useState('');
  const [line2, setLine2] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [postalCode, setPostalCode] = useState('');
  const [country, setCountry] = useState('');
  const [isPrimary, setIsPrimary] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!line1.trim() || !city.trim() || !country.trim() || !postalCode.trim()) {
      setErrorMsg('Street line 1, City, Country, and Postal Code are required.');
      return;
    }

    const payload: AddressRequest = {
      addressType,
      line1: line1.trim(),
      line2: line2.trim() || undefined,
      city: city.trim(),
      state: state.trim() || undefined,
      country: country.trim(),
      postalCode: postalCode.trim(),
      primary: isPrimary,
    };

    addAddressMutation.mutate(payload, {
      onSuccess: () => {
        onClose();
      },
      onError: (err: any) => {
        const msg = err?.response?.data?.message || err?.response?.data?.errors?.[0]?.defaultMessage || 'Failed to add address.';
        setErrorMsg(msg);
      },
    });
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
        <div className="modal-header">
          <h3>Add Address</h3>
          <button className="btn btn-ghost" onClick={onClose} disabled={addAddressMutation.isPending}>
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
            <label className="form-label">Address Type *</label>
            <select
              className="form-input"
              value={addressType}
              onChange={(e) => setAddressType(e.target.value as AddressType)}
              disabled={addAddressMutation.isPending}
            >
              {ADDRESS_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>

          <div style={{ gridColumn: '1 / -1' }}>
            <label className="form-label">Street Line 1 *</label>
            <input 
              type="text" 
              className="form-input" 
              value={line1}
              onChange={(e) => setLine1(e.target.value)}
              disabled={addAddressMutation.isPending}
              placeholder="123 Main St"
              required
            />
          </div>

          <div style={{ gridColumn: '1 / -1' }}>
            <label className="form-label">Street Line 2 (Optional)</label>
            <input 
              type="text" 
              className="form-input" 
              value={line2}
              onChange={(e) => setLine2(e.target.value)}
              disabled={addAddressMutation.isPending}
              placeholder="Suite 400 / Apt 2B"
            />
          </div>

          <div>
            <label className="form-label">City *</label>
            <input 
              type="text" 
              className="form-input" 
              value={city}
              onChange={(e) => setCity(e.target.value)}
              disabled={addAddressMutation.isPending}
              placeholder="San Francisco"
              required
            />
          </div>

          <div>
            <label className="form-label">State / Province</label>
            <input 
              type="text" 
              className="form-input" 
              value={state}
              onChange={(e) => setState(e.target.value)}
              disabled={addAddressMutation.isPending}
              placeholder="CA"
            />
          </div>

          <div>
            <label className="form-label">Country *</label>
            <input 
              type="text" 
              className="form-input" 
              value={country}
              onChange={(e) => setCountry(e.target.value)}
              disabled={addAddressMutation.isPending}
              placeholder="USA"
              required
            />
          </div>

          <div>
            <label className="form-label">Postal Code *</label>
            <input 
              type="text" 
              className="form-input" 
              value={postalCode}
              onChange={(e) => setPostalCode(e.target.value)}
              disabled={addAddressMutation.isPending}
              placeholder="94105"
              required
            />
          </div>

          <div style={{ gridColumn: '1 / -1', display: 'flex', alignItems: 'center', gap: '8px', marginTop: '4px' }}>
            <input 
              type="checkbox"
              id="isPrimaryAddress"
              checked={isPrimary}
              onChange={(e) => setIsPrimary(e.target.checked)}
              disabled={addAddressMutation.isPending}
              style={{ width: '16px', height: '16px', cursor: 'pointer' }}
            />
            <label htmlFor="isPrimaryAddress" className="form-label" style={{ margin: 0, cursor: 'pointer' }}>
              Set as primary address
            </label>
          </div>

          <div style={{ gridColumn: '1 / -1', display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
            <button 
              type="button" 
              className="btn btn-ghost" 
              onClick={onClose}
              disabled={addAddressMutation.isPending}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={addAddressMutation.isPending || !line1.trim() || !city.trim() || !country.trim() || !postalCode.trim()}
            >
              {addAddressMutation.isPending ? 'Adding...' : 'Add Address'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
