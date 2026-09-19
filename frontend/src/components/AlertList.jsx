export default function AlertList({ alerts, onRead, onReadAll }) {
  if (!alerts?.length) {
    return <div className="empty">No alerts yet.</div>;
  }

  return (
    <div className="stack">
      <div className="row space-between">
        <h2 style={{ margin: 0 }}>Alerts</h2>
        <button type="button" className="btn btn-secondary btn-sm" onClick={onReadAll}>
          Mark all read
        </button>
      </div>
      <ul className="stack" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {alerts.map((a) => (
          <li
            key={a.id}
            className="panel"
            style={{
              opacity: a.isRead ? 0.7 : 1,
              borderLeft: a.isRead ? undefined : '4px solid var(--clay)',
            }}
          >
            <div className="row space-between">
              <div>
                <strong>{a.type}</strong>
                <p style={{ margin: '0.35rem 0' }}>{a.message}</p>
                <span className="muted" style={{ fontSize: '0.85rem' }}>
                  {a.createdAt ? new Date(a.createdAt).toLocaleString() : ''}
                </span>
              </div>
              {!a.isRead && (
                <button type="button" className="btn btn-sm btn-secondary" onClick={() => onRead(a.id)}>
                  Mark read
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
