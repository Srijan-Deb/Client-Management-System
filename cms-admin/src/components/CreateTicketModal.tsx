import { useState, useRef, useEffect } from 'react';
import { useCreateTicket } from '../hooks/useTickets';

interface CreateTicketModalProps {
  clientId: number;
  accountId?: number;
  onClose: () => void;
}

const CATEGORIES = ['Billing', 'Technical', 'Account', 'General Support'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const CreateTicketModal: React.FC<CreateTicketModalProps> = ({ clientId, accountId, onClose }) => {
  const mutation = useCreateTicket();
  
  const subjectInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    // Auto-focus on mount
    subjectInputRef.current?.focus();
  }, []);

  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [category, setCategory] = useState(CATEGORIES[1]); // Default to Technical

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subject.trim() || !description.trim()) return;

    try {
      await mutation.mutateAsync({
        clientId,
        accountId,
        subject,
        description,
        priority,
        category,
      });
      onClose();
    } catch (err) {
      console.error('Failed to create ticket', err);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
        <div className="modal-header">
          <h2 className="modal-title">Create Support Ticket</h2>
          <button className="modal-close" type="button" onClick={onClose} aria-label="Close modal">✕</button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          <div className="form-group">
            <label className="form-label">Subject</label>
            <input
              ref={subjectInputRef}
              className="form-input"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              placeholder="e.g. Cannot access billing dashboard"
              required
              disabled={mutation.isPending}
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Category</label>
              <select
                className="form-input"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                disabled={mutation.isPending}
              >
                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Priority</label>
              <select
                className="form-input"
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                disabled={mutation.isPending}
              >
                {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
              </select>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Description</label>
            <textarea
              className="form-input"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe the issue in detail..."
              rows={4}
              required
              disabled={mutation.isPending}
            />
          </div>

          {mutation.isError && (
            <p className="form-error">
              {(mutation.error as any)?.response?.data?.message || 'Failed to create ticket.'}
            </p>
          )}

          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={onClose} disabled={mutation.isPending}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={mutation.isPending}>
              {mutation.isPending ? 'Creating...' : 'Create Ticket'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateTicketModal;
