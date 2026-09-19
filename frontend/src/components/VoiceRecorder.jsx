import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Browser speech recognition.
 * @param {function} onTranscript
 * @param {string} lang BCP-47 locale e.g. en-IN, te-IN
 * @param {boolean} finalOnly If true, only emit final results (avoids garbled interim text)
 */
export default function VoiceRecorder({ onTranscript, lang = 'en-IN', finalOnly = false }) {
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
    recognition.interimResults = !finalOnly;
    recognition.lang = lang;
    recognition.onresult = (event) => {
      let transcript = '';
      let isFinal = false;
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        transcript += event.results[i][0].transcript;
        if (event.results[i].isFinal) isFinal = true;
      }
      const text = transcript.trim();
      if (!text) return;
      if (finalOnly && !isFinal) return;
      onTranscriptRef.current?.(text);
    };
    recognition.onend = () => setListening(false);
    recognition.onerror = () => setListening(false);
    recognitionRef.current = recognition;
    return () => {
      try {
        recognition.stop();
      } catch {
        /* ignore */
      }
    };
  }, [lang, finalOnly]);

  const toggle = useCallback(() => {
    const recognition = recognitionRef.current;
    if (!recognition) return;
    if (listening) {
      recognition.stop();
      setListening(false);
    } else {
      try {
        recognition.lang = lang;
        recognition.start();
        setListening(true);
      } catch {
        setListening(false);
      }
    }
  }, [listening, lang]);

  if (!supported) {
    return <p className="muted">Speech recognition unavailable — type the command instead.</p>;
  }

  return (
    <button type="button" className={`btn ${listening ? 'btn-clay' : 'btn-secondary'}`} onClick={toggle}>
      {listening ? 'Stop listening' : 'Speak'}
      {listening && <span className="listening"> ●</span>}
    </button>
  );
}
