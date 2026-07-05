import React, { useState, useEffect } from 'react';
import { useTeacherContext } from '../context/TeacherContext';
import { db, auth } from '../firebase';
import { collection, query, where, getDocs } from 'firebase/firestore';
import useLanguage from '../utils/useLanguage';
import './AppSettings.css';

export default function AppSettings() {
  const { 
    activeClass, setActiveClass,
    activeAcademicYear, setActiveAcademicYear,
    activeSemester, setActiveSemester,
    activeSchool, academicYearsList, semestersList, loadingContext
  } = useTeacherContext();
  
  const { t } = useLanguage();

  const [classes, setClasses] = useState([]);
  const [loading, setLoading] = useState(true);

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [newClassName, setNewClassName] = useState('');
  const [newDivision, setNewDivision] = useState('');
  const [newAcademicYearId, setNewAcademicYearId] = useState('');
  const [examName, setExamName] = useState('');
  const [teacherName, setTeacherName] = useState('');
  const [assistantTeacherName, setAssistantTeacherName] = useState('');
  const [teacherPhone, setTeacherPhone] = useState('');
  const [teacherEmail, setTeacherEmail] = useState('');
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    async function loadTeacherClasses() {
      if (!auth.currentUser || loadingContext) {
        if (!loadingContext) setLoading(false);
        return;
      }
      try {
        let q;
        if (activeAcademicYear) {
          q = query(collection(db, 'classes'), 
              where('teacherId', '==', auth.currentUser.uid),
              where('yearId', '==', activeAcademicYear.id)
          );
        } else {
          q = query(collection(db, 'classes'), where('teacherId', '==', auth.currentUser.uid));
        }
        
        const snap = await getDocs(q);
        const list = snap.docs.map(doc => ({
          id: doc.id,
          name: `${t('Class', 'इयत्ता')} ${doc.data().className || ''} ${doc.data().division || ''}`,
          ...doc.data()
        }));
        setClasses(list);
        
        if (list.length > 0) {
          const isValidClass = activeClass && list.find(c => c.id === activeClass.id);
          if (!isValidClass) {
            setActiveClass(list[0]);
          }
        } else if (list.length === 0 && activeClass) {
          setActiveClass(null);
        }
      } catch (err) {
        console.error("Error loading classes:", err);
      } finally {
        setLoading(false);
      }
    }
    loadTeacherClasses();
  }, [activeClass, setActiveClass, activeAcademicYear, loadingContext, t]);

  useEffect(() => {
    if (!loadingContext) {
      let currentYear = activeAcademicYear;
      if (!currentYear && academicYearsList.length > 0) {
        currentYear = academicYearsList[0];
        setActiveAcademicYear(currentYear);
      }
      
      if (currentYear) {
        const yearSems = semestersList.filter(s => s.yearId === currentYear.id);
        const isValidSem = activeSemester && yearSems.find(s => s.id === activeSemester.id);
        
        if (!isValidSem && yearSems.length > 0) {
          setActiveSemester(yearSems[0]);
        } else if (yearSems.length === 0 && activeSemester) {
          setActiveSemester(null);
        }
      }
      
      if (isCreateModalOpen && !newAcademicYearId && academicYearsList.length > 0) {
        setNewAcademicYearId(academicYearsList[0].id);
      }
    }
  }, [loadingContext, activeAcademicYear, academicYearsList, activeSemester, semestersList, isCreateModalOpen]);

  const handleClassChange = (classId) => {
    const selected = classes.find(c => c.id === classId);
    if (selected) {
      setActiveClass(selected);
    }
  };

  const handleYearChange = (yearId) => {
    const selected = academicYearsList.find(y => y.id === yearId);
    if (selected) {
      setActiveAcademicYear(selected);
      setActiveClass(null); 
      
      const sems = semestersList.filter(s => s.yearId === yearId);
      if (sems.length > 0) setActiveSemester(sems[0]);
      else setActiveSemester(null);
    }
  };

  const handleSemesterChange = (semId) => {
    const selected = semestersList.find(s => s.id === semId);
    if (selected) {
      setActiveSemester(selected);
    }
  };

  const handleCreateClass = async (e) => {
    e.preventDefault();
    if (!newClassName.trim() || !newDivision.trim() || !newAcademicYearId) return;
    
    setCreating(true);
    try {
      import('firebase/firestore').then(async ({ addDoc, serverTimestamp }) => {
        const selectedYear = academicYearsList.find(y => y.id === newAcademicYearId);
        const newClassData = {
          teacherId: auth.currentUser.uid,
          schoolId: activeSchool?.id || '',
          yearId: selectedYear?.id || '',
          academicYearLabel: selectedYear?.name || '',
          className: newClassName.trim(),
          division: newDivision.trim(),
          examName: examName.trim(),
          teacherName: teacherName.trim(),
          assistantTeacherName: assistantTeacherName.trim(),
          teacherPhone: teacherPhone.trim(),
          teacherEmail: teacherEmail.trim(),
          studentCount: 0,
          subjects: [],
          monthlyWorkingDays: {},
          createdAt: serverTimestamp()
        };
        const docRef = await addDoc(collection(db, 'classes'), newClassData);
        
        const createdClass = {
          id: docRef.id,
          name: `${t('Class', 'इयत्ता')} ${newClassData.className} ${newClassData.division}`,
          ...newClassData
        };
        
        setClasses([...classes, createdClass]);
        setActiveClass(createdClass);
        setIsCreateModalOpen(false);
        setNewClassName('');
        setNewDivision('');
        setExamName('');
        setTeacherName('');
        setAssistantTeacherName('');
        setTeacherPhone('');
        setTeacherEmail('');
      });
    } catch (err) {
      console.error("Error creating class:", err);
      alert(t("Failed to create class.", "वर्ग तयार करण्यात त्रुटी."));
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="app-settings animate-fade-in">
      <div className="settings-header">
        <div className="page-kicker">{t("Configuration", "कॉन्फिगरेशन")}</div>
        <h2>{t('Teacher Preferences & Settings', 'शिक्षक प्राधान्ये आणि सेटिंग्ज')}</h2>
        <p>{t('Configure your active classroom session. These selections scope your Students, Attendance, and Marks pages.', 'तुमचे सक्रिय वर्ग सत्र कॉन्फिगर करा. या निवडीवर विद्यार्थी, हजेरी आणि गुण अवलंबून आहेत.')}</p>
      </div>

      <div className="settings-grid">
        <div className="settings-card card-panel">
          <div className="card-header-with-action">
            <h3>{t('Active Session Configuration', 'सक्रिय सत्र कॉन्फिगरेशन')}</h3>
            <button className="btn-primary-small" onClick={() => setIsCreateModalOpen(true)}>
              + {t('New Class', 'नवीन वर्ग')}
            </button>
          </div>

          <div className="form-group">
            <label>{t('Academic Year', 'शैक्षणिक वर्ष')}</label>
            {loadingContext ? <p>{t('Loading years...', 'वर्ष लोड होत आहेत...')}</p> : (
              <select 
                value={activeAcademicYear?.id || ''} 
                onChange={e => handleYearChange(e.target.value)}
              >
                {academicYearsList.length === 0 && <option value="">{t('No years found (Login to Android App)', 'वर्षे आढळली नाहीत (अँड्रॉइड ॲपमध्ये लॉगिन करा)')}</option>}
                {academicYearsList.map(y => (
                  <option key={y.id} value={y.id}>{y.name}</option>
                ))}
              </select>
            )}
          </div>
          
          <div className="form-group">
            <label>{t('Selected Classroom', 'निवडलेला वर्ग')}</label>
            {loading ? <p>{t('Loading classes...', 'वर्ग लोड होत आहेत...')}</p> : (
              classes.length === 0 ? (
                <div className="no-classes-warning">
                  ⚠️ {t('No classes found for this year. Please create one.', 'या वर्षासाठी कोणतेही वर्ग आढळले नाहीत. कृपया नवीन वर्ग तयार करा.')}
                </div>
              ) : (
                <select 
                  value={activeClass?.id || ''} 
                  onChange={e => handleClassChange(e.target.value)}
                >
                  <option value="" disabled>{t('Select a Class', 'वर्ग निवडा')}</option>
                  {classes.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              )
            )}
          </div>

          <div className="form-group">
            <label>{t('Active Semester', 'सक्रिय सत्र')}</label>
            {loadingContext ? <p>{t('Loading semesters...', 'सत्र लोड होत आहेत...')}</p> : (
              <select 
                value={activeSemester?.id || ''} 
                onChange={e => handleSemesterChange(e.target.value)}
              >
                {semestersList.filter(s => s.yearId === activeAcademicYear?.id).length === 0 && <option value="">{t('No semesters found', 'सत्र आढळले नाही')}</option>}
                {semestersList
                  .filter(s => s.yearId === activeAcademicYear?.id)
                  .map(s => (
                    <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            )}
          </div>

          <div className="save-indicator">
            ✔️ {t('Selections automatically save and apply.', 'निवडी आपोआप सेव्ह आणि लागू होतात.')}
          </div>
        </div>

        <div className="settings-card card-panel help-card">
          <h3>{t('Web-Mobile Sync', 'वेब-मोबाईल सिंक')}</h3>
          <p>
            {t('The web portal synchronizes with your Android app in real-time using Firestore. Any student details, attendance days, or marks entered here will update the mobile app immediately without needing manually triggered sync runs.', 'वेब पोर्टल फायरस्टोअर वापरून तुमच्या अँड्रॉइड ॲपशी रिअल-टाइममध्ये सिंक करते. येथे प्रविष्ट केलेले कोणतेही विद्यार्थी तपशील, उपस्थितीचे दिवस किंवा गुण मोबाईल ॲपवर त्वरित अपडेट होतील.')}
          </p>
          <div className="sync-badges">
            <div className="sync-badge">
              <span>{t('Sync Connection', 'सिंक कनेक्शन')}</span>
              <strong>{t('Connected (Live)', 'जोडले आहे (लाईव्ह)')}</strong>
            </div>
            <div className="sync-badge">
              <span>{t('School', 'शाळा')}</span>
              <strong>{activeSchool?.name || t('Not Linked', 'लिंक केलेले नाही')}</strong>
            </div>
          </div>
        </div>
      </div>

      {isCreateModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>{t('Create New Class', 'नवीन वर्ग तयार करा')}</h3>
            <form onSubmit={handleCreateClass} className="auth-form" style={{ marginTop: '20px' }}>
              <div className="input-group">
                <label>{t('Class Name (e.g., 10, V, 12)', 'इयत्ता (उदा. 10, V, 12)')}</label>
                <input 
                  type="text" 
                  value={newClassName} 
                  onChange={e => setNewClassName(e.target.value)} 
                  required 
                  placeholder={t("Enter class name", "वर्गाचे नाव टाका")}
                />
              </div>
              <div className="input-group">
                <label>{t('Division (e.g., A, B, Science)', 'तुकडी (उदा. A, B)')}</label>
                <input 
                  type="text" 
                  value={newDivision} 
                  onChange={e => setNewDivision(e.target.value)} 
                  required 
                  placeholder={t("Enter division", "तुकडी टाका")}
                />
              </div>
              <div className="input-group">
                <label>{t('Academic Year', 'शैक्षणिक वर्ष')}</label>
                <select value={newAcademicYearId} onChange={e => setNewAcademicYearId(e.target.value)}>
                  {academicYearsList.map(y => (
                    <option key={y.id} value={y.id}>{y.name}</option>
                  ))}
                </select>
              </div>
              <div className="input-group">
                <label>{t('Exam Name (Optional)', 'परीक्षेचे नाव (ऐच्छिक)')}</label>
                <input 
                  type="text" 
                  value={examName} 
                  onChange={e => setExamName(e.target.value)} 
                  placeholder={t("e.g. Annual Exam", "उदा. वार्षिक परीक्षा")}
                />
              </div>
              <div className="input-group">
                <label>{t('Teacher Name (Optional)', 'शिक्षकाचे नाव (ऐच्छिक)')}</label>
                <input 
                  type="text" 
                  value={teacherName} 
                  onChange={e => setTeacherName(e.target.value)} 
                  placeholder={t("Enter class teacher name", "वर्गशिक्षकाचे नाव टाका")}
                />
              </div>
              <div className="input-group">
                <label>{t('Assistant Teacher (Optional)', 'सहशिक्षक (ऐच्छिक)')}</label>
                <input 
                  type="text" 
                  value={assistantTeacherName} 
                  onChange={e => setAssistantTeacherName(e.target.value)} 
                  placeholder={t("Enter assistant teacher name", "सहशिक्षकाचे नाव टाका")}
                />
              </div>
              <div className="input-group">
                <label>{t('Teacher Phone (Optional)', 'शिक्षकाचा फोन (ऐच्छिक)')}</label>
                <input 
                  type="tel" 
                  value={teacherPhone} 
                  onChange={e => setTeacherPhone(e.target.value)} 
                  placeholder={t("Enter phone number", "फोन नंबर टाका")}
                />
              </div>
              <div className="input-group">
                <label>{t('Teacher Email (Optional)', 'शिक्षकाचा ईमेल (ऐच्छिक)')}</label>
                <input 
                  type="email" 
                  value={teacherEmail} 
                  onChange={e => setTeacherEmail(e.target.value)} 
                  placeholder={t("Enter email address", "ईमेल आयडी टाका")}
                />
              </div>
              <div className="modal-actions" style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn-secondary" onClick={() => setIsCreateModalOpen(false)}>{t('Cancel', 'रद्द करा')}</button>
                <button type="submit" className="btn-primary" disabled={creating}>
                  {creating ? t('Creating...', 'तयार होत आहे...') : t('Create Class', 'वर्ग तयार करा')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
