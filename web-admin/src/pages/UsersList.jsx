import { useEffect, useMemo, useState } from 'react';
import { collection, getDocs } from 'firebase/firestore';
import { useNavigate } from 'react-router-dom';
import { db } from '../firebase';
import './UsersList.css';

export default function UsersList() {
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        const snap = await getDocs(collection(db, 'teachers'));
        const data = [];
        snap.forEach((teacherDoc) => {
          data.push({ id: teacherDoc.id, ...teacherDoc.data() });
        });
        setUsers(data);
        setError(null);
      } catch (err) {
        console.error('Error fetching users:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchUsers();
  }, []);

  const filteredUsers = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return users;

    return users.filter((user) => (
      user.name?.toLowerCase().includes(term)
      || user.email?.toLowerCase().includes(term)
      || user.schoolName?.toLowerCase().includes(term)
      || String(user.udiseCode || '').toLowerCase().includes(term)
    ));
  }, [search, users]);

  const activeUsers = users.filter((user) => user.subscriptionStatus === 'active').length;
  const suspendedUsers = users.filter((user) => user.accountStatus === 'suspended').length;
  const totalStudents = users.reduce((sum, user) => sum + (Number(user.studentsCount) || 0), 0);

  return (
    <main className="users-page">
      <div className="page-kicker">Teacher Directory</div>
      <div className="page-header">
        <div>
          <h1>App Users</h1>
          <p>View every teacher account using the MySchool app and control their access.</p>
        </div>
        <div className="users-search">
          <input
            type="search"
            placeholder="Search name, email, school, UDISE"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
      </div>

      <section className="users-summary">
        <div className="glass-panel users-stat">
          <span>Total teachers</span>
          <strong>{users.length}</strong>
        </div>
        <div className="glass-panel users-stat">
          <span>Active subscriptions</span>
          <strong>{activeUsers}</strong>
        </div>
        <div className="glass-panel users-stat">
          <span>Suspended accounts</span>
          <strong>{suspendedUsers}</strong>
        </div>
        <div className="glass-panel users-stat">
          <span>Reported students</span>
          <strong>{totalStudents}</strong>
        </div>
      </section>

      {loading ? (
        <div className="loading">Loading users...</div>
      ) : error ? (
        <div className="glass-panel empty-state animate-fade-in error-state">
          <h3>Firebase Error</h3>
          <p>{error}</p>
          <p className="helper-text">Check your Firestore Database Security Rules in the Firebase Console.</p>
        </div>
      ) : filteredUsers.length === 0 ? (
        <div className="glass-panel empty-state">
          <h3>No users found</h3>
          <p>Try a different search term.</p>
        </div>
      ) : (
        <div className="users-grid">
          {filteredUsers.map((user) => (
            <button
              key={user.id}
              className="glass-panel user-card animate-fade-in"
              onClick={() => navigate(`/users/${user.id}`)}
              type="button"
            >
              <div className="user-avatar">
                {user.name ? user.name.charAt(0).toUpperCase() : '?'}
              </div>
              <div className="user-info">
                <h3>{user.name || 'Unnamed Teacher'}</h3>
                <p className="email">{user.email || 'No email provided'}</p>
                <p className="school">{user.schoolName || 'No school specified'}</p>
              </div>
              <div className="user-status">
                <span className={`status-badge status-${user.accountStatus || 'active'}`}>
                  {user.accountStatus || 'active'}
                </span>
                <span className={`status-badge status-${user.subscriptionStatus || 'inactive'}`}>
                  {user.subscriptionStatus || 'inactive'}
                </span>
                <span className="student-count">
                  {user.studentsCount || 0} Students
                </span>
              </div>
            </button>
          ))}
        </div>
      )}
    </main>
  );
}
