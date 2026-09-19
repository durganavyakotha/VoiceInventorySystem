import { createContext, useContext, useMemo } from 'react';
import { useAuth } from './AuthContext';
import { normalizeLang, t as translate } from '../i18n/translations';

const LanguageContext = createContext(null);

export function LanguageProvider({ children }) {
  const { user } = useAuth();
  const lang = normalizeLang(user?.language || 'en');

  const value = useMemo(
    () => ({
      lang,
      t: (key) => translate(key, lang),
    }),
    [lang]
  );

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useI18n() {
  const ctx = useContext(LanguageContext);
  if (!ctx) {
    return { lang: 'en', t: (key) => translate(key, 'en') };
  }
  return ctx;
}
