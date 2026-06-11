import { useState, useEffect } from 'react';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { db } from '../firebase';
import './AdminRemarks.css';

const CLASSES = ['1', '2', '3', '4', '5', '6', '7', '8'];
const SEMESTERS = ['1', '2'];
const SUBJECTS = [
  'Marathi',
  'Hindi',
  'English',
  'Mathematics',
  'Science',
  'Science / EVS',
  'Soc. Science',
  'Drawing',
  'Work Experience',
  'Physical Education',
  'Personality Development',
  'Special Development',
  'Information & Comm. Technology (ICT)',
  'Water Security & Environment Studies'
];

const getDefaultRemarks = (subjectName, semesterNumber) => {
  const s = (subjectName || '').toLowerCase();
  const isSem1 = parseInt(semesterNumber) === 1;
  const defaults = [];

  if (s.includes('marathi') || s.includes('hindi') || s.includes('english')) {
    if (isSem1) {
      defaults.push("प्रथम सत्रातील भाषिक प्रगती चांगली आहे.", "वाचन व लेखन उत्तम आहे.", "कविता गायन चांगल्या प्रकारे करतो.", "अक्षर सुंदर व वळणदार आहे.", "वाचनाचा सराव आवश्यक आहे.", "लेखनात सुधारणा करावी.");
    } else {
      defaults.push("द्वितीय सत्रात भाषिक कौशल्य वाढले आहे.", "वार्षिक प्रगती समाधानकारक आहे.", "निबंध लेखन उत्तम करतो.", "भाषण कौशल्य प्रभावी आहे.", "व्याकरणाचा अधिक सराव हवा.");
    }
  } else if (s.includes('math')) {
    if (isSem1) {
      defaults.push("प्रथम सत्रातील गणितात प्रगती उत्तम.", "बेरीज-वजाबाकी अचूक करतो.", "पाढे पाठांतर चांगले आहे.", "गणितात अधिक सराव आवश्यक.");
    } else {
      defaults.push("द्वितीय सत्रात गणितात चांगली समज आली आहे.", "गुणाकार-भागाकार चांगला जमतो.", "शाब्दिक उदाहरणे सहज सोडवतो.", "वार्षिक प्रगती उत्तम आहे.");
    }
  } else if (s.includes('science') || s.includes('history') || s.includes('geography')) {
    if (isSem1) {
      defaults.push("प्रथम सत्रात परिसराची माहिती उत्तम ठेवली आहे.", "प्रयोगात रस घेतो.", "निरीक्षण कौशल्य छान आहे.");
    } else {
      defaults.push("द्वितीय सत्रात विज्ञानातील प्रगती चांगली.", "शास्त्रीय विचारसरणीत वाढ झाली आहे.", "वार्षिक मूल्यमापनात छान कामगिरी.");
    }
  } else if (s.includes('art') || s.includes('work')) {
    if (isSem1) {
      defaults.push("प्रथम सत्रात कलाकुसरीत चांगली प्रगती.", "चित्रकलेची आवड आहे.");
    } else {
      defaults.push("द्वितीय सत्रात नवनिर्मिती छान केली.", "उपक्रमात उत्स्फूर्त सहभाग.");
    }
  } else if (s.includes('physical')) {
    if (isSem1) {
      defaults.push("प्रथम सत्रात खेळांमध्ये उत्साह दिसला.", "मैदानी खेळात चांगली चमक.");
    } else {
      defaults.push("द्वितीय सत्रात शारीरिक क्षमता वाढली.", "खेळाडूवृत्ती उत्तम आहे.");
    }
  } else {
    if (isSem1) {
      defaults.push("प्रथम सत्रात अभ्यासात चांगली गती.", "वर्गकार्यात सक्रिय सहभाग.", "नियमित शाळेत उपस्थित असतो.", "प्रथम सत्रात अधिक लक्ष देणे गरजेचे.");
    } else {
      defaults.push("द्वितीय सत्रातील प्रगती समाधानकारक.", "संपूर्ण वर्षातील काम छान.", "पुढील वर्षासाठी शुभेच्छा.", "वार्षिक अभ्यासात अधिक मेहनत हवी.");
    }
  }
  return defaults;
};

export default function AdminRemarks() {
  const [selectedClass, setSelectedClass] = useState('5');
  const [selectedSemester, setSelectedSemester] = useState('1');
  const [selectedSubject, setSelectedSubject] = useState('Marathi');
  
  const [remarks, setRemarks] = useState([]);
  const [newRemarkText, setNewRemarkText] = useState('');
  
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '' });

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
      const docId = getDocId();
      const docRef = doc(db, 'remarkBanks', docId);
      await setDoc(docRef, {
        schoolId: 'default',
        subjectName: selectedSubject,
        options: remarks,
        className: selectedClass,
        semesterId: `sem_${selectedSemester}`
      });
      setMessage({ text: 'Remarks saved successfully!', type: 'success' });
    } catch (error) {
      console.error('Error saving remarks:', error);
      setMessage({ text: 'Error saving remarks: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const addRemark = (e) => {
    e.preventDefault();
    const txt = newRemarkText.trim();
    if (!txt) return;
    if (remarks.includes(txt)) {
      setMessage({ text: 'This remark already exists.', type: 'error' });
      return;
    }
    setRemarks([...remarks, txt]);
    setNewRemarkText('');
    setMessage({ text: '', type: '' });
  };

  const removeRemark = (index) => {
    const updated = [...remarks];
    updated.splice(index, 1);
    setRemarks(updated);
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
            <label>Subject</label>
            <select value={selectedSubject} onChange={(e) => setSelectedSubject(e.target.value)}>
              {SUBJECTS.map(sub => <option key={sub} value={sub}>{sub}</option>)}
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
            <button 
              className="btn btn-primary" 
              onClick={handleSave} 
              disabled={loading || saving}
            >
              {saving ? 'Saving...' : '💾 Save Changes'}
            </button>
          </div>

          {message.text && (
            <div className={`message-banner ${message.type}`}>
              {message.text}
            </div>
          )}

          <form onSubmit={addRemark} className="add-remark-form">
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
                remarks.map((remark, index) => (
                  <div key={index} className="remark-item">
                    <span className="remark-text">{remark}</span>
                    <button 
                      type="button" 
                      className="btn-delete-remark"
                      onClick={() => removeRemark(index)}
                      title="Remove"
                    >
                      ×
                    </button>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
