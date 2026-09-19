import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '../services/api';
import { chatService } from '../services/chatService';
import { useAuth } from '../context/AuthContext';
import { mediaUrl } from '../services/api';
import ChatBox from './ChatBox';
import { LanguageNames } from '../utils/languages';

export default function VoiceContactFab() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [contacts, setContacts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [active, setActive] = useState(null); // { conversationId, otherUser }

  const role = user?.role;
  const show = role === 'SHOPKEEPER' || role === 'VENDOR';
  const onDashboard = location.pathname.startsWith('/shopkeeper') || location.pathname.startsWith('/vendor');

  useEffect(() => {
    if (!open || !show) return;
    setLoading(true);
    setError('');
    api.get('/api/contacts')
      .then(({ data }) => setContacts(data || []))
      .catch((err) => setError(err.response?.data?.message || 'Failed to load contacts'))
      .finally(() => setLoading(false));
  }, [open, show]);

  if (!show || !onDashboard) return null;

  const startChat = async (contact) => {
    try {
      const { data } = await chatService.getOrCreate(contact.id);
      setActive({
        conversationId: data.id,
        otherUser: data.otherUser || {
          id: contact.id,
          firstName: contact.firstName,
          lastName: contact.lastName,
          language: contact.language,
          languageDisplay: contact.languageDisplay,
          email: contact.email,
          role: contact.role,
        },
      });
    } catch (err) {
      setError(err.response?.data?.message || 'Could not start chat');
    }
  };

  const closeAll = () => {
    setActive(null);
    setOpen(false);
  };

  const goHistory = () => {
    const base = role === 'VENDOR' ? '/vendor/chat' : '/shopkeeper/chat';
    if (active?.conversationId) {
      navigate(`${base}?c=${active.conversationId}`);
    } else {
      navigate(base);
    }
    closeAll();
  };

  return (
    <>
      <button
        type="button"
        className="voice-fab"
        title="Voice call / chat"
        aria-label="Voice contact"
        onClick={() => {
          setOpen(true);
          setActive(null);
        }}
      >
        <svg className="voice-fab-svg" viewBox="0 0 24 24" width="28" height="28" aria-hidden>
          <path
            fill="currentColor"
            d="M12 14a3 3 0 0 0 3-3V6a3 3 0 0 0-6 0v5a3 3 0 0 0 3 3zm5-3a5 5 0 0 1-10 0H5a7 7 0 0 0 6 6.92V21h2v-3.08A7 7 0 0 0 19 11h-2z"
          />
        </svg>
        <span className="voice-fab-label">Chat</span>
      </button>

      {open && (
        <div className="modal-backdrop" onClick={closeAll} role="presentation">
          <div
            className="modal-panel voice-contact-modal"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-label="Voice communication"
          >
            {!active ? (
              <>
                <div className="row space-between">
                  <h2 style={{ margin: 0 }}>Who do you want to contact?</h2>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={closeAll}>Close</button>
                </div>
                <p className="muted">
                  Speak in your language ({LanguageNames[user?.language] || user?.language || 'English'}).
                  AI translates and plays audio for them.
                </p>
                {error && <div className="alert alert-error">{error}</div>}
                {loading ? (
                  <p>Loading contacts…</p>
                ) : (
                  <div className="contact-pick-grid">
                    {contacts.map((c) => (
                      <button
                        key={c.id}
                        type="button"
                        className="contact-pick-card"
                        onClick={() => startChat(c)}
                      >
                        {c.profileImageUrl ? (
                          <img src={mediaUrl(c.profileImageUrl)} alt="" className="vendor-avatar" />
                        ) : (
                          <span className="vendor-avatar fallback">{(c.firstName || '?')[0]}</span>
                        )}
                        <div>
                          <strong>{c.firstName} {c.lastName}</strong>
                          <div className="muted" style={{ fontSize: '0.85rem' }}>
                            {c.role} · {c.languageDisplay || c.language} · {c.location || '—'}
                          </div>
                        </div>
                      </button>
                    ))}
                    {!contacts.length && <div className="empty">No contacts found.</div>}
                  </div>
                )}
              </>
            ) : (
              <>
                <div className="row space-between" style={{ marginBottom: '0.75rem' }}>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => setActive(null)}>
                    ← Back
                  </button>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={goHistory}>
                    Open in Chat History
                  </button>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={closeAll}>Close</button>
                </div>
                <ChatBox
                  conversationId={active.conversationId}
                  otherUser={active.otherUser}
                  voiceFirst
                />
              </>
            )}
          </div>
        </div>
      )}
    </>
  );
}
