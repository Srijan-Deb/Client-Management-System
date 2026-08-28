import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';

const navItems = [
  {
    label: 'Dashboard',
    path: '/',
    icon: '⊞',
    roles: ['admin', 'account_manager', 'support_agent'],
  },
  {
    label: 'Clients',
    path: '/clients',
    icon: '👥',
    roles: ['admin', 'account_manager'],
  },
  {
    label: 'Accounts',
    path: '/accounts',
    icon: '🏢',
    roles: ['admin', 'account_manager'],
  },
  {
    label: 'Billing',
    path: '/billing',
    icon: '💳',
    roles: ['admin', 'account_manager'],
  },
  {
    label: 'Support',
    path: '/support',
    icon: '🎫',
    roles: ['admin', 'support_agent', 'account_manager'],
  },
  {
    label: 'Users',
    path: '/users',
    icon: '👤',
    roles: ['admin'],
  },
];

const Sidebar = () => {
  const { roles, user, logout } = useAuth();

  const visibleItems = navItems.filter((item) =>
    item.roles.some((r) => roles.includes(r))
  );

  return (
    <aside className="sidebar">
      {/* Logo */}
      <div className="sidebar-logo">
        <div className="logo-icon">C</div>
        <span className="logo-text">CMS Portal</span>
      </div>

      {/* Navigation */}
      <nav className="sidebar-nav">
        <p className="nav-section-label">MAIN MENU</p>
        {visibleItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            className={({ isActive }) =>
              `nav-link${isActive ? ' nav-link-active' : ''}`
            }
          >
            <span className="nav-icon">{item.icon}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* User footer */}
      <div className="sidebar-footer">
        <div className="user-info">
          <div className="user-avatar">
            {(user?.firstName?.[0] ?? user?.username?.[0] ?? '?').toUpperCase()}
          </div>
          <div className="user-details">
            <p className="user-name">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="user-role">{roles.filter(r => ['admin','account_manager','support_agent'].includes(r)).join(', ')}</p>
          </div>
        </div>
        <button onClick={logout} className="logout-btn" title="Logout" aria-label="Logout">
          ⏻
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
