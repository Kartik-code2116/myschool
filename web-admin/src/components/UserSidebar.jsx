import { NavLink, useNavigate } from 'react-router-dom';
import { signOut } from 'firebase/auth';
import { auth } from '../firebase';
import './Sidebar.css';

export default function UserSidebar({ isOpen, onClose }) {
  const navigate = useNavigate();

  const handleLogout = () => {
    signOut(auth).then(() => navigate('/'));
  };

  return (
    <aside className={`sidebar ${isOpen ? 'open' : 'closed'}`}>
      <button className="sidebar-close-btn" onClick={onClose} title="Close Sidebar">
        &times;
      </button>
      <div className="sidebar-brand">
        <div className="sidebar-logo">MS</div>
        <div>
          <h2>EduReport</h2>
          <span>Teacher Console</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        <NavLink to="/app" end className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">🏠</span>
          <span>Dashboard</span>
        </NavLink>
        <NavLink to="/app/students" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">👥</span>
          <span>Students</span>
        </NavLink>
        <NavLink to="/app/attendance" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">📋</span>
          <span>Attendance</span>
        </NavLink>
        <NavLink to="/app/marks" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">📝</span>
          <span>Marks Entry</span>
        </NavLink>
        <NavLink to="/app/remarks" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">⭐</span>
          <span>Remarks</span>
        </NavLink>
        <NavLink to="/app/reports" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">📊</span>
          <span>Report Cards</span>
        </NavLink>
        <NavLink to="/app/subjects" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">📚</span>
          <span>Subjects</span>
        </NavLink>
        <NavLink to="/app/settings" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">⚙️</span>
          <span>Settings</span>
        </NavLink>
        <NavLink to="/app/profile" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">👤</span>
          <span>Profile</span>
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <button onClick={handleLogout} className="btn btn-danger logout-btn">Logout</button>
      </div>
    </aside>
  );
}
