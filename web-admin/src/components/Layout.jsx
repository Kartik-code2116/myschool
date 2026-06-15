import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import './Layout.css';

export default function Layout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  return (
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
      <Sidebar isOpen={isSidebarOpen} onClose={() => setIsSidebarOpen(false)} />
      <div className="main-content">
        <Outlet />
      </div>
    </div>
  );
}
