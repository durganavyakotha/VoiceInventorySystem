import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminService } from '../../services/adminService';
import { chatbotService } from '../../services/chatbotService';
import VoiceRecorder from '../../components/VoiceRecorder';

export default function AdminQueries() {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [chatInput, setChatInput] = useState('');
  const [messages, setMessages] = useState([
    { role: 'bot', text: 'Hi Admin! Ask about users, queries, or platform help.' },
  ]);
  const [sending, setSending] = useState(false);
  const bottomRef = useRef(null);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await adminService.getQueries();
      setRows(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load queries');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const updateStatus = async (id, status) => {
    try {
      await adminService.updateQuery(id, status);
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed');
    }
  };

  const askBot = async (e) => {
    e?.preventDefault();
    if (!chatInput.trim()) return;
    const text = chatInput.trim();
    setChatInput('');
    setMessages((prev) => [...prev, { role: 'user', text }]);
    setSending(true);
    try {
      const { data } = await chatbotService.ask(text);
      setMessages((prev) => [...prev, { role: 'bot', text: data.reply }]);
    } catch (err) {
      setMessages((prev) => [...prev, {
        role: 'bot',
        text: err.response?.data?.message || 'Assistant unavailable',
      }]);
    } finally {
      setSending(false);
    }
  };

  const openInHistory = async () => {
    try {
      const { data } = await chatbotService.open();
      navigate(`/admin/queries?bot=${data.id}`);
      setMessages((prev) => [...prev, {
        role: 'bot',
        text: 'Conversation saved. Open Chat History from a shop/vendor account to continue with VoiceStock Assistant.',
      }]);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not open bot chat');
    }
  };

  return (
    <div className="stack">
      <h1>Queries & Assistant</h1>
      {error && <div className="alert alert-error">{error}</div>}

      <div className="panel stack chatbot-panel">
        <div className="row space-between">
          <h2 style={{ margin: 0 }}>VoiceStock Chatbot</h2>
          <button type="button" className="btn btn-sm btn-secondary" onClick={openInHistory}>
            Save to chat history
          </button>
        </div>
        <div className="chatbot-messages">
          {messages.map((m, i) => (
            <div key={i} className={`chatbot-bubble ${m.role}`}>{m.text}</div>
          ))}
          <div ref={bottomRef} />
        </div>
        <form className="row" onSubmit={askBot}>
          <input
            style={{ flex: 1 }}
            value={chatInput}
            onChange={(e) => setChatInput(e.target.value)}
            placeholder="Ask the assistant…"
          />
          <VoiceRecorder onTranscript={setChatInput} />
          <button className="btn" type="submit" disabled={sending}>
            {sending ? '…' : 'Send'}
          </button>
        </form>
      </div>

      <h2>Contact queries</h2>
      {loading ? (
        <p>Loading…</p>
      ) : (
        <div className="table-wrap panel">
          <table className="data-table">
            <thead>
              <tr>
                <th>From</th>
                <th>Subject</th>
                <th>Message</th>
                <th>Status</th>
                <th>Update</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((q) => (
                <tr key={q.id}>
                  <td>
                    <div>{q.userName}</div>
                    <div className="muted" style={{ fontSize: '0.85rem' }}>{q.userEmail}</div>
                  </td>
                  <td>{q.subject}</td>
                  <td style={{ maxWidth: 280 }}>{q.message}</td>
                  <td><span className="badge badge-new">{q.status}</span></td>
                  <td>
                    <select value={q.status} onChange={(e) => updateStatus(q.id, e.target.value)}>
                      <option value="NEW">NEW</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="RESOLVED">RESOLVED</option>
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!rows.length && <div className="empty">No queries yet.</div>}
        </div>
      )}
    </div>
  );
}
