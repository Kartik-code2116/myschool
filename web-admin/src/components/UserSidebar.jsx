import { NavLink, useNavigate } from 'react-router-dom';
import { signOut } from 'firebase/auth';
import { auth } from '../firebase';
import './Sidebar.css'; // Reusing the same admin sidebar CSS for identical aesthetic

export default function UserSidebar({ isOpen, onClose }) {
  const navigate = useNavigate();

  const handleLogout = () => {
    signOut(auth).then(() => navigate('/'));
  };

  return (
    <>
      <div 
        className={`sidebar-overlay ${isOpen ? 'show' : ''}`}
        onClick={onClose}
      />
      <div className={`admin-sidebar ${isOpen ? 'open' : 'closed'}`}>
        <div className="sidebar-header">
          <div className="brand-logo">EduReport</div>
          {isOpen && (
            <button className="close-sidebar-btn" onClick={onClose} aria-label="Close sidebar">
              ×
            </button>
          )}
        </div>

        <div className="sidebar-user">
          <div className="user-avatar">
            {auth.currentUser?.email?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div className="user-info">
            <span className="user-email">{auth.currentUser?.email}</span>
            <span className="user-role">Teacher</span>
          </div>
        </div>

        <nav className="sidebar-nav">
          <NavLink to="/app" end className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <span className="nav-icon">🏠</span>
            <span className="nav-text">Dashboard</span>
          </NavLink>
          
          <div className="nav-section-title">Classroom</div>
          
          <NavLink to="/app/students" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <span className="nav-icon">👥</span>
            <span className="nav-text">Students</span>
          </NavLink>
          <NavLink to="/app/attendance" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <span className="nav-icon">📋</span>
            <span className="nav-text">Attendance</span>
          </NavLink>
          <NavLink to="/app/marks" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <span className="nav-icon">📝</span>
            <span className="nav-text">Marks Entry</span>
          </NavLink>
          <NavLink to="/app/reports" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <span className="nav-icon">📄</span>
            <span className="nav-text">Report Cards</span>
          </NavLink>

          <div className="nav-section-title">Account</div>
          <NavLink to="/app/settings" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <span className="nav-icon">⚙️</span>
            <span className="nav-text">Settings</span>
          </NavLink>
        </nav>

        <div className="sidebar-footer">
          <button onClick={handleLogout} className="btn-logout">
            <span className="nav-icon">🚪</span>
            <span className="nav-text">Sign Out</span>
          </button>
        </div>
      </div>
    </>
  );
}
