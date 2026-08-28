import Sidebar from './Sidebar';
import Topbar from './Topbar';
import { Outlet } from 'react-router-dom';

const AppShell = () => {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-area">
        <Topbar />
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AppShell;
