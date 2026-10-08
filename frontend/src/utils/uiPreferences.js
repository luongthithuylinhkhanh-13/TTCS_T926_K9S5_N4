const PREFERENCES_KEY = 'constructflow_preferences';
const DEFAULT_PREFERENCES = { theme: 'light', language: 'vi' };

export const getUiPreferences = () => {
  try {
    const stored = JSON.parse(localStorage.getItem(PREFERENCES_KEY) || '{}');
    return {
      theme: stored.theme === 'dark' ? 'dark' : 'light',
      language: stored.language === 'en' ? 'en' : 'vi',
    };
  } catch (error) {
    console.error('Error reading UI preferences:', error);
    return DEFAULT_PREFERENCES;
  }
};

export const saveUiPreferences = preferences => {
  const next = {
    theme: preferences.theme === 'dark' ? 'dark' : 'light',
    language: preferences.language === 'en' ? 'en' : 'vi',
  };
  localStorage.setItem(PREFERENCES_KEY, JSON.stringify(next));
  document.documentElement.dataset.theme = next.theme;
  document.documentElement.lang = next.language;
  return next;
};
