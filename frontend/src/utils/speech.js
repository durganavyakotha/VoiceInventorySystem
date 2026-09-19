/** Speech helpers: STT language codes + browser TTS (free Web Speech API). */

export function toSpeechLocale(lang) {
  if (!lang) return 'en-IN';
  const l = String(lang).toLowerCase();
  if (l.startsWith('te') || l.includes('telugu')) return 'te-IN';
  if (l.startsWith('hi') || l.includes('hindi')) return 'hi-IN';
  if (l.startsWith('ta') || l.includes('tamil')) return 'ta-IN';
  if (l.startsWith('kn') || l.includes('kannada')) return 'kn-IN';
  return 'en-IN';
}

export function speakText(text, locale = 'en-IN') {
  if (!text || typeof window === 'undefined' || !window.speechSynthesis) return;
  window.speechSynthesis.cancel();
  const utter = new SpeechSynthesisUtterance(text);
  utter.lang = locale;
  utter.rate = 0.95;
  const voices = window.speechSynthesis.getVoices();
  const match = voices.find((v) => v.lang === locale)
    || voices.find((v) => v.lang.startsWith(locale.split('-')[0]));
  if (match) utter.voice = match;
  window.speechSynthesis.speak(utter);
}

export function stopSpeaking() {
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel();
  }
}
