import { mediaUrl } from '../services/api';

export default function ProductRow({
  item,
  onAddStock,
  onRemoveStock,
  onSetThreshold,
  onDelete,
}) {
  return (
    <tr>
      <td>{item.productName}</td>
      <td>
        {item.imageUrl ? (
          <img src={mediaUrl(item.imageUrl)} alt="" className="thumb" />
        ) : (
          <span className="muted">—</span>
        )}
      </td>
      <td>{item.quantity}</td>
      <td>{item.threshold}</td>
      <td>
        <span className={`badge ${item.lowStock ? 'badge-low' : 'badge-ok'}`}>
          {item.lowStock ? 'Low stock' : 'OK'}
        </span>
      </td>
      <td>
        <div className="row">
          <button type="button" className="btn btn-sm btn-secondary" onClick={() => onAddStock(item)}>
            + Stock
          </button>
          <button type="button" className="btn btn-sm btn-ghost" onClick={() => onRemoveStock(item)}>
            − Stock
          </button>
          <button type="button" className="btn btn-sm btn-ghost" onClick={() => onSetThreshold(item)}>
            Threshold
          </button>
          <button type="button" className="btn btn-sm btn-danger" onClick={() => onDelete(item)}>
            Delete
          </button>
        </div>
      </td>
    </tr>
  );
}
