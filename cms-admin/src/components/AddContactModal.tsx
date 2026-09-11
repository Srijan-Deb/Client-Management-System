import React, { useState } from 'react';
import { useAddContact } from '../hooks/useClients';
import { PhoneInputWithCountry } from './PhoneInputWithCountry';
import type { ContactRequest, ContactType } from '../types/client';

interface AddContactModalProps {
  clientId: number;
  onClose: () => void;
}

const CONTACT_TYPES: ContactType[] = ['PRIMARY', 'SECONDARY', 'BILLING', 'TECHNICAL'];

export default function AddContactModal({ clientId, onClose }: AddContactModalProps) {
  const addContactMutation = useAddContact(clientId);

  const [contactType, setContactType] = useState<ContactType>('PRIMARY');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [phoneError, setPhoneError] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!firstName.trim() || !lastName.trim() || !email.trim()) {
      setErrorMsg('First name, last name, and email are required.');
      return;
    }
    if (phoneError) {
      setErrorMsg('Please correct the phone number error before submitting.');
      return;
    }

    const payload: ContactRequest = {
      contactType,
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      email: email.trim(),
      phone: phone.trim() || undefined,
    };

    addContactMutation.mutate(payload, {
      onSuccess: () => {
        onClose();
      },
      onError: (err: any) => {
        const msg = err?.response?.data?.message || err?.response?.data?.errors?.[0]?.defaultMessage || 'Failed to add contact.';
        setErrorMsg(msg);
      },
    });
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
        <div className="modal-header">
          <h3>Add Contact</h3>
          <button className="btn btn-ghost" onClick={onClose} disabled={addContactMutation.isPending}>
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
            <label className="form-label">Contact Type *</label>
            <select
              className="form-input"
              value={contactType}
              onChange={(e) => setContactType(e.target.value as ContactType)}
              disabled={addContactMutation.isPending}
            >
              {CONTACT_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">First Name *</label>
            <input 
              type="text" 
              className="form-input" 
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              disabled={addContactMutation.isPending}
              placeholder="John"
              required
            />
          </div>

          <div>
            <label className="form-label">Last Name *</label>
            <input 
              type="text" 
              className="form-input" 
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              disabled={addContactMutation.isPending}
              placeholder="Doe"
              required
            />
          </div>

          <div style={{ gridColumn: '1 / -1' }}>
            <label className="form-label">Email *</label>
            <input 
              type="email" 
              className="form-input" 
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={addContactMutation.isPending}
              placeholder="john@example.com"
              required
            />
          </div>

          <div style={{ gridColumn: '1 / -1' }}>
            <PhoneInputWithCountry
              value={phone}
              onChange={(formatted, isValid) => {
                setPhone(formatted);
                if (!isValid && formatted) {
                  setPhoneError('Invalid phone number for selected country.');
                } else {
                  setPhoneError('');
                }
              }}
              error={phoneError}
              disabled={addContactMutation.isPending}
            />
          </div>

          <div style={{ gridColumn: '1 / -1', display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
            <button 
              type="button" 
              className="btn btn-ghost" 
              onClick={onClose}
              disabled={addContactMutation.isPending}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={addContactMutation.isPending || !firstName.trim() || !lastName.trim() || !email.trim()}
            >
              {addContactMutation.isPending ? 'Adding...' : 'Add Contact'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
