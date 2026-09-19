import { useCallback, useEffect, useRef, useState } from 'react';
import { chatService } from '../services/chatService';
import { useAuth } from '../context/AuthContext';
import VoiceRecorder from './VoiceRecorder';
import { speakText, stopSpeaking, toSpeechLocale } from '../utils/speech';

export default function ChatBox({ conversationId, otherUser, voiceFirst = false }) {
  const { user } = useAuth();
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);
  const [autoPlay, setAutoPlay] = useState(true);
  const bottomRef = useRef(null);
  const heardIds = useRef(new Set());

  const myLang = user?.language || 'en';
  const theirLang = otherUser?.language || 'en';
  const listenLocale = toSpeechLocale(myLang);

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
    heardIds.current = new Set();
    load();
    const timer = setInterval(load, 5000);
    return () => {
      clearInterval(timer);
      stopSpeaking();
    };
  }, [load]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Auto TTS for incoming translated messages (receiver hears in their language)
  useEffect(() => {
    if (!autoPlay || !user?.id) return;
    messages.forEach((m) => {
      if (m.senderId === user.id) return;
      if (heardIds.current.has(m.id)) return;
      heardIds.current.add(m.id);
      const toSpeak = m.translatedText || m.messageText || m.transcript;
      const locale = m.speechLocale || toSpeechLocale(myLang);
      if (toSpeak) speakText(toSpeak, locale);
    });
  }, [messages, autoPlay, user?.id, myLang]);

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
        sourceLanguage: myLang,
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
        sourceLanguage: myLang,
      });
      setMessages((prev) => [...prev, data]);
      setText('');
    } catch (err) {
      setError(err.response?.data?.message || 'Voice send failed');
    } finally {
      setSending(false);
    }
  };

  const playMessage = (m) => {
    const mine = m.senderId === user?.id;
    const toSpeak = mine
      ? (m.messageText || m.transcript)
      : (m.translatedText || m.messageText || m.transcript);
    const locale = mine
      ? (m.sourceSpeechLocale || toSpeechLocale(myLang))
      : (m.speechLocale || toSpeechLocale(myLang));
    speakText(toSpeak, locale);
  };

  if (!conversationId) {
    return <div className="empty">Select a conversation</div>;
  }

  return (
    <div className={`chat-box panel ${voiceFirst ? 'voice-first' : ''}`}>
      <div className="row space-between" style={{ alignItems: 'center' }}>
        <h3 style={{ margin: 0 }}>
          {otherUser
            ? `${otherUser.firstName || ''} ${otherUser.lastName || ''}`.trim()
            : 'Chat'}
          {otherUser?.language && (
            <span className="muted" style={{ fontSize: '0.85rem', fontWeight: 400, marginLeft: 8 }}>
              · {otherUser.languageDisplay || theirLang}
            </span>
          )}
        </h3>
        <label className="row" style={{ gap: '0.35rem', fontSize: '0.85rem' }}>
          <input type="checkbox" checked={autoPlay} onChange={(e) => setAutoPlay(e.target.checked)} />
          Auto listen
        </label>
      </div>
      <p className="muted" style={{ margin: '0.35rem 0 0.75rem', fontSize: '0.85rem' }}>
        Speak in your language → AI translates to theirs → they hear speech (and vice versa).
      </p>
      {voiceFirst && (
        <div className="voice-first-bar row" style={{ marginBottom: '0.75rem', gap: '0.5rem', flexWrap: 'wrap' }}>
          <VoiceRecorder onTranscript={sendVoice} lang={listenLocale} />
          <span className="muted" style={{ fontSize: '0.85rem' }}>
            Hold Speak, talk in {myLang?.toUpperCase() || 'EN'}, then stop — message is translated for them.
          </span>
        </div>
      )}
      {error && <div className="alert alert-error">{error}</div>}
      {loading && messages.length === 0 && <p className="muted">Loading…</p>}
      <div className="chat-messages">
        {messages.map((m) => {
          const mine = m.senderId === user?.id;
          return (
            <div key={m.id} className={`bubble ${mine ? 'mine' : 'theirs'}`}>
              <div>
                {m.messageType === 'VOICE' && <strong>🎤 </strong>}
                {mine ? (m.messageText || m.transcript) : (m.translatedText || m.messageText || m.transcript)}
              </div>
              {mine && m.translatedText && m.translatedText !== m.messageText && (
                <div className="translated">→ for them: {m.translatedText}</div>
              )}
              {!mine && m.messageText && m.translatedText && m.translatedText !== m.messageText && (
                <div className="translated">Original: {m.messageText}</div>
              )}
              <div className="meta row" style={{ justifyContent: 'space-between' }}>
                <span>
                  {m.messageType || 'TEXT'} · {m.createdAt ? new Date(m.createdAt).toLocaleString() : ''}
                </span>
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => playMessage(m)}>
                  🔊
                </button>
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
          placeholder="Type or use voice…"
        />
        {!voiceFirst && <VoiceRecorder onTranscript={sendVoice} lang={listenLocale} />}
        <button className="btn" type="submit" disabled={sending || !text.trim()}>
          Send
        </button>
      </form>
    </div>
  );
}
