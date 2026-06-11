import { useState, useEffect } from 'react';
import { collection, getDocs, addDoc, deleteDoc, doc } from 'firebase/firestore';
import { db } from '../firebase';
import './AdminSubjects.css';

export default function AdminSubjects() {
  const [globalSubjects, setGlobalSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '' });

  // Form states
  const [name, setName] = useState('');
  const [maxMarks, setMaxMarks] = useState('100');
  const [category, setCategory] = useState('Academic');

  useEffect(() => {
    fetchGlobalSubjects();
  }, []);

  const fetchGlobalSubjects = async () => {
    setLoading(true);
    try {
      const colRef = collection(db, 'global_subjects');
      const snapshot = await getDocs(colRef);
      const subjects = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      setGlobalSubjects(subjects);
    } catch (error) {
      console.error('Error fetching global subjects:', error);
      setMessage({ text: 'Error fetching subjects: ' + error.message, type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleAddSubject = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    
    setSaving(true);
    setMessage({ text: '', type: '' });
    
    try {
      const colRef = collection(db, 'global_subjects');
      const newSubject = {
        name: name.trim(),
        maxMarks: parseInt(maxMarks, 10) || 100,
        category: category,
        createdAt: new Date().getTime()
      };
      
      const docRef = await addDoc(colRef, newSubject);
      setGlobalSubjects([...globalSubjects, { id: docRef.id, ...newSubject }]);
      
      // Reset form
      setName('');
      setMaxMarks('100');
      setCategory('Academic');
      setMessage({ text: 'Subject added successfully!', type: 'success' });
    } catch (error) {
      console.error('Error adding subject:', error);
      setMessage({ text: 'Error adding subject: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteSubject = async (id) => {
    if (!window.confirm('Are you sure you want to delete this global subject? It will not be removed from existing classes, but new classes will not see it as an option.')) {
      return;
    }
    
    try {
      await deleteDoc(doc(db, 'global_subjects', id));
      setGlobalSubjects(globalSubjects.filter(s => s.id !== id));
      setMessage({ text: 'Subject deleted.', type: 'info' });
    } catch (error) {
      console.error('Error deleting subject:', error);
      setMessage({ text: 'Error deleting subject: ' + error.message, type: 'error' });
    }
  };

  return (
    <main className="admin-subjects-page">
      <div className="page-kicker">Manage Content</div>
      <div className="page-header">
        <div>
          <h1>Global Subjects</h1>
          <p>Add and manage custom subjects that will be available to all schools by default.</p>
        </div>
      </div>

      <div className="subjects-layout">
        {/* Left Column: Add Subject Form */}
        <div className="glass-panel add-panel animate-fade-in">
          <h3>Add New Subject</h3>
          <p className="helper-text">This subject will appear in the app for all teachers when they set up their class.</p>
          
          <form onSubmit={handleAddSubject} className="add-subject-form">
            <div className="form-group">
              <label>Subject Name</label>
              <input 
                type="text" 
                value={name} 
                onChange={(e) => setName(e.target.value)} 
                placeholder="e.g. Computer Science" 
                required 
              />
            </div>
            
            <div className="form-group">
              <label>Category</label>
              <select value={category} onChange={(e) => setCategory(e.target.value)}>
                <option value="Academic">Academic</option>
                <option value="Activities">Activities</option>
                <option value="Personality">Personality Development</option>
                <option value="State Board">State Board Addition</option>
                <option value="Other">Other</option>
              </select>
            </div>

            <div className="form-group">
              <label>Default Max Marks</label>
              <input 
                type="number" 
                value={maxMarks} 
                onChange={(e) => setMaxMarks(e.target.value)} 
                min="10" 
                max="1000" 
                required 
              />
            </div>

            <button type="submit" className="btn btn-primary" disabled={saving || !name.trim()}>
              {saving ? 'Adding...' : '+ Add Global Subject'}
            </button>
          </form>
          
          {message.text && (
            <div className={`message-banner ${message.type}`} style={{ marginTop: '16px' }}>
              {message.text}
            </div>
          )}
        </div>

        {/* Right Column: Global Subjects List */}
        <div className="glass-panel list-panel animate-fade-in">
          <div className="list-header">
            <h3>Custom Global Subjects</h3>
          </div>

          {loading ? (
            <div className="loading">Loading global subjects...</div>
          ) : (
            <div className="subjects-list">
              {globalSubjects.length === 0 ? (
                <div className="empty-state">No custom global subjects added yet.</div>
              ) : (
                globalSubjects.map(subject => (
                  <div key={subject.id} className="subject-item">
                    <div className="subject-info">
                      <span className="subject-name">{subject.name}</span>
                      <div className="subject-meta">
                        <span className="meta-badge">{subject.category}</span>
                        <span className="meta-badge marks">Max: {subject.maxMarks}</span>
                      </div>
                    </div>
                    <button 
                      type="button" 
                      className="btn-delete-subject"
                      onClick={() => handleDeleteSubject(subject.id)}
                      title="Delete Subject"
                    >
                      ×
                    </button>
                  </div>
                ))
              )}
            </div>
          )}
          
          <div className="builtin-info">
            <p><strong>Note:</strong> Built-in defaults like Marathi, Hindi, English, Mathematics, Science, etc. are permanently available in the app and don't need to be added here.</p>
          </div>
        </div>
      </div>
    </main>
  );
}
