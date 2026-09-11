import React, { useState } from 'react';
import { useUpdateClient } from '../hooks/useClients';
import { PhoneInputWithCountry } from './PhoneInputWithCountry';
import type { Client, UpdateClientRequest } from '../types/client';

interface EditClientModalProps {
  client: Client;
  onClose: () => void;
}

export default function EditClientModal({ client, onClose }: EditClientModalProps) {
  const updateClientMutation = useUpdateClient(client.clientId);

  const [formData, setFormData] = useState<UpdateClientRequest>({
    firstName: client.firstName,
    lastName: client.lastName,
    email: client.email,
    phone: client.phone || '',
    companyName: client.companyName || '',
    tier: client.tier,
    status: client.status,
  });
  const [phoneError, setPhoneError] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.firstName || !formData.lastName || !formData.email) {
      setErrorMsg('First name, last name, and email are required.');
      return;
    }
    if (phoneError) {
      setErrorMsg('Please correct the phone number error before submitting.');
      return;
    }

    updateClientMutation.mutate(formData, {
      onSuccess: () => {
        onClose();
      },
      onError: (err: any) => {
        setErrorMsg(err?.response?.data?.message || 'Failed to update client profile.');
      },
    });
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-content" style={{ maxWidth: '600px' }}>
        <div className="modal-header">
          <h3>Edit Client Profile</h3>
          <button className="btn btn-ghost" onClick={onClose} disabled={updateClientMutation.isPending}>
            ✕
          </button>
        </div>
        
        {errorMsg && (
          <div style={{ background: 'var(--status-suspended-bg)', color: 'var(--status-suspended)', padding: '12px', borderRadius: '6px', marginBottom: '16px', fontSize: '13px' }}>
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="form-row">
          <div>
            <label className="form-label">First Name *</label>
            <input 
              type="text" 
              name="firstName"
              className="form-input" 
              value={formData.firstName}
              onChange={handleChange}
              disabled={updateClientMutation.isPending}
              required
            />
          </div>

          <div>
            <label className="form-label">Last Name *</label>
            <input 
              type="text" 
              name="lastName"
              className="form-input" 
              value={formData.lastName}
              onChange={handleChange}
              disabled={updateClientMutation.isPending}
              required
            />
          </div>

          <div style={{ gridColumn: '1 / -1' }}>
            <label className="form-label">Email *</label>
            <input 
              type="email" 
              name="email"
              className="form-input" 
              value={formData.email}
              onChange={handleChange}
              disabled={updateClientMutation.isPending}
              required
            />
          </div>

          <div style={{ gridColumn: '1 / -1' }}>
            <PhoneInputWithCountry
              value={formData.phone || ''}
              onChange={(formatted, isValid) => {
                setFormData((prev) => ({ ...prev, phone: formatted }));
                if (!isValid && formatted) {
                  setPhoneError('Invalid phone number for selected country.');
                } else {
                  setPhoneError('');
                }
              }}
              error={phoneError}
              disabled={updateClientMutation.isPending}
            />
          </div>

          <div>
            <label className="form-label">Company Name</label>
            <input 
              type="text" 
              name="companyName"
              className="form-input" 
              value={formData.companyName}
              onChange={handleChange}
              disabled={updateClientMutation.isPending}
            />
          </div>

          <div>
            <label className="form-label">Tier</label>
            <select 
              name="tier"
              className="form-input"
              value={formData.tier}
              onChange={handleChange}
              disabled={updateClientMutation.isPending}
            >
              <option value="STANDARD">Standard</option>
              <option value="PREMIUM">Premium</option>
              <option value="ENTERPRISE">Enterprise</option>
            </select>
          </div>

          <div>
            <label className="form-label">Status</label>
            <select 
              name="status"
              className="form-input"
              value={formData.status}
              onChange={handleChange}
              disabled={updateClientMutation.isPending}
            >
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="SUSPENDED">Suspended</option>
            </select>
          </div>

          <div style={{ gridColumn: '1 / -1', display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
            <button 
              type="button" 
              className="btn btn-ghost" 
              onClick={onClose}
              disabled={updateClientMutation.isPending}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={updateClientMutation.isPending || !formData.firstName || !formData.lastName || !formData.email}
            >
              {updateClientMutation.isPending ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
