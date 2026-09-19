import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { inventoryService } from '../../services/inventoryService';
import { mediaUrl } from '../../services/api';
import { useI18n } from '../../context/LanguageContext';

export default function InventoryList() {
  const { t } = useI18n();
  const location = useLocation();
  const addPath = location.pathname.startsWith('/vendor') ? '/vendor/add' : '/shopkeeper/add';

  const [items, setItems] = useState([]);
  const [name, setName] = useState('');
  const [category, setCategory] = useState('ALL');
  const [availability, setAvailability] = useState('ALL');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await inventoryService.list({
        name: name || undefined,
        category: category !== 'ALL' ? category : undefined,
        availability: availability !== 'ALL' ? availability : undefined,
      });
      setItems(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load inventory');
    } finally {
      setLoading(false);
    }
  }, [name, category, availability]);

  useEffect(() => {
    load();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const categories = useMemo(() => {
    const set = new Set(items.map((i) => i.category).filter(Boolean));
    return ['ALL', ...Array.from(set).sort()];
  }, [items]);

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
    const qty = promptNumber(t('quantity'), item.quantity + 1);
    if (qty == null) return;
    try {
      await inventoryService.updateQuantity(item.id, qty);
      setMessage('OK');
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed');
    }
  };

  const onRemoveStock = async (item) => {
    const qty = promptNumber(t('quantity'), 1);
    if (qty == null) return;
    try {
      await inventoryService.removeStock(item.id, qty);
      setMessage('OK');
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Remove failed');
    }
  };

  const onSetThreshold = async (item) => {
    const thr = promptNumber(t('threshold'), item.threshold);
    if (thr == null) return;
    try {
      await inventoryService.setThreshold(item.id, thr);
      setMessage('OK');
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Threshold update failed');
    }
  };

  const onDelete = async (item) => {
    if (!window.confirm(`${t('delete')} ${item.productName}?`)) return;
    try {
      await inventoryService.delete(item.id);
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Delete failed');
    }
  };

  const statusLabel = (a) => {
    if (a === 'LOW_STOCK') return t('lowStock');
    if (a === 'OUT_OF_STOCK') return t('outOfStock');
    return t('available');
  };

  return (
    <div className="stack">
      <div className="row space-between">
        <h1 style={{ margin: 0 }}>{t('inventory')}</h1>
        <Link className="btn btn-secondary btn-sm" to={addPath}>{t('addItems')}</Link>
      </div>

      <form
        className="panel filters-row"
        onSubmit={(e) => {
          e.preventDefault();
          load();
        }}
      >
        <label>
          {t('name')}
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder={t('search')} />
        </label>
        <label>
          {t('category')}
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="ALL">{t('all')}</option>
            {categories.filter((c) => c !== 'ALL').map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
            {['Grains', 'Grocery', 'Beverages', 'Snacks', 'Personal Care', 'General']
              .filter((c) => !categories.includes(c))
              .map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
          </select>
        </label>
        <label>
          {t('availability')}
          <select value={availability} onChange={(e) => setAvailability(e.target.value)}>
            <option value="ALL">{t('all')}</option>
            <option value="AVAILABLE">{t('available')}</option>
            <option value="LOW_STOCK">{t('lowStock')}</option>
            <option value="OUT_OF_STOCK">{t('outOfStock')}</option>
          </select>
        </label>
        <button className="btn" type="submit">{t('search')}</button>
      </form>

      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      {loading ? (
        <p>{t('loading')}</p>
      ) : (
        <div className="item-grid">
          {items.map((item) => (
            <div key={item.id} className="item-card">
              <div className="item-card-media">
                {item.imageUrl ? (
                  <img src={mediaUrl(item.imageUrl)} alt={item.productName} />
                ) : (
                  <div className="item-card-placeholder">—</div>
                )}
              </div>
              <div className="item-card-body">
                <span className="item-category">{item.category || 'General'}</span>
                <h3>{item.productName}</h3>
                <p className="item-qty">
                  {item.quantity} {item.unit || 'pieces'}
                </p>
                <p className={`status-pill ${item.availability?.toLowerCase()}`}>
                  {statusLabel(item.availability)}
                </p>
                <div className="row" style={{ flexWrap: 'wrap', gap: '0.35rem' }}>
                  <button type="button" className="btn btn-sm" onClick={() => onAddStock(item)}>
                    {t('addStock')}
                  </button>
                  <button type="button" className="btn btn-sm btn-secondary" onClick={() => onRemoveStock(item)}>
                    {t('removeStock')}
                  </button>
                  <button type="button" className="btn btn-sm btn-ghost" onClick={() => onSetThreshold(item)}>
                    {t('setThreshold')}
                  </button>
                  <button type="button" className="btn btn-sm btn-ghost" onClick={() => onDelete(item)}>
                    {t('delete')}
                  </button>
                </div>
              </div>
            </div>
          ))}
          {!items.length && <div className="empty">{t('noProducts')}</div>}
        </div>
      )}
    </div>
  );
}
