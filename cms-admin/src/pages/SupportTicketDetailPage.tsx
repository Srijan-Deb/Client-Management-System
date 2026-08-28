import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { 
  useTicket, 
  useAssignTicket, 
  useResolveTicket, 
  useReopenTicket, 
  useCloseTicket, 
  useAddTicketComment 
} from '../hooks/useTickets';
import { DetailSkeleton } from '../components/DetailSkeleton';
import { useHasRole } from '../auth/RoleGate';

const SupportTicketDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const ticketId = Number(id);

  const { data: ticket, isLoading, isError } = useTicket(ticketId);
  const assignMutation = useAssignTicket();
  const resolveMutation = useResolveTicket();
  const reopenMutation = useReopenTicket();
  const closeMutation = useCloseTicket();
  const commentMutation = useAddTicketComment();

  const [commentText, setCommentText] = useState('');

  const canManage = useHasRole(['admin', 'support_agent']);
  // Client role logic if we had client auth here, but usually admin UI handles agent side.

  if (isLoading) return <DetailSkeleton />;
  if (isError || !ticket) return <div className="table-error"><p>Failed to load ticket details.</p></div>;

  const handleAssign = () => {
    // Hardcode agentId to 1 for MVP or show an agent selector
    const agentId = 1; 
    assignMutation.mutate({ id: ticketId, agentId });
  };

  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim()) return;
    try {
      await commentMutation.mutateAsync({ id: ticketId, body: { commentText } });
      setCommentText('');
    } catch (err) {
      console.error('Failed to add comment', err);
    }
  };

  return (
    <div className="clients-page">
      <div className="page-header" style={{ alignItems: 'center' }}>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          <Link to="/support" className="btn btn-ghost btn-icon" style={{ padding: '8px 12px' }}>
            ←
          </Link>
          <div>
            <h2 className="page-heading">
              #{ticket.ticketId} - {ticket.subject}
            </h2>
            <p className="page-subheading">
              Client #{ticket.clientId} {ticket.accountId ? `· Account #${ticket.accountId}` : ''} · Created {new Date(ticket.createdAt).toLocaleString()}
            </p>
          </div>
        </div>
        
        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '8px' }}>
          {canManage && ticket.status === 'OPEN' && (
            <button className="btn btn-primary btn-sm" onClick={handleAssign} disabled={assignMutation.isPending}>
              {assignMutation.isPending ? 'Assigning...' : 'Assign to Me'}
            </button>
          )}
          {canManage && (ticket.status === 'IN_PROGRESS' || ticket.status === 'OPEN' || ticket.status === 'REOPENED') && (
            <button className="btn btn-primary btn-sm" onClick={() => resolveMutation.mutate(ticketId)} disabled={resolveMutation.isPending}>
              Resolve
            </button>
          )}
          {(ticket.status === 'RESOLVED' || ticket.status === 'CLOSED') && (
            <button className="btn btn-ghost btn-sm" onClick={() => reopenMutation.mutate(ticketId)} disabled={reopenMutation.isPending}>
              Reopen
            </button>
          )}
          {canManage && ticket.status !== 'CLOSED' && (
            <button className="btn btn-ghost btn-sm" onClick={() => closeMutation.mutate(ticketId)} disabled={closeMutation.isPending}>
              Close Ticket
            </button>
          )}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '24px', marginTop: '24px' }}>
        {/* Left Column: Description & Comments */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <div className="table-card" style={{ padding: '24px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '16px' }}>Description</h3>
            <p style={{ whiteSpace: 'pre-wrap', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
              {ticket.description}
            </p>
          </div>

          <div className="table-card" style={{ padding: '24px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '16px' }}>Comments</h3>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '24px' }}>
              {ticket.comments.length === 0 && (
                <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>No comments yet.</p>
              )}
              {ticket.comments.map(c => (
                <div key={c.commentId} style={{ 
                  background: 'var(--bg-elevated)', 
                  padding: '16px', 
                  borderRadius: 'var(--radius)',
                  border: '1px solid var(--border)'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <span style={{ fontWeight: 600, fontSize: '14px' }}>Author #{c.authorId}</span>
                    <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                      {new Date(c.createdAt).toLocaleString()}
                    </span>
                  </div>
                  <p style={{ fontSize: '14px', color: 'var(--text-primary)', whiteSpace: 'pre-wrap' }}>
                    {c.commentText}
                  </p>
                </div>
              ))}
            </div>

            <form onSubmit={handleCommentSubmit}>
              <textarea
                className="form-input"
                placeholder="Write a comment..."
                rows={3}
                value={commentText}
                onChange={e => setCommentText(e.target.value)}
                disabled={commentMutation.isPending}
                style={{ marginBottom: '12px' }}
                required
              />
              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <button type="submit" className="btn btn-primary btn-sm" disabled={commentMutation.isPending}>
                  {commentMutation.isPending ? 'Posting...' : 'Post Comment'}
                </button>
              </div>
            </form>
          </div>
        </div>

        {/* Right Column: Metadata */}
        <div>
          <div className="table-card" style={{ padding: '24px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '16px' }}>Details</h3>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <p className="form-label" style={{ marginBottom: '4px' }}>Status</p>
                <span className={`badge status-${ticket.status.toLowerCase()}`}>{ticket.status}</span>
              </div>
              
              <div>
                <p className="form-label" style={{ marginBottom: '4px' }}>Priority</p>
                <span className="badge" style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border)' }}>
                  {ticket.priority}
                </span>
              </div>

              <div>
                <p className="form-label" style={{ marginBottom: '4px' }}>Category</p>
                <p style={{ fontSize: '14px' }}>{ticket.category}</p>
              </div>

              <div>
                <p className="form-label" style={{ marginBottom: '4px' }}>Assigned To</p>
                <p style={{ fontSize: '14px' }}>{ticket.assignedTo ? `Agent #${ticket.assignedTo}` : 'Unassigned'}</p>
              </div>

              {ticket.resolvedAt && (
                <div>
                  <p className="form-label" style={{ marginBottom: '4px' }}>Resolved At</p>
                  <p style={{ fontSize: '14px' }}>{new Date(ticket.resolvedAt).toLocaleString()}</p>
                </div>
              )}
              {ticket.closedAt && (
                <div>
                  <p className="form-label" style={{ marginBottom: '4px' }}>Closed At</p>
                  <p style={{ fontSize: '14px' }}>{new Date(ticket.closedAt).toLocaleString()}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SupportTicketDetailPage;
