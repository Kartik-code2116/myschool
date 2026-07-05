import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTeacherContext } from '../context/TeacherContext';
import { db, auth } from '../firebase';
import { collection, query, where, getDocs } from 'firebase/firestore';
import './AppDashboard.css';

export default function AppDashboard() {
  const { activeClass } = useTeacherContext();
  const [stats, setStats] = useState({ students: 0, attendance: 0 });
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    async function loadStats() {
      if (!activeClass || !auth.currentUser) {
        setLoading(false);
        return;
      }
      try {
        const studentQ = query(collection(db, 'students'), where('classId', '==', activeClass.id));
        const sSnap = await getDocs(studentQ);
        const studentCount = sSnap.size;
        setStats({ students: studentCount, attendance: 95 }); // Mock attendance for now
      } catch (err) {
        console.error("Failed to load stats", err);
      } finally {
        setLoading(false);
      }
    }
    loadStats();
  }, [activeClass]);

  return (
    <div className="app-dashboard">
      <div className="header-greeting">
        <h2>Hello, {auth.currentUser?.email}</h2>
        <p>Manage your classroom seamlessly.</p>
      </div>

      <div className="class-status-card">
        {activeClass ? (
          <>
            <span className="badge">Active Class</span>
            <h3>{activeClass.name}</h3>
            <div className="class-stats-row">
              <div className="stat-item">
                <strong>{loading ? '...' : stats.students}</strong>
                <span>Students</span>
              </div>
              <div className="stat-item">
                <strong>{loading ? '...' : `${stats.attendance}%`}</strong>
                <span>Attendance</span>
              </div>
            </div>
          </>
        ) : (
          <div className="no-class">
            <p>No active class selected.</p>
            <button className="btn-primary-small" onClick={() => navigate('/app/settings')}>Select Class</button>
          </div>
        )}
      </div>

      <h3 className="section-title">Modules</h3>
      <div className="modules-grid">
        <div className="module-card" onClick={() => navigate('/app/marks')}>
          <span className="icon">📝</span>
          <span>Marks Entry</span>
        </div>
        <div className="module-card" onClick={() => navigate('/app/attendance')}>
          <span className="icon">📋</span>
          <span>Attendance</span>
        </div>
        <div className="module-card" onClick={() => navigate('/app/reports')}>
          <span className="icon">📄</span>
          <span>Report Cards</span>
        </div>
        <div className="module-card" onClick={() => navigate('/app/analytics')}>
          <span className="icon">📊</span>
          <span>Analytics</span>
        </div>
      </div>
    </div>
  );
}
