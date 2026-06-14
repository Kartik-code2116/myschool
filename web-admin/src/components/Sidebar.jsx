import { NavLink, useNavigate } from 'react-router-dom';
import { signOut } from 'firebase/auth';
import { auth } from '../firebase';
import './Sidebar.css';

export default function Sidebar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    signOut(auth).then(() => navigate('/'));
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="sidebar-logo">MS</div>
        <div>
          <h2>MySchool</h2>
          <span>Admin Console</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        <NavLink to="/admin" end className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">D</span>
          <span>Dashboard</span>
        </NavLink>
        <NavLink to="/admin/subscriptions" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">S</span>
          <span>Subscriptions</span>
        </NavLink>
        <NavLink to="/admin/users" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">U</span>
          <span>Users</span>
        </NavLink>
        <NavLink to="/admin/subjects" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">📚</span>
          <span>Subjects</span>
        </NavLink>
        <NavLink to="/admin/remarks" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">R</span>
          <span>Remarks</span>
        </NavLink>
        <NavLink to="/admin/profile" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
          <span className="nav-icon">P</span>
          <span>Admin Profile</span>
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <button onClick={handleLogout} className="btn btn-danger logout-btn">Logout</button>
      </div>
    </aside>
  );
}
