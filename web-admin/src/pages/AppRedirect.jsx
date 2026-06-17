import { useState } from 'react';
import { signOut } from 'firebase/auth';
import { useNavigate } from 'react-router-dom';
import { auth } from '../firebase';
import './AppRedirect.css';

const translations = {
  en: {
    logoText: "MS",
    title: "EduReport",
    signOut: "Sign Out",
    statusBanner: "ACCOUNT ACTIVE & SYNCHRONIZED",
    welcomeTitle: "Welcome, ",
    leadText: "Your classroom profile has been successfully registered. To start managing students and grading cards, please transition to the mobile application.",
    whyTitle: "Why is this service mobile-only?",
    whyDesc: "EduReport is designed to function seamlessly in school classrooms, including rural areas with poor internet connection. By hosting grading databases, formative/summative exams, and attendance grids on the mobile app, we utilize local Android SQLite databases to guarantee 0ms offline latency.",
    guideTitle: "Setup Guide",
    step1Title: "1. Download APK",
    step1Desc: "Click the button below to download the latest stable release of the Android package.",
    step2Title: "2. Sign In",
    step2Desc: "Open the app and log in with your registered email: ",
    step3Title: "3. Assign Class",
    step3Desc: "Set up your classroom standards, divisions, and semesters to begin student grading logs.",
    btnDownload: "📥 Download Android App (APK)",
    mockActiveRoom: "Active Room",
    mockClass: "Class 7 - Div A",
    mockYear: "Academic Year: 2026-27",
    mockStudents: "Students",
    mockAttendance: "Attendance",
    mockModulesTitle: "Classroom Modules",
    mockMod1: "Formative (Aakarik)",
    mockMod2: "Summative (Sankalit)",
    mockMod3: "Daily Attendance",
    mockMod4: "Print Report Cards",
    mockNavHome: "🏠 Home",
    mockNavStudents: "👥 Students",
    mockNavSettings: "⚙️ Settings",
    footerText: "© 2026 EduReport. Fully compliant with local CCE grading standards."
  },
  mr: {
    logoText: "एमएस",
    title: "वार्षिक मूल्यमापन",
    signOut: "साइन आउट करा",
    statusBanner: "खाते सक्रिय आणि समक्रमित (SYNCHRONIZED) आहे",
    welcomeTitle: "आपले स्वागत आहे, ",
    leadText: "तुमचे वर्ग प्रोफाईल यशस्वीरित्या नोंदणीकृत झाले आहे. विद्यार्थी आणि श्रेणी पत्रके व्यवस्थापित करण्यासाठी, कृपया मोबाईल ॲप्लिकेशन वापरा.",
    whyTitle: "ही सेवा केवळ मोबाईलवर का उपलब्ध आहे?",
    whyDesc: "वार्षिक मूल्यमापन हे वर्गात, विशेषत: खराब इंटरनेट सुविधा असलेल्या ग्रामीण भागातील शाळांमध्ये अखंडपणे काम करण्यासाठी तयार केले आहे. मोबाईल ॲपवर श्रेणी डेटाबेस, आकारिक/संकलित परीक्षा आणि उपस्थिती ग्रिड्स साठवून, आम्ही शून्य-विलंबता (0ms) ऑफलाईन कामाची हमी देण्यासाठी स्थानिक अँड्रॉइड SQLite डेटाबेसचा वापर करतो.",
    guideTitle: "मार्गदर्शिका (Setup Guide)",
    step1Title: "१. एपीके (APK) डाउनलोड करा",
    step1Desc: "अँड्रॉइड पॅकेजची नवीनतम स्थिर आवृत्ती डाउनलोड करण्यासाठी खालील बटणावर क्लिक करा.",
    step2Title: "२. लॉग इन करा",
    step2Desc: "ॲप उघडा आणि तुमच्या नोंदणीकृत ईमेलने लॉग इन करा: ",
    step3Title: "३. वर्ग वाटप करा",
    step3Desc: "विद्यार्थ्यांच्या गुण नोंदी सुरू करण्यासाठी तुमचे इयत्ता, तुकडी आणि सत्र सेट करा.",
    btnDownload: "📥 अँड्रॉइड ॲप (APK) डाउनलोड करा",
    mockActiveRoom: "सक्रिय वर्ग",
    mockClass: "इयत्ता ७ वी - तुकडी अ",
    mockYear: "शैक्षणिक वर्ष: २०२६-२७",
    mockStudents: "विद्यार्थी",
    mockAttendance: "उपस्थिती",
    mockModulesTitle: "वर्गखोली मॉड्यूल्स",
    mockMod1: "आकारिक (Aakarik)",
    mockMod2: "संकलित (Sankalit)",
    mockMod3: "दैनिक उपस्थिती",
    mockMod4: "प्रगतीपत्रक मुद्रण",
    mockNavHome: "🏠 होम",
    mockNavStudents: "👥 विद्यार्थी",
    mockNavSettings: "⚙️ सेटिंग्ज",
    footerText: "© २०२६ वार्षिक मूल्यमापन. स्थानिक सीसीई (CCE) श्रेणी मानकांशी सुसंगत."
  }
};

export default function AppRedirect({ user, lang }) {
  const navigate = useNavigate();

  const handleLogout = () => {
    signOut(auth).then(() => navigate('/'));
  };

  const t = translations[lang];

  return (
    <div className="redirect-container animate-fade-in">
      <header className="redirect-header">
        <div className="brand-logo-small">{t.logoText}</div>
        <h2>{t.title}</h2>
        <div className="header-actions">
          <button onClick={handleLogout} className="btn btn-danger logout-top-btn">
            {t.signOut}
          </button>
        </div>
      </header>

      <main className="redirect-content">
        {/* Left column: instructions */}
        <section className="redirect-instructions">
          <div className="status-banner">
            <span className="pulse-dot" />
            <span>{t.statusBanner}</span>
          </div>

          <h1>{t.welcomeTitle}{user?.email || 'Teacher'}!</h1>
          <p className="lead">{t.leadText}</p>

          <div className="redirect-explanation glass-panel">
            <h3>{t.whyTitle}</h3>
            <p>{t.whyDesc}</p>
          </div>

          <div className="redirect-steps">
            <h3>{t.guideTitle}</h3>
            <ol className="steps-list">
              <li>
                <strong>{t.step1Title}</strong>
                <p>{t.step1Desc}</p>
              </li>
              <li>
                <strong>{t.step2Title}</strong>
                <p>{t.step2Desc} <code>{user?.email}</code></p>
              </li>
              <li>
                <strong>{t.step3Title}</strong>
                <p>{t.step3Desc}</p>
              </li>
            </ol>
          </div>

          <a href="#" className="btn btn-primary download-apk-btn" onClick={(e) => { e.preventDefault(); alert('Downloading app APK bundle...'); }}>
            {t.btnDownload}
          </a>
        </section>

        {/* Right column: visual mock phone */}
        <section className="mobile-preview-container">
          <div className="phone-wrapper">
            <div className="phone-notch" />
            <div className="phone-screen">
              {/* App Status Bar */}
              <div className="app-status-bar">
                <span>12:30 PM</span>
                <div className="status-icons">📶 🔋 98%</div>
              </div>

              {/* App Header */}
              <div className="app-header-mock">
                <span>Welcome back</span>
                <h3>Kartik Thorat</h3>
              </div>

              {/* Active Class Card */}
              <div className="mock-card mock-class-card">
                <span className="card-badge">{t.mockActiveRoom}</span>
                <h4>{t.mockClass}</h4>
                <p>{t.mockYear}</p>
                <div className="class-stats">
                  <div>
                    <strong>33</strong>
                    <span>{t.mockStudents}</span>
                  </div>
                  <div>
                    <strong>98.2%</strong>
                    <span>{t.mockAttendance}</span>
                  </div>
                </div>
              </div>

              {/* Features List */}
              <div className="mock-section-title">{t.mockModulesTitle}</div>
              <div className="mock-modules-grid">
                <div className="mock-module-item">
                  <span className="module-icon">📝</span>
                  <span>{t.mockMod1}</span>
                </div>
                <div className="mock-module-item">
                  <span className="module-icon">🏆</span>
                  <span>{t.mockMod2}</span>
                </div>
                <div className="mock-module-item">
                  <span className="module-icon">📋</span>
                  <span>{t.mockMod3}</span>
                </div>
                <div className="mock-module-item">
                  <span className="module-icon">📄</span>
                  <span>{t.mockMod4}</span>
                </div>
              </div>

              {/* Bottom Nav Bar */}
              <div className="mock-bottom-nav">
                <span className="active-nav">{t.mockNavHome}</span>
                <span>{t.mockNavStudents}</span>
                <span>{t.mockNavSettings}</span>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="redirect-footer">
        <p>{t.footerText}</p>
      </footer>
    </div>
  );
}
