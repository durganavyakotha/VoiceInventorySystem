import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function homeForRole(role) {
  if (role === 'ADMIN') return '/admin';
  if (role === 'VENDOR') return '/vendor/inventory';
  return '/shopkeeper/inventory';
}

export default function Login() {
  const { login, isAuthenticated, user, loading } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!loading && isAuthenticated) {
    return <Navigate to={homeForRole(user?.role)} replace />;
  }

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const loggedIn = await login(email.trim(), password);
      navigate(homeForRole(loggedIn.role));
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="hero-auth page">
      <div className="hero-card">
        <p className="brand">VoiceStock</p>
        <h1 className="headline">Welcome back to your shop floor</h1>
        <p className="muted">Sign in to manage inventory by voice, barcode, or camera.</p>
        {error && <div className="alert alert-error" style={{ marginTop: '1rem' }}>{error}</div>}
        <form className="form-grid" style={{ marginTop: '1.25rem' }} onSubmit={onSubmit}>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          <button className="btn" type="submit" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
        <p style={{ marginTop: '1rem' }}>
          New here? <Link to="/register">Create an account</Link>
        </p>
        <div className="demo-hint">
          <strong>Demo credentials</strong>
          <div>Admin: <code>admin@admin.gmail.com</code> / <code>Admin@123</code></div>
          <div>Shop: <code>shop@demo.com</code> / <code>Shop@123</code></div>
          <div>Vendor: <code>vendor@demo.com</code> / <code>Vendor@123</code></div>
        </div>
      </div>
    </div>
  );
}
