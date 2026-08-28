import { useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import { useTheme } from '../lib/ThemeProvider';

const pageTitles: Record<string, string> = {
  '/': 'Dashboard',
  '/clients': 'Clients',
  '/accounts': 'Accounts',
  '/billing': 'Billing',
  '/support': 'Support Tickets',
  '/users': 'User Management',
};

const Topbar = () => {
  const { pathname } = useLocation();
  const { user } = useAuth();
  const { theme, toggleTheme } = useTheme();

  const title = pageTitles[pathname] ?? 'CMS Portal';

  return (
    <header className="topbar">
      <div className="topbar-left">
        <h1 className="topbar-title">{title}</h1>
      </div>
      <div className="topbar-right">
        <div className="topbar-greeting">
          Welcome back, <strong>{user?.firstName ?? user?.username ?? 'User'}</strong>
        </div>

        {/* Theme toggle */}
        <button
          onClick={toggleTheme}
          className="theme-toggle"
          title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
          aria-label="Toggle theme"
        >
          {theme === 'dark' ? '☀️' : '🌙'}
        </button>

        <div className="topbar-avatar">
          {(user?.firstName?.[0] ?? user?.username?.[0] ?? '?').toUpperCase()}
        </div>
      </div>
    </header>
  );
};

export default Topbar;
