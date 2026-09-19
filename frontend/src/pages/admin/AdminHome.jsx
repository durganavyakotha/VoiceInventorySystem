import { useEffect, useState } from 'react';
import { adminService } from '../../services/adminService';

export default function AdminHome() {
  const [counts, setCounts] = useState(null);
  const [locations, setLocations] = useState(null);
  const [location, setLocation] = useState('');
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [c, l] = await Promise.all([adminService.getCounts(), adminService.getLocations()]);
        setCounts(c.data);
        setLocations(l.data);
        const keys = Object.keys(l.data?.vendors || {});
        if (keys.length) setLocation(keys[0]);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load dashboard');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  useEffect(() => {
    if (!location) return;
    (async () => {
      try {
        const { data } = await adminService.getLocationStats(location);
        setStats(data);
      } catch {
        setStats(null);
      }
    })();
  }, [location]);

  if (loading) return <p>Loading…</p>;

  const locationOptions = Array.from(
    new Set([
      ...Object.keys(locations?.vendors || {}),
      ...Object.keys(locations?.shopkeepers || {}),
    ])
  );

  return (
    <div className="stack">
      <h1>Admin dashboard</h1>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="stats-row">
        <div className="stat">
          <span className="muted">Vendors</span>
          <strong>{counts?.vendors ?? 0}</strong>
        </div>
        <div className="stat">
          <span className="muted">Shopkeepers</span>
          <strong>{counts?.shopkeepers ?? 0}</strong>
        </div>
        <div className="stat">
          <span className="muted">Admins</span>
          <strong>{counts?.admins ?? 0}</strong>
        </div>
        <div className="stat">
          <span className="muted">Total users</span>
          <strong>{counts?.total ?? 0}</strong>
        </div>
      </div>

      <div className="panel stack">
        <h2>Location stats</h2>
        <label style={{ maxWidth: 280 }}>
          Location
          <select value={location} onChange={(e) => setLocation(e.target.value)}>
            {locationOptions.map((loc) => (
              <option key={loc} value={loc}>{loc}</option>
            ))}
          </select>
        </label>
        {stats && (
          <div className="stats-row">
            <div className="stat">
              <span className="muted">Vendors in {location}</span>
              <strong>{stats.vendors ?? 0}</strong>
            </div>
            <div className="stat">
              <span className="muted">Shopkeepers in {location}</span>
              <strong>{stats.shopkeepers ?? 0}</strong>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
