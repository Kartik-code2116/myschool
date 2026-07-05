import React, { useState, useEffect } from 'react';
import { useTeacherContext } from '../context/TeacherContext';
import { db, auth } from '../firebase';
import { collection, query, where, doc, updateDoc, onSnapshot, writeBatch } from 'firebase/firestore';
import useLanguage from '../utils/useLanguage';
import './AppAttendance.css';

const MONTHS_MR = ["जून", "जुलै", "ऑगस्ट", "सप्टें", "ऑक्टो", "नोव्हे", "डिसें", "जाने", "फेब्रु", "मार्च", "एप्रिल", "मे"];
const MONTHS_EN = ["June", "July", "August", "Sept", "Oct", "Nov", "Dec", "Jan", "Feb", "March", "April", "May"];

export default function AppAttendance() {
  const { activeClass, setActiveClass, teacherProfile } = useTeacherContext();
  const { t } = useLanguage();
  
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  
  const [selectedMonth, setSelectedMonth] = useState(MONTHS_MR[0]);
  const [workingDays, setWorkingDays] = useState(0);
  const [workingDaysSaving, setWorkingDaysSaving] = useState(false);

  // Local state for edits before saving
  const [editedAttendance, setEditedAttendance] = useState({});

  useEffect(() => {
    if (!activeClass || !auth.currentUser) {
      setLoading(false);
      return;
    }
    
    setLoading(true);
    const qStu = query(collection(db, 'students'), 
        where('classId', '==', activeClass.id),
        where('teacherId', '==', auth.currentUser.uid)
    );
    
    const unsubscribe = onSnapshot(qStu, (snap) => {
        const stuList = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
        stuList.sort((a,b) => parseInt(a.rollNo || 0) - parseInt(b.rollNo || 0));
        setStudents(stuList);
        
        // Populate editedAttendance with current db values so inputs show correctly
        const initialEdits = {};
        stuList.forEach(stu => {
            const val = stu.monthlyAttendance?.[selectedMonth];
            if (val && val.includes('/')) {
                initialEdits[stu.id] = val.split('/')[0];
            } else {
                initialEdits[stu.id] = "";
            }
        });
        setEditedAttendance(initialEdits);
        setLoading(false);
    }, (err) => {
        console.error("Error fetching students:", err);
        setLoading(false);
    });

    return () => unsubscribe();
  }, [activeClass, selectedMonth]);

  // Update working days local state when class or month changes
  useEffect(() => {
    if (activeClass) {
        const classDays = activeClass.monthlyWorkingDays?.[selectedMonth] || 0;
        setWorkingDays(classDays);
    }
  }, [activeClass, selectedMonth]);

  const handleWorkingDaysChange = (e) => {
      const val = parseInt(e.target.value) || 0;
      setWorkingDays(val);
  };

  const saveWorkingDays = async () => {
      if (!activeClass) return;
      setWorkingDaysSaving(true);
      try {
          const classRef = doc(db, 'classes', activeClass.id);
          const currentMap = activeClass.monthlyWorkingDays || {};
          const newMap = { ...currentMap, [selectedMonth]: workingDays };
          
          await updateDoc(classRef, { monthlyWorkingDays: newMap });
          
          // Update context
          setActiveClass({ ...activeClass, monthlyWorkingDays: newMap });
          // Note: we do not show alert here to keep it seamless, button just shows "Saved" briefly
          setTimeout(() => setWorkingDaysSaving(false), 1000);
      } catch(err) {
          console.error(err);
          alert(t("Error saving working days.", "कामकाजाचे दिवस सेव्ह करताना त्रुटी."));
          setWorkingDaysSaving(false);
      }
  };

  const handlePresentChange = (studentId, val) => {
      setEditedAttendance(prev => ({
          ...prev,
          [studentId]: val
      }));
  };

  const handleSaveAttendance = async () => {
    if (!activeClass || !auth.currentUser) return;
    setSaving(true);
    
    try {
        const batch = writeBatch(db);
        
        students.forEach(stu => {
            const present = editedAttendance[stu.id] || "0";
            const newAttendanceStr = `${present}/${workingDays}`;
            
            // Only update if it changed to avoid unnecessary writes
            const currentStr = stu.monthlyAttendance?.[selectedMonth];
            if (currentStr !== newAttendanceStr) {
                const stuRef = doc(db, 'students', stu.id);
                const currentMap = stu.monthlyAttendance || {};
                batch.update(stuRef, {
                    monthlyAttendance: { ...currentMap, [selectedMonth]: newAttendanceStr }
                });
            }
        });
        
        await batch.commit();
        alert(t("Attendance saved successfully!", "उपस्थिती यशस्वीरित्या सेव्ह झाली!"));
    } catch(err) {
        console.error(err);
        alert(t("Error saving attendance.", "उपस्थिती सेव्ह करताना त्रुटी."));
    } finally {
        setSaving(false);
    }
  };

  if (!activeClass) {
      return (
          <div className="app-attendance">
              <div className="warning-banner">{t("Please select an Active Class from the Dashboard.", "कृपया डॅशबोर्डवरून सक्रिय वर्ग निवडा.")}</div>
          </div>
      );
  }

  return (
    <div className="app-attendance animate-fade-in">
      <div className="attendance-header">
        <h2>{t("Attendance Tracking", "विद्यार्थी उपस्थिती")}</h2>
        <p>{t("Record monthly student attendance here. This syncs directly with the mobile app.", "महिन्यानुसार विद्यार्थ्यांची उपस्थिती येथे भरा. हे मोबाईल ॲपसोबत रिअल-टाइममध्ये सिंक होते.")}</p>
      </div>
      
      <div className="attendance-layout">
          {/* Sidebar Configuration */}
          <div className="attendance-sidebar">
              <h3>{t("Configuration", "महिना आणि दिवस")}</h3>
              
              <div className="form-group" style={{ marginTop: '15px' }}>
                  <label>{t("Select Month", "महिना निवडा")}</label>
                  <select 
                      value={selectedMonth} 
                      onChange={e => setSelectedMonth(e.target.value)}
                      className="styled-select"
                  >
                      {MONTHS_MR.map((m, i) => (
                          <option key={m} value={m}>{t(MONTHS_EN[i], m)}</option>
                      ))}
                  </select>
              </div>

              <div className="form-group">
                  <label>{t("Total Working Days", "एकूण कामकाजाचे दिवस")}</label>
                  <div style={{ display: 'flex', gap: '10px' }}>
                      <input 
                          type="number" 
                          className="styled-input"
                          value={workingDays} 
                          onChange={handleWorkingDaysChange} 
                          min="0"
                          max="31"
                      />
                      <button 
                          className="btn btn-secondary" 
                          onClick={saveWorkingDays}
                          disabled={workingDaysSaving || (activeClass.monthlyWorkingDays?.[selectedMonth] === workingDays)}
                          style={{ padding: '0 15px', whiteSpace: 'nowrap' }}
                      >
                          {workingDaysSaving ? t("Saved", "सेव्ह झाले") : t("Apply", "लागू करा")}
                      </button>
                  </div>
                  <small style={{ color: 'var(--text-secondary)', marginTop: '5px', display: 'block' }}>
                      {t("Apply working days before entering attendance.", "उपस्थिती भरण्यापूर्वी कामकाजाचे दिवस लागू करा.")}
                  </small>
              </div>
              
              <div className="sidebar-stats card-panel" style={{ marginTop: '20px', padding: '15px' }}>
                  <h4>{t("Month Summary", "महिन्याचा गोषवारा")}</h4>
                  <p><strong>{t("Class:", "वर्ग:")}</strong> {activeClass.className} {activeClass.division}</p>
                  <p><strong>{t("Students:", "एकूण विद्यार्थी:")}</strong> {students.length}</p>
                  <p><strong>{t("Working Days:", "कामकाजाचे दिवस:")}</strong> {workingDays}</p>
              </div>
          </div>

          {/* Main Grid */}
          <div className="attendance-main">
              <div className="main-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                  <h3>{t("Students List", "विद्यार्थ्यांची यादी")} - {t(MONTHS_EN[MONTHS_MR.indexOf(selectedMonth)], selectedMonth)}</h3>
                  <button className="btn btn-primary" onClick={handleSaveAttendance} disabled={saving || workingDays === 0}>
                      {saving ? t('Saving...', 'सेव्ह होत आहे...') : t('Save Attendance', 'उपस्थिती सेव्ह करा')}
                  </button>
              </div>

              {workingDays === 0 && (
                  <div className="warning-banner" style={{ marginBottom: '20px' }}>
                      {t("Please set Total Working Days to greater than 0.", "कृपया एकूण कामकाजाचे दिवस ० पेक्षा जास्त सेट करा.")}
                  </div>
              )}

              {loading ? (
                  <p>{t("Loading students...", "विद्यार्थी लोड होत आहेत...")}</p>
              ) : students.length === 0 ? (
                  <div className="empty-state">
                      <p>{t("No students found in this class.", "या वर्गात कोणतेही विद्यार्थी आढळले नाहीत.")}</p>
                  </div>
              ) : (
                  <div className="attendance-grid">
                      <div className="grid-header">
                          <div className="col-roll">{t("Roll", "हजेरी क्र.")}</div>
                          <div className="col-name">{t("Student Name", "विद्यार्थ्याचे नाव")}</div>
                          <div className="col-present">{t("Present Days", "उपस्थित दिवस")}</div>
                          <div className="col-total">{t("Total Days", "एकूण दिवस")}</div>
                      </div>
                      <div className="grid-body">
                          {students.map((stu, index) => {
                              const present = editedAttendance[stu.id] !== undefined ? editedAttendance[stu.id] : "";
                              
                              // Check validation
                              const presentNum = parseInt(present) || 0;
                              const isError = presentNum > workingDays;
                              const isLocked = teacherProfile?.subscriptionStatus !== 'active' && index >= 3;

                              return (
                                  <div key={stu.id} className="grid-row" style={isLocked ? { opacity: 0.5 } : {}} title={isLocked ? "Upgrade to premium to enter data" : ""}>
                                      <div className="col-roll">
                                          <div className="stu-avatar small">{stu.rollNo}</div>
                                      </div>
                                      <div className="col-name">{stu.name} {isLocked && '🔒'}</div>
                                      <div className="col-present">
                                          <input 
                                              type="number" 
                                              className={`styled-input text-center ${isError ? 'input-error' : ''}`}
                                              value={present}
                                              onChange={e => handlePresentChange(stu.id, e.target.value)}
                                              min="0"
                                              max={workingDays}
                                              disabled={workingDays === 0 || isLocked}
                                          />
                                      </div>
                                      <div className="col-total text-muted">/ {workingDays}</div>
                                  </div>
                              );
                          })}
                      </div>
                  </div>
              )}
          </div>
      </div>
    </div>
  );
}
