import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

export default function DashboardLayout({ basePath }) {
  return (
    <div className="app-shell">
      <Navbar basePath={basePath} />
      <main className="main-content">
        <div className="container-wide page">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
