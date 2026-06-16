import { useState, useEffect } from 'react';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { db } from '../firebase';
import './AdminWeightage.css';

const getHardcodedDefaults = (stdStr) => {
  const std = parseInt(stdStr, 10);
  const list = [];
  const add = (name, maxMarks = 100) => {
    // Basic weightage calculation logic mirroring Subject.java
    let akarikMax = Math.floor(maxMarks / 2);
    let sanklitMax = maxMarks - akarikMax;
    
    let isNonAcademic = name.toLowerCase().includes("art") || name.toLowerCase().includes("work experience") 
      || name.toLowerCase().includes("physical education") || name.toLowerCase().includes("play");
      
    if (isNonAcademic) {
      akarikMax = maxMarks;
      sanklitMax = 0;
    }

    list.push({
      name,
      maxMarks,
      subjectCode: String(list.length + 1),
      maxNirikhshan: 0,
      maxTondiKam: Math.floor((akarikMax * 10) / 50),
      maxPratyakshik: Math.floor((akarikMax * 10) / 50),
      maxUpkram: Math.floor((akarikMax * 10) / 50),
      maxPrakalp: 0,
      maxChachani: Math.floor((akarikMax * 20) / 50),
      maxSwadhyay: 0,
      maxItar: 0,
      maxTondi: Math.floor((sanklitMax * 10) / 50),
      maxPratyakshikB: Math.floor((sanklitMax * 10) / 50),
      maxLekhi: sanklitMax - Math.floor((sanklitMax * 10) / 50) - Math.floor((sanklitMax * 10) / 50)
    });
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

export default function AdminWeightage() {
  const [selectedClass, setSelectedClass] = useState('1');
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '' });
  const [expandedSubject, setExpandedSubject] = useState(null);

  useEffect(() => {
    fetchClassDefaults(selectedClass);
  }, [selectedClass]);

  const fetchClassDefaults = async (className) => {
    setLoading(true);
    setMessage({ text: '', type: '' });
    try {
      const docRef = doc(db, 'class_default_subjects', `Class_${className}`);
      const docSnap = await getDoc(docRef);
      if (docSnap.exists()) {
        const data = docSnap.data();
        let fetchedSubjects = data.subjects || [];
        
        // Ensure all fields exist
        fetchedSubjects = fetchedSubjects.map(sub => {
          let isNonAcademic = sub.name.toLowerCase().includes("art") || sub.name.toLowerCase().includes("work experience") || sub.name.toLowerCase().includes("physical education") || sub.name.toLowerCase().includes("play");
          let akarikMax = Math.floor((sub.maxMarks || 100) / 2);
          let sanklitMax = (sub.maxMarks || 100) - akarikMax;
          if (isNonAcademic) { akarikMax = sub.maxMarks || 100; sanklitMax = 0; }
          
          return {
            ...sub,
            maxMarks: sub.maxMarks || 100,
            maxNirikhshan: sub.maxNirikhshan ?? 0,
            maxTondiKam: sub.maxTondiKam ?? Math.floor((akarikMax * 10) / 50),
            maxPratyakshik: sub.maxPratyakshik ?? Math.floor((akarikMax * 10) / 50),
            maxUpkram: sub.maxUpkram ?? Math.floor((akarikMax * 10) / 50),
            maxPrakalp: sub.maxPrakalp ?? 0,
            maxChachani: sub.maxChachani ?? Math.floor((akarikMax * 20) / 50),
            maxSwadhyay: sub.maxSwadhyay ?? 0,
            maxItar: sub.maxItar ?? 0,
            maxTondi: sub.maxTondi ?? Math.floor((sanklitMax * 10) / 50),
            maxPratyakshikB: sub.maxPratyakshikB ?? Math.floor((sanklitMax * 10) / 50),
            maxLekhi: sub.maxLekhi ?? (sanklitMax - Math.floor((sanklitMax * 10) / 50) - Math.floor((sanklitMax * 10) / 50))
          };
        });
        
        fetchedSubjects.sort((a, b) => parseInt(a.subjectCode) - parseInt(b.subjectCode));
        setSubjects(fetchedSubjects);
      } else {
        setSubjects(getHardcodedDefaults(className));
      }
    } catch (error) {
      console.error('Error fetching class defaults:', error);
      setMessage({ text: 'Error loading subjects: ' + error.message, type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setMessage({ text: '', type: '' });
    try {
      const docRef = doc(db, 'class_default_subjects', `Class_${selectedClass}`);
      await setDoc(docRef, { subjects: subjects, className: selectedClass });
      setMessage({ text: `Weightage for Class ${selectedClass} saved successfully!`, type: 'success' });
    } catch (error) {
      console.error('Error saving class defaults:', error);
      setMessage({ text: 'Error saving: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleFieldChange = (index, field, value) => {
    const newSubjects = [...subjects];
    const val = parseInt(value, 10) || 0;
    newSubjects[index] = { ...newSubjects[index], [field]: val };
    
    // Auto calculate totals for UI display logic (optional, we're tracking individual fields directly)
    setSubjects(newSubjects);
  };

  const toggleSubject = (index) => {
    if (expandedSubject === index) {
      setExpandedSubject(null);
    } else {
      setExpandedSubject(index);
    }
  };

  return (
    <main className="admin-weightage-page">
      <div className="page-kicker">Manage Configuration</div>
      <div className="page-header">
        <div>
          <h1>Weightage Declaration</h1>
          <p>Define the default Formative (आकारिक) and Summative (संकलित) marks breakdown for each subject.</p>
        </div>
      </div>

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

      {message.text && (
        <div className={`message-banner ${message.type}`}>
          {message.text}
        </div>
      )}

      <div className="weightage-editor-panel glass-panel">
        <div className="editor-header">
          <h3>Subjects for Class {selectedClass}</h3>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving || loading || subjects.length === 0}>
            {saving ? 'Saving...' : 'Save All Weightages'}
          </button>
        </div>

        {loading ? (
          <div className="loading">Loading subjects...</div>
        ) : subjects.length === 0 ? (
          <div className="empty-state">No subjects found for this class. Add them in the Subjects page first.</div>
        ) : (
          <div className="subjects-accordion">
            {subjects.map((sub, idx) => {
              const feTotal = sub.maxNirikhshan + sub.maxTondiKam + sub.maxPratyakshik + sub.maxUpkram + sub.maxPrakalp + sub.maxChachani + sub.maxSwadhyay + sub.maxItar;
              const seTotal = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
              const overallTotal = feTotal + seTotal;

              return (
                <div key={idx} className={`subject-card ${expandedSubject === idx ? 'expanded' : ''}`}>
                  <div className="subject-card-header" onClick={() => toggleSubject(idx)}>
                    <div className="subject-title">
                      <span className="seq-number">{sub.subjectCode}</span>
                      <span className="name">{sub.name}</span>
                    </div>
                    <div className="subject-summary">
                      <span className={`total-badge ${overallTotal !== sub.maxMarks ? 'error' : ''}`}>
                        Total: {overallTotal} / {sub.maxMarks}
                      </span>
                      <span className="fe-se-summary">FE: {feTotal} | SE: {seTotal}</span>
                      <span className="expand-icon">{expandedSubject === idx ? '▲' : '▼'}</span>
                    </div>
                  </div>

                  {expandedSubject === idx && (
                    <div className="subject-card-body">
                      {overallTotal !== sub.maxMarks && (
                        <div className="warning-text">Warning: Total sub-marks ({overallTotal}) do not match the expected Max Marks ({sub.maxMarks}).</div>
                      )}
                      
                      <div className="weightage-grid">
                        <div className="weightage-section">
                          <h4>Formative (आकारिक) - Total: {feTotal}</h4>
                          <div className="input-group-grid">
                            <label><span>Nirikhshan</span><input type="number" min="0" max="100" value={sub.maxNirikhshan} onChange={(e) => handleFieldChange(idx, 'maxNirikhshan', e.target.value)} /></label>
                            <label><span>Tondi Kam</span><input type="number" min="0" max="100" value={sub.maxTondiKam} onChange={(e) => handleFieldChange(idx, 'maxTondiKam', e.target.value)} /></label>
                            <label><span>Pratyakshik</span><input type="number" min="0" max="100" value={sub.maxPratyakshik} onChange={(e) => handleFieldChange(idx, 'maxPratyakshik', e.target.value)} /></label>
                            <label><span>Upkram</span><input type="number" min="0" max="100" value={sub.maxUpkram} onChange={(e) => handleFieldChange(idx, 'maxUpkram', e.target.value)} /></label>
                            <label><span>Prakalp</span><input type="number" min="0" max="100" value={sub.maxPrakalp} onChange={(e) => handleFieldChange(idx, 'maxPrakalp', e.target.value)} /></label>
                            <label><span>Chachani</span><input type="number" min="0" max="100" value={sub.maxChachani} onChange={(e) => handleFieldChange(idx, 'maxChachani', e.target.value)} /></label>
                            <label><span>Swadhyay</span><input type="number" min="0" max="100" value={sub.maxSwadhyay} onChange={(e) => handleFieldChange(idx, 'maxSwadhyay', e.target.value)} /></label>
                            <label><span>Itar (Other)</span><input type="number" min="0" max="100" value={sub.maxItar} onChange={(e) => handleFieldChange(idx, 'maxItar', e.target.value)} /></label>
                          </div>
                        </div>

                        <div className="weightage-section">
                          <h4>Summative (संकलित) - Total: {seTotal}</h4>
                          <div className="input-group-grid">
                            <label><span>Lekhi (Written)</span><input type="number" min="0" max="100" value={sub.maxLekhi} onChange={(e) => handleFieldChange(idx, 'maxLekhi', e.target.value)} /></label>
                            <label><span>Tondi (Oral)</span><input type="number" min="0" max="100" value={sub.maxTondi} onChange={(e) => handleFieldChange(idx, 'maxTondi', e.target.value)} /></label>
                            <label><span>Pratyakshik</span><input type="number" min="0" max="100" value={sub.maxPratyakshikB} onChange={(e) => handleFieldChange(idx, 'maxPratyakshikB', e.target.value)} /></label>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </main>
  );
}
