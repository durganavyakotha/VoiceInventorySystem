import { useEffect, useState } from 'react';
import { vendorService } from '../../services/vendorService';
import { orderService } from '../../services/orderService';
import { mediaUrl } from '../../services/api';
import { useI18n } from '../../context/LanguageContext';

export default function VendorsPage() {
  const { t } = useI18n();
  const [vendors, setVendors] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [itemsLoading, setItemsLoading] = useState(false);
  const [bookingId, setBookingId] = useState(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const { data } = await vendorService.listAll();
        setVendors(data || []);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load vendors');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const openItems = async (vendor) => {
    setError('');
    setMessage('');
    setItemsLoading(true);
    try {
      const { data } = await vendorService.getItems(vendor.vendorId);
      setSelected(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load items');
    } finally {
      setItemsLoading(false);
    }
  };

  const bookItem = async (item) => {
    if (!selected) return;
    const qty = window.prompt(t('quantity'), String(Math.min(item.quantity, 10)));
    if (qty == null) return;
    const n = Number(qty);
    if (!n || n <= 0) {
      setError('Invalid quantity');
      return;
    }
    setBookingId(item.id);
    try {
      await orderService.create({
        vendorId: selected.vendorId,
        productName: item.productName,
        quantity: n,
        deliveryWithinDays: 2,
      });
      setMessage(t('bookRequestSent'));
    } catch (err) {
      setError(err.response?.data?.message || 'Book failed');
    } finally {
      setBookingId(null);
    }
  };

  return (
    <div className="stack">
      <h1>{t('vendors')}</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      {loading ? (
        <p>{t('loading')}</p>
      ) : (
        <div className="vendor-grid">
          {vendors.map((v) => (
            <div key={v.vendorId} className="vendor-card">
              <div className="vendor-card-top">
                {v.profileImageUrl ? (
                  <img src={mediaUrl(v.profileImageUrl)} alt="" className="vendor-avatar" />
                ) : (
                  <span className="vendor-avatar fallback">{(v.firstName || '?')[0]}</span>
                )}
                <div>
                  <h3>{v.name || `${v.firstName} ${v.lastName}`}</h3>
                  <p className="muted" style={{ margin: 0 }}>{v.shopName}</p>
                </div>
              </div>
              <ul className="vendor-meta">
                <li><strong>{t('phone')}:</strong> {v.phone || '—'}</li>
                <li><strong>{t('location')}:</strong> {v.location || '—'}</li>
                <li><strong>{t('email')}:</strong> {v.email}</li>
              </ul>
              <button type="button" className="btn btn-sm" onClick={() => openItems(v)}>
                {t('availableItems')}
              </button>
            </div>
          ))}
          {!vendors.length && <div className="empty">{t('noVendors')}</div>}
        </div>
      )}

      {selected && (
        <div className="modal-backdrop" onClick={() => setSelected(null)} role="presentation">
          <div className="modal-panel" onClick={(e) => e.stopPropagation()} role="dialog">
            <div className="row space-between">
              <h2 style={{ margin: 0 }}>
                {selected.name || `${selected.firstName} ${selected.lastName}`} — {t('availableItems')}
              </h2>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => setSelected(null)}>
                {t('close')}
              </button>
            </div>
            {itemsLoading ? (
              <p>{t('loading')}</p>
            ) : (
              <div className="item-grid" style={{ marginTop: '1rem' }}>
                {(selected.items || []).map((item) => (
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
                      <p className="item-qty">{item.quantity} {item.unit || 'pieces'}</p>
                      <button
                        type="button"
                        className="btn btn-sm"
                        disabled={bookingId === item.id || item.quantity <= 0}
                        onClick={() => bookItem(item)}
                      >
                        {t('book')}
                      </button>
                    </div>
                  </div>
                ))}
                {!(selected.items || []).length && <div className="empty">{t('noProducts')}</div>}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
