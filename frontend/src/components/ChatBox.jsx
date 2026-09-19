import { useCallback, useEffect, useRef, useState } from 'react';
import { chatService } from '../services/chatService';
import { useAuth } from '../context/AuthContext';
import VoiceRecorder from './VoiceRecorder';

export default function ChatBox({ conversationId, otherUser }) {
  const { user } = useAuth();
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);
  const bottomRef = useRef(null);

  const load = useCallback(async () => {
    if (!conversationId) return;
    setLoading(true);
    setError('');
    try {
      const { data } = await chatService.getMessages(conversationId);
      setMessages(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load messages');
    } finally {
      setLoading(false);
    }
  }, [conversationId]);

  useEffect(() => {
    load();
    const timer = setInterval(load, 8000);
    return () => clearInterval(timer);
  }, [load]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const receiverId = otherUser?.id;

  const sendText = async (e) => {
    e?.preventDefault();
    if (!text.trim() || !receiverId) return;
    setSending(true);
    setError('');
    try {
      const { data } = await chatService.sendText({
        conversationId,
        receiverId,
        messageText: text.trim(),
        messageType: 'TEXT',
      });
      setMessages((prev) => [...prev, data]);
      setText('');
    } catch (err) {
      setError(err.response?.data?.message || 'Send failed');
    } finally {
      setSending(false);
    }
  };

  const sendVoice = async (transcript) => {
    if (!transcript?.trim() || !receiverId) return;
    setText(transcript);
    setSending(true);
    setError('');
    try {
      const { data } = await chatService.sendVoice({
        conversationId,
        receiverId,
        transcript: transcript.trim(),
        messageText: transcript.trim(),
        messageType: 'VOICE',
      });
      setMessages((prev) => [...prev, data]);
      setText('');
    } catch (err) {
      setError(err.response?.data?.message || 'Voice send failed');
    } finally {
      setSending(false);
    }
  };

  if (!conversationId) {
    return <div className="empty">Select a conversation</div>;
  }

  return (
    <div className="chat-box panel">
      <h3 style={{ marginTop: 0 }}>
        {otherUser
          ? `${otherUser.firstName || ''} ${otherUser.lastName || ''}`.trim()
          : 'Chat'}
      </h3>
      {error && <div className="alert alert-error">{error}</div>}
      {loading && messages.length === 0 && <p className="muted">Loading…</p>}
      <div className="chat-messages">
        {messages.map((m) => {
          const mine = m.senderId === user?.id;
          return (
            <div key={m.id} className={`bubble ${mine ? 'mine' : 'theirs'}`}>
              <div>
                {m.messageType === 'VOICE' && <strong>🎤 </strong>}
                {m.messageText || m.transcript}
              </div>
              {m.translatedText && m.translatedText !== m.messageText && (
                <div className="translated">→ {m.translatedText}</div>
              )}
              <div className="meta">
                {m.messageType || 'TEXT'} · {m.createdAt ? new Date(m.createdAt).toLocaleString() : ''}
              </div>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>
      <form className="chat-compose" onSubmit={sendText}>
        <textarea
          rows={2}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Type a message…"
        />
        <VoiceRecorder onTranscript={sendVoice} />
        <button className="btn" type="submit" disabled={sending || !text.trim()}>
          Send
        </button>
      </form>
    </div>
  );
}
