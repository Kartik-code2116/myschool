import React, { useState, useEffect } from 'react';
import { useTeacherContext } from '../context/TeacherContext';
import { db, auth } from '../firebase';
import { collection, query, where, getDocs, doc, setDoc } from 'firebase/firestore';
import './AppMarks.css';

const DEFAULT_SUBJECTS = ["Marathi", "Hindi", "English", "Mathematics", "Science", "Social Science", "Art", "Physical Education"];

export default function AppMarks() {
  const { activeClass, activeAcademicYear, activeSemester } = useTeacherContext();
  const [students, setStudents] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);
  
  const [marksRecord, setMarksRecord] = useState(null);
  const [detailedMarks, setDetailedMarks] = useState({});

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function fetchStudents() {
      if (!activeClass) { setLoading(false); return; }
      setLoading(true);
      try {
        const qStu = query(collection(db, 'students'), 
            where('classId', '==', activeClass.id),
            where('teacherId', '==', auth.currentUser.uid)
        );
        const snap = await getDocs(qStu);
        const stuList = snap.docs.map(d => ({ id: d.id, ...d.data() }));
        stuList.sort((a,b) => parseInt(a.rollNo||0) - parseInt(b.rollNo||0));
        setStudents(stuList);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    fetchStudents();
  }, [activeClass]);

  useEffect(() => {
    async function loadMarks() {
      if (!selectedStudent || !activeAcademicYear) return;
      
      try {
        const q = query(collection(db, 'marks'), 
            where('studentId', '==', selectedStudent.id),
            where('semesterId', '==', activeSemester?.id || "1"),
            where('teacherId', '==', auth.currentUser.uid)
        );
        const snap = await getDocs(q);
        
        if (!snap.empty) {
            const docData = { id: snap.docs[0].id, ...snap.docs[0].data() };
            setMarksRecord(docData);
            setDetailedMarks(docData.detailedMarks || {});
        } else {
            setMarksRecord(null);
            setDetailedMarks({});
        }
      } catch (err) {
          console.error(err);
      }
    }
    loadMarks();
  }, [selectedStudent, activeAcademicYear, activeSemester]);

  const [entryMode, setEntryMode] = useState('marks'); // 'marks' or 'remarks'

  const handleMarkChange = (subject, field, value) => {
      setDetailedMarks(prev => {
          const current = prev[subject] || { akarikTotal: 0, sanklit: 0, grandTotal: 0, remark: '' };
          const updated = { ...current, [field]: value };
          if (field === 'akarikTotal' || field === 'sanklit') {
            updated[field] = parseInt(value) || 0;
            updated.grandTotal = (parseInt(updated.akarikTotal) || 0) + (parseInt(updated.sanklit) || 0);
          }
          return { ...prev, [subject]: updated };
      });
  };

  const handleSave = async () => {
      if (!selectedStudent || !activeClass || !auth.currentUser) return;
      setSaving(true);
      try {
          const docRef = marksRecord?.id ? doc(db, 'marks', marksRecord.id) : doc(collection(db, 'marks'));
          const payload = {
              id: docRef.id,
              studentId: selectedStudent.id,
              classId: activeClass.id,
              teacherId: auth.currentUser.uid,
              semesterId: activeSemester?.id || "1",
              detailedMarks: detailedMarks,
              updatedAt: Date.now()
          };
          await setDoc(docRef, payload, { merge: true });
          setMarksRecord(payload);
          alert("Marks saved successfully!");
      } catch(err) {
          console.error(err);
          alert("Error saving marks.");
      } finally {
          setSaving(false);
      }
  };

  if (!activeClass) {
      return <div className="app-marks"><div className="warning-banner">Please select an Active Class from the Dashboard.</div></div>;
  }

  return (
    <div className="app-marks">
      <div className="marks-header">
        <h2>Marks Entry (Formative & Summative)</h2>
      </div>

      <div className="marks-layout">
          <div className="students-sidebar card-panel">
              <h3>Students</h3>
              {loading ? <p>Loading...</p> : (
                  <ul className="student-list-compact">
                      {students.map(stu => (
                          <li 
                            key={stu.id} 
                            className={selectedStudent?.id === stu.id ? 'active' : ''}
                            onClick={() => setSelectedStudent(stu)}
                          >
                              {stu.rollNo}. {stu.name}
                          </li>
                      ))}
                  </ul>
              )}
          </div>

          <div className="marks-editor card-panel">
              {!selectedStudent ? (
                  <div className="empty-state">Select a student from the list to enter marks.</div>
              ) : (
                  <>
                      <div className="editor-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                            <h3 style={{ margin: 0 }}>Grading: {selectedStudent.name}</h3>
                            <div className="mode-toggle" style={{ display: 'flex', background: 'var(--bg-color)', borderRadius: '8px', padding: '4px' }}>
                              <button 
                                className={`toggle-btn ${entryMode === 'marks' ? 'active' : ''}`}
                                onClick={() => setEntryMode('marks')}
                                style={{ padding: '6px 12px', border: 'none', background: entryMode === 'marks' ? 'var(--surface-color)' : 'transparent', borderRadius: '6px', cursor: 'pointer', fontWeight: '600', boxShadow: entryMode === 'marks' ? 'var(--shadow-sm)' : 'none' }}
                              >
                                Marks
                              </button>
                              <button 
                                className={`toggle-btn ${entryMode === 'remarks' ? 'active' : ''}`}
                                onClick={() => setEntryMode('remarks')}
                                style={{ padding: '6px 12px', border: 'none', background: entryMode === 'remarks' ? 'var(--surface-color)' : 'transparent', borderRadius: '6px', cursor: 'pointer', fontWeight: '600', boxShadow: entryMode === 'remarks' ? 'var(--shadow-sm)' : 'none' }}
                              >
                                Descriptive Remarks
                              </button>
                            </div>
                          </div>
                          <button className="btn-primary" onClick={handleSave} disabled={saving}>
                              {saving ? 'Saving...' : 'Save Data'}
                          </button>
                      </div>
                      <div className="marks-table-container">
                          {entryMode === 'marks' ? (
                            <table className="marks-table">
                                <thead>
                                    <tr>
                                        <th>Subject</th>
                                        <th>Formative (Akarik)</th>
                                        <th>Summative (Sankalit)</th>
                                        <th>Total</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {DEFAULT_SUBJECTS.map(sub => {
                                        const data = detailedMarks[sub] || {};
                                        return (
                                            <tr key={sub}>
                                                <td>{sub}</td>
                                                <td>
                                                    <input 
                                                      type="number" 
                                                      value={data.akarikTotal || ''} 
                                                      onChange={e => handleMarkChange(sub, 'akarikTotal', e.target.value)} 
                                                      placeholder="0"
                                                    />
                                                </td>
                                                <td>
                                                    <input 
                                                      type="number" 
                                                      value={data.sanklit || ''} 
                                                      onChange={e => handleMarkChange(sub, 'sanklit', e.target.value)}
                                                      placeholder="0" 
                                                    />
                                                </td>
                                                <td><strong>{data.grandTotal || 0}</strong></td>
                                            </tr>
                                        )
                                    })}
                                </tbody>
                            </table>
                          ) : (
                            <table className="marks-table remarks-table">
                                <thead>
                                    <tr>
                                        <th style={{ width: '20%' }}>Subject</th>
                                        <th>Descriptive Remarks (वर्णनात्मक नोंदी)</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {DEFAULT_SUBJECTS.map(sub => {
                                        const data = detailedMarks[sub] || {};
                                        return (
                                            <tr key={sub}>
                                                <td>{sub}</td>
                                                <td>
                                                    <textarea 
                                                      value={data.remark || ''} 
                                                      onChange={e => handleMarkChange(sub, 'remark', e.target.value)}
                                                      placeholder={`Enter descriptive remarks for ${sub}...`}
                                                      style={{ width: '100%', minHeight: '60px', padding: '10px', borderRadius: '8px', border: '1px solid var(--border-color)', resize: 'vertical' }}
                                                    />
                                                </td>
                                            </tr>
                                        )
                                    })}
                                </tbody>
                            </table>
                          )}
                      </div>
                  </>
              )}
          </div>
      </div>
    </div>
  );
}
