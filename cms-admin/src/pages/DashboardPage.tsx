import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import { useHasRole } from '../auth/RoleGate';
import { 
  useClientMetrics, 
  useBillingMetrics,
  useClientGrowth,
  useTicketStatus,
  useTicketPriority,
  useInvoiceStatus
} from '../hooks/useDashboard';
import { useClients, useMyClient } from '../hooks/useClients';
import { useTickets } from '../hooks/useTickets';
import { useInvoices } from '../hooks/useBilling';
import { billingApi } from '../api/billing';
import type { Invoice } from '../types/billing';
import PaymentModal from '../components/PaymentModal';
import { LiveLineChart } from '../components/charts/LiveLineChart';
import { LiveDonutChart } from '../components/charts/LiveDonutChart';
import { LiveBarChart } from '../components/charts/LiveBarChart';

// ── Animated stat card ────────────────────────────────────────────────────────
const StatCard = ({
  label, value, icon, color, trend,
}: {
  label: string;
  value: string | number;
  icon: string;
  color: string;
  trend?: { value: string; up: boolean };
}) => (
  <div className={`stat-card stat-card-${color}`} style={{ position: 'relative', overflow: 'hidden' }}>
    {/* Background glow blob */}
    <div style={{
      position: 'absolute', top: '-18px', right: '-18px',
      width: '80px', height: '80px',
      borderRadius: '50%',
      background: color === 'blue'   ? 'rgba(99,102,241,0.12)'
               : color === 'green'  ? 'rgba(16,185,129,0.12)'
               : color === 'orange' ? 'rgba(245,158,11,0.12)'
               :                      'rgba(168,85,247,0.12)',
      filter: 'blur(12px)',
      pointerEvents: 'none',
    }} />
    <div className="stat-icon">{icon}</div>
    <div style={{ flex: 1, minWidth: 0 }}>
      <p className="stat-value" style={{ fontVariantNumeric: 'tabular-nums' }}>{value}</p>
      <p className="stat-label">{label}</p>
      {trend && (
        <p style={{
          fontSize: '11px',
          marginTop: '4px',
          color: trend.up ? '#34d399' : '#f87171',
          fontWeight: 600,
        }}>
          {trend.up ? '↑' : '↓'} {trend.value}
        </p>
      )}
    </div>
  </div>
);

// ── Main Dashboard ────────────────────────────────────────────────────────────
const DashboardPage = () => {
  const { user } = useAuth();
  const isAdmin = useHasRole(['admin']);
  const isAccountManager = useHasRole(['account_manager']);
  const isSupportAgent = useHasRole(['support_agent']);
  const isClient = useHasRole(['client']) && !isAdmin && !isAccountManager && !isSupportAgent;

  const canViewBilling = isAdmin || isAccountManager;
  const canViewSupport = isAdmin || isSupportAgent;

  const [payingInvoice, setPayingInvoice] = useState<Invoice | null>(null);

  // ── Client queries (Only enabled when logged in as client) ──
  const { data: myClient, isLoading: isLoadingMyClient } = useMyClient({ enabled: isClient });
  const { data: clientInvoicesData, isLoading: isLoadingClientInvoices } = useInvoices(undefined, { enabled: isClient });
  const clientInvoices: Invoice[] = Array.isArray(clientInvoicesData) 
    ? clientInvoicesData 
    : (clientInvoicesData as any)?.content ?? [];

  const { data: clientTicketsData, isLoading: isLoadingClientTickets } = useTickets(undefined, { enabled: isClient });
  const clientTickets = clientTicketsData?.content ?? [];

  // ── Staff queries (Disabled for clients to prevent unauthorized 403 calls) ──
  const { data: clientMetrics, isLoading: isLoadingClientMetrics } = useClientMetrics({ enabled: !isClient });
  const { data: billingMetrics, isLoading: isLoadingBillingMetrics } = useBillingMetrics({ enabled: !isClient && canViewBilling });

  const { data: recentClientsData } = useClients(undefined, 0, 20, { enabled: !isClient && canViewBilling });
  const recentClients = recentClientsData?.content ?? [];

  const { data: recentTicketsData } = useTickets(undefined, { enabled: !isClient && canViewSupport });
  const allTickets = recentTicketsData?.content ?? [];
  const recentTickets = allTickets.slice(0, 5);

  // ── Pre-aggregated analytics data (Staff-only) ──
  const { data: clientGrowthData = [] } = useClientGrowth({ enabled: !isClient && canViewBilling });
  
  const { data: rawTicketStatus = [] } = useTicketStatus({ enabled: !isClient && canViewSupport });
  const ticketStatusColors: Record<string, string> = { OPEN: '#6366f1', IN_PROGRESS: '#f59e0b', RESOLVED: '#10b981', CLOSED: '#94a3b8', REOPENED: '#ef4444' };
  const ticketStatusData = rawTicketStatus.map((d: any) => ({ ...d, color: ticketStatusColors[d.name] ?? '#8b91b0' }));

  const { data: rawInvoiceStatus = [] } = useInvoiceStatus({ enabled: !isClient && canViewBilling });
  const invoiceStatusColors: Record<string, string> = { PAID: '#10b981', PENDING: '#f59e0b', OVERDUE: '#ef4444', CANCELLED: '#94a3b8' };
  const invoiceStatusData = rawInvoiceStatus.map((d: any) => ({ ...d, color: invoiceStatusColors[d.name] ?? '#8b91b0' }));

  const { data: rawTicketPriority = [] } = useTicketPriority({ enabled: !isClient && canViewSupport });
  const priorityColors: Record<string, string> = { LOW: '#10b981', MEDIUM: '#6366f1', HIGH: '#f59e0b', CRITICAL: '#ef4444' };
  const ticketPriorityData = rawTicketPriority.map((d: any) => ({ label: d.label, value: d.value, color: priorityColors[d.label] ?? '#8b91b0' }));

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';

  const handleDownloadPdf = async (pdfObjectKey: string) => {
    try {
      const url = await billingApi.getPdfDownloadUrl(pdfObjectKey);
      window.open(url, '_blank', 'noopener,noreferrer');
    } catch {
      alert('Failed to generate download link. Please try again.');
    }
  };

  // ─────────────────────────────────────────────────────────────────────────────
  // CLIENT PORTAL VIEW (For client1 & other client users)
  // ─────────────────────────────────────────────────────────────────────────────
  if (isClient) {
    const pendingInvoices = clientInvoices.filter((inv) => inv.status === 'PENDING');
    const totalDue = pendingInvoices.reduce((acc, inv) => acc + Number(inv.totalAmount || 0), 0);
    const openTickets = clientTickets.filter((t: any) => t.status === 'OPEN' || t.status === 'IN_PROGRESS');

    return (
      <div className="dashboard">
        {/* ── Welcome header ── */}
        <div className="dashboard-welcome" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <h2>{greeting}, {myClient?.companyName || user?.firstName || user?.username} 👋</h2>
            <p>Welcome to your CMS Client Portal. Manage your invoices, payments, and support requests.</p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span className={`badge tier-${myClient?.tier === 'STANDARD' ? 'bronze' : myClient?.tier === 'PREMIUM' ? 'silver' : 'gold'}`}>
              {myClient?.tier ?? 'STANDARD'} TIER
            </span>
            <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              🟢 Live Portal
            </div>
          </div>
        </div>

        {/* ── Stat cards ── */}
        <div className="stats-grid">
          <StatCard
            label="Invoices Due"
            value={isLoadingClientInvoices ? '...' : (pendingInvoices.length > 0 ? `${pendingInvoices.length} (₹${totalDue.toFixed(2)})` : 'All Paid ✓')}
            icon="🧾"
            color={pendingInvoices.length > 0 ? 'orange' : 'green'}
          />
          <StatCard
            label="Client Status"
            value={isLoadingMyClient ? '...' : (myClient?.status ?? 'ACTIVE')}
            icon="🏢"
            color="blue"
          />
          <StatCard
            label="Open Tickets"
            value={isLoadingClientTickets ? '...' : `${openTickets.length}`}
            icon="🎫"
            color="purple"
          />
        </div>

        {/* ── Client Content Rows ── */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '20px', marginTop: '8px' }}>
          {/* Recent Invoices Card */}
          <div className="table-card">
            <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 600, margin: 0 }}>🧾 Your Invoices</h3>
              <Link to="/billing" className="text-primary" style={{ fontSize: '13px', fontWeight: 500 }}>Go to Billing →</Link>
            </div>
            {isLoadingClientInvoices ? (
              <div className="table-empty">Loading invoices...</div>
            ) : clientInvoices.length === 0 ? (
              <div className="table-empty">No invoices found.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th className="th">Invoice #</th>
                    <th className="th">Due Date</th>
                    <th className="th">Amount</th>
                    <th className="th">Status</th>
                    <th className="th">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {clientInvoices.slice(0, 5).map((inv) => (
                    <tr key={inv.id} className="tr">
                      <td className="td" style={{ fontWeight: 600 }}>{inv.invoiceNumber}</td>
                      <td className="td td-secondary">{new Date(inv.dueDate).toLocaleDateString()}</td>
                      <td className="td" style={{ fontWeight: 600 }}>{inv.currency} {Number(inv.totalAmount).toFixed(2)}</td>
                      <td className="td">
                        <span className={`badge ${inv.status === 'PAID' ? 'status-active' : 'status-pending'}`}>{inv.status}</span>
                      </td>
                      <td className="td" style={{ display: 'flex', gap: '6px' }}>
                        {inv.status === 'PENDING' && (
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => setPayingInvoice(inv)}
                          >
                            💳 Pay
                          </button>
                        )}
                        {inv.pdfObjectKey && (
                          <button
                            className="btn btn-ghost btn-sm"
                            title="Download PDF"
                            onClick={() => handleDownloadPdf(inv.pdfObjectKey!)}
                          >
                            📄 PDF
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          {/* Recent Support Tickets Card */}
          <div className="table-card">
            <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 600, margin: 0 }}>🎫 Your Support Tickets</h3>
              <Link to="/support" className="text-primary" style={{ fontSize: '13px', fontWeight: 500 }}>All Tickets →</Link>
            </div>
            {isLoadingClientTickets ? (
              <div className="table-empty">Loading tickets...</div>
            ) : clientTickets.length === 0 ? (
              <div className="table-empty">No support tickets found.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th className="th">Ticket</th>
                    <th className="th">Priority</th>
                    <th className="th">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {clientTickets.slice(0, 5).map((t: any) => (
                    <tr key={t.ticketId} className="tr">
                      <td className="td">
                        <Link to={`/support/${t.ticketId}`} style={{ fontWeight: 600, color: 'var(--primary)', textDecoration: 'none' }}>
                          #{t.ticketId} {t.subject.substring(0, 26)}{t.subject.length > 26 ? '…' : ''}
                        </Link>
                      </td>
                      <td className="td">
                        <span style={{
                          fontSize: '11px', fontWeight: 700, padding: '2px 8px', borderRadius: '12px',
                          background: t.priority === 'CRITICAL' ? 'rgba(239,68,68,0.15)'
                                    : t.priority === 'HIGH'     ? 'rgba(245,158,11,0.15)'
                                    : t.priority === 'MEDIUM'   ? 'rgba(99,102,241,0.15)'
                                    :                             'rgba(16,185,129,0.15)',
                          color: t.priority === 'CRITICAL' ? '#f87171'
                               : t.priority === 'HIGH'     ? '#fbbf24'
                               : t.priority === 'MEDIUM'   ? '#818cf8'
                               :                             '#34d399',
                        }}>
                          {t.priority}
                        </span>
                      </td>
                      <td className="td">
                        <span className={`badge status-${t.status.toLowerCase().replace('_', '-')}`}>{t.status}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* ── Client Profile Details Card ── */}
        {myClient && (
          <div className="table-card" style={{ marginTop: '20px', padding: '20px 24px' }}>
            <h3 style={{ fontSize: '15px', fontWeight: 600, margin: '0 0 16px 0' }}>🏢 Account &amp; Contact Details</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
              <div>
                <p style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '4px' }}>Company</p>
                <p style={{ fontSize: '14px', fontWeight: 600, margin: 0 }}>{myClient.companyName || `${myClient.firstName} ${myClient.lastName}`}</p>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '2px 0 0' }}>Email: {myClient.email}</p>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '2px 0 0' }}>Phone: {myClient.phone || '—'}</p>
              </div>
              <div>
                <p style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '4px' }}>Primary Contact</p>
                {myClient.contacts && myClient.contacts.length > 0 ? (
                  <>
                    <p style={{ fontSize: '14px', fontWeight: 600, margin: 0 }}>{myClient.contacts[0].firstName} {myClient.contacts[0].lastName}</p>
                    <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '2px 0 0' }}>Email: {myClient.contacts[0].email}</p>
                    <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '2px 0 0' }}>Phone: {myClient.contacts[0].phone}</p>
                  </>
                ) : (
                  <p style={{ fontSize: '13px', color: 'var(--text-muted)', margin: 0 }}>No contacts on file</p>
                )}
              </div>
              <div>
                <p style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '4px' }}>Billing Address</p>
                {myClient.addresses && myClient.addresses.length > 0 ? (
                  <>
                    <p style={{ fontSize: '13px', margin: 0 }}>{myClient.addresses[0].line1}</p>
                    <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '2px 0 0' }}>
                      {myClient.addresses[0].city}, {myClient.addresses[0].postalCode}
                    </p>
                    <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '2px 0 0' }}>{myClient.addresses[0].country}</p>
                  </>
                ) : (
                  <p style={{ fontSize: '13px', color: 'var(--text-muted)', margin: 0 }}>No address on file</p>
                )}
              </div>
            </div>
          </div>
        )}

        {/* ── Payment Modal ── */}
        {payingInvoice && (
          <PaymentModal
            invoice={payingInvoice}
            clientEmail={user?.email}
            onClose={() => setPayingInvoice(null)}
          />
        )}
      </div>
    );
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // STAFF VIEW (Admin, Account Manager, Support Agent)
  // ─────────────────────────────────────────────────────────────────────────────
  return (
    <div className="dashboard">
      {/* ── Welcome header ── */}
      <div className="dashboard-welcome" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h2>{greeting}, {user?.firstName ?? user?.username} 👋</h2>
          <p>Here's a live overview of your system right now.</p>
        </div>
        <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px' }}>
          🟢 Live • Updated just now
        </div>
      </div>

      {/* ── Stat cards ── */}
      <div className="stats-grid">
        {canViewBilling && (
          <>
            <StatCard
              label="Total Clients"
              value={isLoadingClientMetrics ? '...' : (clientMetrics?.totalClients ?? '—')}
              icon="👥"
              color="blue"
            />
            <StatCard
              label="Active Subscriptions"
              value={isLoadingBillingMetrics ? '...' : (billingMetrics?.activeSubscriptions ?? '—')}
              icon="📄"
              color="green"
            />
            <StatCard
              label="Invoices Outstanding"
              value={isLoadingBillingMetrics ? '...' : (billingMetrics?.outstandingInvoices ?? '—')}
              icon="💳"
              color="orange"
            />
          </>
        )}
        {canViewSupport && (
          <StatCard
            label="Open Tickets"
            value={isLoadingClientMetrics ? '...' : (clientMetrics?.openTickets ?? '—')}
            icon="🎫"
            color="purple"
          />
        )}
      </div>

      {/* ── Row 1: Client growth line + Ticket status donut ── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '20px' }}>
        {canViewBilling && recentClients.length > 0 && (
          <LiveLineChart
            title="📈 Client Registrations — Last 6 Months"
            data={clientGrowthData}
            series={[{ key: 'clients', name: 'New Clients', color: '#6366f1' }]}
            height={240}
          />
        )}

        {canViewSupport && ticketStatusData.length > 0 && (
          <LiveDonutChart
            title="🎫 Tickets by Status"
            data={ticketStatusData}
            centerLabel="tickets"
            height={240}
          />
        )}

        {canViewBilling && invoiceStatusData.length > 0 && (
          <LiveDonutChart
            title="🧾 Invoices by Status"
            data={invoiceStatusData}
            centerLabel="invoices"
            height={240}
          />
        )}

        {canViewSupport && ticketPriorityData.length > 0 && (
          <LiveBarChart
            title="🔥 Tickets by Priority"
            data={ticketPriorityData}
            height={240}
          />
        )}
      </div>

      {/* ── Row 2: Recent activity tables ── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '20px' }}>
        {canViewBilling && (
          <div className="table-card">
            <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 600, margin: 0 }}>🆕 Recent Clients</h3>
              <Link to="/clients" className="text-primary" style={{ fontSize: '13px', fontWeight: 500 }}>View All →</Link>
            </div>
            {recentClients.length === 0 ? (
              <div className="table-empty">No clients found.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th className="th">Client</th>
                    <th className="th">Tier</th>
                    <th className="th">Joined</th>
                  </tr>
                </thead>
                <tbody>
                  {recentClients.slice(0, 5).map((c) => (
                    <tr key={c.clientId} className="tr">
                      <td className="td">
                        <Link to={`/clients/${c.clientId}`} style={{ fontWeight: 600, color: 'var(--primary)', textDecoration: 'none' }}>
                          {c.companyName || `${c.firstName} ${c.lastName}`}
                        </Link>
                      </td>
                      <td className="td">
                        <span className={`badge tier-${c.tier === 'STANDARD' ? 'bronze' : c.tier === 'PREMIUM' ? 'silver' : 'gold'}`}>
                          {c.tier}
                        </span>
                      </td>
                      <td className="td td-secondary">{new Date(c.createdAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {canViewSupport && (
          <div className="table-card">
            <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 600, margin: 0 }}>🎫 Recent Tickets</h3>
              <Link to="/support" className="text-primary" style={{ fontSize: '13px', fontWeight: 500 }}>View All →</Link>
            </div>
            {recentTickets.length === 0 ? (
              <div className="table-empty">No open tickets.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th className="th">Ticket</th>
                    <th className="th">Priority</th>
                    <th className="th">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentTickets.map((t) => (
                    <tr key={t.ticketId} className="tr">
                      <td className="td">
                        <Link to={`/support/${t.ticketId}`} style={{ fontWeight: 600, color: 'var(--primary)', textDecoration: 'none' }}>
                          #{t.ticketId} {t.subject.substring(0, 28)}{t.subject.length > 28 ? '…' : ''}
                        </Link>
                      </td>
                      <td className="td">
                        <span style={{
                          fontSize: '11px', fontWeight: 700, padding: '2px 8px', borderRadius: '12px',
                          background: t.priority === 'CRITICAL' ? 'rgba(239,68,68,0.15)'
                                    : t.priority === 'HIGH'     ? 'rgba(245,158,11,0.15)'
                                    : t.priority === 'MEDIUM'   ? 'rgba(99,102,241,0.15)'
                                    :                             'rgba(16,185,129,0.15)',
                          color: t.priority === 'CRITICAL' ? '#f87171'
                               : t.priority === 'HIGH'     ? '#fbbf24'
                               : t.priority === 'MEDIUM'   ? '#818cf8'
                               :                             '#34d399',
                        }}>
                          {t.priority}
                        </span>
                      </td>
                      <td className="td">
                        <span className={`badge status-${t.status.toLowerCase().replace('_', '-')}`}>{t.status}</span>
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
  );
};

export default DashboardPage;
