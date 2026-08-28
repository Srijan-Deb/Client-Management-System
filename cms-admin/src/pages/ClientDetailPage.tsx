import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useClient } from '../hooks/useClients';
import { useInvoices } from '../hooks/useBilling';
import api from '../api/axios';
import { DetailSkeleton } from '../components/DetailSkeleton';
import PaymentModal from '../components/PaymentModal';
import type { Invoice } from '../types/billing';
import { useTickets } from '../hooks/useTickets';
import CreateTicketModal from '../components/CreateTicketModal';

const useClientActivity = (clientId: number) =>
  useQuery({
    queryKey: ['clients', clientId, 'activity'],
    queryFn: () => api.get(`/clients/api/v1/clients/${clientId}/activity`).then(r => r.data),
    enabled: !!clientId,
  });

const ClientDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const clientId = Number(id);
  const [activeTab, setActiveTab] = useState<'profile' | 'contacts' | 'addresses' | 'activity' | 'billing' | 'tickets'>('profile');
  const [payingInvoice, setPayingInvoice] = useState<Invoice | null>(null);
  const [creatingTicket, setCreatingTicket] = useState(false);

  const { data: client, isLoading, isError } = useClient(clientId);
  const { data: activityLogs = [] } = useClientActivity(clientId);
  const { data: invoices = [] } = useInvoices(clientId);
  const { data: ticketsData } = useTickets(clientId);
  const tickets = ticketsData?.content || [];

  if (isLoading) return <DetailSkeleton />;
  if (isError || !client) return <div className="table-error"><p>Failed to load client details.</p></div>;

  return (
    <>
    <div className="clients-page">
      <div className="page-header" style={{ alignItems: 'center' }}>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          <Link to="/clients" className="btn btn-ghost btn-icon" style={{ padding: '8px 12px' }}>
            ←
          </Link>
          <div className="client-avatar" style={{ width: '56px', height: '56px', fontSize: '20px' }}>
            {client.firstName[0]}{client.lastName[0]}
          </div>
          <div>
            <h2 className="page-heading">{client.firstName} {client.lastName}</h2>
            <p className="page-subheading">{client.email} {client.companyName ? `· ${client.companyName}` : ''}</p>
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', gap: '20px', marginTop: '20px', borderBottom: '1px solid var(--border)' }}>
        {(['profile', 'contacts', 'addresses', 'billing', 'activity', 'tickets'] as const).map(tab => (
          <button
            key={tab}
            className={`tab-btn ${activeTab === tab ? 'active' : ''}`}
            onClick={() => setActiveTab(tab)}
            style={{
              padding: '12px 16px',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === tab ? '2px solid var(--primary)' : '2px solid transparent',
              color: activeTab === tab ? 'var(--text-primary)' : 'var(--text-secondary)',
              cursor: 'pointer',
              fontWeight: activeTab === tab ? 600 : 400,
              textTransform: 'capitalize'
            }}
          >
            {tab}
          </button>
        ))}
      </div>

      <div style={{ marginTop: '24px' }}>
        {activeTab === 'profile' && (
          <div className="table-card" style={{ padding: '24px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '16px' }}>Client Profile</h3>
            <div className="form-row">
              <div>
                <p className="form-label">First Name</p>
                <p>{client.firstName}</p>
              </div>
              <div>
                <p className="form-label">Last Name</p>
                <p>{client.lastName}</p>
              </div>
              <div>
                <p className="form-label">Email</p>
                <p>{client.email}</p>
              </div>
              <div>
                <p className="form-label">Phone</p>
                <p>{client.phone || '—'}</p>
              </div>
              <div>
                <p className="form-label">Company</p>
                <p>{client.companyName || '—'}</p>
              </div>
              <div>
                <p className="form-label">Tier</p>
                <span className={`badge tier-${client.tier.toLowerCase()}`}>{client.tier}</span>
              </div>
              <div>
                <p className="form-label">Status</p>
                <span className={`badge status-${client.status.toLowerCase()}`}>{client.status}</span>
              </div>
              <div>
                <p className="form-label">Created At</p>
                <p>{new Date(client.createdAt).toLocaleString()}</p>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'contacts' && (
          <div className="table-card">
            <table className="table">
              <thead>
                <tr>
                  <th className="th">Name</th>
                  <th className="th">Email</th>
                  <th className="th">Phone</th>
                  <th className="th">Role</th>
                </tr>
              </thead>
              <tbody>
                {client.contacts.length === 0 && (
                  <tr><td colSpan={4} className="table-empty">No contacts added.</td></tr>
                )}
                {client.contacts.map((c: any) => (
                  <tr key={c.contactId} className="tr">
                    <td className="td">{c.name}</td>
                    <td className="td td-secondary">{c.email}</td>
                    <td className="td td-secondary">{c.phone || '—'}</td>
                    <td className="td">{c.role || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'addresses' && (
          <div className="table-card">
            <table className="table">
              <thead>
                <tr>
                  <th className="th">Street</th>
                  <th className="th">City</th>
                  <th className="th">State</th>
                  <th className="th">Country</th>
                  <th className="th">Postal Code</th>
                </tr>
              </thead>
              <tbody>
                {client.addresses.length === 0 && (
                  <tr><td colSpan={5} className="table-empty">No addresses added.</td></tr>
                )}
                {client.addresses.map((a: any) => (
                  <tr key={a.addressId} className="tr">
                    <td className="td">{a.street}</td>
                    <td className="td">{a.city}</td>
                    <td className="td td-secondary">{a.state || '—'}</td>
                    <td className="td">{a.country}</td>
                    <td className="td td-secondary">{a.postalCode || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'activity' && (
          <div className="table-card" style={{ padding: '0 24px' }}>
            {activityLogs.length === 0 && (
               <div className="table-empty">No activity logs found.</div>
            )}
            {activityLogs.map((log: any) => (
              <div key={log.logId} style={{ padding: '16px 0', borderBottom: '1px solid var(--border)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                  <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{log.action}</span>
                  <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                    {new Date(log.createdAt).toLocaleString()}
                  </span>
                </div>
                {log.description && <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>{log.description}</p>}
                <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '8px' }}>
                  Entity: {log.entityType} {log.entityId ? `#${log.entityId}` : ''} | IP: {log.ipAddress}
                </div>
              </div>
            ))}
          </div>
        )}

        {activeTab === 'billing' && (
          <div className="table-card">
            {invoices.length === 0 && (
              <div className="table-empty">No invoices found for this client.</div>
            )}
            {invoices.length > 0 && (
              <table className="table">
                <thead>
                  <tr>
                    <th className="th">Invoice #</th>
                    <th className="th">Due Date</th>
                    <th className="th">Total</th>
                    <th className="th">Status</th>
                    <th className="th">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {invoices.map((inv: Invoice) => (
                    <tr key={inv.id} className="tr">
                      <td className="td" style={{ fontWeight: 600 }}>{inv.invoiceNumber}</td>
                      <td className="td td-secondary">{new Date(inv.dueDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                      <td className="td" style={{ fontWeight: 600 }}>{inv.currency} {Number(inv.totalAmount).toFixed(2)}</td>
                      <td className="td">
                        <span className={`badge ${inv.status === 'PAID' ? 'status-active' : inv.status === 'OVERDUE' ? 'status-suspended' : 'status-pending'}`}>
                          {inv.status}
                        </span>
                      </td>
                      <td className="td" style={{ display: 'flex', gap: '8px' }}>
                        {inv.pdfObjectKey && (
                          <a href={inv.pdfObjectKey} target="_blank" rel="noopener noreferrer" className="btn btn-ghost btn-sm">📄 PDF</a>
                        )}
                        {inv.status === 'PENDING' && (
                          <button className="btn btn-primary btn-sm" onClick={() => setPayingInvoice(inv)}>💳 Pay</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
        {activeTab === 'tickets' && (
          <div className="table-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 600, margin: 0 }}>Support Tickets</h3>
              <button className="btn btn-primary btn-sm" onClick={() => setCreatingTicket(true)}>
                + Create Ticket
              </button>
            </div>
            {tickets.length === 0 && (
              <div className="table-empty">No tickets found for this client.</div>
            )}
            {tickets.length > 0 && (
              <table className="table">
                <thead>
                  <tr>
                    <th className="th">ID</th>
                    <th className="th">Subject</th>
                    <th className="th">Status</th>
                    <th className="th">Priority</th>
                    <th className="th">Created At</th>
                  </tr>
                </thead>
                <tbody>
                  {tickets.map((t: any) => (
                    <tr key={t.ticketId} className="tr">
                      <td className="td" style={{ fontWeight: 600 }}>
                        <Link to={`/support/${t.ticketId}`} className="text-primary">
                          #{t.ticketId}
                        </Link>
                      </td>
                      <td className="td">{t.subject}</td>
                      <td className="td">
                        <span className={`badge status-${t.status.toLowerCase()}`}>{t.status}</span>
                      </td>
                      <td className="td td-secondary">{t.priority}</td>
                      <td className="td td-secondary">
                        {new Date(t.createdAt).toLocaleDateString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
    </div>

    {creatingTicket && client && (
      <CreateTicketModal
        clientId={client.clientId}
        onClose={() => setCreatingTicket(false)}
      />
    )}

    {payingInvoice && client && (
      <PaymentModal
        invoice={payingInvoice}
        clientEmail={client.email}
        onClose={() => setPayingInvoice(null)}
      />
    )}
  </>);
};

export default ClientDetailPage;
