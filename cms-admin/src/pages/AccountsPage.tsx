import { useState } from 'react';
import { useAccounts } from '../hooks/useAccounts';
import { TableSkeleton } from '../components/TableSkeleton';
import { EmptyState } from '../components/EmptyState';
import type { AccountStatus } from '../types/account';

// ─── Helpers ───────────────────────────────────────────────────────────────

const STATUS_COLORS: Record<AccountStatus, string> = {
  ACTIVE: 'status-active',
  INACTIVE: 'status-inactive',
  SUSPENDED: 'status-suspended',
};

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

// ─── Main Page ─────────────────────────────────────────────────────────────

const AccountsPage = () => {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useAccounts(search || undefined, page);

  return (
    <div className="clients-page">
      {/* Header */}
      <div className="page-header">
        <div>
          <h2 className="page-heading">Accounts</h2>
          <p className="page-subheading">
            {data ? `${data.totalElements.toLocaleString()} accounts total` : 'Loading…'}
          </p>
        </div>
      </div>

      {/* Search */}
      <div className="search-bar">
        <span className="search-icon">🔍</span>
        <input
          className="search-input"
          placeholder="Search by account name or email…"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
        />
        {search && (
          <button className="search-clear" onClick={() => setSearch('')}>✕</button>
        )}
      </div>

      {/* Table */}
      <div className="table-card">
        {isLoading && <TableSkeleton rows={5} columns={5} />}

        {isError && (
          <div className="table-error">
            <p>⚠️ Failed to load accounts. Check that the gateway and account-service are running.</p>
          </div>
        )}

        {!isLoading && !isError && (
          <>
            <table className="table">
              <thead>
                <tr>
                  <th className="th">Account</th>
                  <th className="th">Email</th>
                  <th className="th">Status</th>
                  <th className="th">Created</th>
                </tr>
              </thead>
              <tbody>
                {data?.content.length === 0 && (
                  <tr>
                    <td colSpan={4} style={{ padding: 0 }}>
                      <EmptyState
                        icon="🏢"
                        title="No accounts found"
                        description={
                          search
                            ? `No accounts matched your search for "${search}".`
                            : 'Accounts are created automatically when a new client is onboarded.'
                        }
                      />
                    </td>
                  </tr>
                )}

                {data?.content.map((account) => (
                  <tr key={account.accountId} className="tr">
                    <td className="td">
                      <div className="client-cell">
                        <div className="client-avatar" style={{ background: 'var(--accent-blue)' }}>
                          {account.accountName.slice(0, 2).toUpperCase()}
                        </div>
                        <div>
                          <p className="client-name">{account.accountName}</p>
                          <p className="client-email">ID #{account.accountId}</p>
                        </div>
                      </div>
                    </td>
                    <td className="td td-secondary">{account.email}</td>
                    <td className="td">
                      <span className={`badge ${STATUS_COLORS[account.status] ?? 'status-inactive'}`}>
                        {account.status}
                      </span>
                    </td>
                    <td className="td td-secondary">{formatDate(account.createdAt)}</td>
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
    </div>
  );
};

export default AccountsPage;
