import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import ChatBox from '../../components/ChatBox';
import { chatService } from '../../services/chatService';

export default function ChatHistory() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [conversations, setConversations] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await chatService.listConversations();
      setConversations(data || []);
      const fromQuery = searchParams.get('c');
      if (fromQuery) {
        setSelectedId(Number(fromQuery));
      } else if (data?.length && !selectedId) {
        setSelectedId(data[0].id);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load chats');
    } finally {
      setLoading(false);
    }
  }, [searchParams, selectedId]);

  useEffect(() => {
    load();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const fromQuery = searchParams.get('c');
    if (fromQuery) setSelectedId(Number(fromQuery));
  }, [searchParams]);

  const select = (id) => {
    setSelectedId(id);
    setSearchParams({ c: String(id) });
  };

  const selected = conversations.find((c) => c.id === selectedId);

  return (
    <div className="stack">
      <h1>Chatting history</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {loading ? (
        <p>Loading…</p>
      ) : (
        <div className="chat-layout panel">
          <div className="chat-list">
            {conversations.map((c) => (
              <button
                key={c.id}
                type="button"
                className={c.id === selectedId ? 'active' : ''}
                onClick={() => select(c.id)}
              >
                <strong>
                  {c.otherUser?.firstName} {c.otherUser?.lastName}
                </strong>
                <div className="muted" style={{ fontSize: '0.85rem' }}>
                  {c.otherUser?.role} · {c.otherUser?.language}
                </div>
              </button>
            ))}
            {!conversations.length && <div className="empty">No conversations yet.</div>}
          </div>
          <ChatBox conversationId={selectedId} otherUser={selected?.otherUser} />
        </div>
      )}
    </div>
  );
}
