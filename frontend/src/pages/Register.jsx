import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LocationSelect from '../components/LocationSelect';
import { t as translate } from '../i18n/translations';

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
    location: 'Markapur',
    phone: '',
    language: 'en',
    role: 'SHOPKEEPER',
    shopName: '',
  });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const tr = (key) => translate(key, form.language);

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
        <h1 className="headline">{tr('createAccount')}</h1>
        {error && <div className="alert alert-error" style={{ marginTop: '1rem' }}>{error}</div>}
        <form className="form-grid two" style={{ marginTop: '1.25rem' }} onSubmit={onSubmit}>
          <label>
            {tr('firstName')}
            <input name="firstName" value={form.firstName} onChange={onChange} required />
          </label>
          <label>
            {tr('lastName')}
            <input name="lastName" value={form.lastName} onChange={onChange} required />
          </label>
          <label style={{ gridColumn: '1 / -1' }}>
            {tr('email')}
            <input type="email" name="email" value={form.email} onChange={onChange} required />
          </label>
          <label style={{ gridColumn: '1 / -1' }}>
            {tr('password')}
            <input type="password" name="password" value={form.password} onChange={onChange} minLength={6} required />
          </label>
          <label>
            {tr('phone')}
            <input name="phone" value={form.phone} onChange={onChange} required placeholder="9876543210" />
          </label>
          <label>
            {tr('language')}
            <select name="language" value={form.language} onChange={onChange}>
              {LANGUAGES.map((l) => (
                <option key={l.value} value={l.value}>{l.label}</option>
              ))}
            </select>
          </label>
          <label>
            {tr('role')}
            <select name="role" value={form.role} onChange={onChange}>
              <option value="SHOPKEEPER">{tr('shopkeeper')}</option>
              <option value="VENDOR">{tr('vendor')}</option>
            </select>
          </label>
          <label>
            {tr('shopName')}
            <input name="shopName" value={form.shopName} onChange={onChange} required />
          </label>
          <div style={{ gridColumn: '1 / -1' }}>
            <LocationSelect
              value={form.location}
              onChange={(loc) => setForm((prev) => ({ ...prev, location: loc }))}
              required
            />
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            <button className="btn" type="submit" disabled={submitting}>
              {submitting ? '…' : tr('createAccount')}
            </button>
          </div>
        </form>
        <p style={{ marginTop: '1rem' }}>
          <Link to="/login">{tr('signIn')}</Link>
        </p>
      </div>
    </div>
  );
}
