import { useCallback, useEffect, useState } from 'react';
import { adminService } from '../../services/adminService';

const LANGUAGES = [
  { value: '', label: 'All languages' },
  { value: 'en', label: 'English' },
  { value: 'te', label: 'Telugu' },
  { value: 'hi', label: 'Hindi' },
  { value: 'ta', label: 'Tamil' },
  { value: 'kn', label: 'Kannada' },
];

export default function AdminVendors() {
  const [rows, setRows] = useState([]);
  const [search, setSearch] = useState('');
  const [location, setLocation] = useState('');
  const [status, setStatus] = useState('');
  const [language, setLanguage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = {};
      if (search) params.search = search;
      if (location) params.location = location;
      if (status) params.status = status;
      if (language) params.language = language;
      const { data } = await adminService.getVendors(params);
      setRows(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load vendors');
    } finally {
      setLoading(false);
    }
  }, [search, location, status, language]);

  useEffect(() => {
    load();
  }, [load]);

  const toggleStatus = async (user) => {
    const next = user.status === 'BLOCKED' ? 'ACTIVE' : 'BLOCKED';
    try {
      await adminService.updateUserStatus(user.id, next);
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Status update failed');
    }
  };

  return (
    <div className="stack">
      <h1>Vendors</h1>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="panel">
        <div className="form-grid two">
          <label>
            Search
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Name or email" />
          </label>
          <label>
            Location
            <input value={location} onChange={(e) => setLocation(e.target.value)} />
          </label>
          <label>
            Language
            <select value={language} onChange={(e) => setLanguage(e.target.value)}>
              {LANGUAGES.map((l) => (
                <option key={l.value || 'all'} value={l.value}>{l.label}</option>
              ))}
            </select>
          </label>
          <label>
            Status
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">All</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="BLOCKED">BLOCKED</option>
            </select>
          </label>
          <div style={{ alignSelf: 'end' }}>
            <button type="button" className="btn" onClick={load}>Refresh</button>
          </div>
        </div>
      </div>
      {loading ? (
        <p>Loading…</p>
      ) : (
        <div className="table-wrap panel">
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Location</th>
                <th>Language</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((u) => (
                <tr key={u.id}>
                  <td>{u.firstName} {u.lastName}</td>
                  <td>{u.email}</td>
                  <td>{u.location || '—'}</td>
                  <td>{u.languageDisplay || u.language || '—'}</td>
                  <td>
                    <span className={`badge ${u.status === 'BLOCKED' ? 'badge-blocked' : 'badge-ok'}`}>
                      {u.status}
                    </span>
                  </td>
                  <td>
                    <button
                      type="button"
                      className={`btn btn-sm ${u.status === 'BLOCKED' ? '' : 'btn-danger'}`}
                      onClick={() => toggleStatus(u)}
                    >
                      {u.status === 'BLOCKED' ? 'Activate' : 'Block'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!rows.length && <div className="empty">No vendors found.</div>}
        </div>
      )}
    </div>
  );
}
