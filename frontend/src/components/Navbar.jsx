import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/LanguageContext';
import { mediaUrl } from '../services/api';

const LINK_KEYS = {
  ADMIN: [
    { to: '/admin', labelKey: 'dashboard', end: true },
    { to: '/admin/vendors', labelKey: 'vendors' },
    { to: '/admin/shopkeepers', labelKey: 'shopkeepers' },
    { to: '/admin/queries', labelKey: 'queries' },
  ],
  SHOPKEEPER: [
    { to: '/shopkeeper/inventory', labelKey: 'listItems' },
    { to: '/shopkeeper/vendors', labelKey: 'vendors' },
    { to: '/shopkeeper/sold', labelKey: 'soldItems' },
    { to: '/shopkeeper/alerts', labelKey: 'alerts' },
    { to: '/shopkeeper/add', labelKey: 'addItems' },
    { to: '/shopkeeper/contact', labelKey: 'contactUs' },
    { to: '/shopkeeper/chat', labelKey: 'chattingHistory' },
    { to: '/shopkeeper/orders', labelKey: 'orders' },
  ],
  VENDOR: [
    { to: '/vendor/vendors', labelKey: 'otherVendors' },
    { to: '/vendor/orders', labelKey: 'orders' },
    { to: '/vendor/contact', labelKey: 'contactUs' },
    { to: '/vendor/alerts', labelKey: 'alerts' },
    { to: '/vendor/inventory', labelKey: 'inventory' },
    { to: '/vendor/add', labelKey: 'addItems' },
    { to: '/vendor/chat', labelKey: 'chattingHistory' },
  ],
};

export default function Navbar({ basePath }) {
  const { user, logout } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const role = user?.role || 'SHOPKEEPER';
  const links = LINK_KEYS[role] || [];
  const profileTo = `${basePath}/profile`;
  const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ') || t('profile');
  const photo = mediaUrl(user?.profileImageUrl);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="navbar">
      <div className="container-wide navbar-inner">
        <NavLink to={basePath} className="nav-brand">
          {t('brand')}
        </NavLink>
        <nav className="nav-links">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) => (isActive ? 'active' : undefined)}
            >
              {t(link.labelKey)}
            </NavLink>
          ))}
        </nav>
        <div className="nav-profile">
          <NavLink
            to={profileTo}
            className={({ isActive }) =>
              `nav-user ${isActive ? 'active' : ''}`
            }
          >
            {photo ? (
              <img src={photo} alt="" className="nav-avatar" />
            ) : (
              <span className="nav-avatar nav-avatar-fallback">
                {(user?.firstName || '?')[0]}
              </span>
            )}
            <span className="nav-user-name">{fullName}</span>
          </NavLink>
          <button type="button" className="btn btn-ghost btn-sm" onClick={handleLogout}>
            {t('logout')}
          </button>
        </div>
      </div>
    </header>
  );
}
