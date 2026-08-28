import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useClients, useCreateClient, useUpdateClient } from '../hooks/useClients';
import { useHasRole } from '../auth/RoleGate';
import { TableSkeleton } from '../components/TableSkeleton';
import { EmptyState } from '../components/EmptyState';
import type { ClientSummary, ClientTier, ClientStatus } from '../types/client';

// ─── Zod schemas matching backend validation ───────────────────────────────

const createSchema = z.object({
  firstName: z.string().min(1, 'First name is required').max(100),
  lastName: z.string().min(1, 'Last name is required').max(100),
  email: z.string().email('Must be a valid email').max(255),
  phone: z.string().regex(/^[+]?[\d\s\-().]{7,20}$/, 'Invalid phone format').optional().or(z.literal('')),
  companyName: z.string().max(255).optional().or(z.literal('')),
  tier: z.enum(['STANDARD', 'PREMIUM', 'ENTERPRISE']),
});

const updateSchema = createSchema.partial().extend({
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING']).optional(),
});

type CreateFormData = z.infer<typeof createSchema>;
type UpdateFormData = z.infer<typeof updateSchema>;

// ─── Helpers ──────────────────────────────────────────────────────────────

const TIER_COLORS: Record<ClientTier, string> = {
  STANDARD: 'tier-bronze',
  PREMIUM: 'tier-silver',
  ENTERPRISE: 'tier-gold',
};

const STATUS_COLORS: Record<ClientStatus, string> = {
  ACTIVE: 'status-active',
  INACTIVE: 'status-inactive',
  SUSPENDED: 'status-suspended',
  PENDING: 'status-pending',
};

// ─── Modal ────────────────────────────────────────────────────────────────

interface ClientModalProps {
  client?: ClientSummary;
  onClose: () => void;
}

const ClientModal = ({ client, onClose }: ClientModalProps) => {
  const isEdit = !!client;
  const createMutation = useCreateClient();
  const updateMutation = useUpdateClient(client?.clientId ?? 0);
  const mutation = isEdit ? updateMutation : createMutation;

  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<CreateFormData | UpdateFormData>({
    resolver: zodResolver(isEdit ? updateSchema : createSchema),
    defaultValues: isEdit ? {
      firstName: client.firstName,
      lastName: client.lastName,
      email: client.email,
      phone: client.phone ?? '',
      companyName: client.companyName ?? '',
      tier: client.tier,
    } : { tier: 'STANDARD' },
  });

  const onSubmit = async (data: any) => {
    try {
      // Strip empty strings → undefined for optional fields
      const cleaned = Object.fromEntries(
        Object.entries(data).map(([k, v]) => [k, v === '' ? undefined : v])
      );
      await mutation.mutateAsync(cleaned as any);
      onClose();
    } catch (error: any) {
      if (error.response?.status === 409 && error.response?.data?.errorCode === 'DUPLICATE_EMAIL') {
        setError('email', { type: 'manual', message: error.response.data.message });
      } else {
        // Handled by the generic error display below
      }
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">{isEdit ? 'Edit Client' : 'New Client'}</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="modal-form">
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">First Name *</label>
              <input className={`form-input ${errors.firstName ? 'form-input-error' : ''}`} {...register('firstName')} />
              {errors.firstName && <p className="form-error">{errors.firstName.message}</p>}
            </div>
            <div className="form-group">
              <label className="form-label">Last Name *</label>
              <input className={`form-input ${errors.lastName ? 'form-input-error' : ''}`} {...register('lastName')} />
              {errors.lastName && <p className="form-error">{errors.lastName.message}</p>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Email *</label>
            <input type="email" className={`form-input ${errors.email ? 'form-input-error' : ''}`} {...register('email')} />
            {errors.email && <p className="form-error">{errors.email.message}</p>}
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Phone</label>
              <input className="form-input" placeholder="+91 9876543210" {...register('phone')} />
              {errors.phone && <p className="form-error">{errors.phone.message as string}</p>}
            </div>
            <div className="form-group">
              <label className="form-label">Tier *</label>
              <select className="form-input" {...register('tier')}>
                {['STANDARD', 'PREMIUM', 'ENTERPRISE'].map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Company Name</label>
            <input className="form-input" {...register('companyName')} />
          </div>

          {isEdit && (
            <div className="form-group">
              <label className="form-label">Status</label>
              <select className="form-input" {...register('status' as any)}>
                {['ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING'].map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>
          )}

          {mutation.isError && (
            <p className="form-error">
              {(mutation.error as any)?.response?.data?.message ?? 'Something went wrong'}
            </p>
          )}

          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={isSubmitting || mutation.isPending}>
              {mutation.isPending ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Client'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

// ─── Main Page ────────────────────────────────────────────────────────────

const ClientsPage = () => {
  const canWrite = useHasRole(['admin', 'account_manager']);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingClient, setEditingClient] = useState<ClientSummary | undefined>();

  const { data, isLoading, isError } = useClients(search || undefined, page);

  const openCreate = () => { setEditingClient(undefined); setModalOpen(true); };
  const openEdit = (c: ClientSummary) => { setEditingClient(c); setModalOpen(true); };
  const closeModal = () => setModalOpen(false);

  return (
    <div className="clients-page">
      {/* Header */}
      <div className="page-header">
        <div>
          <h2 className="page-heading">Clients</h2>
          <p className="page-subheading">
            {data ? `${data.totalElements.toLocaleString()} clients total` : 'Loading…'}
          </p>
        </div>
        {canWrite && (
          <button className="btn btn-primary" onClick={openCreate}>
            + New Client
          </button>
        )}
      </div>

      {/* Search */}
      <div className="search-bar">
        <span className="search-icon">🔍</span>
        <input
          className="search-input"
          placeholder="Search by name, email, or company…"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
        />
        {search && (
          <button className="search-clear" onClick={() => setSearch('')}>✕</button>
        )}
      </div>

      {/* Table */}
      <div className="table-card">
        {isLoading && (
          <TableSkeleton rows={5} columns={canWrite ? 6 : 5} />
        )}

        {isError && (
          <div className="table-error">
            <p>⚠️ Failed to load clients. Check that the gateway and client-service are running.</p>
          </div>
        )}

        {!isLoading && !isError && (
          <>
            <table className="table">
              <thead>
                <tr>
                  <th className="th">Client</th>
                  <th className="th">Company</th>
                  <th className="th">Tier</th>
                  <th className="th">Status</th>
                  <th className="th">Created</th>
                  {canWrite && <th className="th">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {data?.content.length === 0 && (
                  <tr>
                    <td colSpan={canWrite ? 6 : 5} style={{ padding: 0 }}>
                      <EmptyState
                        icon="👥"
                        title="No clients found"
                        description={search ? `No clients matched your search for "${search}".` : "You haven't added any clients yet. Start by creating your first client profile."}
                        action={canWrite && !search ? (
                          <button className="btn btn-primary" onClick={openCreate}>
                            + New Client
                          </button>
                        ) : undefined}
                      />
                    </td>
                  </tr>
                )}
                {data?.content.map((client) => (
                  <tr key={client.clientId} className="tr">
                    <td className="td">
                      <Link to={`/clients/${client.clientId}`} className="client-cell" style={{ textDecoration: 'none' }}>
                        <div className="client-avatar">
                          {client.firstName[0]}{client.lastName[0]}
                        </div>
                        <div>
                          <p className="client-name">{client.firstName} {client.lastName}</p>
                          <p className="client-email">{client.email}</p>
                        </div>
                      </Link>
                    </td>
                    <td className="td td-secondary">{client.companyName ?? '—'}</td>
                    <td className="td">
                      <span className={`badge ${TIER_COLORS[client.tier]}`}>{client.tier}</span>
                    </td>
                    <td className="td">
                      <span className={`badge ${STATUS_COLORS[client.status]}`}>{client.status}</span>
                    </td>
                    <td className="td td-secondary">
                      {new Date(client.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                    </td>
                    {canWrite && (
                      <td className="td">
                        <button className="btn-icon" onClick={() => openEdit(client)} title="Edit">✏️</button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Pagination */}
            {data && data.totalPages > 1 && (
              <div className="pagination">
                <button
                  className="btn btn-ghost btn-sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  ← Prev
                </button>
                <span className="pagination-info">
                  Page {page + 1} of {data.totalPages}
                </span>
                <button
                  className="btn btn-ghost btn-sm"
                  disabled={page >= data.totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Modal */}
      {modalOpen && (
        <ClientModal client={editingClient} onClose={closeModal} />
      )}
    </div>
  );
};

export default ClientsPage;
