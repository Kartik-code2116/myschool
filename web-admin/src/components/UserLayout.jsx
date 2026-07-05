import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import UserSidebar from './UserSidebar';
import { TeacherProvider } from '../context/TeacherContext';
import './UserLayout.css';

export default function UserLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  return (
    <TeacherProvider>
      <div className={`app-layout ${isSidebarOpen ? 'sidebar-open' : 'sidebar-closed'}`}>
        {!isSidebarOpen && (
          <button 
            className="toggle-sidebar-btn" 
            onClick={() => setIsSidebarOpen(true)}
            title="Open Sidebar"
          >
            ☰
          </button>
        )}
        <UserSidebar isOpen={isSidebarOpen} onClose={() => setIsSidebarOpen(false)} />
        <div className="main-content">
          <Outlet />
        </div>
      </div>
    </TeacherProvider>
  );
}
