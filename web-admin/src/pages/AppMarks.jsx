import React, { useState, useEffect } from 'react';
import { useTeacherContext } from '../context/TeacherContext';
import { db, auth } from '../firebase';
import { collection, query, where, getDocs, doc, setDoc, onSnapshot } from 'firebase/firestore';
import useLanguage from '../utils/useLanguage';
import './AppMarks.css';

// Utility to match Android's MarksRecord.sanitizeKey()
const sanitizeKey = (key) => {
  if (!key) return "unknown";
  return key.replace(/[\.#\$\[\]\/\\~\*]/g, "_");
};

export default function AppMarks() {
  const { activeClass, activeAcademicYear, activeSemester, teacherProfile } = useTeacherContext();
  const { t } = useLanguage();
  
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
    if (!selectedStudent || !activeAcademicYear) return;
    
    const q = query(collection(db, 'marks'), 
        where('studentId', '==', selectedStudent.id),
        where('classId', '==', activeClass.id),
        where('teacherId', '==', auth.currentUser.uid)
    );
    
    const unsubscribe = onSnapshot(q, (snap) => {
        let foundRecord = null;
        let fallbackRecord = null;
        const targetSemId = activeSemester?.id;
        const targetSemNum = activeSemester?.number || 1;

        snap.forEach(doc => {
            const data = { id: doc.id, ...doc.data() };
            const recSemId = data.semesterId;
            let recSemNum = 1;
            if (data.semesterNumber) recSemNum = parseInt(data.semesterNumber) || 1;
            
            if (recSemId === targetSemId || recSemNum === targetSemNum) {
                if (!foundRecord || data.updatedAt > foundRecord.updatedAt) {
                    foundRecord = data;
                }
            } else if (!recSemId) {
                // Legacy Android app data fallback (Sem 1)
                if (targetSemNum === 1) {
                    if (!fallbackRecord || data.updatedAt > fallbackRecord.updatedAt) {
                        fallbackRecord = data;
                    }
                }
            }
        });

        const docData = foundRecord || fallbackRecord;
        
        if (docData) {
            setMarksRecord(docData);
            setDetailedMarks(docData.detailedMarks || {});
        } else {
            setMarksRecord(null);
            setDetailedMarks({});
        }
    }, (err) => {
        console.error(err);
    });

    return () => unsubscribe();
  }, [selectedStudent, activeAcademicYear, activeSemester]);

  const handleMarkChange = (subjectName, field, value) => {
      const sanitizedKey = sanitizeKey(subjectName);
      
      setDetailedMarks(prev => {
          const current = prev[sanitizedKey] || { 
            nirikhshan: 0, tondiKam: 0, pratyakshik: 0, upkram: 0, prakalp: 0, chachani: 0, swadhyay: 0, itar: 0, akarikTotal: 0,
            tondi: 0, pratyakshikB: 0, lekhi: 0, sanklit: 0,
            grandTotal: 0 
          };
          
          const val = value === '' ? '' : parseInt(value) || 0;
          const updated = { ...current, [field]: val };
          
          // Auto-calculate Formative (Akarik) Total
          updated.akarikTotal = 
            (parseInt(updated.nirikhshan)||0) + 
            (parseInt(updated.tondiKam)||0) + 
            (parseInt(updated.pratyakshik)||0) + 
            (parseInt(updated.upkram)||0) + 
            (parseInt(updated.prakalp)||0) + 
            (parseInt(updated.chachani)||0) + 
            (parseInt(updated.swadhyay)||0) + 
            (parseInt(updated.itar)||0);
            
          // Auto-calculate Summative (Sanklit) Total
          updated.sanklit = 
            (parseInt(updated.tondi)||0) + 
            (parseInt(updated.pratyakshikB)||0) + 
            (parseInt(updated.lekhi)||0);
            
          // Grand Total
          updated.grandTotal = updated.akarikTotal + updated.sanklit;

          return { ...prev, [sanitizedKey]: updated };
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
              semesterNumber: activeSemester?.number?.toString() || "1",
              detailedMarks: detailedMarks,
              updatedAt: Date.now()
          };
          await setDoc(docRef, payload, { merge: true });
          setMarksRecord(payload);
          alert(t("Marks saved successfully!", "गुण यशस्वीरित्या सेव्ह झाले!"));
      } catch(err) {
          console.error(err);
          alert(t("Error saving marks.", "गुण सेव्ह करताना त्रुटी."));
      } finally {
          setSaving(false);
      }
  };

  if (!activeClass) {
      return <div className="app-marks"><div className="warning-banner">{t("Please select an Active Class from the Dashboard.", "कृपया डॅशबोर्डवरून सक्रिय वर्ग निवडा.")}</div></div>;
  }

  const subjects = activeClass.subjects || [];

  return (
    <div className="app-marks">
      <div className="marks-header">
        <h2>{t("Marks Entry", "गुण नोंद")} (Formative & Summative)</h2>
        <p>{t("Enter detailed marks for each student. Calculations are automatic.", "प्रत्येक विद्यार्थ्यासाठी सविस्तर गुण नोंदवा. एकूण बेरीज आपोआप होईल.")}</p>
      </div>

      <div className="marks-layout">
          <div className="students-sidebar">
              <h3>{t("Students", "विद्यार्थी")}</h3>
              {loading ? <p>{t("Loading...", "लोड होत आहे...")}</p> : (
                  <ul className="student-list-compact">
                      {students.map((stu, index) => {
                          const isLocked = teacherProfile?.subscriptionStatus !== 'active' && index >= 3;
                          return (
                            <li 
                              key={stu.id} 
                              className={`${selectedStudent?.id === stu.id ? 'active' : ''} ${isLocked ? 'locked-item' : ''}`}
                              onClick={() => {
                                if (isLocked) {
                                  alert(t("Free limit reached. Upgrade to premium to enter marks for more students.", "अधिक विद्यार्थ्यांचे गुण नोंदवण्यासाठी कृपया प्रीमियम सबस्क्रिप्शन घ्या."));
                                } else {
                                  setSelectedStudent(stu);
                                }
                              }}
                              style={isLocked ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
                            >
                                {stu.rollNo}. {stu.name} {isLocked && '🔒'}
                            </li>
                          );
                      })}
                  </ul>
              )}
          </div>

          <div className="marks-editor">
              {!selectedStudent ? (
                  <div className="empty-state">
                    <h3>👆</h3>
                    <p>{t("Select a student from the list to enter marks.", "गुण नोंदवण्यासाठी यादीतून विद्यार्थी निवडा.")}</p>
                  </div>
              ) : (
                  <>
                      <div className="editor-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <h3 style={{ margin: 0, color: 'var(--primary-color)' }}>{selectedStudent.rollNo}. {selectedStudent.name}</h3>
                          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
                              {saving ? t('Saving...', 'सेव्ह होत आहे...') : t('Save Marks', 'गुण सेव्ह करा')}
                          </button>
                      </div>
                      
                      {subjects.length === 0 ? (
                        <div className="empty-state" style={{ marginTop: '20px' }}>
                          <p>{t("No subjects found. Please configure subjects in Settings.", "कोणतेही विषय आढळले नाहीत. कृपया सेटिंग्ज मध्ये विषय सेट करा.")}</p>
                        </div>
                      ) : (
                        <div className="spreadsheet-container">
                            <table className="spreadsheet-table">
                                <thead>
                                    <tr>
                                        <th rowSpan="2" className="subject-cell">{t("Subject", "विषय")}</th>
                                        <th colSpan="8" className="group-header header-formative">{t("Formative (आकारिक)", "आकारिक (Formative)")}</th>
                                        <th rowSpan="2" className="group-header header-formative">{t("Total A", "एकूण A")}</th>
                                        <th colSpan="3" className="group-header header-summative">{t("Summative (संकलित)", "संकलित (Summative)")}</th>
                                        <th rowSpan="2" className="group-header header-summative">{t("Total B", "एकूण B")}</th>
                                        <th rowSpan="2" className="group-header header-total">{t("Grand Total", "एकूण (A+B)")}</th>
                                    </tr>
                                    <tr>
                                        {/* Formative */}
                                        <th>{t("Obs.", "निरी.")}</th>
                                        <th>{t("Oral", "तोंडी")}</th>
                                        <th>{t("Prac", "प्रात्य.")}</th>
                                        <th>{t("Act.", "उप.")}</th>
                                        <th>{t("Proj", "प्रक.")}</th>
                                        <th>{t("Test", "चाच.")}</th>
                                        <th>{t("HW", "स्वा.")}</th>
                                        <th>{t("Oth.", "इतर")}</th>
                                        {/* Summative */}
                                        <th>{t("Oral", "तोंडी")}</th>
                                        <th>{t("Prac", "प्रात्य.")}</th>
                                        <th>{t("Writ.", "लेखी")}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {subjects.map(sub => {
                                        const sanitizedKey = sanitizeKey(sub.name);
                                        const data = detailedMarks[sanitizedKey] || {};
                                        return (
                                            <tr key={sub.name}>
                                                <td className="subject-cell">{sub.name}</td>
                                                
                                                {/* Formative Inputs */}
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxNirikhshan} value={data.nirikhshan ?? ''} onChange={e => handleMarkChange(sub.name, 'nirikhshan', e.target.value)} disabled={!sub.maxNirikhshan} /></td>
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxTondiKam} value={data.tondiKam ?? ''} onChange={e => handleMarkChange(sub.name, 'tondiKam', e.target.value)} disabled={!sub.maxTondiKam} /></td>
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxPratyakshik} value={data.pratyakshik ?? ''} onChange={e => handleMarkChange(sub.name, 'pratyakshik', e.target.value)} disabled={!sub.maxPratyakshik} /></td>
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxUpkram} value={data.upkram ?? ''} onChange={e => handleMarkChange(sub.name, 'upkram', e.target.value)} disabled={!sub.maxUpkram} /></td>
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxPrakalp} value={data.prakalp ?? ''} onChange={e => handleMarkChange(sub.name, 'prakalp', e.target.value)} disabled={!sub.maxPrakalp} /></td>
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxChachani} value={data.chachani ?? ''} onChange={e => handleMarkChange(sub.name, 'chachani', e.target.value)} disabled={!sub.maxChachani} /></td>
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxSwadhyay} value={data.swadhyay ?? ''} onChange={e => handleMarkChange(sub.name, 'swadhyay', e.target.value)} disabled={!sub.maxSwadhyay} /></td>
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxItar} value={data.itar ?? ''} onChange={e => handleMarkChange(sub.name, 'itar', e.target.value)} disabled={!sub.maxItar} /></td>
                                                
                                                <td className="calc-cell">{data.akarikTotal || 0}</td>
                                                
                                                {/* Summative Inputs */}
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxTondi} value={data.tondi ?? ''} onChange={e => handleMarkChange(sub.name, 'tondi', e.target.value)} disabled={!sub.maxTondi} /></td>
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxPratyakshikB} value={data.pratyakshikB ?? ''} onChange={e => handleMarkChange(sub.name, 'pratyakshikB', e.target.value)} disabled={!sub.maxPratyakshikB} /></td>
                                                <td><input className="mark-input" type="number" min="0" max={sub.maxLekhi} value={data.lekhi ?? ''} onChange={e => handleMarkChange(sub.name, 'lekhi', e.target.value)} disabled={!sub.maxLekhi} /></td>
                                                
                                                <td className="calc-cell">{data.sanklit || 0}</td>
                                                
                                                <td className="grand-total-cell">{data.grandTotal || 0}</td>
                                            </tr>
                                        )
                                    })}
                                </tbody>
                            </table>
                        </div>
                      )}
                  </>
              )}
          </div>
      </div>
    </div>
  );
}
