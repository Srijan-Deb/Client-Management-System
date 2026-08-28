import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import { useHasRole } from '../auth/RoleGate';
import { useClientMetrics, useBillingMetrics } from '../hooks/useDashboard';
import { useClients } from '../hooks/useClients';
import { useTickets } from '../hooks/useTickets';
import { useInvoices } from '../hooks/useBilling';
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

// ── Helpers to build chart data from live data ────────────────────────────────
function buildClientGrowthData(clients: any[]) {
  // Group clients by month of creation (last 6 months)
  const now = new Date();
  const months: { label: string; date: Date }[] = [];
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    months.push({
      label: d.toLocaleString('default', { month: 'short', year: '2-digit' }),
      date: d,
    });
  }
  return months.map((m) => ({
    label: m.label,
    clients: clients.filter((c) => {
      const created = new Date(c.createdAt);
      return created.getFullYear() === m.date.getFullYear() &&
             created.getMonth() === m.date.getMonth();
    }).length,
  }));
}

function buildTicketStatusData(tickets: any[]) {
  const counts: Record<string, number> = {};
  tickets.forEach((t) => { counts[t.status] = (counts[t.status] ?? 0) + 1; });
  const COLORS: Record<string, string> = {
    OPEN: '#6366f1',
    IN_PROGRESS: '#f59e0b',
    RESOLVED: '#10b981',
    CLOSED: '#94a3b8',
    REOPENED: '#ef4444',
  };
  return Object.entries(counts).map(([status, value]) => ({
    name: status,
    value,
    color: COLORS[status] ?? '#8b91b0',
  }));
}

function buildInvoiceStatusData(invoices: any[]) {
  const counts: Record<string, number> = {};
  invoices.forEach((inv) => { counts[inv.status] = (counts[inv.status] ?? 0) + 1; });
  const COLORS: Record<string, string> = {
    PAID: '#10b981',
    PENDING: '#f59e0b',
    OVERDUE: '#ef4444',
    CANCELLED: '#94a3b8',
  };
  return Object.entries(counts).map(([status, value]) => ({
    name: status,
    value,
    color: COLORS[status] ?? '#8b91b0',
  }));
}

function buildTicketPriorityBarData(tickets: any[]) {
  const priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  const COLORS: Record<string, string> = {
    LOW: '#10b981',
    MEDIUM: '#6366f1',
    HIGH: '#f59e0b',
    CRITICAL: '#ef4444',
  };
  const counts: Record<string, number> = {};
  tickets.forEach((t) => { counts[t.priority] = (counts[t.priority] ?? 0) + 1; });
  return priorities
    .filter((p) => counts[p] !== undefined)
    .map((p) => ({ label: p, value: counts[p] ?? 0, color: COLORS[p] }));
}

// ── Main Dashboard ────────────────────────────────────────────────────────────
const DashboardPage = () => {
  const { user } = useAuth();
  const isAdmin = useHasRole(['admin']);
  const isAccountManager = useHasRole(['account_manager']);
  const isSupportAgent = useHasRole(['support_agent']);

  const canViewBilling = isAdmin || isAccountManager;
  const canViewSupport = isAdmin || isSupportAgent;

  const { data: clientMetrics, isLoading: isLoadingClientMetrics } = useClientMetrics();
  const { data: billingMetrics, isLoading: isLoadingBillingMetrics } = useBillingMetrics();

  const { data: recentClientsData } = useClients(undefined, 0, 20);
  const recentClients = recentClientsData?.content ?? [];

  const { data: recentTicketsData } = useTickets(undefined);
  const allTickets = recentTicketsData?.content ?? [];
  const recentTickets = allTickets.slice(0, 5);

  const { data: invoicesData } = useInvoices();
  // API may return a paginated Page<Invoice> OR a plain Invoice[] depending on backend version
  const invoices = Array.isArray(invoicesData)
    ? invoicesData
    : (invoicesData as any)?.content ?? [];

  // ── Derived chart data ──────────────────────────────────────────────────────
  const clientGrowthData = useMemo(() => buildClientGrowthData(recentClients), [recentClients]);
  const ticketStatusData = useMemo(() => buildTicketStatusData(allTickets), [allTickets]);
  const invoiceStatusData = useMemo(() => buildInvoiceStatusData(invoices), [invoices]);
  const ticketPriorityData = useMemo(() => buildTicketPriorityBarData(allTickets), [allTickets]);

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';

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
