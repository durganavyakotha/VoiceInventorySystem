import { useCallback, useEffect, useState } from 'react';
import OrderTable from '../../components/OrderTable';
import { orderService } from '../../services/orderService';
import { useAuth } from '../../context/AuthContext';

export default function OrdersPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [offer, setOffer] = useState({
    vendorId: '',
    productName: '',
    quantity: 1,
    deliveryWithinDays: 3,
    message: '',
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await orderService.list();
      setOrders(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const run = async (fn, okMsg) => {
    setError('');
    setMessage('');
    try {
      await fn();
      setMessage(okMsg);
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Action failed');
    }
  };

  const submitOffer = async (e) => {
    e.preventDefault();
    await run(
      () =>
        orderService.offer({
          vendorId: Number(offer.vendorId),
          productName: offer.productName,
          quantity: Number(offer.quantity),
          deliveryWithinDays: Number(offer.deliveryWithinDays) || undefined,
          message: offer.message || undefined,
        }),
      'Offer / order created'
    );
  };

  return (
    <div className="stack">
      <h1>Orders</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      {user?.role === 'SHOPKEEPER' && (
        <form className="panel form-grid two" onSubmit={submitOffer}>
          <h3 style={{ gridColumn: '1 / -1', margin: 0 }}>Book / offer order</h3>
          <label>
            Vendor ID
            <input
              required
              value={offer.vendorId}
              onChange={(e) => setOffer({ ...offer, vendorId: e.target.value })}
            />
          </label>
          <label>
            Product
            <input
              required
              value={offer.productName}
              onChange={(e) => setOffer({ ...offer, productName: e.target.value })}
            />
          </label>
          <label>
            Quantity
            <input
              type="number"
              min={1}
              required
              value={offer.quantity}
              onChange={(e) => setOffer({ ...offer, quantity: e.target.value })}
            />
          </label>
          <label>
            Delivery within (days)
            <input
              type="number"
              min={1}
              value={offer.deliveryWithinDays}
              onChange={(e) => setOffer({ ...offer, deliveryWithinDays: e.target.value })}
            />
          </label>
          <label style={{ gridColumn: '1 / -1' }}>
            Message
            <input
              value={offer.message}
              onChange={(e) => setOffer({ ...offer, message: e.target.value })}
            />
          </label>
          <button className="btn" type="submit">Place order</button>
        </form>
      )}

      {loading ? (
        <p>Loading…</p>
      ) : (
        <div className="panel">
          <OrderTable
            orders={orders}
            role={user?.role}
            onAccept={(id) => run(() => orderService.accept(id), 'Accepted')}
            onReject={(id) => run(() => orderService.reject(id), 'Rejected')}
            onCancel={(id) => run(() => orderService.cancel(id), 'Cancelled')}
            onWithdraw={(id) => run(() => orderService.withdraw(id), 'Withdrawn')}
            onStatus={(id, status) => run(() => orderService.updateStatus(id, status), 'Status updated')}
          />
        </div>
      )}
    </div>
  );
}
