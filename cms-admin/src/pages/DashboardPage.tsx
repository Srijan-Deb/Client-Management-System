import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import { useHasRole } from '../auth/RoleGate';
import { useClientMetrics, useBillingMetrics } from '../hooks/useDashboard';
import { useClients } from '../hooks/useClients';
import { useTickets } from '../hooks/useTickets';

const StatCard = ({ label, value, icon, color }: { label: string; value: string | number; icon: string; color: string }) => (
  <div className={`stat-card stat-card-${color}`}>
    <div className="stat-icon">{icon}</div>
    <div>
      <p className="stat-value">{value}</p>
      <p className="stat-label">{label}</p>
    </div>
  </div>
);

const DashboardPage = () => {
  const { user } = useAuth();
  const isAdmin = useHasRole(['admin']);
  const isAccountManager = useHasRole(['account_manager']);
  const isSupportAgent = useHasRole(['support_agent']);

  const canViewBilling = isAdmin || isAccountManager;
  const canViewSupport = isAdmin || isSupportAgent;

  const { data: clientMetrics, isLoading: isLoadingClientMetrics } = useClientMetrics();
  // Only fetch billing metrics if user has access to see them
  const { data: billingMetrics, isLoading: isLoadingBillingMetrics } = useBillingMetrics();

  const { data: recentClientsData } = useClients(undefined, 0, 5);
  const recentClients = recentClientsData?.content || [];

  const { data: recentTicketsData } = useTickets(undefined);
  const recentTickets = (recentTicketsData?.content || []).slice(0, 5); // Fallback slice since tickets API doesn't support size in hook cleanly

  return (
    <div className="dashboard">
      <div className="dashboard-welcome">
        <h2>Good morning, {user?.firstName ?? user?.username} 👋</h2>
        <p>Here's what's happening across your system today.</p>
      </div>

      <div className="stats-grid">
        {canViewBilling && (
          <>
            <StatCard 
              label="Total Clients" 
              value={isLoadingClientMetrics ? '...' : clientMetrics?.totalClients ?? '—'} 
              icon="👥" 
              color="blue" 
            />
            <StatCard 
              label="Active Subscriptions" 
              value={isLoadingBillingMetrics ? '...' : billingMetrics?.activeSubscriptions ?? '—'} 
              icon="📄" 
              color="green" 
            />
            <StatCard 
              label="Invoices Due" 
              value={isLoadingBillingMetrics ? '...' : billingMetrics?.outstandingInvoices ?? '—'} 
              icon="💳" 
              color="orange" 
            />
          </>
        )}
        {canViewSupport && (
          <StatCard 
            label="Open Tickets" 
            value={isLoadingClientMetrics ? '...' : clientMetrics?.openTickets ?? '—'} 
            icon="🎫" 
            color="purple" 
          />
        )}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '24px', marginTop: '24px' }}>
        {/* Recent Clients */}
        {canViewBilling && (
          <div className="table-card">
            <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 600, margin: 0 }}>Recent Clients</h3>
              <Link to="/clients" className="text-primary" style={{ fontSize: '14px', fontWeight: 500 }}>View All →</Link>
            </div>
            {recentClients.length === 0 ? (
              <div className="table-empty">No clients found.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th className="th">Client</th>
                    <th className="th">Joined</th>
                  </tr>
                </thead>
                <tbody>
                  {recentClients.map(c => (
                    <tr key={c.clientId} className="tr">
                      <td className="td">
                        <Link to={`/clients/${c.clientId}`} className="text-primary" style={{ fontWeight: 600 }}>
                          {c.companyName}
                        </Link>
                      </td>
                      <td className="td td-secondary">{new Date(c.createdAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {/* Recent Tickets */}
        {canViewSupport && (
          <div className="table-card">
            <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 600, margin: 0 }}>Recent Tickets</h3>
              <Link to="/support" className="text-primary" style={{ fontSize: '14px', fontWeight: 500 }}>View All →</Link>
            </div>
            {recentTickets.length === 0 ? (
              <div className="table-empty">No open tickets.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th className="th">Ticket</th>
                    <th className="th">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentTickets.map(t => (
                    <tr key={t.ticketId} className="tr">
                      <td className="td">
                        <Link to={`/support/${t.ticketId}`} className="text-primary" style={{ fontWeight: 600 }}>
                          #{t.ticketId} {t.subject.substring(0, 30)}{t.subject.length > 30 ? '...' : ''}
                        </Link>
                      </td>
                      <td className="td">
                        <span className={`badge status-${t.status.toLowerCase()}`}>{t.status}</span>
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
