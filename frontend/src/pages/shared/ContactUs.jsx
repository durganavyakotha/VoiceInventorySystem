import { useState } from 'react';
import { contactService } from '../../services/contactService';

export default function ContactUs() {
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [sending, setSending] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSending(true);
    try {
      await contactService.submit({ subject, message });
      setSuccess('Message sent to admin');
      setSubject('');
      setMessage('');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send');
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="stack" style={{ maxWidth: 560 }}>
      <h1>Contact us</h1>
      <p className="muted">Send a query to the VoiceStock admin team.</p>
      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}
      <form className="panel form-grid" onSubmit={onSubmit}>
        <label>
          Subject
          <input value={subject} onChange={(e) => setSubject(e.target.value)} required />
        </label>
        <label>
          Message
          <textarea rows={5} value={message} onChange={(e) => setMessage(e.target.value)} required />
        </label>
        <button className="btn" type="submit" disabled={sending}>
          {sending ? 'Sending…' : 'Send'}
        </button>
      </form>
    </div>
  );
}
