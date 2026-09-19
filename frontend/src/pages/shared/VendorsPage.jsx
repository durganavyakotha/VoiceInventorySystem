import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { vendorService } from '../../services/vendorService';
import { chatService } from '../../services/chatService';
import { orderService } from '../../services/orderService';

export default function VendorsPage({ chatBase = '/shopkeeper/chat', showOrder = true }) {
  const navigate = useNavigate();
  const [product, setProduct] = useState('');
  const [radius, setRadius] = useState('10');
  const [customRadius, setCustomRadius] = useState('15');
  const [minQty, setMinQty] = useState(1);
  const [results, setResults] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const search = async (e) => {
    e?.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');
    try {
      const r = radius === 'custom' ? Number(customRadius) : Number(radius);
      const { data } = await vendorService.nearby({
        product: product.trim(),
        radius: r,
        minQty: Number(minQty) || 1,
      });
      setResults(data || []);
      if (!data?.length) setMessage('No nearby vendors found');
    } catch (err) {
      setError(err.response?.data?.message || 'Search failed');
    } finally {
      setLoading(false);
    }
  };

  const startChat = async (vendorId) => {
    try {
      const { data } = await chatService.getOrCreate(vendorId);
      navigate(`${chatBase}?c=${data.id}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not start chat');
    }
  };

  const placeOrder = async (row) => {
    const qty = window.prompt('Quantity to order', String(Math.min(row.availableQty, 10)));
    if (qty == null) return;
    try {
      await orderService.create({
        vendorId: row.vendorId,
        productName: row.productName,
        quantity: Number(qty),
        deliveryWithinDays: 3,
      });
      setMessage('Order placed');
    } catch (err) {
      setError(err.response?.data?.message || 'Order failed');
    }
  };

  return (
    <div className="stack">
      <h1>{showOrder ? 'Nearby vendors' : 'Other vendors'}</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}
      <form className="panel stack" onSubmit={search}>
        <label>
          Product
          <input value={product} onChange={(e) => setProduct(e.target.value)} required placeholder="e.g. Rice" />
        </label>
        <div>
          <strong>Radius (km)</strong>
          <div className="radio-row" style={{ marginTop: '0.5rem' }}>
            {['2', '5', '10', '20', 'custom'].map((v) => (
              <label key={v}>
                <input
                  type="radio"
                  name="radius"
                  value={v}
                  checked={radius === v}
                  onChange={(e) => setRadius(e.target.value)}
                />
                {v === 'custom' ? 'Custom' : `${v} km`}
              </label>
            ))}
          </div>
          {radius === 'custom' && (
            <input
              type="number"
              min={1}
              value={customRadius}
              onChange={(e) => setCustomRadius(e.target.value)}
              style={{ maxWidth: 160, marginTop: '0.5rem' }}
            />
          )}
        </div>
        <label style={{ maxWidth: 200 }}>
          Min quantity
          <input type="number" min={1} value={minQty} onChange={(e) => setMinQty(e.target.value)} />
        </label>
        <button className="btn" type="submit" disabled={loading}>
          {loading ? 'Searching…' : 'Search'}
        </button>
      </form>

      <div className="table-wrap panel">
        <table className="data-table">
          <thead>
            <tr>
              <th>Vendor</th>
              <th>Product</th>
              <th>Distance</th>
              <th>Stock</th>
              <th>Location</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {results.map((row) => (
              <tr key={`${row.vendorId}-${row.productName}`}>
                <td>{row.firstName} {row.lastName}</td>
                <td>{row.productName}</td>
                <td>{row.distanceKm} km</td>
                <td>{row.availableQty}</td>
                <td>{row.location || '—'}</td>
                <td>
                  <div className="row">
                    <button type="button" className="btn btn-sm" onClick={() => startChat(row.vendorId)}>
                      Start chat
                    </button>
                    {showOrder && (
                      <button type="button" className="btn btn-sm btn-secondary" onClick={() => placeOrder(row)}>
                        Order
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!results.length && !loading && <div className="empty">Search to see vendors nearby.</div>}
      </div>
    </div>
  );
}
