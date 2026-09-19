import { useCallback, useEffect, useRef, useState } from 'react';

export default function VoiceRecorder({ onTranscript, lang = 'en-IN' }) {
  const [supported, setSupported] = useState(false);
  const [listening, setListening] = useState(false);
  const recognitionRef = useRef(null);
  const onTranscriptRef = useRef(onTranscript);
  onTranscriptRef.current = onTranscript;

  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) return undefined;
    setSupported(true);
    const recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = true;
    recognition.lang = lang;
    recognition.onresult = (event) => {
      let transcript = '';
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        transcript += event.results[i][0].transcript;
      }
      onTranscriptRef.current?.(transcript.trim());
    };
    recognition.onend = () => setListening(false);
    recognition.onerror = () => setListening(false);
    recognitionRef.current = recognition;
    return () => {
      recognition.stop();
    };
  }, [lang]);

  const toggle = useCallback(() => {
    const recognition = recognitionRef.current;
    if (!recognition) return;
    if (listening) {
      recognition.stop();
      setListening(false);
    } else {
      recognition.start();
      setListening(true);
    }
  }, [listening]);

  if (!supported) {
    return <p className="muted">Speech recognition is not available in this browser. Type instead.</p>;
  }

  return (
    <button type="button" className={`btn ${listening ? 'btn-clay' : 'btn-secondary'}`} onClick={toggle}>
      {listening ? 'Stop listening' : 'Speak'}
      {listening && <span className="listening"> ●</span>}
    </button>
  );
}
