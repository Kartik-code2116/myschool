import { useState, useEffect } from 'react';
import { doc, getDoc, setDoc, collection, getDocs } from 'firebase/firestore';
import { db } from '../firebase';
import { getDefaultSubjectsForClass } from '../utils/subjectUtils';

import './AdminRemarks.css';

const CLASSES = ['1', '2', '3', '4', '5', '6', '7', '8'];
const SEMESTERS = ['1', '2'];
const SPECIAL_CATEGORIES = [
  { value: 'Vishesh pragati', label: 'Special Progress (विशेष प्रगती)' },
  { value: 'Sudharna Aavashyaka', label: 'Improvement Needed (सुधारणा आवश्यक)' },
  { value: 'Aavad, chanda, etc', label: 'Interests & Hobbies (आवड व छंद)' },
  { value: 'Vyaktimatva gun vishgesh', label: 'Personality Traits (व्यक्तिमत्त्व गुण विशेष)' }
];

const getDefaultRemarks = (subjectName, semesterNumber) => {
  return [];
};

export default function AdminRemarks() {
  const [selectedClass, setSelectedClass] = useState('5');
  const [selectedSemester, setSelectedSemester] = useState('1');
  const [selectedSubject, setSelectedSubject] = useState('Marathi');

  const [remarks, setRemarks] = useState([]);
  const [applyToAll, setApplyToAll] = useState(false);
  const [newRemarkText, setNewRemarkText] = useState('');
  const [remarkCategory, setRemarkCategory] = useState('उत्कृष्ट');

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '' });

  const [fetchedGlobalSubjects, setFetchedGlobalSubjects] = useState([]);
  const [allSubjects, setAllSubjects] = useState([]);

  const [draggedItemIndex, setDraggedItemIndex] = useState(null);

  useEffect(() => {
    fetchGlobalSubjects();
  }, []);

  const fetchGlobalSubjects = async () => {
    try {
      const colRef = collection(db, 'global_subjects');
      const snapshot = await getDocs(colRef);
      const fetchedSubjects = snapshot.docs.map(doc => {
        const data = doc.data();
        return { value: data.name, label: data.name };
      });
      setFetchedGlobalSubjects(fetchedSubjects);
    } catch (error) {
      console.error('Error fetching global subjects:', error);
    }
  };

  useEffect(() => {
    const classSubjects = getDefaultSubjectsForClass(selectedClass).map(s => ({ value: s, label: s }));
    const merged = [...classSubjects, ...SPECIAL_CATEGORIES];

    fetchedGlobalSubjects.forEach(fs => {
      if (!merged.find(s => s.value === fs.value)) {
        merged.push(fs);
      }
    });
    setAllSubjects(merged);

    // Auto-select a valid subject if current selection is not in the new list
    if (merged.length > 0 && !merged.find(s => s.value === selectedSubject)) {
      setSelectedSubject(merged[0].value);
    }
  }, [selectedClass, fetchedGlobalSubjects]);

  const handleDragStart = (index) => {
    setDraggedItemIndex(index);
  };

  const handleDragEnter = (index) => {
    if (draggedItemIndex === null) return;
    if (draggedItemIndex === index) return;

    const newRemarks = [...remarks];
    let draggedItem = newRemarks[draggedItemIndex];
    let targetItem = newRemarks[index];

    // Find what category we are dragging over
    let targetCat = 'Other';
    if (targetItem.startsWith('[') && targetItem.includes(']')) {
      targetCat = targetItem.substring(1, targetItem.indexOf(']')).trim();
    }

    // Find the pure text of the item being dragged
    let draggedText = draggedItem;
    if (draggedItem.startsWith('[') && draggedItem.includes(']')) {
      draggedText = draggedItem.substring(draggedItem.indexOf(']') + 1).trim();
    }

    // Apply the new category tag to the dragged item
    let updatedDraggedItem = targetCat === 'Other' ? draggedText : `[${targetCat}] ${draggedText}`;

    // Perform the swap
    newRemarks.splice(draggedItemIndex, 1);
    newRemarks.splice(index, 0, updatedDraggedItem);

    setDraggedItemIndex(index);
    setRemarks(newRemarks);
  };

  const handleDragEnd = () => {
    setDraggedItemIndex(null);
  };

  const getDocId = () => {
    const safe = selectedSubject ? selectedSubject.replace(/[^a-zA-Z0-9\u0900-\u097F]/g, '_') : 'general';
    return `default_${selectedClass}_sem_${selectedSemester}_${safe}`;
  };

  const getLegacyDocId = () => {
    const safe = selectedSubject ? selectedSubject.replace(/[^a-zA-Z0-9\u0900-\u097F]/g, '_') : 'general';
    return `default_${safe}`;
  };

  useEffect(() => {
    fetchRemarks();
  }, [selectedClass, selectedSemester, selectedSubject]);

  const fetchRemarks = async () => {
    setLoading(true);
    setMessage({ text: '', type: '' });
    setRemarks([]);
    try {
      const docId = getDocId();
      const docRef = doc(db, 'remarkBanks', docId);
      const docSnap = await getDoc(docRef);

      if (docSnap.exists() && docSnap.data().options) {
        setRemarks(docSnap.data().options);
      } else {
        // Fallback to legacy default
        const legacyDocRef = doc(db, 'remarkBanks', getLegacyDocId());
        const legacySnap = await getDoc(legacyDocRef);
        if (legacySnap.exists() && legacySnap.data().options) {
          setRemarks(legacySnap.data().options);
          setMessage({ text: 'Loaded global fallback remarks for this subject.', type: 'info' });
        } else {
          // Fallback to hardcoded defaults
          setRemarks(getDefaultRemarks(selectedSubject, selectedSemester));
          setMessage({ text: 'Loaded hardcoded system defaults. Click save to make them official.', type: 'info' });
        }
      }
    } catch (error) {
      console.error('Error fetching remarks:', error);
      setMessage({ text: 'Error fetching remarks: ' + error.message, type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setMessage({ text: '', type: '' });
    try {
      const getDocIdFor = (cls, sem, subject) => {
        const safe = subject ? subject.replace(/[^a-zA-Z0-9\u0900-\u097F]/g, '_') : 'general';
        return `default_${cls}_sem_${sem}_${safe}`;
      };

      if (applyToAll) {
        const promises = [];
        for (const cls of CLASSES) {
          for (const sem of SEMESTERS) {
            const docId = getDocIdFor(cls, sem, selectedSubject);
            const docRef = doc(db, 'remarkBanks', docId);
            promises.push(setDoc(docRef, {
              schoolId: 'default',
              subjectName: selectedSubject,
              options: remarks,
              className: cls,
              semesterId: `sem_${sem}`
            }));
          }
        }
        await Promise.all(promises);
        setMessage({ text: 'Remarks saved successfully across all classes and semesters!', type: 'success' });
      } else {
        const docId = getDocIdFor(selectedClass, selectedSemester, selectedSubject);
        const docRef = doc(db, 'remarkBanks', docId);
        await setDoc(docRef, {
          schoolId: 'default',
          subjectName: selectedSubject,
          options: remarks,
          className: selectedClass,
          semesterId: `sem_${selectedSemester}`
        });
        setMessage({ text: 'Remarks saved successfully!', type: 'success' });
      }
    } catch (error) {
      console.error('Error saving remarks:', error);
      setMessage({ text: 'Error saving remarks: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleFixCorrupted = async () => {
    if (!window.confirm("Are you sure you want to fix corrupted remarks across ALL subjects?")) return;
    setSaving(true);
    setMessage({ text: 'Fixing corrupted remarks...', type: 'info' });
    try {
      const banksRef = collection(db, 'remarkBanks');
      const snap = await getDocs(banksRef);
      let fixedCount = 0;
      for (const docSnap of snap.docs) {
        const data = docSnap.data();
        if (data.options && Array.isArray(data.options)) {
          let modified = false;
          const newOptions = data.options.map(opt => {
            if (typeof opt !== 'string') return opt;
            let res = opt;
            if (res.includes('à¤‰à¤¤à¥à¤•à¥ƒà¤·à¥à¤Ÿ')) { res = res.replace(/à¤‰à¤¤à¥à¤•à¥ƒà¤·à¥à¤Ÿ/g, 'उत्कृष्ट'); modified = true; }
            if (res.includes('à¤šà¤¾à¤‚à¤—à¤²à¥€ à¤ªà¥à¤°à¤—à¤¤à¥€')) { res = res.replace(/à¤šà¤¾à¤‚à¤—à¤²à¥€ à¤ªà¥à¤°à¤—à¤¤à¥€/g, 'चांगली प्रगती'); modified = true; }
            if (res.includes('à¤¸à¤®à¤¾à¤§à¤¾à¤¨à¤•à¤¾à¤°à¤•')) { res = res.replace(/à¤¸à¤®à¤¾à¤§à¤¾à¤¨à¤•à¤¾à¤°à¤•/g, 'समाधानकारक'); modified = true; }
            return res;
          });
          if (modified) {
            await setDoc(doc(db, 'remarkBanks', docSnap.id), { ...data, options: newOptions });
            fixedCount++;
          }
        }
      }
      setMessage({ text: `Successfully fixed corrupted remarks in ${fixedCount} subjects!`, type: 'success' });
      // Refresh current
      fetchRemarks();
    } catch (error) {
      console.error(error);
      setMessage({ text: 'Error fixing remarks: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const addRemark = (e) => {
    e.preventDefault();
    const txt = newRemarkText.trim();
    if (!txt) return;

    let finalTxt = txt;
    if (remarkCategory !== 'Other') {
      finalTxt = `[${remarkCategory}] ${txt}`;
    }

    if (remarks.includes(finalTxt)) {
      setMessage({ text: 'This remark already exists.', type: 'error' });
      return;
    }
    setRemarks([...remarks, finalTxt]);
    setNewRemarkText('');
    setMessage({ text: '', type: '' });
  };

  const removeRemark = (index) => {
    const updated = [...remarks];
    updated.splice(index, 1);
    setRemarks(updated);
  };

  const handleAutoFillAiRemarks = () => {
    alert("api key is not join");
  };

  return (
    <main className="admin-remarks-page">
      <div className="page-kicker">Manage Content</div>
      <div className="page-header">
        <div>
          <h1>Descriptive Remarks</h1>
          <p>Configure the default descriptive remarks for specific Classes, Semesters, and Subjects.</p>
        </div>
      </div>

      <div className="remarks-layout">
        {/* Left Column: Selectors */}
        <div className="glass-panel selectors-panel animate-fade-in">
          <h3>Target Selection</h3>
          <p className="helper-text">Select where these remarks will be applied.</p>

          <div className="form-group">
            <label>Class</label>
            <select value={selectedClass} onChange={(e) => setSelectedClass(e.target.value)}>
              {CLASSES.map(c => <option key={c} value={c}>Class {c}</option>)}
            </select>
          </div>

          <div className="form-group">
            <label>Semester</label>
            <select value={selectedSemester} onChange={(e) => setSelectedSemester(e.target.value)}>
              {SEMESTERS.map(s => <option key={s} value={s}>Semester {s}</option>)}
            </select>
          </div>

          <div className="form-group">
            <label>Subject / Category</label>
            <select value={selectedSubject} onChange={(e) => setSelectedSubject(e.target.value)}>
              <optgroup label="Academic & Activity Subjects">
                {getDefaultSubjectsForClass(selectedClass).map(sub => (
                  <option key={sub} value={sub}>{sub}</option>
                ))}
              </optgroup>
              <optgroup label="Descriptive Entries">
                {SPECIAL_CATEGORIES.map(cat => (
                  <option key={cat.value} value={cat.value}>{cat.label}</option>
                ))}
              </optgroup>
              {fetchedGlobalSubjects.length > 0 && (
                <optgroup label="Global Custom Subjects">
                  {fetchedGlobalSubjects.map(fs => {
                    const isDefault = getDefaultSubjectsForClass(selectedClass).includes(fs.value);
                    const isSpecial = SPECIAL_CATEGORIES.find(c => c.value === fs.value);
                    if (isDefault || isSpecial) return null;
                    return <option key={fs.value} value={fs.value}>{fs.label}</option>;
                  })}
                </optgroup>
              )}
            </select>
          </div>

          <div className="selectors-info">
            <p><strong>Current ID:</strong></p>
            <code>{getDocId()}</code>
          </div>
        </div>

        {/* Right Column: Remarks Editor */}
        <div className="glass-panel editor-panel animate-fade-in">
          <div className="editor-header">
            <h3>Remarks List</h3>
            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                className="btn btn-secondary"
                onClick={handleAutoFillAiRemarks}
                disabled={loading || saving}
                title="Automatically generate 30 highly specific remarks based on this subject's syllabus"
              >
                ✨ Auto-Fill AI Remarks
              </button>
              <button
                className="btn btn-primary"
                onClick={handleSave}
                disabled={loading || saving}
              >
                {saving ? 'Saving...' : '💾 Save Changes'}
              </button>
            </div>
          </div>

          {message.text && (
            <div className={`message-banner ${message.type}`}>
              {message.text}
            </div>
          )}

          <form onSubmit={addRemark} className="add-remark-form">
            <select
              value={remarkCategory}
              onChange={(e) => setRemarkCategory(e.target.value)}
              disabled={loading}
              className="category-select"
            >
              <option value="उत्कृष्ट">उत्कृष्ट</option>
              <option value="चांगली प्रगती">चांगली प्रगती</option>
              <option value="समाधानकारक">समाधानकारक</option>
              <option value="Other">Other / None</option>
            </select>
            <input
              type="text"
              placeholder="Enter a new descriptive remark..."
              value={newRemarkText}
              onChange={(e) => setNewRemarkText(e.target.value)}
              disabled={loading}
            />
            <button type="submit" className="btn btn-secondary" disabled={loading || !newRemarkText.trim()}>
              + Add
            </button>
          </form>

          {loading ? (
            <div className="loading">Loading remarks...</div>
          ) : (
            <div className="remarks-list">
              {remarks.length === 0 ? (
                <div className="empty-state">No remarks configured for this selection.</div>
              ) : (
                (() => {
                  const categorized = {
                    'उत्कृष्ट': [],
                    'चांगली प्रगती': [],
                    'समाधानकारक': [],
                    'Other': []
                  };

                  remarks.forEach((remark, index) => {
                    let cat = 'Other';
                    let text = remark;
                    if (remark.startsWith('[') && remark.includes(']')) {
                      const end = remark.indexOf(']');
                      cat = remark.substring(1, end).trim();
                      text = remark.substring(end + 1).trim();
                      if (!categorized[cat]) categorized[cat] = [];
                    }
                    categorized[cat].push({ text, originalIndex: index });
                  });

                  return Object.entries(categorized).map(([catName, items]) => {
                    if (items.length === 0) return null;
                    return (
                      <div key={catName} className="category-group" style={{ marginBottom: '16px' }}>
                        <h4 style={{ margin: '0 0 8px 0', color: 'var(--primary-color)', fontSize: '14px', textTransform: 'uppercase', letterSpacing: '1px' }}>{catName}</h4>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                          {items.map((item) => {
                            const index = item.originalIndex;
                            return (
                              <div
                                key={index}
                                className={`remark-item ${draggedItemIndex === index ? 'dragging' : ''}`}
                                draggable
                                onDragStart={() => handleDragStart(index)}
                                onDragEnter={() => handleDragEnter(index)}
                                onDragEnd={handleDragEnd}
                                onDragOver={(e) => e.preventDefault()}
                              >
                                <div className="remark-content">
                                  <span className="drag-handle" title="Drag to reorder">↕️</span>
                                  <span className="remark-text">{item.text}</span>
                                </div>
                                <button
                                  type="button"
                                  className="btn-delete-remark"
                                  onClick={() => removeRemark(index)}
                                  title="Remove"
                                >
                                  ×
                                </button>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    );
                  });
                })()
              )}
              <div className="form-actions" style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
                  <input 
                    type="checkbox" 
                    id="applyToAll" 
                    checked={applyToAll} 
                    onChange={(e) => setApplyToAll(e.target.checked)} 
                    style={{ width: '18px', height: '18px' }}
                  />
                  <label htmlFor="applyToAll" style={{ margin: 0, fontWeight: 'bold', cursor: 'pointer' }}>
                    Apply these remarks to ALL Classes & Semesters
                  </label>
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button 
                    className="btn btn-secondary" 
                    onClick={handleFixCorrupted} 
                    disabled={loading || saving}
                    title="Fix all corrupted remarks in the database across all classes and subjects"
                  >
                    {saving ? 'Processing...' : '🛠️ Fix Corrupted Data'}
                  </button>
                  <button 
                    className="btn btn-primary btn-save" 
                    onClick={handleSave} 
                    disabled={loading || saving}
                  >
                    {saving ? 'Saving...' : '💾 Save Changes'}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
