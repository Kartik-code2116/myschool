import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { signOut } from 'firebase/auth';
import { auth } from '../firebase';
import './Sidebar.css';

export default function Sidebar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    signOut(auth).then(() => navigate('/login'));
  };

  return (
    <div className="sidebar glass-panel">
      <div className="sidebar-brand">
        <h2>MySchool</h2>
        <span className="badge">Admin</span>
      </div>
      
      <nav className="sidebar-nav">
        <NavLink to="/" end className={({isActive}) => isActive ? "nav-item active" : "nav-item"}>
          Subscriptions
        </NavLink>
        <NavLink to="/users" className={({isActive}) => isActive ? "nav-item active" : "nav-item"}>
          Users
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <button onClick={handleLogout} className="btn btn-danger logout-btn">Logout</button>
      </div>
    </div>
  );
}
