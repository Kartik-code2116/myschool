import React, { useState, useEffect } from 'react';
import { useTeacherContext } from '../context/TeacherContext';
import { db, auth } from '../firebase';
import { collection, query, where, getDocs, doc, setDoc, getDoc, onSnapshot } from 'firebase/firestore';
import useLanguage from '../utils/useLanguage';
import './AppRemarks.css';

// Match Android's exact keys for special remarks so they sync perfectly
const SPECIAL_SUBJECTS = [
  { id: 'Vishesh pragati', nameEn: 'Progress', nameMr: 'विशेष प्रगती', icon: '⭐' },
  { id: 'Aavad, chanda, etc', nameEn: 'Interests / Hobbies', nameMr: 'आवड / छंद', icon: '🎨' },
  { id: 'Sudharna Aavashyaka', nameEn: 'Needs Improvement', nameMr: 'सुधारणा आवश्यक', icon: '📈' },
  { id: 'Vyaktimatva gun vishgesh', nameEn: 'Personality Traits', nameMr: 'व्यक्तिमत्व गुणविशेष', icon: '👤' }
];

const sanitizeKey = (key) => {
  if (!key) return "unknown";
  return key.replace(/[\.#\$\[\]\/\\~\*]/g, "_");
};

export default function AppRemarks() {
  const { activeClass, activeAcademicYear, activeSemester, teacherProfile } = useTeacherContext();
  const { t } = useLanguage();
  
  const [students, setStudents] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);
  
  const [marksRecord, setMarksRecord] = useState(null);
  const [detailedMarks, setDetailedMarks] = useState({});

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Bank Modal State
  const [bankModalOpen, setBankModalOpen] = useState(false);
  const [bankLoading, setBankLoading] = useState(false);
  const [bankOptions, setBankOptions] = useState([]);
  const [activeBankSubject, setActiveBankSubject] = useState(null);

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

  const handleRemarkChange = (subjectName, value) => {
      const sanitizedKey = sanitizeKey(subjectName);
      
      setDetailedMarks(prev => {
          const current = prev[sanitizedKey] || { 
            nirikhshan: 0, tondiKam: 0, pratyakshik: 0, upkram: 0, prakalp: 0, chachani: 0, swadhyay: 0, itar: 0, akarikTotal: 0,
            tondi: 0, pratyakshikB: 0, lekhi: 0, sanklit: 0, grandTotal: 0,
            remark: ''
          };
          
          const updated = { ...current, remark: value };
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
          alert(t("Remarks saved successfully!", "नोंदी यशस्वीरित्या सेव्ह झाल्या!"));
      } catch(err) {
          console.error(err);
          alert(t("Error saving remarks.", "नोंदी सेव्ह करताना त्रुटी."));
      } finally {
          setSaving(false);
      }
  };

  const getStandardSubjectName = (name) => {
      if (!name) return "general";
      const s = name.trim().toLowerCase();
      if (s.includes("marathi") || s.includes("मराठी") || s.includes("भाषा")) return "Marathi";
      if (s.includes("english") || s.includes("इंग्रजी")) return "English";
      if (s.includes("hindi") || s.includes("हिंदी")) return "Hindi";
      if (s.includes("mathematics") || s.includes("maths") || s.includes("math") || s.includes("गणित")) return "Mathematics";
      if (s.includes("science") || s.includes("विज्ञान")) return "Science";
      if (s.includes("history") || s.includes("इतिहास") || s.includes("civics") || s.includes("नागरिकशास्त्र")) return "History and Civics";
      if (s.includes("geography") || s.includes("भूगोल")) return "Geography";
      if (s.includes("physical education") || s.includes("शारीरिक") || s.includes("p.e") || s.includes("pe")) return "Health & Physical Education";
      if (s.includes("work experience") || s.includes("कार्यानुभव")) return "Work Experience";
      if (s.includes("art") || s.includes("कला") || s.includes("drawing")) return "Art";
      if (s.includes("play") || s.includes("learn") || s.includes("खेळू")) return "Play, Do, Learn";
      if (s.includes("environmental studies part 1") || s.includes("परिसर अभ्यास १") || s.includes("परिसर अभ्यास भाग १")) return "Environmental Studies Part 1";
      if (s.includes("environmental studies part 2") || s.includes("परिसर अभ्यास २") || s.includes("परिसर अभ्यास भाग २")) return "Environmental Studies Part 2";
      return name.trim();
  };

  const openBankModal = async (subjectName) => {
    setActiveBankSubject(subjectName);
    setBankModalOpen(true);
    setBankLoading(true);
    setBankOptions([]);

    try {
      const semNum = activeSemester?.number || 1;
      const clsName = activeClass?.className || '5';
      
      const safeRegex = /[^a-zA-Z0-9\u0900-\u097F]/g;
      const normalized = getStandardSubjectName(subjectName);
      const normalizedSafe = normalized.replace(safeRegex, "_");
      const rawSafe = subjectName.replace(safeRegex, "_");
      
      let subs = [normalizedSafe, rawSafe, subjectName, subjectName.trim()];
      
      const lower = subjectName.toLowerCase();
      if (lower.includes("play") || lower.includes("learn") || lower.includes("खेळू")) {
          subs.push("Play_Do_Learn", "खेळू_करू_शिकू");
      } else if (lower.includes("art") || lower.includes("कला") || lower.includes("drawing")) {
          subs.push("Art_Education", "कला");
      } else if (lower.includes("physical education") || lower.includes("शारीरिक") || lower.includes("p.e") || lower.includes("pe")) {
          subs.push("Health_Physical_Education", "शारीरिक_शिक्षण");
      } else if (lower.includes("work experience") || lower.includes("कार्यानुभव")) {
          subs.push("Work_Experience", "कार्यानुभव");
      }
      
      subs = [...new Set(subs)];

      const schools = ['default'];
      const cleanClass = clsName.replace(/[^0-9]/g, "");
      const classes = [clsName];
      if (cleanClass && cleanClass !== clsName) classes.push(cleanClass);
      
      const sems = [`_sem_${semNum}_`, "_"];

      const docIds = new Set();
      
      for (let sch of schools) {
          for (let sub of subs) {
              docIds.add(`${sch}_${sub}`);
              for (let cls of classes) {
                  for (let sem of sems) {
                      docIds.add(`${sch}_${cls}${sem}${sub}`);
                  }
              }
          }
      }

      const mergedOptions = new Set();
      const docPromises = Array.from(docIds).map(id => getDoc(doc(db, 'remarkBanks', id)).catch(() => null));
      const snaps = await Promise.all(docPromises);
      
      snaps.forEach(snap => {
          if (snap && snap.exists() && snap.data().options) {
              snap.data().options.forEach(opt => mergedOptions.add(opt));
          }
      });

      const cleanOptions = Array.from(mergedOptions).map(opt => {
          if (opt.startsWith('[') && opt.includes(']')) {
            return opt.substring(opt.indexOf(']') + 1).trim();
          }
          return opt;
      });

      setBankOptions(cleanOptions);
    } catch (err) {
      console.error(err);
    } finally {
      setBankLoading(false);
    }
  };

  const selectBankRemark = (remarkText) => {
    if (!activeBankSubject) return;
    
    // Append to existing text
    const sanitizedKey = sanitizeKey(activeBankSubject);
    const existing = detailedMarks[sanitizedKey]?.remark || '';
    const newText = existing ? `${existing} ${remarkText}` : remarkText;
    
    handleRemarkChange(activeBankSubject, newText);
    setBankModalOpen(false);
  };

  if (!activeClass) {
      return <div className="app-remarks"><div className="warning-banner">{t("Please select an Active Class from the Dashboard.", "कृपया डॅशबोर्डवरून सक्रिय वर्ग निवडा.")}</div></div>;
  }

  const academicSubjects = activeClass.subjects || [];

  return (
    <div className="app-remarks animate-fade-in">
      <div className="remarks-header">
        <h2>{t("Descriptive Remarks", "वर्णनात्मक नोंदी")}</h2>
        <p>{t("Enter detailed progress reports and personal traits for the report card.", "निकालपत्रावर छापण्यासाठी विद्यार्थ्यांच्या प्रगती आणि छंद विषयी नोंदी करा.")}</p>
      </div>

      <div className="remarks-layout">
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
                                alert(t("Free limit reached. Upgrade to premium to enter remarks for more students.", "अधिक विद्यार्थ्यांचे शेरे नोंदवण्यासाठी कृपया प्रीमियम सबस्क्रिप्शन घ्या."));
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

          <div className="remarks-editor">
              {!selectedStudent ? (
                  <div className="empty-state">
                    <h3>👆</h3>
                    <p>{t("Select a student to enter remarks.", "नोंदी करण्यासाठी यादीतून विद्यार्थी निवडा.")}</p>
                  </div>
              ) : (
                  <>
                      <div className="editor-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <h3 style={{ margin: 0, color: 'var(--primary-color)' }}>{selectedStudent.rollNo}. {selectedStudent.name}</h3>
                          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
                              {saving ? t('Saving...', 'सेव्ह होत आहे...') : t('Save Remarks', 'नोंदी सेव्ह करा')}
                          </button>
                      </div>

                      <div className="section-divider">
                        🌟 {t("Overall Assessment", "सर्वसाधारण मूल्यमापन")}
                      </div>
                      
                      <div className="remarks-grid">
                          {SPECIAL_SUBJECTS.map(spec => {
                              const sanitizedKey = sanitizeKey(spec.id);
                              const data = detailedMarks[sanitizedKey] || {};
                              return (
                                  <div key={spec.id} className="remark-card special-card">
                                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                                        <h4>{spec.icon} {t(spec.nameEn, spec.nameMr)}</h4>
                                        <button 
                                          className="btn btn-secondary" 
                                          style={{ padding: '4px 8px', fontSize: '12px', minHeight: 'auto' }}
                                          onClick={() => openBankModal(spec.id)}
                                        >
                                          {t('Bank', 'नोंदी यादी')}
                                        </button>
                                      </div>
                                      <textarea 
                                        className="remark-textarea"
                                        placeholder={t(`Enter remarks for ${spec.nameEn}...`, `${spec.nameMr} ची नोंद करा...`)}
                                        value={data.remark || ''}
                                        onChange={e => handleRemarkChange(spec.id, e.target.value)}
                                      />
                                  </div>
                              )
                          })}
                      </div>

                      {academicSubjects.length > 0 && (
                          <>
                            <div className="section-divider">
                              📚 {t("Subject Specific Remarks", "विषयानुसार नोंदी")}
                            </div>
                            <div className="remarks-grid">
                                {academicSubjects.map(sub => {
                                    const sanitizedKey = sanitizeKey(sub.name);
                                    const data = detailedMarks[sanitizedKey] || {};
                                    return (
                                        <div key={sub.name} className="remark-card">
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                                              <h4>{sub.name}</h4>
                                              <button 
                                                className="btn btn-secondary" 
                                                style={{ padding: '4px 8px', fontSize: '12px', minHeight: 'auto' }}
                                                onClick={() => openBankModal(sub.name)}
                                              >
                                                {t('Bank', 'नोंदी यादी')}
                                              </button>
                                            </div>
                                            <textarea 
                                              className="remark-textarea"
                                              placeholder={t(`Enter remarks for ${sub.name}...`, `${sub.name} विषयाची नोंद करा...`)}
                                              value={data.remark || ''}
                                              onChange={e => handleRemarkChange(sub.name, e.target.value)}
                                            />
                                        </div>
                                    )
                                })}
                            </div>
                          </>
                      )}
                  </>
              )}
          </div>
      </div>

      {/* Remark Bank Modal */}
      {bankModalOpen && (
        <div className="modal-overlay" onClick={() => setBankModalOpen(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: '600px', width: '90%' }}>
            <h3 style={{ marginBottom: '15px' }}>{t("Select a Remark", "नोंद निवडा")}</h3>
            
            {bankLoading ? (
              <p>{t("Loading from bank...", "नोंदी लोड होत आहेत...")}</p>
            ) : bankOptions.length === 0 ? (
              <div className="empty-state">
                <p>{t("No default remarks configured for this subject.", "या विषयासाठी कोणत्याही नोंदी सेव्ह केलेल्या नाहीत.")}</p>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  {t("Use the Admin Console to add remarks.", "नोंदी जोडण्यासाठी ऍडमिन कन्सोल वापरा.")}
                </p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '400px', overflowY: 'auto' }}>
                {bankOptions.map((opt, i) => (
                  <button 
                    key={i} 
                    className="btn btn-secondary"
                    style={{ textAlign: 'left', justifyContent: 'flex-start', padding: '12px', fontWeight: '500' }}
                    onClick={() => selectBankRemark(opt)}
                  >
                    {opt}
                  </button>
                ))}
              </div>
            )}
            
            <div className="modal-actions" style={{ marginTop: '20px', justifyContent: 'flex-end', display: 'flex' }}>
              <button className="btn btn-secondary" onClick={() => setBankModalOpen(false)}>
                {t("Close", "बंद करा")}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
