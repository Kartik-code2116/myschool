import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { onAuthStateChanged } from 'firebase/auth';
import { auth, checkIsAdmin } from './firebase';
import Login from './Login';
import Dashboard from './Dashboard';
import Layout from './components/Layout';
import UsersList from './pages/UsersList';
import UserDetail from './pages/UserDetail';
import AdminProfile from './pages/AdminProfile';
import LandingPage from './pages/LandingPage';
import AppRedirect from './pages/AppRedirect';
import Subscriptions from './pages/Subscriptions';

const getInitialTheme = () => {
  const savedTheme = localStorage.getItem('myschool-theme');
  if (savedTheme) return savedTheme;
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

function ProtectedRoute({ children, user, loading, requireAdmin }) {
  if (loading) return <div className="loading">Loading...</div>;
  if (!user) {
    return <Navigate to={requireAdmin ? "/admin-login" : "/"} replace />;
  }
  const isAdmin = checkIsAdmin(user.email);
  if (requireAdmin && !isAdmin) {
    return <Navigate to="/app-redirect" replace />;
  }
  if (!requireAdmin && isAdmin) {
    return <Navigate to="/admin" replace />;
  }
  return children;
}

export default function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [theme, setTheme] = useState(getInitialTheme);
  const [lang, setLang] = useState(() => localStorage.getItem('myschool-lang') || 'en');

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem('myschool-theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((currentTheme) => currentTheme === 'dark' ? 'light' : 'dark');
  };

  const toggleLanguage = () => {
    const newLang = lang === 'en' ? 'mr' : 'en';
    setLang(newLang);
    localStorage.setItem('myschool-lang', newLang);
  };

  return (
    <BrowserRouter>
      <div className="global-actions-panel">
        <button onClick={toggleLanguage} className="action-btn lang-btn" type="button">
          🌐 {lang === 'en' ? 'मराठी' : 'English'}
        </button>
        <button
          className="action-btn theme-btn"
          onClick={toggleTheme}
          type="button"
          aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
        >
          <span className="theme-toggle-track">
            <span className="theme-toggle-thumb" />
          </span>
          <span>{theme === 'dark' ? 'Light' : 'Dark'}</span>
        </button>
      </div>

      <Routes>
        <Route path="/" element={<LandingPage user={user} loading={loading} lang={lang} />} />
        
        <Route path="/admin-login" element={
          loading ? <div className="loading">Loading...</div> :
          user ? (checkIsAdmin(user.email) ? <Navigate to="/admin" replace /> : <Navigate to="/app-redirect" replace />) :
          <Login />
        } />

        <Route path="/app-redirect" element={
          <ProtectedRoute user={user} loading={loading} requireAdmin={false}>
            <AppRedirect user={user} lang={lang} />
          </ProtectedRoute>
        } />
        
        <Route path="/admin" element={
          <ProtectedRoute user={user} loading={loading} requireAdmin={true}>
            <Layout />
          </ProtectedRoute>
        }>
          <Route index element={<Dashboard />} />
          <Route path="subscriptions" element={<Subscriptions />} />
          <Route path="users" element={<UsersList />} />
          <Route path="users/:id" element={<UserDetail />} />
          <Route path="profile" element={<AdminProfile />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
