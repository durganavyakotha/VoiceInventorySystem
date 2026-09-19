import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import ChatBox from '../../components/ChatBox';
import { chatService } from '../../services/chatService';
import { chatbotService } from '../../services/chatbotService';
import { useI18n } from '../../context/LanguageContext';

export default function ChatHistory() {
  const { t } = useI18n();
  const [searchParams, setSearchParams] = useSearchParams();
  const [conversations, setConversations] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [botInput, setBotInput] = useState('');

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

  const openAssistant = async () => {
    try {
      const { data } = await chatbotService.open();
      await load();
      select(data.id);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not open assistant');
    }
  };

  const askAssistant = async (e) => {
    e?.preventDefault();
    if (!botInput.trim()) return;
    try {
      const { data } = await chatbotService.ask(botInput.trim());
      setBotInput('');
      await load();
      if (data.conversationId) select(data.conversationId);
    } catch (err) {
      setError(err.response?.data?.message || 'Assistant failed');
    }
  };

  const selected = conversations.find((c) => c.id === selectedId);
  const isBot = selected?.otherUser?.email === 'assistant@voicestock.bot'
    || `${selected?.otherUser?.firstName} ${selected?.otherUser?.lastName}`.includes('Assistant');

  return (
    <div className="stack">
      <div className="row space-between">
        <h1 style={{ margin: 0 }}>{t('chattingHistory')}</h1>
        <button type="button" className="btn btn-secondary btn-sm" onClick={openAssistant}>
          VoiceStock Assistant
        </button>
      </div>
      {error && <div className="alert alert-error">{error}</div>}
      {loading ? (
        <p>{t('loading')}</p>
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
            {!conversations.length && <div className="empty">No conversations yet. Open the Assistant.</div>}
          </div>
          <div className="stack" style={{ minHeight: 360 }}>
            <ChatBox conversationId={selectedId} otherUser={selected?.otherUser} />
            {isBot && (
              <form className="row" onSubmit={askAssistant} style={{ padding: '0 0.5rem 0.5rem' }}>
                <input
                  style={{ flex: 1 }}
                  value={botInput}
                  onChange={(e) => setBotInput(e.target.value)}
                  placeholder="Ask VoiceStock Assistant…"
                />
                <button className="btn btn-sm" type="submit">Ask</button>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
