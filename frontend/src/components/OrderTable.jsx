export default function OrderTable({
  orders,
  role,
  onAccept,
  onReject,
  onCancel,
  onWithdraw,
  onStatus,
}) {
  if (!orders?.length) {
    return <div className="empty">No orders yet.</div>;
  }

  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Product</th>
            <th>Qty</th>
            <th>Shopkeeper</th>
            <th>Vendor</th>
            <th>Status</th>
            <th>Delivery</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.id}>
              <td>{o.id}</td>
              <td>{o.productName}</td>
              <td>{o.quantity}</td>
              <td>{o.shopkeeperName}</td>
              <td>{o.vendorName}</td>
              <td>
                <span className="badge badge-new">{o.status}</span>
              </td>
              <td>{o.expectedDeliveryDate || o.deliveryWithinDays || '—'}</td>
              <td>
                <div className="row">
                  {role === 'VENDOR' && o.status === 'PENDING' && (
                    <>
                      <button type="button" className="btn btn-sm" onClick={() => onAccept(o.id)}>
                        Accept
                      </button>
                      <button type="button" className="btn btn-sm btn-danger" onClick={() => onReject(o.id)}>
                        Reject
                      </button>
                    </>
                  )}
                  {role === 'VENDOR' && o.status === 'ACCEPTED' && (
                    <button
                      type="button"
                      className="btn btn-sm btn-secondary"
                      onClick={() => onStatus(o.id, 'DELIVERED')}
                    >
                      Mark delivered
                    </button>
                  )}
                  {role === 'SHOPKEEPER' && o.status === 'PENDING' && (
                    <>
                      <button type="button" className="btn btn-sm btn-ghost" onClick={() => onCancel(o.id)}>
                        Cancel
                      </button>
                      <button type="button" className="btn btn-sm btn-ghost" onClick={() => onWithdraw(o.id)}>
                        Withdraw
                      </button>
                    </>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
