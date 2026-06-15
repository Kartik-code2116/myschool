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
  const [viewMode, setViewMode] = useState('table'); // 'grid' or 'table'
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUsersAndStudents = async () => {
      try {
        // Fetch all teachers
        const teachersSnap = await getDocs(collection(db, 'teachers'));
        const teachersList = [];
        teachersSnap.forEach((doc) => {
          teachersList.push({ id: doc.id, ...doc.data() });
        });

        // Fetch all students to compute true counts per teacher
        const studentsSnap = await getDocs(collection(db, 'students'));
        const studentCounts = {};
        studentsSnap.forEach((doc) => {
          const studentData = doc.data();
          const teacherId = studentData.teacherId;
          if (teacherId) {
            studentCounts[teacherId] = (studentCounts[teacherId] || 0) + 1;
          }
        });

        // Assign correct counts to teachers
        const updatedUsers = teachersList.map(user => ({
          ...user,
          studentsCount: studentCounts[user.id] || 0
        }));

        setUsers(updatedUsers);
        setError(null);
      } catch (err) {
        console.error('Error fetching users and students:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchUsersAndStudents();
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
          <p>View every teacher account using the Edu Report app and control their access.</p>
        </div>
        <div className="users-header-actions">
          <div className="users-search">
            <input
              type="search"
              placeholder="Search name, email, school, UDISE"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <button 
            type="button" 
            className="btn view-toggle-btn"
            onClick={() => setViewMode(prev => prev === 'grid' ? 'table' : 'grid')}
          >
            {viewMode === 'grid' ? '📋 Table View' : '🗂️ Grid View'}
          </button>
        </div>
      </div>

      <section className="users-summary animate-fade-in">
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
      ) : viewMode === 'grid' ? (
        <div className="users-grid">
          {filteredUsers.map((user) => (
            <button
              key={user.id}
              className="glass-panel user-card animate-fade-in"
              onClick={() => navigate(`/admin/users/${user.id}`)}
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
                <span className="expiry-text">
                  Expires: {user.subscriptionExpiry ? new Date(user.subscriptionExpiry).toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' }) : 'N/A'}
                </span>
              </div>
            </button>
          ))}
        </div>
      ) : (
        <div className="glass-panel users-table-wrapper animate-fade-in">
          <table className="users-table">
            <thead>
              <tr>
                <th>Avatar</th>
                <th>Full Name</th>
                <th>Email / Contact</th>
                <th>School / UDISE</th>
                <th>Students</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map((user) => (
                <tr key={user.id} onClick={() => navigate(`/admin/users/${user.id}`)} className="table-row-clickable">
                  <td>
                    <div className="user-avatar-small">
                      {user.name ? user.name.charAt(0).toUpperCase() : '?'}
                    </div>
                  </td>
                  <td className="table-user-name"><strong>{user.name || 'Unnamed Teacher'}</strong></td>
                  <td>
                    <div className="table-contact-info">
                      <span>{user.email || 'No Email'}</span>
                      <span className="subtext">{user.phone || 'No Phone'}</span>
                    </div>
                  </td>
                  <td>
                    <div className="table-school-info">
                      <span>{user.schoolName || 'No School'}</span>
                      <span className="subtext">{user.udiseCode || 'No UDISE'}</span>
                    </div>
                  </td>
                  <td>
                    <span className="status-badge status-active">{user.studentsCount || 0}</span>
                  </td>
                  <td>
                    <div className="table-status-group">
                      <span className={`status-badge status-${user.accountStatus || 'active'}`}>
                        {user.accountStatus || 'active'}
                      </span>
                      <span className={`status-badge status-${user.subscriptionStatus || 'inactive'}`}>
                        {user.subscriptionStatus || 'inactive'}
                      </span>
                    </div>
                  </td>
                  <td>
                    <button className="btn btn-primary btn-sm" type="button">Edit</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  );
}
