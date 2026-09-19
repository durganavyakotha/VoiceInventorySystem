import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const LANGUAGES = [
  { value: 'en', label: 'English' },
  { value: 'te', label: 'Telugu' },
  { value: 'hi', label: 'Hindi' },
  { value: 'ta', label: 'Tamil' },
  { value: 'kn', label: 'Kannada' },
];

function homeForRole(role) {
  if (role === 'ADMIN') return '/admin';
  if (role === 'VENDOR') return '/vendor/inventory';
  return '/shopkeeper/inventory';
}

export default function Register() {
  const { register, isAuthenticated, user, loading } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    location: '',
    language: 'en',
    role: 'SHOPKEEPER',
    shopName: '',
  });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!loading && isAuthenticated) {
    return <Navigate to={homeForRole(user?.role)} replace />;
  }

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const registered = await register(form);
      navigate(homeForRole(registered.role));
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="hero-auth page">
      <div className="hero-card" style={{ width: 'min(560px, 100%)' }}>
        <p className="brand">VoiceStock</p>
        <h1 className="headline">Open your digital shop shelf</h1>
        <p className="muted">Register as a shopkeeper or vendor to start tracking stock.</p>
        {error && <div className="alert alert-error" style={{ marginTop: '1rem' }}>{error}</div>}
        <form className="form-grid two" style={{ marginTop: '1.25rem' }} onSubmit={onSubmit}>
          <label>
            First name
            <input name="firstName" value={form.firstName} onChange={onChange} required />
          </label>
          <label>
            Last name
            <input name="lastName" value={form.lastName} onChange={onChange} required />
          </label>
          <label style={{ gridColumn: '1 / -1' }}>
            Email
            <input type="email" name="email" value={form.email} onChange={onChange} required />
          </label>
          <label style={{ gridColumn: '1 / -1' }}>
            Password
            <input type="password" name="password" value={form.password} onChange={onChange} minLength={6} required />
          </label>
          <label>
            Location
            <input name="location" value={form.location} onChange={onChange} />
          </label>
          <label>
            Language
            <select name="language" value={form.language} onChange={onChange}>
              {LANGUAGES.map((l) => (
                <option key={l.value} value={l.value}>{l.label}</option>
              ))}
            </select>
          </label>
          <label>
            Role
            <select name="role" value={form.role} onChange={onChange}>
              <option value="SHOPKEEPER">Shopkeeper</option>
              <option value="VENDOR">Vendor</option>
            </select>
          </label>
          <label>
            Shop name
            <input name="shopName" value={form.shopName} onChange={onChange} />
          </label>
          <div style={{ gridColumn: '1 / -1' }}>
            <p className="muted" style={{ fontSize: '0.9rem', marginTop: 0 }}>
              Admin accounts require a special email ending in <code>@admin.gmail.com</code>.
            </p>
            <button className="btn" type="submit" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create account'}
            </button>
          </div>
        </form>
        <p style={{ marginTop: '1rem' }}>
          Already registered? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
