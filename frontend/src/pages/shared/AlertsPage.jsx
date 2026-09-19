import { useCallback, useEffect, useState } from 'react';
import AlertList from '../../components/AlertList';
import { alertService } from '../../services/alertService';

export default function AlertsPage() {
  const [alerts, setAlerts] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await alertService.list();
      setAlerts(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load alerts');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const onRead = async (id) => {
    try {
      await alertService.markRead(id);
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to mark read');
    }
  };

  const onReadAll = async () => {
    try {
      await alertService.markAllRead();
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to mark all read');
    }
  };

  return (
    <div className="stack">
      {error && <div className="alert alert-error">{error}</div>}
      {loading ? <p>Loading…</p> : <AlertList alerts={alerts} onRead={onRead} onReadAll={onReadAll} />}
    </div>
  );
}
