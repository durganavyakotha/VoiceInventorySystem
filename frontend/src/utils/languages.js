export const LanguageNames = {
  en: 'English',
  te: 'Telugu',
  hi: 'Hindi',
  ta: 'Tamil',
  kn: 'Kannada',
};

export function fullLanguage(code) {
  if (!code) return 'English';
  const c = String(code).toLowerCase();
  if (LanguageNames[c]) return LanguageNames[c];
  if (c.startsWith('te')) return 'Telugu';
  if (c.startsWith('hi')) return 'Hindi';
  if (c.startsWith('ta')) return 'Tamil';
  if (c.startsWith('kn')) return 'Kannada';
  return code;
}
