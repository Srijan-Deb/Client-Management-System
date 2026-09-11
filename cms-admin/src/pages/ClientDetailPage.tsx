import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useClient, useDeleteContact, useDeleteAddress } from '../hooks/useClients';
import { useInvoices, useContracts } from '../hooks/useBilling';
import api from '../api/axios';
import { DetailSkeleton } from '../components/DetailSkeleton';
import PaymentModal from '../components/PaymentModal';
import type { Invoice } from '../types/billing';
import { useTickets } from '../hooks/useTickets';
import CreateTicketModal from '../components/CreateTicketModal';
import CreateSubscriptionModal from '../components/CreateSubscriptionModal';
import CreateContractModal from '../components/CreateContractModal';
import AddContactModal from '../components/AddContactModal';
import AddAddressModal from '../components/AddAddressModal';
import EditClientModal from '../components/EditClientModal';
import { useHasRole } from '../auth/RoleGate';

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
  const [creatingSubscription, setCreatingSubscription] = useState(false);
  const [creatingContract, setCreatingContract] = useState(false);
  const [creatingContact, setCreatingContact] = useState(false);
  const [creatingAddress, setCreatingAddress] = useState(false);
  const [editingProfile, setEditingProfile] = useState(false);

  const { data: client, isLoading, isError } = useClient(clientId);
  const deleteContactMutation = useDeleteContact(clientId);
  const deleteAddressMutation = useDeleteAddress(clientId);
  const { data: activityLogs = [] } = useClientActivity(clientId);
  const { data: invoicesData } = useInvoices(clientId);
  const invoices = Array.isArray(invoicesData) ? invoicesData : (invoicesData as any)?.content ?? [];
  const { data: contractsData } = useContracts(clientId);
  const contracts = Array.isArray(contractsData) ? contractsData : (contractsData as any)?.content ?? [];
  const { data: ticketsData } = useTickets(clientId);
  const tickets = ticketsData?.content || [];

  // Role-based UI guards (backend @PreAuthorize is the real enforcement)
  const canWrite = useHasRole(['admin', 'account_manager']);   // create/edit/delete clients, contracts

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
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 600, margin: 0 }}>Client Profile</h3>
              {canWrite && (
                <button className="btn btn-primary btn-sm" onClick={() => setEditingProfile(true)}>
                  Edit Profile
                </button>
              )}
            </div>
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
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 600, margin: 0 }}>Contacts</h3>
              {canWrite && (
                <button className="btn btn-primary btn-sm" onClick={() => setCreatingContact(true)}>
                  + Add Contact
                </button>
              )}
            </div>
            <table className="table">
              <thead>
                <tr>
                  <th className="th">Name</th>
                  <th className="th">Type</th>
                  <th className="th">Email</th>
                  <th className="th">Phone</th>
                  <th className="th">Actions</th>
                </tr>
              </thead>
              <tbody>
                {client.contacts.length === 0 && (
                  <tr><td colSpan={5} className="table-empty">No contacts added.</td></tr>
                )}
                {client.contacts.map((c: any) => (
                  <tr key={c.contactId} className="tr">
                    <td className="td" style={{ fontWeight: 500 }}>
                      {c.name || `${c.firstName || ''} ${c.lastName || ''}`.trim() || '—'}
                    </td>
                    <td className="td">
                      <span className="badge tier-silver" style={{ fontSize: '11px', textTransform: 'uppercase' }}>
                        {c.contactType || c.role || 'PRIMARY'}
                      </span>
                    </td>
                    <td className="td td-secondary">{c.email}</td>
                    <td className="td td-secondary">{c.phone || '—'}</td>
                    <td className="td">
                      {canWrite && (
                        <button 
                          className="btn btn-ghost btn-sm" 
                          style={{ color: 'var(--status-suspended)' }}
                          onClick={() => {
                            if (confirm('Are you sure you want to delete this contact?')) {
                              deleteContactMutation.mutate(c.contactId);
                            }
                          }}
                          disabled={deleteContactMutation.isPending}
                        >
                          Delete
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'addresses' && (
          <div className="table-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 600, margin: 0 }}>Addresses</h3>
              {canWrite && (
                <button className="btn btn-primary btn-sm" onClick={() => setCreatingAddress(true)}>
                  + Add Address
                </button>
              )}
            </div>
            <table className="table">
              <thead>
                <tr>
                  <th className="th">Type</th>
                  <th className="th">Street</th>
                  <th className="th">City</th>
                  <th className="th">State</th>
                  <th className="th">Country</th>
                  <th className="th">Postal Code</th>
                  <th className="th">Actions</th>
                </tr>
              </thead>
              <tbody>
                {client.addresses.length === 0 && (
                  <tr><td colSpan={7} className="table-empty">No addresses added.</td></tr>
                )}
                {client.addresses.map((a: any) => (
                  <tr key={a.addressId} className="tr">
                    <td className="td">
                      <span className="badge tier-silver" style={{ fontSize: '11px', textTransform: 'uppercase' }}>
                        {a.addressType || 'BILLING'}
                        {a.primary ? ' ★' : ''}
                      </span>
                    </td>
                    <td className="td">{a.line1 || a.street}{a.line2 ? `, ${a.line2}` : ''}</td>
                    <td className="td">{a.city}</td>
                    <td className="td td-secondary">{a.state || '—'}</td>
                    <td className="td">{a.country}</td>
                    <td className="td td-secondary">{a.postalCode || '—'}</td>
                    <td className="td">
                      {canWrite && (
                        <button 
                          className="btn btn-ghost btn-sm" 
                          style={{ color: 'var(--status-suspended)' }}
                          onClick={() => {
                            if (confirm('Are you sure you want to delete this address?')) {
                              deleteAddressMutation.mutate(a.addressId);
                            }
                          }}
                          disabled={deleteAddressMutation.isPending}
                        >
                          Delete
                        </button>
                      )}
                    </td>
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
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

            {/* ── Contracts ─────────────────────────────────────────────── */}
            <div className="table-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
                <div>
                  <h3 style={{ fontSize: '16px', fontWeight: 600, margin: 0 }}>Contracts</h3>
                  <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: '2px 0 0 0' }}>
                    Creating a contract automatically generates an invoice &amp; emails it to the client.
                  </p>
                </div>
                {canWrite && (
                  <button
                    id="create-contract-btn"
                    className="btn btn-primary btn-sm"
                    onClick={() => setCreatingContract(true)}
                  >
                    + New Contract
                  </button>
                )}
              </div>
              {contracts.length === 0 ? (
                <div className="table-empty">No contracts yet. Click &ldquo;+ New Contract&rdquo; to create the first one.</div>
              ) : (
                <table className="table">
                  <thead>
                    <tr>
                      <th className="th">Contract ID</th>
                      <th className="th">Status</th>
                      <th className="th">Total Value</th>
                      <th className="th">Start Date</th>
                      <th className="th">Created At</th>
                    </tr>
                  </thead>
                  <tbody>
                    {contracts.map((c: any) => (
                      <tr key={c.id} className="tr">
                        <td className="td" style={{ fontWeight: 600 }}>#{c.id}</td>
                        <td className="td">
                          <span className={`badge ${c.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}`}>
                            {c.status}
                          </span>
                        </td>
                        <td className="td" style={{ fontWeight: 600 }}>INR {Number(c.totalValue).toFixed(2)}</td>
                        <td className="td td-secondary">
                          {new Date(c.startDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                        </td>
                        <td className="td td-secondary">
                          {new Date(c.createdAt).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            {/* ── Invoices ──────────────────────────────────────────────── */}
            <div className="table-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
                <h3 style={{ fontSize: '16px', fontWeight: 600, margin: 0 }}>Invoices</h3>
              </div>
              {invoices.length === 0 ? (
                <div className="table-empty">No invoices found. Invoices are auto-generated when you create a contract.</div>
              ) : (
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

    {creatingContract && client && (
      <CreateContractModal
        clientId={client.clientId}
        accountId={client.accountId}
        clientEmail={client.email}
        onClose={() => setCreatingContract(false)}
      />
    )}

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

    {creatingSubscription && client && (
      <CreateSubscriptionModal
        clientId={client.clientId}
        clientEmail={client.email}
        onClose={() => setCreatingSubscription(false)}
      />
    )}

    {creatingContact && client && (
      <AddContactModal
        clientId={client.clientId}
        onClose={() => setCreatingContact(false)}
      />
    )}

    {creatingAddress && client && (
      <AddAddressModal
        clientId={client.clientId}
        onClose={() => setCreatingAddress(false)}
      />
    )}

    {editingProfile && client && (
      <EditClientModal
        client={client}
        onClose={() => setEditingProfile(false)}
      />
    )}
  </>);
};

export default ClientDetailPage;
