import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { doc, getDoc, collection, query, where, getDocs } from 'firebase/firestore';
import { db } from '../firebase';
import './UserDetail.css';

export default function UserDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [classes, setClasses] = useState([]);
  const [students, setStudents] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedScreenshot, setSelectedScreenshot] = useState(null);

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        // 1. Fetch User Profile
        const userDoc = await getDoc(doc(db, "teachers", id));
        if (userDoc.exists()) {
          setUser({ id: userDoc.id, ...userDoc.data() });
        } else {
          console.error("User not found");
          setLoading(false);
          return;
        }

        // 2. Fetch Classes
        const classesQuery = query(collection(db, "classes"), where("teacherId", "==", id));
        const classesSnap = await getDocs(classesQuery);
        const classesData = [];
        classesSnap.forEach(doc => classesData.push({ id: doc.id, ...doc.data() }));
        setClasses(classesData);

        // 3. Fetch Students
        const studentsQuery = query(collection(db, "students"), where("teacherId", "==", id));
        const studentsSnap = await getDocs(studentsQuery);
        const studentsData = [];
        studentsSnap.forEach(doc => studentsData.push({ id: doc.id, ...doc.data() }));
        setStudents(studentsData);

        // 4. Fetch Subscriptions (Check both new 'teacherId' and old 'userId' fields for backward compatibility)
        const subsQuery1 = query(collection(db, "subscriptions"), where("teacherId", "==", id));
        const subsQuery2 = query(collection(db, "subscriptions"), where("userId", "==", id));
        const [subsSnap1, subsSnap2] = await Promise.all([getDocs(subsQuery1), getDocs(subsQuery2)]);
        
        const subsMap = new Map();
        subsSnap1.forEach(doc => subsMap.set(doc.id, { id: doc.id, ...doc.data() }));
        subsSnap2.forEach(doc => subsMap.set(doc.id, { id: doc.id, ...doc.data() }));
        
        const subsData = Array.from(subsMap.values());
        // Sort descending
        subsData.sort((a, b) => b.timestamp - a.timestamp);
        setSubscriptions(subsData);

      } catch (err) {
        console.error("Error fetching user data:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchUserData();
  }, [id]);

  if (loading) return <div className="loading">Loading user details...</div>;
  if (!user) return <div className="loading">User not found</div>;

  return (
    <div className="user-detail-page">
      <div className="back-nav">
        <button className="btn" onClick={() => navigate('/users')}>&larr; Back to Users</button>
      </div>

      <div className="glass-panel profile-header">
        <div className="profile-avatar">
          {user.name ? user.name.charAt(0).toUpperCase() : '?'}
        </div>
        <div className="profile-info">
          <h1>{user.name || 'Unnamed Teacher'}</h1>
          <p className="email">{user.email || 'No email'}</p>
          <p className="school">School: {user.schoolName || 'Not specified'} (UDISE: {user.udiseCode || 'N/A'})</p>
        </div>
        <div className="profile-status">
          <span className={`status-badge status-${user.subscriptionStatus || 'inactive'}`}>
            {(user.subscriptionStatus || 'inactive').toUpperCase()}
          </span>
          <p className="expiry">
            Expires: {user.subscriptionExpiry ? new Date(user.subscriptionExpiry).toLocaleDateString() : 'N/A'}
          </p>
        </div>
      </div>

      <div className="stats-row">
        <div className="glass-panel stat-card">
          <h3>Total Classes</h3>
          <div className="stat-value">{classes.length}</div>
        </div>
        <div className="glass-panel stat-card">
          <h3>Total Students</h3>
          <div className="stat-value">{students.length}</div>
        </div>
      </div>

      <div className="data-sections">
        <div className="glass-panel data-section">
          <h2>Classes Created</h2>
          {classes.length === 0 ? (
            <p className="empty-text">No classes found.</p>
          ) : (
            <ul className="data-list">
              {classes.map(c => (
                <li key={c.id}>
                  <strong>Class {c.className} {c.division}</strong>
                  <span className="subtitle">Year: {c.academicYearLabel}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="glass-panel data-section">
          <h2>Students</h2>
          {students.length === 0 ? (
            <p className="empty-text">No students found.</p>
          ) : (
            <ul className="data-list">
              {students.map(s => (
                <li key={s.id}>
                  <strong>{s.name}</strong>
                  <span className="subtitle">Class: {s.className} {s.division} | Roll: {s.rollNo}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="glass-panel data-section" style={{ gridColumn: "1 / -1" }}>
          <h2>Subscription History</h2>
          {subscriptions.length === 0 ? (
            <p className="empty-text">No subscription requests found.</p>
          ) : (
            <div className="requests-grid" style={{ marginTop: '16px' }}>
              {subscriptions.map(sub => (
                <div key={sub.id} className="glass-panel request-card">
                  <div className="request-header">
                    <span className="user-id">{new Date(sub.timestamp).toLocaleString()}</span>
                    <span className={`status-badge status-${sub.status}`}>
                      {sub.status.toUpperCase()}
                    </span>
                  </div>
                  <div className="screenshot-container" onClick={() => setSelectedScreenshot(sub.screenshotUrl)} style={{ cursor: 'pointer' }}>
                    <img src={sub.screenshotUrl} alt="Payment Screenshot" />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {selectedScreenshot && (
        <div className="modal-overlay animate-fade-in" onClick={() => setSelectedScreenshot(null)}>
          <div className="glass-panel modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Payment Screenshot</h2>
              <button className="close-btn" onClick={() => setSelectedScreenshot(null)}>×</button>
            </div>
            <div className="modal-body">
              <div className="full-screenshot">
                <img src={selectedScreenshot} alt="Screenshot Full" />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => setSelectedScreenshot(null)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
