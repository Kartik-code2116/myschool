import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTeacherContext } from '../context/TeacherContext';
import { db, auth } from '../firebase';
import { collection, query, where, getDocs, doc, getDoc } from 'firebase/firestore';
import useLanguage from '../utils/useLanguage';
import './AppDashboard.css';

export default function AppDashboard() {
  const { activeClass, activeSchool, activeAcademicYear, activeSemester } = useTeacherContext();
  const { t } = useLanguage();
  const navigate = useNavigate();
  
  const [stats, setStats] = useState({ 
    students: 0, 
    subjects: 0, 
    attendance: 0,
    teacherName: '' 
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadStats() {
      if (!auth.currentUser) {
        setLoading(false);
        return;
      }

      let studentsCount = 0;
      let subjectsCount = 0;
      let teacherName = auth.currentUser.email?.split('@')[0] || 'Teacher';

      try {
        // Get teacher name
        const tRef = doc(db, 'teachers', auth.currentUser.uid);
        const tSnap = await getDoc(tRef);
        if (tSnap.exists() && tSnap.data().name) {
          teacherName = tSnap.data().name;
        }

        if (activeClass) {
          // Get students count
          const studentQ = query(collection(db, 'students'), where('classId', '==', activeClass.id));
          const sSnap = await getDocs(studentQ);
          studentsCount = sSnap.size;

          // Get subjects count directly from class document
          const classRef = doc(db, 'classes', activeClass.id);
          const classSnap = await getDoc(classRef);
          if (classSnap.exists() && classSnap.data().subjects) {
            subjectsCount = classSnap.data().subjects.length;
          }
        }

        setStats({
          students: studentsCount,
          subjects: subjectsCount,
          attendance: studentsCount > 0 ? 94 : 0, // Mock average attendance for UI demonstration
          teacherName: teacherName
        });
      } catch (err) {
        console.error("Failed to load stats", err);
      } finally {
        setLoading(false);
      }
    }
    loadStats();
  }, [activeClass]);

  return (
    <div className="app-dashboard animate-fade-in">
      <div className="dashboard-header">
        <div className="welcome-text">
          <h1>{t('Hello', 'नमस्कार')}, {stats.teacherName} 👋</h1>
          <p>{t('Here is an overview of your classroom.', 'येथे तुमच्या वर्गाचा आढावा आहे.')}</p>
        </div>
        
        {activeAcademicYear && activeSemester && (
          <div className="session-badge">
            📅 {activeAcademicYear.name} • {activeSemester.name}
          </div>
        )}
      </div>

      {!activeClass ? (
        <div className="no-class-banner">
          <h3>⚠️ {t('No Active Class Selected', 'कोणताही सक्रिय वर्ग निवडलेला नाही')}</h3>
          <p>{t('Please configure your classroom settings to unlock all features.', 'सर्व वैशिष्ट्ये अनलॉक करण्यासाठी कृपया तुमचे वर्ग सेटिंग्ज कॉन्फिगर करा.')}</p>
          <button className="btn btn-primary" onClick={() => navigate('/app/settings')}>
            {t('Go to Settings', 'सेटिंग्ज मध्ये जा')}
          </button>
        </div>
      ) : (
        <>
          <div className="quick-stats-grid">
            <div className="stat-card">
              <div className="stat-icon students">👥</div>
              <div className="stat-details">
                <h3>{loading ? '-' : stats.students}</h3>
                <p>{t('Total Students', 'एकूण विद्यार्थी')}</p>
              </div>
            </div>
            
            <div className="stat-card">
              <div className="stat-icon subjects">📚</div>
              <div className="stat-details">
                <h3>{loading ? '-' : stats.subjects}</h3>
                <p>{t('Subjects', 'विषय')}</p>
              </div>
            </div>
            
            <div className="stat-card">
              <div className="stat-icon attendance">📊</div>
              <div className="stat-details">
                <h3>{loading ? '-' : `${stats.attendance}%`}</h3>
                <p>{t('Avg. Attendance', 'सरासरी उपस्थिती')}</p>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon reports">📄</div>
              <div className="stat-details">
                <h3>{loading ? '-' : stats.students}</h3>
                <p>{t('Ready Reports', 'तयार निकाल')}</p>
              </div>
            </div>
          </div>

          <div className="dashboard-sections">
            <div className="left-section">
              <h3 className="section-title">
                🚀 {t('Quick Modules', 'जलद मॉड्यूल्स')}
              </h3>
              <div className="modules-grid">
                <div className="module-card" onClick={() => navigate('/app/marks')}>
                  <div className="module-icon">📝</div>
                  <h4>{t('Marks Entry', 'गुण नोंद')}</h4>
                  <p>{t('Enter formative and summative scores.', 'आकारिक आणि संकलित गुणांची नोंद करा.')}</p>
                </div>
                
                <div className="module-card" onClick={() => navigate('/app/attendance')}>
                  <div className="module-icon">📋</div>
                  <h4>{t('Attendance', 'हजेरी')}</h4>
                  <p>{t('Manage monthly working days and present days.', 'मासिक कामकाजाचे आणि उपस्थितीचे दिवस व्यवस्थापित करा.')}</p>
                </div>
                
                <div className="module-card" onClick={() => navigate('/app/students')}>
                  <div className="module-icon">👩‍🎓</div>
                  <h4>{t('Students', 'विद्यार्थी')}</h4>
                  <p>{t('Add or edit comprehensive student profiles.', 'विद्यार्थ्यांची माहिती जोडा किंवा बदला.')}</p>
                </div>
                
                <div className="module-card" onClick={() => navigate('/app/reports')}>
                  <div className="module-icon">🖨️</div>
                  <h4>{t('Report Cards', 'निकालपत्रक')}</h4>
                  <p>{t('Generate and print beautiful PDF reports.', 'सुंदर PDF निकालपत्रक तयार करा आणि प्रिंट करा.')}</p>
                </div>
              </div>
            </div>

            <div className="right-section">
              <h3 className="section-title">
                🏫 {t('Classroom Info', 'वर्गाची माहिती')}
              </h3>
              <div className="info-card">
                <div className="info-list">
                  <div className="info-item">
                    <span className="info-label">{t('School Name', 'शाळेचे नाव')}</span>
                    <span className="info-value">{activeSchool?.name || '-'}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">{t('Class & Division', 'इयत्ता व तुकडी')}</span>
                    <span className="info-value">{activeClass.className} - {activeClass.division}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">{t('Class Teacher', 'वर्गशिक्षक')}</span>
                    <span className="info-value">{activeClass.teacherName || stats.teacherName}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">{t('Academic Year', 'शैक्षणिक वर्ष')}</span>
                    <span className="info-value">{activeAcademicYear?.name || '-'}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">{t('Semester', 'सत्र')}</span>
                    <span className="info-value">{activeSemester?.name || '-'}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
