import { useEffect, useState } from 'react';
import { profileService } from '../services/profileService';
import { mediaUrl } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/LanguageContext';
import LocationSelect from './LocationSelect';

const LANGUAGES = [
  { value: 'en', label: 'English' },
  { value: 'te', label: 'Telugu' },
  { value: 'hi', label: 'Hindi' },
  { value: 'ta', label: 'Tamil' },
  { value: 'kn', label: 'Kannada' },
];

export default function ProfileForm() {
  const { updateUser } = useAuth();
  const { t } = useI18n();
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    location: '',
    phone: '',
    language: 'en',
    shopName: '',
  });
  const [profileImageUrl, setProfileImageUrl] = useState(null);
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const { data } = await profileService.get();
        if (!alive) return;
        setForm({
          firstName: data.firstName || '',
          lastName: data.lastName || '',
          location: data.location || '',
          phone: data.phone || '',
          language: data.language || 'en',
          shopName: data.shopName || '',
        });
        setEmail(data.email || '');
        setProfileImageUrl(data.profileImageUrl);
        updateUser(data);
      } catch (err) {
        if (alive) setError(err.response?.data?.message || 'Failed to load profile');
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage('');
    setError('');
    try {
      const { data } = await profileService.update(form);
      updateUser(data);
      setMessage('Profile updated');
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  const onImage = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setError('');
    try {
      const { data } = await profileService.uploadImage(file);
      setProfileImageUrl(data.profileImageUrl);
      updateUser(data);
      setMessage('Photo updated');
    } catch (err) {
      setError(err.response?.data?.message || 'Image upload failed');
    }
  };

  if (loading) return <p>{t('loading')}</p>;

  return (
    <div className="panel stack" style={{ maxWidth: 640 }}>
      <h2>{t('profile')}</h2>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}
      <div className="row">
        {profileImageUrl ? (
          <img src={mediaUrl(profileImageUrl)} alt="Profile" className="thumb" style={{ width: 72, height: 72 }} />
        ) : (
          <div className="thumb" style={{ width: 72, height: 72 }} />
        )}
        <label style={{ flex: 1 }}>
          {t('uploadPhoto')}
          <input type="file" accept="image/*" onChange={onImage} />
        </label>
      </div>
      <p className="muted">{t('email')}: {email}</p>
      <form className="form-grid two" onSubmit={onSubmit}>
        <label>
          {t('firstName')}
          <input name="firstName" value={form.firstName} onChange={onChange} required />
        </label>
        <label>
          {t('lastName')}
          <input name="lastName" value={form.lastName} onChange={onChange} required />
        </label>
        <label>
          {t('phone')}
          <input name="phone" value={form.phone} onChange={onChange} placeholder="9876543210" />
        </label>
        <label>
          {t('language')}
          <select name="language" value={form.language} onChange={onChange}>
            {LANGUAGES.map((l) => (
              <option key={l.value} value={l.value}>{l.label}</option>
            ))}
          </select>
        </label>
        <label>
          {t('shopName')}
          <input name="shopName" value={form.shopName} onChange={onChange} />
        </label>
        <div style={{ gridColumn: '1 / -1' }}>
          <LocationSelect
            value={form.location}
            onChange={(loc) => setForm((prev) => ({ ...prev, location: loc }))}
            required
          />
        </div>
        <div style={{ gridColumn: '1 / -1' }}>
          <button className="btn" type="submit" disabled={saving}>
            {saving ? t('loading') : t('save')}
          </button>
        </div>
      </form>
    </div>
  );
}
