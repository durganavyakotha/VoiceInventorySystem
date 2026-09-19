import { useCallback, useEffect, useState } from 'react';
import { adminService } from '../../services/adminService';

export default function AdminQueries() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await adminService.getQueries();
      setRows(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load queries');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const updateStatus = async (id, status) => {
    try {
      await adminService.updateQuery(id, status);
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed');
    }
  };

  return (
    <div className="stack">
      <h1>Contact queries</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {loading ? (
        <p>Loading…</p>
      ) : (
        <div className="table-wrap panel">
          <table className="data-table">
            <thead>
              <tr>
                <th>From</th>
                <th>Subject</th>
                <th>Message</th>
                <th>Status</th>
                <th>Update</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((q) => (
                <tr key={q.id}>
                  <td>
                    <div>{q.userName}</div>
                    <div className="muted" style={{ fontSize: '0.85rem' }}>{q.userEmail}</div>
                  </td>
                  <td>{q.subject}</td>
                  <td style={{ maxWidth: 280 }}>{q.message}</td>
                  <td><span className="badge badge-new">{q.status}</span></td>
                  <td>
                    <select
                      value={q.status}
                      onChange={(e) => updateStatus(q.id, e.target.value)}
                    >
                      <option value="NEW">NEW</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="RESOLVED">RESOLVED</option>
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!rows.length && <div className="empty">No queries yet.</div>}
        </div>
      )}
    </div>
  );
}
