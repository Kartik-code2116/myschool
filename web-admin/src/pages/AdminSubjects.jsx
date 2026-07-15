import React, { useState, useEffect } from 'react';
import { collection, getDocs, addDoc, deleteDoc, doc, getDoc, setDoc } from 'firebase/firestore';
import { db } from '../firebase';
import './AdminSubjects.css';

const BUILT_IN_SUBJECTS = [
  "मराठी", "इंग्रजी", "हिंदी", "गणित", "सामान्य विज्ञान",
  "परिसर अभ्यास", "परिसर अभ्यास भाग १", "परिसर अभ्यास भाग २",
  "इतिहास व नागरिकशास्त्र", "भूगोल", "आरोग्य व शारीरिक शिक्षण",
  "कार्यानुभव", "कला", "खेळू, करू, शिकू", "माहिती व संप्रेषण तंत्रज्ञान (ICT)",
  "जलसुरक्षा व पर्यावरण अभ्यास",
  "विशेष प्रगती", "आवड/छंद", "सुधारणा आवश्यक", "व्यक्तिमत्व गुणविशेष"
];

const EN_TO_MR = {
  "Marathi": "मराठी", "English": "इंग्रजी", "Hindi": "हिंदी",
  "Mathematics": "गणित", "Science": "सामान्य विज्ञान", "General Science": "सामान्य विज्ञान",
  "Environmental Studies": "परिसर अभ्यास", "Environmental Studies Part 1": "परिसर अभ्यास भाग १", "Environmental Studies Part 2": "परिसर अभ्यास भाग २",
  "History and Civics": "इतिहास व नागरिकशास्त्र", "Geography": "भूगोल",
  "Health & Physical Education": "आरोग्य व शारीरिक शिक्षण", "Work Experience": "कार्यानुभव",
  "Art": "कला", "Art Education": "कला", "Play, Do, Learn": "खेळू, करू, शिकू",
  "Information & Comm. Technology (ICT)": "माहिती व संप्रेषण तंत्रज्ञान (ICT)",
  "Water Security & Environment Studies": "जलसुरक्षा व पर्यावरण अभ्यास"
};

const getHardcodedDefaults = (stdStr) => {
  const std = parseInt(stdStr, 10);
  const list = [];
  const add = (name, maxMarks = 100) => {
    let isFeMarksOnly = name === "कला" || name === "कार्यानुभव" || name === "आरोग्य व शारीरिक शिक्षण" || name === "खेळू, करू, शिकू";
    let isTrueDescriptive = name === "विशेष प्रगती" || name === "आवड/छंद" || name === "सुधारणा आवश्यक" || name === "व्यक्तिमत्व गुणविशेष";
    list.push({ 
      name, 
      maxMarks: isTrueDescriptive ? 0 : maxMarks, 
      isNonAcademic: isFeMarksOnly || isTrueDescriptive
    });
  };
  
  if (isNaN(std) || std < 1 || std > 10) {
    add("मराठी"); add("इंग्रजी"); add("गणित"); add("सामान्य विज्ञान");
    return list;
  }
  
  add("मराठी");
  add("इंग्रजी");
  if (std >= 5) add("हिंदी");
  add("गणित");
  
  if (std === 1 || std === 2) {
    add("खेळू, करू, शिकू");
  } else if (std === 3 || std === 4) {
    add("परिसर अभ्यास"); add("खेळू, करू, शिकू");
  } else if (std === 5) {
    add("परिसर अभ्यास भाग १"); add("परिसर अभ्यास भाग २");
    add("आरोग्य व शारीरिक शिक्षण"); add("कार्यानुभव"); add("कला");
  } else if (std >= 6 && std <= 8) {
    add("सामान्य विज्ञान"); add("इतिहास व नागरिकशास्त्र"); add("भूगोल");
    add("आरोग्य व शारीरिक शिक्षण"); add("कार्यानुभव"); add("कला");
  } else {
    add("सामान्य विज्ञान"); add("इतिहास व नागरिकशास्त्र"); add("भूगोल");
    add("आरोग्य व शारीरिक शिक्षण"); add("कार्यानुभव"); add("कला");
    add("माहिती व संप्रेषण तंत्रज्ञान (ICT)"); add("जलसुरक्षा व पर्यावरण अभ्यास");
  }
  
  // Add true descriptive subjects to all classes by default
  add("विशेष प्रगती");
  add("आवड/छंद");
  add("सुधारणा आवश्यक");
  add("व्यक्तिमत्व गुणविशेष");
  
  return list;
};

export default function AdminSubjects() {
  const [activeTab, setActiveTab] = useState('global');
  const [globalSubjects, setGlobalSubjects] = useState([]);
  const [globalSequence, setGlobalSequence] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '' });

  // Form states for Global
  const [name, setName] = useState('');
  const [maxMarks, setMaxMarks] = useState('100');
  const [category, setCategory] = useState('Academic');
  const [isDescriptiveOnly, setIsDescriptiveOnly] = useState(false);

  // States for Class Defaults
  const [selectedClass, setSelectedClass] = useState('1');
  const [classDefaults, setClassDefaults] = useState([]);
  const [loadingClass, setLoadingClass] = useState(false);
  const [selectedSubjectToAdd, setSelectedSubjectToAdd] = useState('');

  // States for Edit Modal
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingSubject, setEditingSubject] = useState(null);
  const [editData, setEditData] = useState({});

  useEffect(() => {
    fetchGlobalData();
  }, []);

  useEffect(() => {
    if (activeTab === 'classDefaults') {
      fetchClassDefaults(selectedClass);
    }
  }, [activeTab, selectedClass, globalSequence]);

  const fetchGlobalData = async () => {
    setLoading(true);
    try {
      const colRef = collection(db, 'global_subjects');
      const snapshot = await getDocs(colRef);
      const customSubjects = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      setGlobalSubjects(customSubjects);

      const seqDocRef = doc(db, 'admin_settings', 'global_subject_sequence');
      const seqSnap = await getDoc(seqDocRef);
      let seq = [];
      if (seqSnap.exists() && seqSnap.data().subjects) {
        // Map any legacy English strings to Marathi
        seq = seqSnap.data().subjects.map(s => EN_TO_MR[s] || s);
      }

      const allKnownNames = [...BUILT_IN_SUBJECTS, ...customSubjects.map(s => s.name)];
      let modified = false;
      allKnownNames.forEach(n => {
        if (!seq.includes(n)) {
          seq.push(n);
          modified = true;
        }
      });
      seq = seq.filter(n => allKnownNames.includes(n));
      
      setGlobalSequence(seq);
      if (modified && seq.length > 0) {
        await setDoc(seqDocRef, { subjects: seq });
      }
    } catch (error) {
      console.error('Error fetching global data:', error);
      setMessage({ text: 'Error fetching subjects: ' + error.message, type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleSaveGlobalSequence = async () => {
    setSaving(true);
    try {
      await setDoc(doc(db, 'admin_settings', 'global_subject_sequence'), { subjects: globalSequence });
      setMessage({ text: 'Global sequence saved! It will apply as default to all users.', type: 'success' });
      // Trigger a re-render of class defaults if we are looking at them
      if (activeTab === 'classDefaults') {
        fetchClassDefaults(selectedClass);
      }
    } catch (error) {
      setMessage({ text: 'Error saving sequence: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleGlobalMoveUp = (index) => {
    if (index === 0) return;
    const newSeq = [...globalSequence];
    const temp = newSeq[index - 1];
    newSeq[index - 1] = newSeq[index];
    newSeq[index] = temp;
    setGlobalSequence(newSeq);
  };

  const handleGlobalMoveDown = (index) => {
    if (index === globalSequence.length - 1) return;
    const newSeq = [...globalSequence];
    const temp = newSeq[index + 1];
    newSeq[index + 1] = newSeq[index];
    newSeq[index] = temp;
    setGlobalSequence(newSeq);
  };

  const fetchClassDefaults = async (className) => {
    setLoadingClass(true);
    try {
      const docRef = doc(db, 'class_default_subjects', `Class_${className}`);
      const docSnap = await getDoc(docRef);
      let subjects = [];
      if (docSnap.exists()) {
        subjects = docSnap.data().subjects || [];
        // Map legacy English names to Marathi
        subjects = subjects.map(s => {
          if (EN_TO_MR[s.name]) {
            s.name = EN_TO_MR[s.name];
          }
          return s;
        });
        
        // Auto-append missing true descriptive subjects to existing classes
        const trueDescriptives = ["विशेष प्रगती", "आवड/छंद", "सुधारणा आवश्यक", "व्यक्तिमत्व गुणविशेष"];
        trueDescriptives.forEach(descName => {
          if (!subjects.find(s => s.name === descName)) {
            subjects.push({
              name: descName,
              maxMarks: 0,
              isNonAcademic: true
            });
          }
        });
        
      } else {
        subjects = getHardcodedDefaults(className);
      }
      
      // Sort subjects exactly according to globalSequence
      subjects.sort((a, b) => {
        let indexA = globalSequence.indexOf(a.name);
        let indexB = globalSequence.indexOf(b.name);
        if (indexA === -1) indexA = 999;
        if (indexB === -1) indexB = 999;
        return indexA - indexB;
      });

      // Ensure every subject has a subjectCode and backfill properties
      subjects = subjects.map((sub) => {
        // Sanitize corrupted data: ensure built-in descriptive subjects have maxMarks=0
        const info = getSubjectInfo(sub.name);
        if (info) {
          if (info.maxMarks === 0) {
            sub.maxMarks = 0;
            sub.isNonAcademic = true;
          } else if (info.isNonAcademic) {
            sub.isNonAcademic = true;
          }
        }
        
        if (!sub.subjectCode) {
          let globalIndex = globalSequence.indexOf(sub.name);
          if (globalIndex === -1) globalIndex = globalSequence.length;
          sub.subjectCode = String(globalIndex + 1).padStart(2, '0');
        }
        if (sub.isNonAcademic === undefined) {
          sub.isNonAcademic = false;
        }
        
        return sub;
      });
      
      setClassDefaults(subjects);
    } catch (error) {
      setMessage({ text: 'Error fetching class defaults: ' + error.message, type: 'error' });
    } finally {
      setLoadingClass(false);
    }
  };

  const handleSaveClassDefaults = async () => {
    setSaving(true);
    setMessage({ text: '', type: '' });
    try {
      const docRef = doc(db, 'class_default_subjects', `Class_${selectedClass}`);
      await setDoc(docRef, { subjects: classDefaults, className: selectedClass });
      setMessage({ text: `Defaults for Class ${selectedClass} saved successfully!`, type: 'success' });
    } catch (error) {
      setMessage({ text: 'Error saving class defaults: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleAddSubjectToClass = () => {
    if (!selectedSubjectToAdd) return;
    
    // Check if it already exists
    if (classDefaults.some(s => s.name === selectedSubjectToAdd)) {
      setMessage({ text: 'Subject already exists in this class', type: 'error' });
      return;
    }

    const info = getSubjectInfo(selectedSubjectToAdd);
    let globalIndex = globalSequence.indexOf(selectedSubjectToAdd);
    if (globalIndex === -1) globalIndex = globalSequence.length;
    const newSub = {
      name: selectedSubjectToAdd,
      maxMarks: info ? info.maxMarks : 100,
      subjectCode: String(globalIndex + 1).padStart(2, '0'),
      isNonAcademic: info ? info.isNonAcademic : false
    };

    let newDefaults = [...classDefaults, newSub];
    // Re-sort immediately based on global sequence
    newDefaults.sort((a, b) => {
      let indexA = globalSequence.indexOf(a.name);
      let indexB = globalSequence.indexOf(b.name);
      if (indexA === -1) indexA = 999;
      if (indexB === -1) indexB = 999;
      return indexA - indexB;
    });

    setClassDefaults(newDefaults);
    setSelectedSubjectToAdd('');
    setMessage({ text: '', type: '' });
  };

  const handleRemoveFromClass = (index) => {
    const newDefaults = [...classDefaults];
    newDefaults.splice(index, 1);
    setClassDefaults(newDefaults);
    setMessage({ text: '', type: '' });
  };

  const handleSubjectCodeChange = (index, value) => {
    const newDefaults = [...classDefaults];
    newDefaults[index].subjectCode = value;
    setClassDefaults(newDefaults);
  };

  const handleAddGlobalSubject = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    
    setSaving(true);
    try {
      const colRef = collection(db, 'global_subjects');
      const newSubject = {
        name: name.trim(),
        maxMarks: isDescriptiveOnly ? 0 : Number(maxMarks),
        category: category,
        isNonAcademic: isDescriptiveOnly,
        createdAt: new Date().getTime()
      };
      
      const docRef = await addDoc(colRef, newSubject);
      setGlobalSubjects([...globalSubjects, { id: docRef.id, ...newSubject }]);
      
      // Also add to global sequence intelligently based on category
      let newSeq = [...globalSequence];
      if (newSubject.maxMarks > 0) {
        let firstDescIdx = -1;
        for (let i = 0; i < newSeq.length; i++) {
           const custom = globalSubjects.find(s => s.name === newSeq[i]);
           let isDesc = false;
           if (custom) {
               isDesc = custom.maxMarks === 0;
           } else {
               isDesc = newSeq[i] === "विशेष प्रगती" || newSeq[i] === "आवड/छंद" || newSeq[i] === "सुधारणा आवश्यक" || newSeq[i] === "व्यक्तिमत्व गुणविशेष";
           }
           if (isDesc) {
               firstDescIdx = i;
               break;
           }
        }
        if (firstDescIdx !== -1) {
           newSeq.splice(firstDescIdx, 0, newSubject.name);
        } else {
           newSeq.push(newSubject.name);
        }
      } else {
        newSeq.push(newSubject.name);
      }
      
      setGlobalSequence(newSeq);
      await setDoc(doc(db, 'admin_settings', 'global_subject_sequence'), { subjects: newSeq });
      
      setName('');
      setMaxMarks('100');
      setCategory('Academic');
      setIsDescriptiveOnly(false);
      setMessage({ text: 'Global subject added successfully!', type: 'success' });
    } catch (error) {
      setMessage({ text: 'Error adding subject: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteGlobalSubject = async (id, subjectName) => {
    if (!window.confirm('Are you sure you want to delete this global subject?')) {
      return;
    }
    
    try {
      await deleteDoc(doc(db, 'global_subjects', id));
      setGlobalSubjects(globalSubjects.filter(s => s.id !== id));
      
      // Remove from sequence
      const newSeq = globalSequence.filter(n => n !== subjectName);
      setGlobalSequence(newSeq);
      await setDoc(doc(db, 'admin_settings', 'global_subject_sequence'), { subjects: newSeq });
      
      setMessage({ text: 'Subject deleted.', type: 'info' });
    } catch (error) {
      setMessage({ text: 'Error deleting subject: ' + error.message, type: 'error' });
    }
  };

  const handleOpenEditModal = (subjectName) => {
    const info = getSubjectInfo(subjectName);
    
    // Check if it exists in global_subjects
    const custom = globalSubjects.find(s => s.name === subjectName);
    
    let defaultFormative = info.maxMarks === 0 ? 0 : (info.maxMarks / 2);
    let defaultSummative = info.maxMarks === 0 ? 0 : (info.maxMarks - defaultFormative);
    
    // Set up default values
    const data = {
      name: subjectName,
      shortName: custom?.shortName || '',
      maxMarks: info.maxMarks,
      category: info.category,
      isNonAcademic: info.isNonAcademic,
      maxNirikhshan: custom?.maxNirikhshan || 0,
      maxTondiKam: custom?.maxTondiKam ?? (defaultFormative * 10 / 50),
      maxPratyakshik: custom?.maxPratyakshik ?? (defaultFormative * 10 / 50),
      maxUpkram: custom?.maxUpkram ?? (defaultFormative * 10 / 50),
      maxPrakalp: custom?.maxPrakalp || 0,
      maxChachani: custom?.maxChachani ?? (defaultFormative * 20 / 50),
      maxSwadhyay: custom?.maxSwadhyay || 0,
      maxItar: custom?.maxItar || 0,
      maxTondi: custom?.maxTondi ?? (defaultSummative * 10 / 50),
      maxPratyakshikB: custom?.maxPratyakshikB ?? (defaultSummative * 10 / 50),
      maxLekhi: custom?.maxLekhi ?? (defaultSummative - (defaultSummative * 10 / 50) - (defaultSummative * 10 / 50))
    };

    if (info.maxMarks === 0 || info.isNonAcademic) {
      data.maxTondi = 0;
      data.maxPratyakshikB = 0;
      data.maxLekhi = 0;
    }

    setEditingSubject({ originalName: subjectName, id: info.id, type: info.type });
    setEditData(data);
    setEditModalOpen(true);
  };

  const handleSaveEditModal = async (e) => {
    e.preventDefault();
    setSaving(true);
    
    try {
      const oldName = editingSubject.originalName;
      const newName = editData.name.trim();
      
      const updatedCustomObj = {
        name: newName,
        shortName: editData.shortName || "",
        category: editData.category,
        maxMarks: Number(editData.maxMarks),
        isNonAcademic: editData.isNonAcademic,
        maxNirikhshan: Number(editData.maxNirikhshan),
        maxTondiKam: Number(editData.maxTondiKam),
        maxPratyakshik: Number(editData.maxPratyakshik),
        maxUpkram: Number(editData.maxUpkram),
        maxPrakalp: Number(editData.maxPrakalp),
        maxChachani: Number(editData.maxChachani),
        maxSwadhyay: Number(editData.maxSwadhyay),
        maxItar: Number(editData.maxItar),
        maxTondi: Number(editData.maxTondi),
        maxPratyakshikB: Number(editData.maxPratyakshikB),
        maxLekhi: Number(editData.maxLekhi),
        updatedAt: new Date().getTime()
      };

      // 1. Save to global_subjects
      let savedId = editingSubject.id;
      if (editingSubject.type === 'Custom' && savedId) {
        await setDoc(doc(db, 'global_subjects', savedId), updatedCustomObj, { merge: true });
        setGlobalSubjects(globalSubjects.map(s => s.id === savedId ? { id: savedId, ...updatedCustomObj } : s));
      } else {
        // It was Built-in, now saving as Custom overriding the built-in
        const docRef = await addDoc(collection(db, 'global_subjects'), { ...updatedCustomObj, createdAt: new Date().getTime() });
        savedId = docRef.id;
        setGlobalSubjects([...globalSubjects, { id: savedId, ...updatedCustomObj }]);
      }

      // 2. Update globalSequence if name changed
      if (oldName !== newName) {
        const newSeq = [...globalSequence];
        const idx = newSeq.indexOf(oldName);
        if (idx !== -1) {
          newSeq[idx] = newName;
          setGlobalSequence(newSeq);
          await setDoc(doc(db, 'admin_settings', 'global_subject_sequence'), { subjects: newSeq });
        }
      }

      // 3. Sync to all class_default_subjects
      for (let i = 1; i <= 10; i++) {
        const classRef = doc(db, 'class_default_subjects', `Class_${i}`);
        const snap = await getDoc(classRef);
        if (snap.exists() && snap.data().subjects) {
          let subs = snap.data().subjects;
          let changed = false;
          
          for (let j = 0; j < subs.length; j++) {
            if (subs[j].name === oldName) {
               // Update all details
               subs[j] = { ...subs[j], ...updatedCustomObj };
               changed = true;
            }
          }
          if (changed) {
            await setDoc(classRef, { subjects: subs }, { merge: true });
          }
        }
      }

      setMessage({ text: 'Subject updated successfully and synced to all classes!', type: 'success' });
      setEditModalOpen(false);
      
      // Refresh current class view if we are on class defaults
      if (activeTab === 'classDefaults') {
        fetchClassDefaults(selectedClass);
      }
    } catch (error) {
      console.error(error);
      setMessage({ text: 'Error saving: ' + error.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const getSubjectInfo = (subjectName) => {
    const custom = globalSubjects.find(s => s.name === subjectName);
    if (custom) return { type: 'Custom', category: custom.category, maxMarks: custom.maxMarks, isNonAcademic: custom.isNonAcademic, id: custom.id };
    
    let isFeMarksOnly = subjectName === "कला" || subjectName === "कार्यानुभव" || subjectName === "आरोग्य व शारीरिक शिक्षण" || subjectName === "खेळू, करू, शिकू";
    let isTrueDescriptive = subjectName === "विशेष प्रगती" || subjectName === "आवड/छंद" || subjectName === "सुधारणा आवश्यक" || subjectName === "व्यक्तिमत्व गुणविशेष";
    
    return { 
      type: 'Built-in', 
      category: isTrueDescriptive ? 'Personality' : isFeMarksOnly ? 'Activities' : 'Academic/Other', 
      maxMarks: isTrueDescriptive ? 0 : 100, 
      isNonAcademic: isFeMarksOnly || isTrueDescriptive,
      id: null 
    };
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
          Global Subjects Sequence
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
            <h3>Add Custom Subject</h3>
            <p className="helper-text">Add custom subjects if they are missing from the built-in list.</p>
            
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
                <input type="number" value={maxMarks} onChange={(e) => setMaxMarks(e.target.value)} min="1" max="1000" disabled={isDescriptiveOnly} required />
              </div>
              <div className="form-group" style={{ flexDirection: 'row', alignItems: 'center', gap: '8px' }}>
                <input 
                  type="checkbox" 
                  id="isDescriptiveOnly"
                  checked={isDescriptiveOnly}
                  onChange={(e) => setIsDescriptiveOnly(e.target.checked)}
                  style={{ width: 'auto' }}
                />
                <label htmlFor="isDescriptiveOnly" style={{ margin: 0, cursor: 'pointer' }}>
                  Only Descriptive Entries (No Marks / Graded)
                </label>
              </div>
              <button type="submit" className="btn btn-primary" disabled={saving || !name.trim()}>
                {saving ? 'Adding...' : '+ Add Custom Subject'}
              </button>
            </form>
          </div>

          <div className="glass-panel list-panel animate-fade-in">
            <div className="list-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3>Global Subjects Sequence</h3>
              <button className="btn btn-primary" onClick={handleSaveGlobalSequence} disabled={saving}>
                💾 Save Global Sequence
              </button>
            </div>
            <p className="helper-text">This order determines the <strong>subjectCode</strong> and default sorting for all classes.</p>
            
            {loading ? (
              <div className="loading">Loading global subjects...</div>
            ) : (
              <div className="subjects-list" style={{ marginTop: '16px' }}>
                {globalSequence.length === 0 ? (
                  <div className="empty-state">No subjects found.</div>
                ) : (
                  globalSequence.map((subjectName, index) => {
                    const info = getSubjectInfo(subjectName);
                    const currentType = info.maxMarks === 0 ? 'descriptive' : 'academic';
                    
                    let showHeader = false;
                    if (index === 0) {
                      showHeader = true;
                    } else {
                      const prevInfo = getSubjectInfo(globalSequence[index - 1]);
                      const prevType = prevInfo.maxMarks === 0 ? 'descriptive' : 'academic';
                      showHeader = currentType !== prevType;
                    }

                    return (
                      <React.Fragment key={subjectName}>
                        {showHeader && (
                          <div style={{ 
                            padding: '8px 12px', 
                            background: currentType === 'descriptive' ? '#fff3e0' : '#e3f2fd', 
                            color: currentType === 'descriptive' ? '#e65100' : '#1565c0',
                            fontWeight: 'bold',
                            borderRadius: '6px',
                            marginTop: index === 0 ? '0' : '16px',
                            marginBottom: '12px',
                            border: `1px solid ${currentType === 'descriptive' ? '#ffe0b2' : '#bbdefb'}`
                          }}>
                            {currentType === 'descriptive' ? 'Only Descriptive Entries' : 'Academic & Graded Subjects'}
                          </div>
                        )}
                        <div className="sequence-item" style={{ marginBottom: '8px' }}>
                        <div className="sequence-controls">
                          <button type="button" className="seq-btn" onClick={() => handleGlobalMoveUp(index)} disabled={index === 0}>↑</button>
                          <span className="seq-number">{index + 1}</span>
                          <button type="button" className="seq-btn" onClick={() => handleGlobalMoveDown(index)} disabled={index === globalSequence.length - 1}>↓</button>
                        </div>
                        <div className="sequence-info">
                          <span className="subject-name">{subjectName}</span>
                          <div className="subject-meta" style={{ display: 'flex', gap: '8px', alignItems: 'center', marginTop: '4px' }}>
                            <span className={`meta-badge ${info.type === 'Built-in' ? 'info' : 'success'}`}>{info.type}</span>
                            {info.type === 'Custom' && <span className="meta-badge">{info.category}</span>}
                            {info.maxMarks === 0 ? (
                              <span className="meta-badge marks" style={{ background: '#ff9800', color: '#fff' }}>Only Descriptive</span>
                            ) : info.isNonAcademic ? (
                              <span className="meta-badge marks" style={{ background: '#2196F3', color: '#fff' }}>FE Marks Only</span>
                            ) : null}
                          </div>
                        </div>
                        <div className="sequence-actions" style={{ display: 'flex', gap: '8px', marginLeft: 'auto' }}>
                          <button type="button" className="btn btn-secondary" onClick={() => handleOpenEditModal(subjectName)} style={{ padding: '4px 8px', fontSize: '0.8rem' }}>Edit</button>
                          {info.type === 'Custom' && (
                            <button type="button" className="btn-delete-subject" onClick={() => handleDeleteGlobalSubject(info.id, subjectName)} title="Delete Custom Subject">×</button>
                          )}
                        </div>
                      </div>
                      </React.Fragment>
                    );
                  })
                )}
              </div>
            )}
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
            <div className="editor-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <h3>Active Subjects for Class {selectedClass}</h3>
                <p className="helper-text" style={{margin: 0}}>Order is locked to the Global Sequence.</p>
              </div>
              <button className="btn btn-primary" onClick={handleSaveClassDefaults} disabled={saving}>
                {saving ? 'Saving...' : '💾 Save Class Defaults'}
              </button>
            </div>

            <div className="add-to-class-bar" style={{ marginTop: '16px' }}>
              <select 
                value={selectedSubjectToAdd} 
                onChange={(e) => setSelectedSubjectToAdd(e.target.value)}
                className="subject-dropdown"
              >
                <option value="">-- Add a subject to this class --</option>
                {globalSequence.filter(s => !classDefaults.some(cd => cd.name === s)).map(s => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
              <button className="btn btn-secondary" onClick={handleAddSubjectToClass} disabled={!selectedSubjectToAdd}>
                + Add
              </button>
            </div>

            {loadingClass ? (
              <div className="loading">Loading class defaults...</div>
            ) : (
              <div className="sequence-list" style={{ marginTop: '16px' }}>
                {classDefaults.length === 0 ? (
                  <div className="empty-state">No subjects active for this class.</div>
                ) : (
                  classDefaults.map((subject, index) => {
                    let globalIndex = globalSequence.indexOf(subject.name);
                    const currentType = subject.maxMarks === 0 ? 'descriptive' : 'academic';
                    
                    let showHeader = false;
                    if (index === 0) {
                      showHeader = true;
                    } else {
                      const prevSubject = classDefaults[index - 1];
                      const prevType = prevSubject.maxMarks === 0 ? 'descriptive' : 'academic';
                      showHeader = currentType !== prevType;
                    }

                    return (
                      <React.Fragment key={index}>
                        {showHeader && (
                          <div style={{ 
                            padding: '8px 12px', 
                            background: currentType === 'descriptive' ? '#fff3e0' : '#e3f2fd', 
                            color: currentType === 'descriptive' ? '#e65100' : '#1565c0',
                            fontWeight: 'bold',
                            borderRadius: '6px',
                            marginTop: index === 0 ? '0' : '16px',
                            marginBottom: '12px',
                            border: `1px solid ${currentType === 'descriptive' ? '#ffe0b2' : '#bbdefb'}`
                          }}>
                            {currentType === 'descriptive' ? 'Only Descriptive Entries' : 'Academic & Graded Subjects'}
                          </div>
                        )}
                        <div className="sequence-item" style={{ marginBottom: '8px' }}>
                        <div className="sequence-controls">
                          <span className="seq-number" style={{ background: '#eee', color: '#666' }}>{globalIndex !== -1 ? globalIndex + 1 : '-'}</span>
                        </div>
                        
                        <div className="sequence-info">
                          <span className="subject-name">{subject.name}</span>
                          <span className="subject-max" style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                            {subject.maxMarks === 0 ? (
                              <span className="meta-badge marks" style={{ background: '#ff9800', color: '#fff' }}>Only Descriptive</span>
                            ) : subject.isNonAcademic ? (
                              <span className="meta-badge marks" style={{ background: '#2196F3', color: '#fff' }}>FE Marks Only (Max {subject.maxMarks})</span>
                            ) : (
                              <>Max Marks: {subject.maxMarks || 100}</>
                            )}
                          </span>
                        </div>
                        
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'flex-end', marginRight: '8px' }}>
                          <label style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                            Subject Code
                          </label>
                          <input 
                            type="text" 
                            value={subject.subjectCode || ''} 
                            onChange={(e) => handleSubjectCodeChange(index, e.target.value)}
                            style={{ 
                              width: '90px', 
                              padding: '6px 10px', 
                              borderRadius: '6px', 
                              border: '1px solid var(--border-color)', 
                              background: 'var(--surface-muted)', 
                              color: 'var(--text-primary)',
                              fontSize: '14px',
                              fontFamily: 'monospace',
                              textAlign: 'center'
                            }}
                          />
                        </div>

                        <button className="btn-delete-subject" onClick={() => handleRemoveFromClass(index)} title="Remove Subject from Class">×</button>
                      </div>
                      </React.Fragment>
                    );
                  })
                )}
              </div>
            )}
          </div>
        </div>
      )}
      {editModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content admin-modal">
            <div className="modal-header">
              <h2>Edit Global Subject: {editingSubject?.originalName}</h2>
              <button className="close-btn" onClick={() => setEditModalOpen(false)}>✕</button>
            </div>
            
            <form onSubmit={handleSaveEditModal} className="admin-form">
              <div className="form-row">
                <div className="form-group">
                  <label>Subject Name</label>
                  <input 
                    type="text" 
                    value={editData.name} 
                    onChange={e => setEditData({...editData, name: e.target.value})} 
                    required 
                  />
                </div>
                <div className="form-group">
                  <label>Short Name</label>
                  <input 
                    type="text" 
                    value={editData.shortName} 
                    onChange={e => setEditData({...editData, shortName: e.target.value})} 
                    placeholder="Used in report headers"
                  />
                </div>
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label>Category</label>
                  <select value={editData.category} onChange={e => setEditData({...editData, category: e.target.value})}>
                    <option value="Academic">Academic</option>
                    <option value="Activities">Activities</option>
                    <option value="Personality">Personality Development</option>
                    <option value="State Board">State Board Addition</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Max Marks</label>
                  <input 
                    type="number" 
                    value={editData.maxMarks} 
                    onChange={e => setEditData({...editData, maxMarks: e.target.value})} 
                    min="0" max="1000" 
                    disabled={editData.isNonAcademic}
                    required 
                  />
                </div>
              </div>
              
              <div className="form-group" style={{ flexDirection: 'row', alignItems: 'center', gap: '8px' }}>
                <input 
                  type="checkbox" 
                  id="editIsDescriptive"
                  checked={editData.isNonAcademic}
                  onChange={e => setEditData({...editData, isNonAcademic: e.target.checked})}
                  style={{ width: 'auto' }}
                />
                <label htmlFor="editIsDescriptive" style={{ margin: 0, cursor: 'pointer' }}>
                  Only Descriptive Entries (No Marks / Graded)
                </label>
              </div>

              {!editData.isNonAcademic && (
                <div className="marks-grid-editor">
                  <div className="marks-section">
                    <h4>Formative (आकारिक)</h4>
                    <div className="grid-2col">
                      <div><label>Observation</label><input type="number" value={editData.maxNirikhshan} onChange={e=>setEditData({...editData, maxNirikhshan: e.target.value})} /></div>
                      <div><label>Oral</label><input type="number" value={editData.maxTondiKam} onChange={e=>setEditData({...editData, maxTondiKam: e.target.value})} /></div>
                      <div><label>Practical</label><input type="number" value={editData.maxPratyakshik} onChange={e=>setEditData({...editData, maxPratyakshik: e.target.value})} /></div>
                      <div><label>Activity</label><input type="number" value={editData.maxUpkram} onChange={e=>setEditData({...editData, maxUpkram: e.target.value})} /></div>
                      <div><label>Project</label><input type="number" value={editData.maxPrakalp} onChange={e=>setEditData({...editData, maxPrakalp: e.target.value})} /></div>
                      <div><label>Test</label><input type="number" value={editData.maxChachani} onChange={e=>setEditData({...editData, maxChachani: e.target.value})} /></div>
                      <div><label>Homework</label><input type="number" value={editData.maxSwadhyay} onChange={e=>setEditData({...editData, maxSwadhyay: e.target.value})} /></div>
                      <div><label>Other</label><input type="number" value={editData.maxItar} onChange={e=>setEditData({...editData, maxItar: e.target.value})} /></div>
                    </div>
                  </div>
                  <div className="marks-section">
                    <h4>Summative (संकलित)</h4>
                    <div className="grid-2col">
                      <div><label>Oral</label><input type="number" value={editData.maxTondi} onChange={e=>setEditData({...editData, maxTondi: e.target.value})} /></div>
                      <div><label>Practical</label><input type="number" value={editData.maxPratyakshikB} onChange={e=>setEditData({...editData, maxPratyakshikB: e.target.value})} /></div>
                      <div><label>Written</label><input type="number" value={editData.maxLekhi} onChange={e=>setEditData({...editData, maxLekhi: e.target.value})} /></div>
                    </div>
                  </div>
                </div>
              )}

              <div className="modal-footer" style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setEditModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? 'Saving...' : '💾 Save & Sync Globally'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </main>
  );
}
