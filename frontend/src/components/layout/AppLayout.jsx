import React, { useEffect, useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopHeader from './TopHeader';
import { isAuthenticated } from '../../utils/auth';
import { getUiPreferences, saveUiPreferences } from '../../utils/uiPreferences';
import { LocaleProvider } from '../../utils/LocaleContext';

const AppLayout = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [preferences, setPreferences] = useState(getUiPreferences);

  useEffect(() => {
    saveUiPreferences(preferences);
  }, [preferences]);

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return (
    <LocaleProvider language={preferences.language}>
      <div className="app-layout" data-theme={preferences.theme}>
        <Sidebar collapsed={collapsed} />
        <div className="app-main">
          <TopHeader
            collapsed={collapsed}
            setCollapsed={setCollapsed}
            preferences={preferences}
            onPreferencesChange={setPreferences}
          />
          <main className="page-content">
            <Outlet />
          </main>
        </div>
      </div>
    </LocaleProvider>
  );
};

export default AppLayout;
