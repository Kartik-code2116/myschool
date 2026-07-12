import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { onAuthStateChanged, getRedirectResult } from 'firebase/auth';
import { doc, setDoc, getDoc } from 'firebase/firestore';
import { auth, db, checkIsAdmin, logAdminLogin } from './firebase';
import Login from './Login';
import Dashboard from './Dashboard';
import Layout from './components/Layout';
import UserLayout from './components/UserLayout';
import UsersList from './pages/UsersList';
import UserDetail from './pages/UserDetail';
import AdminProfile from './pages/AdminProfile';
import LandingPage from './pages/LandingPage';
import AppRedirect from './pages/AppRedirect';
import AppDashboard from './pages/AppDashboard';
import AppStudents from './pages/AppStudents';
import AppAttendance from './pages/AppAttendance';
import AppMarks from './pages/AppMarks';
import AppSettings from './pages/AppSettings';
import AppRemarks from './pages/AppRemarks';
import Subscriptions from './pages/Subscriptions';
import AppSubjects from './pages/AppSubjects';
import AppReports from './pages/AppReports';
import AppProfile from './pages/AppProfile';
import AppSubscription from './pages/AppSubscription';
import AdminRemarks from './pages/AdminRemarks';
import AdminSubjects from './pages/AdminSubjects';
import AdminWeightage from './pages/AdminWeightage';

const getInitialTheme = () => {
  const savedTheme = localStorage.getItem('myschool-theme');
  if (savedTheme) return savedTheme;
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

function ProtectedRoute({ children, user, loading, requireAdmin, isAdmin }) {
  if (loading) return <div className="loading">Loading...</div>;
  if (!user) {
    return <Navigate to="/" replace />;
  }
  if (requireAdmin && !isAdmin) {
    return <Navigate to="/app" replace />;
  }
  if (!requireAdmin && isAdmin) {
    return <Navigate to="/admin" replace />;
  }
  return children;
}

export default function App() {
  const [user, setUser] = useState(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [loading, setLoading] = useState(true);
  const [theme, setTheme] = useState(getInitialTheme);
  const [lang, setLang] = useState(() => localStorage.getItem('myschool-lang') || 'en');

  useEffect(() => {
    const checkRedirect = async () => {
      try {
        const result = await getRedirectResult(auth);
        if (result?.user) {
          const user = result.user;
          const teacherRef = doc(db, 'teachers', user.uid);
          const teacherDoc = await getDoc(teacherRef);
          if (!teacherDoc.exists()) {
            await setDoc(teacherRef, {
              id: user.uid,
              name: user.displayName || 'Teacher',
              email: user.email,
              phone: user.phoneNumber || '',
              schoolName: '',
              udiseCode: '',
              schoolIds: []
            });
          }
          await logAdminLogin(user, 'Google');
        }
      } catch (e) {
        console.error("Redirect login error:", e);
      }
    };
    checkRedirect();

    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      if (currentUser) {
        setUser(currentUser);
        const adminStatus = checkIsAdmin(currentUser);
        setIsAdmin(adminStatus);
      } else {
        setUser(null);
        setIsAdmin(false);
      }
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
    window.dispatchEvent(new Event('languageChange'));
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
          user ? (isAdmin ? <Navigate to="/admin" replace /> : <Navigate to="/app" replace />) :
          <Login />
        } />

        <Route path="/app" element={
          <ProtectedRoute user={user} loading={loading} requireAdmin={false} isAdmin={isAdmin}>
            <UserLayout />
          </ProtectedRoute>
        }>
          <Route index element={<AppDashboard />} />
          <Route path="students" element={<AppStudents />} />
          <Route path="attendance" element={<AppAttendance />} />
          <Route path="marks" element={<AppMarks />} />
          <Route path="remarks" element={<AppRemarks />} />
          <Route path="subjects" element={<AppSubjects />} />
          <Route path="reports" element={<AppReports />} />
          <Route path="settings" element={<AppSettings />} />
          <Route path="profile" element={<AppProfile />} />
          <Route path="subscription" element={<AppSubscription />} />
        </Route>

        <Route path="/app-redirect" element={
          <ProtectedRoute user={user} loading={loading} requireAdmin={false} isAdmin={isAdmin}>
            <AppRedirect user={user} lang={lang} />
          </ProtectedRoute>
        } />
        
        <Route path="/admin" element={
          <ProtectedRoute user={user} loading={loading} requireAdmin={true} isAdmin={isAdmin}>
            <Layout />
          </ProtectedRoute>
        }>
          <Route index element={<Dashboard />} />
          <Route path="subscriptions" element={<Subscriptions />} />
          <Route path="users" element={<UsersList />} />
          <Route path="users/:id" element={<UserDetail />} />
          <Route path="subjects" element={<AdminSubjects />} />
          <Route path="weightage" element={<AdminWeightage />} />
          <Route path="remarks" element={<AdminRemarks />} />
          <Route path="profile" element={<AdminProfile />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
