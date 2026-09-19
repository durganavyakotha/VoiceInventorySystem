import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Navigate } from 'react-router-dom';

function homeForRole(role) {
  if (role === 'ADMIN') return '/admin';
  if (role === 'VENDOR') return '/vendor/inventory';
  return '/shopkeeper/inventory';
}

export default function LandingPage() {
  const { isAuthenticated, user, loading } = useAuth();

  if (!loading && isAuthenticated) {
    return <Navigate to={homeForRole(user?.role)} replace />;
  }

  return (
    <div className="hero-auth page">
      <div className="hero-card" style={{ width: 'min(520px, 100%)', textAlign: 'center' }}>
        <p className="brand">VoiceStock</p>
        <h1 className="headline">Speak your stock into order</h1>
        <p className="muted" style={{ marginBottom: '1.5rem' }}>
          Voice-first inventory for shops and vendors — track stock, find nearby suppliers, and restock faster.
        </p>
        <div className="row" style={{ justifyContent: 'center' }}>
          <Link className="btn" to="/login">
            Sign in
          </Link>
          <Link className="btn btn-secondary" to="/register">
            Create account
          </Link>
        </div>
      </div>
    </div>
  );
}
