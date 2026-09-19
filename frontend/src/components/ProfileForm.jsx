import { useEffect, useState } from 'react';
import { profileService } from '../services/profileService';
import { mediaUrl } from '../services/api';
import { useAuth } from '../context/AuthContext';

const LANGUAGES = [
  { value: 'en', label: 'English' },
  { value: 'te', label: 'Telugu' },
  { value: 'hi', label: 'Hindi' },
  { value: 'ta', label: 'Tamil' },
  { value: 'kn', label: 'Kannada' },
];

export default function ProfileForm() {
  const { updateUser } = useAuth();
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    location: '',
    language: 'en',
    shopName: '',
    latitude: '',
    longitude: '',
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
          language: data.language || 'en',
          shopName: data.shopName || '',
          latitude: data.latitude ?? '',
          longitude: data.longitude ?? '',
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
      const payload = {
        ...form,
        latitude: form.latitude === '' ? null : Number(form.latitude),
        longitude: form.longitude === '' ? null : Number(form.longitude),
      };
      const { data } = await profileService.update(payload);
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

  if (loading) return <p>Loading profile…</p>;

  return (
    <div className="panel stack" style={{ maxWidth: 640 }}>
      <h2>Profile</h2>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}
      <div className="row">
        {profileImageUrl ? (
          <img src={mediaUrl(profileImageUrl)} alt="Profile" className="thumb" style={{ width: 72, height: 72 }} />
        ) : (
          <div className="thumb" style={{ width: 72, height: 72 }} />
        )}
        <label style={{ flex: 1 }}>
          Profile photo
          <input type="file" accept="image/*" onChange={onImage} />
        </label>
      </div>
      <p className="muted">Email: {email}</p>
      <form className="form-grid two" onSubmit={onSubmit}>
        <label>
          First name
          <input name="firstName" value={form.firstName} onChange={onChange} required />
        </label>
        <label>
          Last name
          <input name="lastName" value={form.lastName} onChange={onChange} required />
        </label>
        <label>
          Location
          <input name="location" value={form.location} onChange={onChange} />
        </label>
        <label>
          Language
          <select name="language" value={form.language} onChange={onChange}>
            {LANGUAGES.map((l) => (
              <option key={l.value} value={l.value}>
                {l.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Shop name
          <input name="shopName" value={form.shopName} onChange={onChange} />
        </label>
        <label>
          Latitude
          <input name="latitude" type="number" step="any" value={form.latitude} onChange={onChange} />
        </label>
        <label>
          Longitude
          <input name="longitude" type="number" step="any" value={form.longitude} onChange={onChange} />
        </label>
        <div style={{ gridColumn: '1 / -1' }}>
          <button className="btn" type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Save profile'}
          </button>
        </div>
      </form>
    </div>
  );
}
