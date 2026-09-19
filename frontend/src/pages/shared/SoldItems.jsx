import { useCallback, useEffect, useState } from 'react';
import { salesService } from '../../services/salesService';
import { inventoryService } from '../../services/inventoryService';

export default function SoldItems() {
  const [history, setHistory] = useState(null);
  const [highest, setHighest] = useState(null);
  const [period, setPeriod] = useState('today');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [productName, setProductName] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [products, setProducts] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = { period };
      if (period === 'custom') {
        if (from) params.startDate = from;
        if (to) params.endDate = to;
      }
      const [h, hi, inv] = await Promise.all([
        salesService.history(params),
        salesService.highestSelling(params),
        inventoryService.list(),
      ]);
      setHistory(h.data);
      setHighest(hi.data);
      setProducts(inv.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load sales');
    } finally {
      setLoading(false);
    }
  }, [period, from, to]);

  useEffect(() => {
    load();
  }, [load]);

  const recordSale = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      await salesService.record(productName, Number(quantity));
      setMessage('Sale recorded');
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not record sale');
    }
  };

  return (
    <div className="stack">
      <h1>Sold items</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      <div className="panel form-grid two">
        <label>
          Period
          <select value={period} onChange={(e) => setPeriod(e.target.value)}>
            <option value="today">Today</option>
            <option value="yesterday">Yesterday</option>
            <option value="week">Week</option>
            <option value="month">Month</option>
            <option value="custom">Custom</option>
          </select>
        </label>
        {period === 'custom' && (
          <>
            <label>
              From
              <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
            </label>
            <label>
              To
              <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
            </label>
          </>
        )}
        <div style={{ alignSelf: 'end' }}>
          <button type="button" className="btn" onClick={load}>Apply</button>
        </div>
      </div>

      {loading ? (
        <p>Loading…</p>
      ) : (
        <>
          <div className="stats-row">
            <div className="stat">
              <span className="muted">Total sold ({history?.period || period})</span>
              <strong>{history?.totalSold ?? 0}</strong>
            </div>
            <div className="stat">
              <span className="muted">Highest selling</span>
              <strong style={{ fontSize: '1.2rem' }}>
                {highest?.productName || history?.highestSellingProduct || '—'}
              </strong>
              <div className="muted">
                Qty: {highest?.quantity ?? history?.highestSellingQuantity ?? 0}
              </div>
            </div>
          </div>

          <form className="panel form-grid two" onSubmit={recordSale}>
            <h3 style={{ gridColumn: '1 / -1', margin: 0 }}>Record a sale</h3>
            <label>
              Product
              <select value={productName} onChange={(e) => setProductName(e.target.value)} required>
                <option value="">Select…</option>
                {products.map((p) => (
                  <option key={p.id} value={p.productName}>{p.productName}</option>
                ))}
              </select>
            </label>
            <label>
              Quantity
              <input type="number" min={1} value={quantity} onChange={(e) => setQuantity(e.target.value)} required />
            </label>
            <button className="btn" type="submit">Record sale</button>
          </form>

          <div className="table-wrap panel">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Quantity</th>
                  <th>When</th>
                </tr>
              </thead>
              <tbody>
                {(history?.sales || []).map((s, idx) => (
                  <tr key={s.id || idx}>
                    <td>{s.productName || s.product || '—'}</td>
                    <td>{s.quantity}</td>
                    <td>{s.soldAt || s.createdAt || s.date || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!history?.sales?.length && <div className="empty">No sales in this period.</div>}
          </div>
        </>
      )}
    </div>
  );
}
