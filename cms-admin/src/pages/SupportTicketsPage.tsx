import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTickets } from '../hooks/useTickets';
import { TableSkeleton } from '../components/TableSkeleton';
import { EmptyState } from '../components/EmptyState';

const SupportTicketsPage = () => {
  const [filter, setFilter] = useState<string>('ALL');
  
  // Pass undefined to get all tickets (since backend is updated to support it)
  const { data, isLoading, isError } = useTickets(undefined);
  const tickets = data?.content || [];

  const filteredTickets = filter === 'ALL' 
    ? tickets 
    : tickets.filter(t => t.status === filter);

  const statuses = ['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED'];

  return (
    <div className="clients-page">
      <div className="page-header">
        <div>
          <h2 className="page-heading">Support Tickets</h2>
          <p className="page-subheading">Manage and respond to client support requests</p>
        </div>
      </div>

      <div className="table-card">
        <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', gap: '12px', alignItems: 'center' }}>
          <span style={{ fontSize: '14px', fontWeight: 500 }}>Filter:</span>
          {statuses.map(s => (
            <button
              key={s}
              onClick={() => setFilter(s)}
              className={`badge ${filter === s ? 'status-active' : ''}`}
              style={{ 
                cursor: 'pointer', 
                border: filter === s ? '1px solid var(--primary)' : '1px solid var(--border)',
                background: filter === s ? 'var(--primary-muted)' : 'transparent',
                color: filter === s ? 'var(--primary)' : 'var(--text-secondary)'
              }}
            >
              {s}
            </button>
          ))}
        </div>

        {isLoading && <TableSkeleton rows={5} columns={6} />}
        
        {!isLoading && isError && (
          <div className="table-error"><p>Failed to load tickets.</p></div>
        )}

        {!isLoading && !isError && (
          <table className="table">
            <thead>
              <tr>
                <th className="th">Ticket ID</th>
                <th className="th">Client ID</th>
                <th className="th">Subject</th>
                <th className="th">Status</th>
                <th className="th">Priority</th>
                <th className="th">Created</th>
              </tr>
            </thead>
            <tbody>
              {filteredTickets.length === 0 && (
                <tr>
                  <td colSpan={6} style={{ padding: 0 }}>
                    <EmptyState
                      icon="🎫"
                      title="No tickets found"
                      description={filter === 'ALL' ? "There are no support tickets available." : `There are no tickets matching the filter "${filter}".`}
                    />
                  </td>
                </tr>
              )}
              {filteredTickets.map(t => (
                <tr key={t.ticketId} className="tr">
                  <td className="td" style={{ fontWeight: 600 }}>
                    <Link to={`/support/${t.ticketId}`} className="text-primary">
                      #{t.ticketId}
                    </Link>
                  </td>
                  <td className="td td-secondary">{t.clientId}</td>
                  <td className="td">{t.subject}</td>
                  <td className="td">
                    <span className={`badge status-${t.status.toLowerCase()}`}>{t.status}</span>
                  </td>
                  <td className="td td-secondary">{t.priority}</td>
                  <td className="td td-secondary">{new Date(t.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default SupportTicketsPage;
