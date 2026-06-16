import { useState, useEffect } from 'react';
import { collection, getDocs, addDoc, deleteDoc, doc, getDoc, setDoc } from 'firebase/firestore';
import { db } from '../firebase';
import './AdminSubjects.css';

const getHardcodedDefaults = (stdStr) => {
  const std = parseInt(stdStr, 10);
  const list = [];
  const add = (name, maxMarks = 100) => {
    list.push({ name, maxMarks, subjectCode: String(list.length + 1) });
  };
  
  if (isNaN(std) || std < 1 || std > 10) {
    add("Marathi"); add("English"); add("Mathematics"); add("Science");
    return list;
  }
  
  add("Marathi");
  add("English");
  if (std >= 5) add("Hindi");
  add("Mathematics");
  
  if (std === 1 || std === 2) {
    add("Play, Do, Learn");
  } else if (std === 3 || std === 4) {
    add("Environmental Studies"); add("Play, Do, Learn");
  } else if (std === 5) {
    add("Environmental Studies Part 1"); add("Environmental Studies Part 2");
    add("Health & Physical Education"); add("Work Experience"); add("Art");
  } else if (std >= 6 && std <= 8) {
    add("Science"); add("History and Civics"); add("Geography");
    add("Health & Physical Education"); add("Work Experience"); add("Art");
  } else {
    add("Science"); add("History and Civics"); add("Geography");
    add("Health & Physical Education"); add("Work Experience"); add("Art");
    add("Information & Comm. Technology (ICT)"); add("Water Security & Environment Studies");
  }
  return list;
};

export default function AdminSubjects() {
  const [activeTab, setActiveTab] = useState('global'); // 'global' or 'classDefaults'
  const [globalSubjects, setGlobalSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '' });

  // Form states for Global
  const [name, setName] = useState('');
  const [maxMarks, setMaxMarks] = useState('100');
  const [category, setCategory] = useState('Academic');

  // States for Class Defaults
  const [selectedClass, setSelectedClass] = useState('1');
  const [classDefaults, setClassDefaults] = useState([]);
  const [loadingClass, setLoadingClass] = useState(false);
  const [selectedSubjectToAdd, setSelectedSubjectToAdd] = useState('');

  useEffect(() => {
    fetchGlobalSubjects();
  }, []);

  useEffect(() => {
    if (activeTab === 'classDefaults') {
      fetchClassDefaults(selectedClass);
    }
  }, [activeTab, selectedClass]);

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

  const fetchClassDefaults = async (className) => {
    setLoadingClass(true);
    try {
      const docRef = doc(db, 'class_default_subjects', `Class_${className}`);
      const docSnap = await getDoc(docRef);
      if (docSnap.exists()) {
        const data = docSnap.data();
        // Sort by subjectCode (treating as sequence)
        const subjects = data.subjects || [];
        subjects.sort((a, b) => parseInt(a.subjectCode) - parseInt(b.subjectCode));
        setClassDefaults(subjects);
      } else {
        setClassDefaults(getHardcodedDefaults(className));
      }
    } catch (error) {
      console.error('Error fetching class defaults:', error);
      setMessage({ text: 'Error fetching class defaults: ' + error.message, type: 'error' });
    } finally {
      setLoadingClass(false);
    }
  };

  const handleSaveClassDefaults = async () => {
    setSaving(true);
    setMessage({ text: '', type: '' });
    try {
      // Re-assign subject codes based on current order
      const updatedSubjects = classDefaults.map((sub, index) => ({
        ...sub,
        subjectCode: String(index + 1)
      }));
      
      const docRef = doc(db, 'class_default_subjects', `Class_${selectedClass}`);
      await setDoc(docRef, { subjects: updatedSubjects, className: selectedClass });
      setClassDefaults(updatedSubjects);
      setMessage({ text: `Defaults for Class ${selectedClass} saved successfully!`, type: 'success' });
    } catch (error) {
      console.error('Error saving class defaults:', error);
      setMessage({ text: 'Error saving class defaults: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleAddSubjectToClass = () => {
    if (!selectedSubjectToAdd) return;
    const subject = globalSubjects.find(s => s.name === selectedSubjectToAdd);
    if (!subject) return;

    // Check if it already exists
    if (classDefaults.some(s => s.name === subject.name)) {
      setMessage({ text: 'Subject already exists in this class', type: 'error' });
      return;
    }

    const newSub = {
      name: subject.name,
      maxMarks: subject.maxMarks,
      subjectCode: String(classDefaults.length + 1)
    };

    setClassDefaults([...classDefaults, newSub]);
    setSelectedSubjectToAdd('');
    setMessage({ text: '', type: '' });
  };

  const handleMoveUp = (index) => {
    if (index === 0) return;
    const newDefaults = [...classDefaults];
    const temp = newDefaults[index - 1];
    newDefaults[index - 1] = newDefaults[index];
    newDefaults[index] = temp;
    setClassDefaults(newDefaults);
    setMessage({ text: '', type: '' });
  };

  const handleMoveDown = (index) => {
    if (index === classDefaults.length - 1) return;
    const newDefaults = [...classDefaults];
    const temp = newDefaults[index + 1];
    newDefaults[index + 1] = newDefaults[index];
    newDefaults[index] = temp;
    setClassDefaults(newDefaults);
    setMessage({ text: '', type: '' });
  };

  const handleRemoveFromClass = (index) => {
    const newDefaults = [...classDefaults];
    newDefaults.splice(index, 1);
    setClassDefaults(newDefaults);
    setMessage({ text: '', type: '' });
  };

  const handleAddGlobalSubject = async (e) => {
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
      
      setName('');
      setMaxMarks('100');
      setCategory('Academic');
      setMessage({ text: 'Global subject added successfully!', type: 'success' });
    } catch (error) {
      console.error('Error adding subject:', error);
      setMessage({ text: 'Error adding subject: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteGlobalSubject = async (id) => {
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
          <h1>Subjects Management</h1>
          <p>Manage global subjects and default sequences for each class.</p>
        </div>
      </div>

      <div className="tabs-container">
        <button 
          className={`tab-btn ${activeTab === 'global' ? 'active' : ''}`}
          onClick={() => { setActiveTab('global'); setMessage({text:'', type:''}); }}
        >
          Global Subjects
        </button>
        <button 
          className={`tab-btn ${activeTab === 'classDefaults' ? 'active' : ''}`}
          onClick={() => { setActiveTab('classDefaults'); setMessage({text:'', type:''}); }}
        >
          Class Defaults
        </button>
      </div>

      {message.text && (
        <div className={`message-banner ${message.type}`} style={{ marginBottom: '16px' }}>
          {message.text}
        </div>
      )}

      {activeTab === 'global' && (
        <div className="subjects-layout">
          <div className="glass-panel add-panel animate-fade-in">
            <h3>Add New Subject</h3>
            <p className="helper-text">This subject will appear in the app for all teachers when they set up their class.</p>
            
            <form onSubmit={handleAddGlobalSubject} className="add-subject-form">
              <div className="form-group">
                <label>Subject Name</label>
                <input type="text" value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Computer Science" required />
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
                <input type="number" value={maxMarks} onChange={(e) => setMaxMarks(e.target.value)} min="10" max="1000" required />
              </div>
              <button type="submit" className="btn btn-primary" disabled={saving || !name.trim()}>
                {saving ? 'Adding...' : '+ Add Global Subject'}
              </button>
            </form>
          </div>

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
                      <button type="button" className="btn-delete-subject" onClick={() => handleDeleteGlobalSubject(subject.id)} title="Delete Subject">×</button>
                    </div>
                  ))
                )}
              </div>
            )}
            <div className="builtin-info">
              <p><strong>Note:</strong> Built-in defaults like Marathi, Hindi, English, Mathematics, Science, etc. are automatically handled in the app.</p>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'classDefaults' && (
        <div className="class-defaults-layout animate-fade-in">
          <div className="class-selector-panel glass-panel">
            <h3>Select Class</h3>
            <div className="class-buttons">
              {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map(c => (
                <button 
                  key={c} 
                  className={`class-btn ${selectedClass === String(c) ? 'active' : ''}`}
                  onClick={() => setSelectedClass(String(c))}
                >
                  Class {c}
                </button>
              ))}
            </div>
          </div>

          <div className="class-defaults-editor glass-panel">
            <div className="editor-header">
              <h3>Default Subjects for Class {selectedClass}</h3>
              <button className="btn btn-primary" onClick={handleSaveClassDefaults} disabled={saving}>
                {saving ? 'Saving...' : 'Save Configuration'}
              </button>
            </div>

            <div className="add-to-class-bar">
              <select 
                value={selectedSubjectToAdd} 
                onChange={(e) => setSelectedSubjectToAdd(e.target.value)}
                className="subject-dropdown"
              >
                <option value="">-- Select a custom global subject to add --</option>
                {globalSubjects.map(s => (
                  <option key={s.id} value={s.name}>{s.name} (Max {s.maxMarks})</option>
                ))}
              </select>
              <button className="btn btn-secondary" onClick={handleAddSubjectToClass} disabled={!selectedSubjectToAdd}>
                Add Subject
              </button>
            </div>

            {loadingClass ? (
              <div className="loading">Loading class defaults...</div>
            ) : (
              <div className="sequence-list">
                {classDefaults.length === 0 ? (
                  <div className="empty-state">No subjects found for this class.</div>
                ) : (
                  classDefaults.map((subject, index) => (
                    <div key={index} className="sequence-item">
                      <div className="sequence-controls">
                        <button className="seq-btn" onClick={() => handleMoveUp(index)} disabled={index === 0}>↑</button>
                        <span className="seq-number">{index + 1}</span>
                        <button className="seq-btn" onClick={() => handleMoveDown(index)} disabled={index === classDefaults.length - 1}>↓</button>
                      </div>
                      <div className="sequence-info">
                        <span className="subject-name">{subject.name}</span>
                        <span className="subject-max">Max Marks: {subject.maxMarks || 100}</span>
                      </div>
                      <button className="btn-delete-subject" onClick={() => handleRemoveFromClass(index)} title="Remove Subject">×</button>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </main>
  );
}
