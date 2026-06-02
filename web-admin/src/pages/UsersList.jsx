import React, { useEffect, useState } from 'react';
import { collection, getDocs } from 'firebase/firestore';
import { db } from '../firebase';
import { useNavigate } from 'react-router-dom';
import './UsersList.css';

export default function UsersList() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        const snap = await getDocs(collection(db, "teachers"));
        const data = [];
        snap.forEach(doc => {
          data.push({ id: doc.id, ...doc.data() });
        });
        setUsers(data);
        setError(null);
      } catch (err) {
        console.error("Error fetching users:", err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchUsers();
  }, []);

  return (
    <div className="users-page">
      <div className="page-header">
        <h1>Registered Users</h1>
        <p>Manage all teachers registered in MySchool</p>
      </div>

      {loading ? (
        <div className="loading">Loading users...</div>
      ) : error ? (
        <div className="glass-panel empty-state animate-fade-in" style={{borderColor: '#ef4444'}}>
          <h3 style={{color: '#ef4444'}}>Firebase Error</h3>
          <p>{error}</p>
          <p style={{fontSize: '12px', marginTop: '10px'}}>Check your Firestore Database Security Rules in the Firebase Console.</p>
        </div>
      ) : users.length === 0 ? (
        <div className="glass-panel empty-state">
          <h3>No users found</h3>
        </div>
      ) : (
        <div className="users-grid">
          {users.map(user => (
            <div key={user.id} className="glass-panel user-card animate-fade-in" onClick={() => navigate(`/users/${user.id}`)}>
              <div className="user-avatar">
                {user.name ? user.name.charAt(0).toUpperCase() : '?'}
              </div>
              <div className="user-info">
                <h3>{user.name || 'Unnamed Teacher'}</h3>
                <p className="email">{user.email || 'No email provided'}</p>
                <p className="school">{user.schoolName || 'No school specified'}</p>
              </div>
              <div className="user-status">
                <span className={`status-badge status-${user.subscriptionStatus || 'inactive'}`}>
                  {(user.subscriptionStatus || 'inactive').toUpperCase()}
                </span>
                <span className="student-count">
                  {user.studentsCount || 0} Students
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
