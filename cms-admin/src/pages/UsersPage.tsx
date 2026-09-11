import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import keycloak from '../lib/keycloak';
import { useHasRole } from '../auth/RoleGate';
import { TableSkeleton } from '../components/TableSkeleton';
import { EmptyState } from '../components/EmptyState';

// ─── Types ─────────────────────────────────────────────────────────────────

interface KeycloakUser {
  id: string;
  username: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  enabled: boolean;
  createdTimestamp: number;
  realmRoles?: string[];
}

// ─── API ───────────────────────────────────────────────────────────────────

const KEYCLOAK_BASE = import.meta.env.VITE_KEYCLOAK_URL as string;
const REALM = import.meta.env.VITE_KEYCLOAK_REALM as string;
const ADMIN_BASE = `${KEYCLOAK_BASE}/admin/realms/${REALM}`;

async function fetchUsers(search: string, first: number, max: number): Promise<KeycloakUser[]> {
  const params: Record<string, string | number> = { first, max };
  if (search) params.search = search;

  const { data } = await axios.get<KeycloakUser[]>(`${ADMIN_BASE}/users`, {
    params,
    headers: { Authorization: `Bearer ${keycloak.token}` },
  });
  return data;
}

async function fetchUserCount(search: string): Promise<number> {
  const params: Record<string, string> = {};
  if (search) params.search = search;
  const { data } = await axios.get<number>(`${ADMIN_BASE}/users/count`, {
    params,
    headers: { Authorization: `Bearer ${keycloak.token}` },
  });
  return data;
}

// ─── Role badge helper ─────────────────────────────────────────────────────

const ROLE_BADGE: Record<string, string> = {
  admin: 'tier-gold',
  account_manager: 'tier-silver',
  support_agent: 'tier-bronze',
  default_roles_cms: '',
  offline_access: '',
  uma_authorization: '',
};

const KNOWN_ROLES = ['admin', 'account_manager', 'support_agent'];

function formatDate(ts: number) {
  return new Date(ts).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

// ─── Main Page ─────────────────────────────────────────────────────────────

const UsersPage = () => {
  const isAdmin = useHasRole(['admin']);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;

  const { data: users = [], isLoading, isError, error } = useQuery({
    queryKey: ['keycloak-users', search, page],
    queryFn: () => fetchUsers(search, page * PAGE_SIZE, PAGE_SIZE),
  });

  const { data: totalCount = 0 } = useQuery({
    queryKey: ['keycloak-users-count', search],
    queryFn: () => fetchUserCount(search),
  });

  const totalPages = Math.ceil(totalCount / PAGE_SIZE);
  const isForbidden = (error as any)?.response?.status === 403;

  return (
    <div className="clients-page">
      {/* Header */}
      <div className="page-header">
        <div>
          <h2 className="page-heading">User Management</h2>
          <p className="page-subheading">
            {isLoading ? 'Loading…' : `${totalCount.toLocaleString()} users in Keycloak realm`}
          </p>
        </div>
        <div className="page-header-meta">
          <span className="badge tier-bronze" style={{ fontSize: '0.75rem' }}>
            🔑 Keycloak · {REALM}
          </span>
        </div>
      </div>

      {/* Access warning for non-admins */}
      {!isAdmin && (
        <div className="table-error" style={{ marginBottom: '1rem' }}>
          <p>⚠️ You need the <strong>admin</strong> role to view full user details.</p>
        </div>
      )}

      {/* Search */}
      <div className="search-bar">
        <span className="search-icon">🔍</span>
        <input
          className="search-input"
          placeholder="Search by username, name, or email…"
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
            {isForbidden ? (
              <p>🔒 Access denied. Your Keycloak account needs the <strong>realm-management → view-users</strong> role to list users.</p>
            ) : (
              <p>⚠️ Failed to load users from Keycloak. Make sure Keycloak is running at {KEYCLOAK_BASE}.</p>
            )}
          </div>
        )}

        {!isLoading && !isError && (
          <>
            <table className="table">
              <thead>
                <tr>
                  <th className="th">User</th>
                  <th className="th">Email</th>
                  <th className="th">Roles</th>
                  <th className="th">Status</th>
                  <th className="th">Created</th>
                </tr>
              </thead>
              <tbody>
                {users.length === 0 && (
                  <tr>
                    <td colSpan={5} style={{ padding: 0 }}>
                      <EmptyState
                        icon="👤"
                        title="No users found"
                        description={search ? `No users matched "${search}".` : 'No users exist in this Keycloak realm yet.'}
                      />
                    </td>
                  </tr>
                )}

                {users.map((user) => {
                  const initials = [user.firstName?.[0], user.lastName?.[0]]
                    .filter(Boolean)
                    .join('')
                    .toUpperCase() || user.username.slice(0, 2).toUpperCase();

                  const displayRoles = (user.realmRoles ?? []).filter((r) => KNOWN_ROLES.includes(r));

                  return (
                    <tr key={user.id} className="tr">
                      <td className="td">
                        <div className="client-cell">
                          <div
                            className="client-avatar"
                            style={{ background: user.enabled ? 'var(--accent-purple)' : 'var(--text-muted)' }}
                          >
                            {initials}
                          </div>
                          <div>
                            <p className="client-name">
                              {user.firstName && user.lastName
                                ? `${user.firstName} ${user.lastName}`
                                : user.username}
                            </p>
                            <p className="client-email">@{user.username}</p>
                          </div>
                        </div>
                      </td>
                      <td className="td td-secondary">{user.email ?? '—'}</td>
                      <td className="td">
                        <div style={{ display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
                          {displayRoles.length === 0 ? (
                            <span className="badge status-inactive">user</span>
                          ) : (
                            displayRoles.map((role) => (
                              <span key={role} className={`badge ${ROLE_BADGE[role] ?? 'status-pending'}`}>
                                {role.replace('_', ' ')}
                              </span>
                            ))
                          )}
                        </div>
                      </td>
                      <td className="td">
                        <span className={`badge ${user.enabled ? 'status-active' : 'status-inactive'}`}>
                          {user.enabled ? 'Active' : 'Disabled'}
                        </span>
                      </td>
                      <td className="td td-secondary">{formatDate(user.createdTimestamp)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="pagination">
                <button
                  className="btn btn-ghost btn-sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  ← Prev
                </button>
                <span className="pagination-info">
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  className="btn btn-ghost btn-sm"
                  disabled={page >= totalPages - 1}
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

export default UsersPage;
