import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import ProductRow from '../../components/ProductRow';
import { inventoryService } from '../../services/inventoryService';

export default function InventoryList() {
  const location = useLocation();
  const addPath = location.pathname.startsWith('/vendor') ? '/vendor/add' : '/shopkeeper/add';

  const [items, setItems] = useState([]);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async (q = search) => {
    setLoading(true);
    setError('');
    try {
      const { data } = await inventoryService.list(q || undefined);
      setItems(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load inventory');
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => {
    load();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const promptNumber = (label, fallback = 1) => {
    const raw = window.prompt(label, String(fallback));
    if (raw == null) return null;
    const n = Number(raw);
    if (Number.isNaN(n) || n < 0) {
      setError('Enter a valid number');
      return null;
    }
    return n;
  };

  const onAddStock = async (item) => {
    const qty = promptNumber('New total quantity', item.quantity + 1);
    if (qty == null) return;
    try {
      await inventoryService.updateQuantity(item.id, qty);
      setMessage('Quantity updated');
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed');
    }
  };

  const onRemoveStock = async (item) => {
    const qty = promptNumber('Quantity to remove', 1);
    if (qty == null) return;
    try {
      await inventoryService.removeStock(item.id, qty);
      setMessage('Stock removed');
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Remove failed');
    }
  };

  const onSetThreshold = async (item) => {
    const thr = promptNumber('New threshold', item.threshold);
    if (thr == null) return;
    try {
      await inventoryService.setThreshold(item.id, thr);
      setMessage('Threshold updated');
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Threshold update failed');
    }
  };

  const onDelete = async (item) => {
    if (!window.confirm(`Delete ${item.productName}?`)) return;
    try {
      await inventoryService.delete(item.id);
      setMessage('Item deleted');
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Delete failed');
    }
  };

  return (
    <div className="stack">
      <div className="row space-between">
        <h1 style={{ margin: 0 }}>Inventory</h1>
        <div className="row">
          <Link className="btn btn-secondary btn-sm" to={addPath}>Add items</Link>
          <form
            className="row"
            onSubmit={(e) => {
              e.preventDefault();
              load(search);
            }}
          >
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search products"
              style={{ minWidth: 200 }}
            />
            <button className="btn" type="submit">Search</button>
          </form>
        </div>
      </div>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}
      {loading ? (
        <p>Loading…</p>
      ) : (
        <div className="table-wrap panel">
          <table className="data-table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Picture</th>
                <th>Available</th>
                <th>Threshold</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <ProductRow
                  key={item.id}
                  item={item}
                  onAddStock={onAddStock}
                  onRemoveStock={onRemoveStock}
                  onSetThreshold={onSetThreshold}
                  onDelete={onDelete}
                />
              ))}
            </tbody>
          </table>
          {!items.length && <div className="empty">No products found.</div>}
        </div>
      )}
    </div>
  );
}
