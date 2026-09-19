import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const LINKS = {
  ADMIN: [
    { to: '/admin', label: 'Dashboard', end: true },
    { to: '/admin/vendors', label: 'Vendors' },
    { to: '/admin/shopkeepers', label: 'Shopkeepers' },
    { to: '/admin/queries', label: 'Queries' },
  ],
  SHOPKEEPER: [
    { to: '/shopkeeper/inventory', label: 'List Items' },
    { to: '/shopkeeper/vendors', label: 'Vendors' },
    { to: '/shopkeeper/sold', label: 'Sold Items' },
    { to: '/shopkeeper/alerts', label: 'Alerts' },
    { to: '/shopkeeper/add', label: 'Add Items' },
    { to: '/shopkeeper/contact', label: 'Contact Us' },
    { to: '/shopkeeper/chat', label: 'Chatting History' },
    { to: '/shopkeeper/orders', label: 'Orders' },
  ],
  VENDOR: [
    { to: '/vendor/vendors', label: 'Other Vendors' },
    { to: '/vendor/orders', label: 'Orders' },
    { to: '/vendor/contact', label: 'Contact Us' },
    { to: '/vendor/alerts', label: 'Alerts' },
    { to: '/vendor/inventory', label: 'Inventory' },
  ],
};

export default function Navbar({ basePath }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const role = user?.role || 'SHOPKEEPER';
  const links = LINKS[role] || [];
  const profileTo = `${basePath}/profile`;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="navbar">
      <div className="container-wide navbar-inner">
        <NavLink to={basePath} className="nav-brand">
          VoiceStock
        </NavLink>
        <nav className="nav-links">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) => (isActive ? 'active' : undefined)}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="nav-profile">
          <NavLink
            to={profileTo}
            className={({ isActive }) => (isActive ? 'active' : undefined)}
          >
            Profile
          </NavLink>
          <button type="button" className="btn btn-ghost btn-sm" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </div>
    </header>
  );
}
