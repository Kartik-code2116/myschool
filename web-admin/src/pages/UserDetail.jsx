import { useEffect, useState } from 'react';
import { collection, getDocs, doc, getDoc, query, updateDoc, where } from 'firebase/firestore';
import { useNavigate, useParams } from 'react-router-dom';
import { db } from '../firebase';
import './UserDetail.css';

const editableFields = [
  ['name', 'Teacher name'],
  ['email', 'Email'],
  ['phone', 'Phone'],
  ['schoolName', 'School name'],
  ['udiseCode', 'UDISE code'],
  ['district', 'District'],
  ['taluka', 'Taluka'],
  ['address', 'Address'],
  ['studentsCount', 'Students count'],
];

const formatDate = (timestamp) => {
  if (!timestamp) return 'N/A';
  return new Date(timestamp).toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
};

const formatDateTime = (timestamp) => {
  if (!timestamp) return 'No date';
  return new Date(timestamp).toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const toDateInput = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) return '';
  return date.toISOString().slice(0, 10);
};

const buildForm = (user) => ({
  name: user.name || '',
  email: user.email || '',
  phone: user.phone || '',
  schoolName: user.schoolName || '',
  udiseCode: user.udiseCode || '',
  district: user.district || '',
  taluka: user.taluka || '',
  address: user.address || '',
  studentsCount: user.studentsCount || '',
  subscriptionStatus: user.subscriptionStatus || 'inactive',
  subscriptionExpiry: toDateInput(user.subscriptionExpiry),
  accountStatus: user.accountStatus || 'active',
  adminNote: user.adminNote || '',
});

export default function UserDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [form, setForm] = useState(null);
  const [classes, setClasses] = useState([]);
  const [students, setStudents] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState('');
  const [error, setError] = useState(null);
  const [selectedScreenshot, setSelectedScreenshot] = useState(null);
  const [selectedClassId, setSelectedClassId] = useState('all');
  const [schoolsMap, setSchoolsMap] = useState({});

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        const userDoc = await getDoc(doc(db, 'teachers', id));
        if (!userDoc.exists()) {
          setUser(null);
          return;
        }

        const userData = { id: userDoc.id, ...userDoc.data() };
        setUser(userData);
        setForm(buildForm(userData));

        const classesQuery = query(collection(db, 'classes'), where('teacherId', '==', id));
        const studentsQuery = query(collection(db, 'students'), where('teacherId', '==', id));
        const subsQuery1 = query(collection(db, 'subscriptions'), where('teacherId', '==', id));
        const subsQuery2 = query(collection(db, 'subscriptions'), where('userId', '==', id));
        const schoolsQuery = query(collection(db, 'schools'), where('teacherId', '==', id));

        const [classesSnap, studentsSnap, subsSnap1, subsSnap2, schoolsSnap] = await Promise.all([
          getDocs(classesQuery),
          getDocs(studentsQuery),
          getDocs(subsQuery1),
          getDocs(subsQuery2),
          getDocs(schoolsQuery),
        ]);

        const classesData = [];
        classesSnap.forEach((classDoc) => classesData.push({ id: classDoc.id, ...classDoc.data() }));
        setClasses(classesData);

        const studentsData = [];
        studentsSnap.forEach((studentDoc) => studentsData.push({ id: studentDoc.id, ...studentDoc.data() }));
        setStudents(studentsData);

        const subsMap = new Map();
        subsSnap1.forEach((subscriptionDoc) => subsMap.set(subscriptionDoc.id, { id: subscriptionDoc.id, ...subscriptionDoc.data() }));
        subsSnap2.forEach((subscriptionDoc) => subsMap.set(subscriptionDoc.id, { id: subscriptionDoc.id, ...subscriptionDoc.data() }));

        const subsData = Array.from(subsMap.values());
        subsData.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
        setSubscriptions(subsData);

        const schoolsData = {};
        schoolsSnap.forEach((schoolDoc) => {
          schoolsData[schoolDoc.id] = schoolDoc.data();
        });
        setSchoolsMap(schoolsData);

        setError(null);
      } catch (err) {
        console.error('Error fetching user data:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, [id]);

  const updateForm = (field, value) => {
    setForm((currentForm) => ({ ...currentForm, [field]: value }));
    setSaveMessage('');
  };

  const saveUser = async (override = {}) => {
    if (!form) return;
    setSaving(true);
    setSaveMessage('');

    const nextForm = { ...form, ...override };
    const payload = {
      name: nextForm.name.trim(),
      email: nextForm.email.trim(),
      phone: nextForm.phone.trim(),
      schoolName: nextForm.schoolName.trim(),
      udiseCode: String(nextForm.udiseCode || '').trim(),
      district: nextForm.district.trim(),
      taluka: nextForm.taluka.trim(),
      address: nextForm.address.trim(),
      studentsCount: Number(nextForm.studentsCount) || 0,
      subscriptionStatus: nextForm.subscriptionStatus,
      subscriptionExpiry: nextForm.subscriptionExpiry ? new Date(nextForm.subscriptionExpiry).getTime() : null,
      accountStatus: nextForm.accountStatus,
      adminNote: nextForm.adminNote.trim(),
      updatedByAdminAt: Date.now(),
    };

    try {
      await updateDoc(doc(db, 'teachers', id), payload);
      const updatedUser = { ...user, ...payload };
      setUser(updatedUser);
      setForm(buildForm(updatedUser));
      setSaveMessage('Changes saved.');
    } catch (err) {
      setSaveMessage(`Failed to save: ${err.message}`);
    } finally {
      setSaving(false);
    }
  };

  const quickSetAccount = (accountStatus) => {
    saveUser({ accountStatus });
  };

  const quickActivateYear = () => {
    const expiry = new Date();
    expiry.setFullYear(expiry.getFullYear() + 1);
    saveUser({
      accountStatus: 'active',
      subscriptionStatus: 'active',
      subscriptionExpiry: expiry.toISOString().slice(0, 10),
    });
  };

  const filteredStudents = selectedClassId === 'all'
    ? students
    : students.filter((student) => student.classId === selectedClassId);

  if (loading) return <div className="loading">Loading user details...</div>;

  if (error) {
    return (
      <main className="user-detail-page">
        <button className="btn" onClick={() => navigate('/admin/users')} type="button">Back to Users</button>
        <div className="glass-panel empty-state error-state">
          <h3>Firebase Error</h3>
          <p>{error}</p>
        </div>
      </main>
    );
  }

  if (!user || !form) {
    return (
      <main className="user-detail-page">
        <button className="btn" onClick={() => navigate('/admin/users')} type="button">Back to Users</button>
        <div className="glass-panel empty-state">
          <h3>User not found</h3>
          <p>This teacher profile is no longer available.</p>
        </div>
      </main>
    );
  }

  return (
    <main className="user-detail-page">
      <div className="back-nav">
        <button className="btn" onClick={() => navigate('/admin/users')} type="button">Back to Users</button>
      </div>

      <section className="glass-panel profile-header">
        <div className="profile-avatar">
          {user.name ? user.name.charAt(0).toUpperCase() : '?'}
        </div>
        <div className="profile-info">
          <span className="label">App user account</span>
          <h1>{user.name || 'Unnamed Teacher'}</h1>
          <p className="email">{user.email || 'No email'}</p>
          <p className="school">{user.schoolName || 'School not specified'}</p>
        </div>
        <div className="profile-status">
          <span className={`status-badge status-${user.accountStatus || 'active'}`}>
            {user.accountStatus || 'active'}
          </span>
          <span className={`status-badge status-${user.subscriptionStatus || 'inactive'}`}>
            {user.subscriptionStatus || 'inactive'}
          </span>
          <p className="expiry">Expires {formatDate(user.subscriptionExpiry)}</p>
        </div>
      </section>

      <section className="control-grid">
        <div className="glass-panel admin-panel">
          <div className="section-title">
            <h2>Edit User Information</h2>
            <span>Admin</span>
          </div>
          <div className="edit-grid">
            {editableFields.map(([field, label]) => (
              <label key={field} className={field === 'address' ? 'field-wide' : ''}>
                <span>{label}</span>
                {field === 'address' ? (
                  <textarea
                    rows="3"
                    value={form[field]}
                    onChange={(event) => updateForm(field, event.target.value)}
                  />
                ) : (
                  <input
                    type={field === 'studentsCount' ? 'number' : 'text'}
                    value={form[field]}
                    onChange={(event) => updateForm(field, event.target.value)}
                  />
                )}
              </label>
            ))}
          </div>
        </div>

        <div className="glass-panel admin-panel">
          <div className="section-title">
            <h2>Account Control</h2>
            <span>Access</span>
          </div>
          <div className="edit-grid single">
            <label>
              <span>App account status</span>
              <select value={form.accountStatus} onChange={(event) => updateForm('accountStatus', event.target.value)}>
                <option value="active">Active</option>
                <option value="suspended">Suspended</option>
                <option value="blocked">Blocked</option>
              </select>
            </label>
            <label>
              <span>Subscription status</span>
              <select value={form.subscriptionStatus} onChange={(event) => updateForm('subscriptionStatus', event.target.value)}>
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
                <option value="pending">Pending</option>
                <option value="rejected">Rejected</option>
              </select>
            </label>
            <label>
              <span>Subscription expiry</span>
              <input
                type="date"
                value={form.subscriptionExpiry}
                onChange={(event) => updateForm('subscriptionExpiry', event.target.value)}
              />
            </label>
            <label>
              <span>Admin note</span>
              <textarea
                rows="4"
                value={form.adminNote}
                onChange={(event) => updateForm('adminNote', event.target.value)}
                placeholder="Internal note for this account"
              />
            </label>
          </div>
          <div className="quick-actions">
            <button className="btn btn-success" onClick={quickActivateYear} disabled={saving} type="button">Activate 1 Year</button>
            <button className="btn" onClick={() => quickSetAccount('active')} disabled={saving} type="button">Allow Access</button>
            <button className="btn btn-danger" onClick={() => quickSetAccount('suspended')} disabled={saving} type="button">Suspend</button>
          </div>
        </div>
      </section>

      <div className="save-bar glass-panel">
        <div>
          <strong>Admin controls</strong>
          <p>Save changes to update what the mobile app can read for this account.</p>
        </div>
        <div className="save-actions">
          {saveMessage && <span className={saveMessage.startsWith('Failed') ? 'save-error' : 'save-success'}>{saveMessage}</span>}
          <button className="btn btn-primary" onClick={() => saveUser()} disabled={saving} type="button">
            {saving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </div>

      <section className="stats-row">
        <div className="glass-panel stat-card">
          <span>Total Classes</span>
          <strong>{classes.length}</strong>
        </div>
        <div className="glass-panel stat-card">
          <span>Total Students</span>
          <strong>{students.length}</strong>
        </div>
        <div className="glass-panel stat-card">
          <span>UDISE Code</span>
          <strong className="code-value">{user.udiseCode || 'N/A'}</strong>
        </div>
      </section>

      <section className="data-sections">
        <div className="glass-panel data-section">
          <div className="section-title">
            <h2>Classes Created</h2>
            <span>{classes.length}</span>
          </div>
          {classes.length === 0 ? (
            <p className="empty-text">No classes found.</p>
          ) : (
            <details className="dropdown-details" open={classes.length > 0 || undefined}>
              <summary className="dropdown-summary">View all {classes.length} classes...</summary>
              <ul className="data-list">
                {classes.map((classItem) => (
                  <li 
                    key={classItem.id} 
                    className={`clickable-class-item ${selectedClassId === classItem.id ? 'active' : ''}`}
                    onClick={() => setSelectedClassId(selectedClassId === classItem.id ? 'all' : classItem.id)}
                  >
                    <strong>Class {classItem.className} {classItem.division}</strong>
                    <span className="subtitle">
                      Year: {classItem.academicYearLabel || 'N/A'} | School: {classItem.schoolId && schoolsMap[classItem.schoolId] ? schoolsMap[classItem.schoolId].name : 'Unknown/No School'}
                    </span>
                  </li>
                ))}
              </ul>
            </details>
          )}
        </div>

        <div className="glass-panel data-section">
          <div className="section-title">
            <h2>Students</h2>
            <span>{filteredStudents.length} / {students.length}</span>
          </div>

          {classes.length > 0 && (
            <div className="class-filter-container">
              <select
                id="class-filter"
                value={selectedClassId}
                onChange={(e) => setSelectedClassId(e.target.value)}
                className="class-filter-select"
              >
                <option value="all">All Classes ({students.length})</option>
                {classes.map((cls) => {
                  const classStudentCount = students.filter(s => s.classId === cls.id).length;
                  return (
                    <option key={cls.id} value={cls.id}>
                      Class {cls.className} {cls.division} ({classStudentCount})
                    </option>
                  );
                })}
              </select>
            </div>
          )}

          {filteredStudents.length === 0 ? (
            <p className="empty-text">No students found for this class.</p>
          ) : (
            <details className="dropdown-details" open={selectedClassId !== 'all' || undefined}>
              <summary className="dropdown-summary">
                View {selectedClassId === 'all' ? 'all' : ''} {filteredStudents.length} students...
              </summary>
              <ul className="data-list">
                {filteredStudents.map((student) => (
                  <li key={student.id}>
                    <strong>{student.name || 'Unnamed Student'}</strong>
                    <span className="subtitle">Class: {student.className || '-'} {student.division || ''} | Roll: {student.rollNo || 'N/A'}</span>
                  </li>
                ))}
              </ul>
            </details>
          )}
        </div>

        <div className="glass-panel data-section subscriptions-section">
          <div className="section-title">
            <h2>Subscription History</h2>
            <span>{subscriptions.length}</span>
          </div>
          {subscriptions.length === 0 ? (
            <p className="empty-text">No subscription requests found.</p>
          ) : (
            <div className="detail-subscriptions">
              {subscriptions.map((sub) => (
                <article key={sub.id} className="subscription-item">
                  <div>
                    <strong>{formatDateTime(sub.timestamp)}</strong>
                    <span className={`status-badge status-${sub.status || 'pending'}`}>{sub.status || 'pending'}</span>
                  </div>
                  <button
                    className="thumb-button"
                    onClick={() => setSelectedScreenshot(sub.screenshotUrl)}
                    type="button"
                  >
                    {sub.screenshotUrl ? (
                      <img 
                        src={sub.screenshotUrl} 
                        alt="Payment screenshot" 
                        onError={(e) => console.error("UserDetail list screenshot failed to load. URL: " + sub.screenshotUrl, e)}
                      />
                    ) : (
                      <span>No screenshot</span>
                    )}
                  </button>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>

      {selectedScreenshot && (
        <div className="modal-overlay animate-fade-in" onClick={() => setSelectedScreenshot(null)}>
          <div className="glass-panel modal-content" onClick={(event) => event.stopPropagation()}>
            <div className="modal-header">
              <div>
                <span className="label">Payment proof</span>
                <h2>Payment Screenshot</h2>
              </div>
              <button className="close-btn" onClick={() => setSelectedScreenshot(null)} type="button">x</button>
            </div>
            <div className="modal-body">
              <div className="full-screenshot">
                <img 
                  src={selectedScreenshot} 
                  alt="Payment screenshot full view" 
                  onError={(e) => console.error("UserDetail modal screenshot failed to load. URL: " + selectedScreenshot, e)}
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => setSelectedScreenshot(null)} type="button">Close</button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
