import { useState, useEffect } from 'react';

export default function useLanguage() {
  const [lang, setLang] = useState(localStorage.getItem('myschool-lang') || 'en');

  useEffect(() => {
    const handleLangChange = () => setLang(localStorage.getItem('myschool-lang') || 'en');
    window.addEventListener('languageChange', handleLangChange);
    return () => window.removeEventListener('languageChange', handleLangChange);
  }, []);

  const t = (en, mr) => (lang === 'mr' && mr) ? mr : en;

  return { lang, t };
}
