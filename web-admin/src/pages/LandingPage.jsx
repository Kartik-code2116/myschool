import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { signInWithEmailAndPassword, createUserWithEmailAndPassword } from 'firebase/auth';
import { doc, setDoc } from 'firebase/firestore';
import { auth, db, checkIsAdmin } from '../firebase';
import './LandingPage.css';

const translations = {
  en: {
    title: "MySchool Pro",
    subtitle: "Next-Gen Classroom Administration",
    heroBadge: "MOBILE-FIRST WORKFLOW",
    heroTitle: "Zero-Latency CCE Grading & Progress Reporting",
    heroDesc: "MySchool Pro enables teachers to record formative & summative scores, compile grades, track demographics, and generate print-ready progress cards instantly in the classroom.",
    feat1Title: "Demographics & Distribution",
    feat1Desc: "Track real-time distribution summaries by cast categories (SC, ST, OBC, Gen) and gender ratios.",
    feat2Title: "Multi-Semester Classrooms",
    feat2Desc: "Assign class teachers and assistant teachers per semester, ensuring structured grading histories.",
    feat3Title: "Official PDF Reports",
    feat3Desc: "Generate clean, government-standard progress templates locally for offline distribution.",
    tabLogin: "Log In",
    tabSignup: "Sign Up",
    loginHeader: "Welcome back",
    loginSub: "Log in to access your teacher profile and check sync status",
    labelEmail: "Email",
    labelPassword: "Password",
    btnSignIn: "Sign In",
    btnSigningIn: "Signing in...",
    signupHeader: "Create Teacher Account",
    signupSub: "Register to manage classrooms on the mobile application",
    labelName: "Full Name",
    labelPhone: "Phone Number",
    labelConfirmPass: "Confirm Password",
    btnRegister: "Register Profile",
    btnRegistering: "Registering...",
    footerCopy: "© 2026 MySchool Pro Classroom Suite. All rights reserved.",
    footerVersion: "Version 2.4.0 (Stable)",
    footerOperator: "🔒 Operator Console",
    
    // Interactive How it works Section
    howItWorksTitle: "How MySchool Pro Simplifies Your Day",
    howItWorksSubtitle: "Four simple steps to digitize classroom admin, designed for speed and reliability.",
    step1Title: "1. Class Setup",
    step1Desc: "Select standard and division. Link semesters and configure subjects with maximum mark weightages.",
    step2Title: "2. Student Roll",
    step2Desc: "Add student info, birthdates, roll numbers, and categories. Dynamic stats calculate counts instantly.",
    step3Title: "3. Grade Entry",
    step3Desc: "Input formative and summative marks. The app computes averages, percentiles, and comments instantly.",
    step4Title: "4. PDF Card Print",
    step4Desc: "Generate government-format school report cards. Save or share PDFs instantly without internet.",
    
    // Demo video section
    videoTitle: "See MySchool Pro in Action",
    videoSubtitle: "Watch our 2-minute walkthrough to see how teachers manage grades offline in the classroom.",
    videoPlaceholder: "App Demo Video Preview",
    videoTag1: "Offline Syncing",
    videoTag2: "CCE Grading",
    videoTag3: "Instant Reports",

    // Mockup preview texts
    mockClassHeader: "Class Setup Panel",
    mockClassLabel: "Active Division:",
    mockSubjectsLabel: "Subject Max Weights",
    mockStudentHeader: "Classroom Enrollees",
    mockCategoryBadge: "Category",
    mockMarksHeader: "Formative Grades Entry",
    mockGradingStatus: "Calculating averages...",
    mockReportHeader: "CCE Progress Card Print",
    mockReportTitle: "Maharashtra State Board Format"
  },
  mr: {
    title: "मायस्कूल प्रो (MySchool Pro)",
    subtitle: "पुढील पिढीचे वर्ग प्रशासन",
    heroBadge: "मोबाईल-प्रथम कार्यप्रवाह",
    heroTitle: "शून्य-विलंबता (0ms) मूल्यमापन आणि प्रगती अहवाल",
    heroDesc: "मायस्कूल प्रो शिक्षकांना वर्गात बसूनच आकारिक आणि संकलित गुण नोंदवण्यास, श्रेणी तयार करण्यास, लोकसंख्याशास्त्र (वर्गवारी) तपासण्यास आणि मुद्रणासाठी तयार प्रगतीपत्रके त्वरित जनरेट करण्यास सक्षम करते.",
    feat1Title: "वर्गवारी आणि वितरण",
    feat1Desc: "जात प्रवर्ग (SC, ST, OBC, Gen) आणि लिंग गुणोत्तरानुसार रिअल-टाइम सारांश तपासा.",
    feat2Title: "बहु-सत्र (Multi-Semester) वर्ग",
    feat2Desc: "सत्रानुसार वर्गशिक्षक आणि सहाय्यक शिक्षक नियुक्त करा, जेणेकरून श्रेणी इतिहास व्यवस्थित राहील.",
    feat3Title: "अधिकृत पीडीएफ अहवाल",
    feat3Desc: "ऑफलाईन वितरणासाठी शासकीय-मानक प्रगतीपत्रक टेम्पलेट्स स्थानिक पातळीवर तयार करा.",
    tabLogin: "लॉग इन",
    tabSignup: "साइन अप",
    loginHeader: "पुन्हा आपले स्वागत आहे",
    loginSub: "आपल्या शिक्षक प्रोफाईलमध्ये प्रवेश करण्यासाठी आणि सिंक स्थिती तपासण्यासाठी लॉग इन करा",
    labelEmail: "ईमेल",
    labelPassword: "पासवर्ड",
    btnSignIn: "लॉग इन करा",
    btnSigningIn: "लॉग इन होत आहे...",
    signupHeader: "नवीन शिक्षक खाते तयार करा",
    signupSub: "मोबाईल ॲप्लिकेशनवर वर्ग व्यवस्थापित करण्यासाठी नोंदणी करा",
    labelName: "पूर्ण नाव",
    labelPhone: "मोबाईल नंबर",
    labelConfirmPass: "पासवर्डची पुष्टी करा",
    btnRegister: "नोंदणी करा",
    btnRegistering: "नोंदणी होत आहे...",
    footerCopy: "© २०२६ मायस्कूल प्रो क्लासरूम सूट. सर्व हक्क राखीव.",
    footerVersion: "आवृत्ती २.४.० (स्थिर)",
    footerOperator: "🔒 ऑपरेटर कन्सोल",
    
    // Interactive How it works Section
    howItWorksTitle: "मायस्कूल प्रो तुमचे काम कसे सोपे करते?",
    howItWorksSubtitle: "वर्गातील प्रशासकीय कामे डिजिटल करण्यासाठी ४ सोप्या पायऱ्या, जे वेग आणि विश्वासार्हतेसाठी डिझाइन केलेले आहेत.",
    step1Title: "१. वर्ग रचना (Class Setup)",
    step1Desc: "इयत्ता आणि तुकडी निवडा. सत्र लिंक करा आणि कमाल गुण भारांशांसह विषय सेट करा.",
    step2Title: "२. विद्यार्थी नोंदणी (Student Roll)",
    step2Desc: "विद्यार्थ्याची माहिती, जन्मतारीख, हजेरी क्रमांक आणि प्रवर्ग जोडा. आकडेवारी त्वरित मोजली जाते.",
    step3Title: "३. गुण नोंदणी (Grade Entry)",
    step3Desc: "आकारिक आणि संकलित गुण प्रविष्ट करा. ॲप सरासरी, टक्केवारी आणि शेरे त्वरित काढते.",
    step4Title: "४. प्रगतीपत्रक पीडीएफ प्रिंट",
    step4Desc: "शासकीय-नमुन्यात प्रगतीपत्रके जनरेट करा. इंटरनेटशिवाय पीडीएफ फाईल्स त्वरित जतन करा किंवा शेअर करा.",
    
    // Demo video section
    videoTitle: "मायस्कूल प्रो कार्यरत पहा",
    videoSubtitle: "शिक्षक वर्गात ऑफलाईन गुण कसे व्यवस्थापित करतात हे पाहण्यासाठी आमचा २-मिनिटांचा डेमो पहा.",
    videoPlaceholder: "ॲप डेमो व्हिडिओचे पूर्वावलोकन",
    videoTag1: "ऑफलाईन सिंकिंग",
    videoTag2: "CCE मूल्यमापन",
    videoTag3: "त्वरित अहवाल",

    // Mockup preview texts
    mockClassHeader: "वर्ग रचना पॅनेल",
    mockClassLabel: "सक्रिय तुकडी:",
    mockSubjectsLabel: "विषय कमाल भारांश",
    mockStudentHeader: "वर्गातील एकूण नोंदणी",
    mockCategoryBadge: "प्रवर्ग",
    mockMarksHeader: "आकारिक गुण नोंदणी",
    mockGradingStatus: "सरासरी मोजत आहे...",
    mockReportHeader: "CCE प्रगतीपत्रक मुद्रण",
    mockReportTitle: "महाराष्ट्र राज्य बोर्ड नमुना"
  }
};

export default function LandingPage({ user, loading, lang }) {
  const [activeTab, setActiveTab] = useState('login'); // 'login' or 'signup'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  // Interactive Animation States
  const [activeStep, setActiveStep] = useState(0);
  const [studentCounter, setStudentCounter] = useState(0);
  const [progressWidth, setProgressWidth] = useState(0);
  const [typedMarks, setTypedMarks] = useState({ kOral: '', kWrite: '', pOral: '', pWrite: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const timerRef = useRef(null);

  const t = translations[lang];

  useEffect(() => {
    if (!loading && user) {
      if (checkIsAdmin(user.email)) {
        navigate('/admin');
      } else {
        navigate('/app-redirect');
      }
    }
  }, [user, loading, navigate]);

  // Autoplay loop for how it works animations
  useEffect(() => {
    timerRef.current = setInterval(() => {
      setActiveStep((prev) => (prev + 1) % 4);
    }, 4500);
    return () => clearInterval(timerRef.current);
  }, []);

  // Control sub-animations inside the step cards based on activeStep
  useEffect(() => {
    if (activeStep === 1) {
      // Step 2: count up students
      setStudentCounter(0);
      let count = 0;
      const interval = setInterval(() => {
        count += 3;
        if (count >= 33) {
          setStudentCounter(33);
          clearInterval(interval);
        } else {
          setStudentCounter(count);
        }
      }, 100);
      return () => clearInterval(interval);
    } else if (activeStep === 2) {
      // Step 3: progress bar filling & typing simulations
      setProgressWidth(0);
      setTypedMarks({ kOral: '', kWrite: '', pOral: '', pWrite: '' });

      let width = 0;
      const interval = setInterval(() => {
        width += 8;
        if (width >= 100) {
          setProgressWidth(100);
          clearInterval(interval);
        } else {
          setProgressWidth(width);
        }
      }, 80);

      const t1 = setTimeout(() => setTypedMarks(prev => ({ ...prev, kOral: '08' })), 500);
      const t2 = setTimeout(() => setTypedMarks(prev => ({ ...prev, kWrite: '34' })), 1100);
      const t3 = setTimeout(() => setTypedMarks(prev => ({ ...prev, pOral: '09' })), 1700);
      const t4 = setTimeout(() => setTypedMarks(prev => ({ ...prev, pWrite: '38' })), 2300);

      return () => {
        clearInterval(interval);
        clearTimeout(t1);
        clearTimeout(t2);
        clearTimeout(t3);
        clearTimeout(t4);
      };
    }
  }, [activeStep]);

  const getPointerCoords = () => {
    switch (activeStep) {
      case 0: return { top: '68%', left: '72%' };
      case 1: return { top: '35%', left: '80%' };
      case 2: return { top: '56%', left: '46%' };
      case 3: return { top: '80%', left: '50%' };
      default: return { top: '50%', left: '50%' };
    }
  };

  const handleStepClick = (index) => {
    setActiveStep(index);
    if (timerRef.current) {
      clearInterval(timerRef.current);
      // Restart loop from this index after delay
      timerRef.current = setInterval(() => {
        setActiveStep((prev) => (prev + 1) % 4);
      }, 4500);
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSubmitting(true);

    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (err) {
      setError(err.message || 'Login failed. Please check your credentials.');
      setSubmitting(false);
    }
  };

  const handleSignup = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setSubmitting(true);

    try {
      const userCredential = await createUserWithEmailAndPassword(auth, email, password);
      const newUser = userCredential.user;

      await setDoc(doc(db, 'teachers', newUser.uid), {
        id: newUser.uid,
        name: name,
        email: email,
        phone: phone,
        schoolIds: []
      });

      setSuccess('Account created successfully!');
    } catch (err) {
      setError(err.message || 'Sign up failed. Please try again.');
      setSubmitting(false);
    }
  };

  // Helper renderer for interactive step animations
  const renderStepMockup = () => {
    switch (activeStep) {
      case 0:
        return (
          <div className="step-mockup setup-mockup animate-scale-up">
            <div className="mock-window-header">
              <span>●</span><span>●</span><span>●</span>
              <strong className="mock-window-title">{t.mockClassHeader}</strong>
            </div>
            <div className="mock-window-body">
              <div className="mock-class-badge">🏫 Room A</div>
              <div className="mock-form-row">
                <span>{t.mockClassLabel}</span>
                <strong>Class 7 - Div A</strong>
              </div>
              <div className="mock-sub-title">{t.mockSubjectsLabel}</div>
              <div className="mock-subjects-weights">
                <div className="mock-sub-row">
                  <span>Marathi</span>
                  <div className="mock-slider"><div className="mock-slider-bar slide-w-100"></div></div>
                  <span>100</span>
                </div>
                <div className="mock-sub-row">
                  <span>English</span>
                  <div className="mock-slider"><div className="mock-slider-bar slide-w-90"></div></div>
                  <span>100</span>
                </div>
                <div className="mock-sub-row">
                  <span>Mathematics</span>
                  <div className="mock-slider"><div className="mock-slider-bar slide-w-100"></div></div>
                  <span>100</span>
                </div>
              </div>
            </div>
          </div>
        );
      case 1:
        return (
          <div className="step-mockup student-mockup animate-scale-up">
            <div className="mock-window-header">
              <span>●</span><span>●</span><span>●</span>
              <strong className="mock-window-title">{t.mockStudentHeader}</strong>
            </div>
            <div className="mock-window-body">
              <div className="mock-stats-grid">
                <div className="mock-stat-box">
                  <span>Total Roll</span>
                  <strong>{studentCounter}</strong>
                </div>
                <div className="mock-stat-box">
                  <span>Boys / Girls</span>
                  <strong>16 / 17</strong>
                </div>
              </div>

              {/* Animated Category Segmented Bar Chart */}
              <div className="mock-category-chart">
                <div className="mock-chart-label">Category Mix</div>
                <div className="chart-bar-wrap">
                  <div className="chart-bar sc" style={{ width: `${Math.min(studentCounter * 0.15 * 100/33, 15)}%` }}>{studentCounter > 5 && 'SC'}</div>
                  <div className="chart-bar st" style={{ width: `${Math.min(studentCounter * 0.12 * 100/33, 12)}%` }}>{studentCounter > 10 && 'ST'}</div>
                  <div className="chart-bar obc" style={{ width: `${Math.min(studentCounter * 0.45 * 100/33, 45)}%` }}>{studentCounter > 15 && 'OBC'}</div>
                  <div className="chart-bar gen" style={{ width: `${Math.min(studentCounter * 0.28 * 100/33, 28)}%` }}>{studentCounter > 20 && 'GEN'}</div>
                </div>
              </div>

              <div className="mock-student-list">
                <div className="mock-student-card">
                  <div className="mock-avatar">K</div>
                  <div>
                    <strong>Kartik Thorat</strong>
                    <span>Roll: 101 • {t.mockCategoryBadge}: OBC</span>
                  </div>
                </div>
                <div className="mock-student-card">
                  <div className="mock-avatar">P</div>
                  <div>
                    <strong>Priya Patil</strong>
                    <span>Roll: 102 • {t.mockCategoryBadge}: Gen</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        );
      case 2:
        return (
          <div className="step-mockup grading-mockup animate-scale-up">
            <div className="mock-window-header">
              <span>●</span><span>●</span><span>●</span>
              <strong className="mock-window-title">{t.mockMarksHeader}</strong>
            </div>
            <div className="mock-window-body">
              <div className="mock-table">
                <div className="mock-table-head">
                  <span>Name</span><span>Oral (10)</span><span>Written (40)</span>
                </div>
                <div className="mock-table-row">
                  <span>Kartik T.</span>
                  <span className="mock-input cell-pop-1">
                    {typedMarks.kOral || (activeStep === 2 && !typedMarks.kOral ? <span className="typing-cursor">|</span> : '')}
                  </span>
                  <span className="mock-input cell-pop-2">
                    {typedMarks.kWrite || (typedMarks.kOral && !typedMarks.kWrite ? <span className="typing-cursor">|</span> : '')}
                  </span>
                </div>
                <div className="mock-table-row">
                  <span>Priya P.</span>
                  <span className="mock-input cell-pop-1">
                    {typedMarks.pOral || (typedMarks.kWrite && !typedMarks.pOral ? <span className="typing-cursor">|</span> : '')}
                  </span>
                  <span className="mock-input cell-pop-2">
                    {typedMarks.pWrite || (typedMarks.pOral && !typedMarks.pWrite ? <span className="typing-cursor">|</span> : '')}
                  </span>
                </div>
              </div>
              <div className="mock-calc-overlay">
                <span>{progressWidth < 100 ? t.mockGradingStatus : (lang === 'en' ? 'Calculated! ✔' : 'गणना पूर्ण! ✔')}</span>
                <div className="mock-progress-track">
                  <div className="mock-progress-bar" style={{ width: `${progressWidth}%` }}></div>
                </div>
              </div>
            </div>
          </div>
        );
      case 3:
        return (
          <div className="step-mockup report-mockup animate-scale-up">
            <div className="mock-window-header">
              <span>●</span><span>●</span><span>●</span>
              <strong className="mock-window-title">{t.mockReportHeader}</strong>
            </div>
            <div className="mock-window-body">
              <div className="mock-report-card">
                <div className="mock-report-header">
                  <strong>MySchool Pro</strong>
                  <span>{t.mockReportTitle}</span>
                </div>
                <div className="mock-report-body">
                  <div className="mock-report-info">
                    <span>Name: Kartik Thorat</span>
                    <span>Std: 7th • Div: A</span>
                  </div>
                  <div className="mock-grades-summary">
                    <div><span>Marathi:</span> <strong>A1</strong></div>
                    <div><span>English:</span> <strong>A2</strong></div>
                    <div><span>Math:</span> <strong>A1</strong></div>
                  </div>
                </div>
              </div>
              <div className="mock-download-overlay">
                <div className="mock-pdf-icon animate-bounce">PDF</div>
                <strong>Report PDF Ready!</strong>
              </div>
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="landing-container animate-fade-in">
      {/* Premium background blobs */}
      <div className="orb orb-one" />
      <div className="orb orb-two" />
      <div className="orb orb-three" />

      <header className="landing-header">
        <div className="brand-header">
          <div className="brand-logo">MS</div>
          <div>
            <h1>{t.title}</h1>
            <p>{t.subtitle}</p>
          </div>
        </div>
      </header>

      <main className="landing-main">
        {/* Left Side: Product Showcase */}
        <section className="product-showcase reveal-fade-in-left">
          <div className="badge reveal-fade-in-up delay-50">{t.heroBadge}</div>
          <h2 className="reveal-fade-in-up delay-100">{t.heroTitle}</h2>
          <p className="description reveal-fade-in-up delay-150">{t.heroDesc}</p>

          {/* Bobbing Stats Badge Widget */}
          <div className="floating-badge-widget reveal-fade-in-up delay-200">
            <span className="widget-pulse" />
            <span>📊 {lang === 'en' ? '10K+ Report Cards Compiled Offline' : '१० हजार+ प्रगतीपत्रके ऑफलाईन तयार केली'}</span>
          </div>

          <div className="showcase-features reveal-fade-in-up delay-250">
            <div className="feature-item">
              <span className="feature-icon">📊</span>
              <div>
                <h3>{t.feat1Title}</h3>
                <p>{t.feat1Desc}</p>
              </div>
            </div>
            <div className="feature-item">
              <span className="feature-icon">🎓</span>
              <div>
                <h3>{t.feat2Title}</h3>
                <p>{t.feat2Desc}</p>
              </div>
            </div>
            <div className="feature-item">
              <span className="feature-icon">📄</span>
              <div>
                <h3>{t.feat3Title}</h3>
                <p>{t.feat3Desc}</p>
              </div>
            </div>
          </div>
        </section>

        {/* Right Side: Log In / Sign Up Card */}
        <section className="auth-card-wrap reveal-fade-in-right delay-200">
          <div className="glass-panel auth-card">
            <div className="auth-tabs">
              <button 
                className={`auth-tab ${activeTab === 'login' ? 'active' : ''}`}
                onClick={() => { setActiveTab('login'); setError(''); }}
              >
                {t.tabLogin}
              </button>
              <button 
                className={`auth-tab ${activeTab === 'signup' ? 'active' : ''}`}
                onClick={() => { setActiveTab('signup'); setError(''); }}
              >
                {t.tabSignup}
              </button>
            </div>

            {error && <div className="error-message">{error}</div>}
            {success && <div className="success-message">{success}</div>}

            {activeTab === 'login' ? (
              <form onSubmit={handleLogin} className="auth-form">
                <div className="auth-form-header">
                  <h3>{t.loginHeader}</h3>
                  <p>{t.loginSub}</p>
                </div>

                <div className="input-group">
                  <label htmlFor="login-email">{t.labelEmail}</label>
                  <input
                    id="login-email"
                    type="email"
                    placeholder="teacher@myschool.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>

                <div className="input-group">
                  <label htmlFor="login-password">{t.labelPassword}</label>
                  <input
                    id="login-password"
                    type="password"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                </div>

                <button 
                  type="submit" 
                  className="btn btn-primary auth-submit-btn"
                  disabled={submitting}
                >
                  {submitting ? t.btnSigningIn : t.btnSignIn}
                </button>
              </form>
            ) : (
              <form onSubmit={handleSignup} className="auth-form">
                <div className="auth-form-header">
                  <h3>{t.signupHeader}</h3>
                  <p>{t.signupSub}</p>
                </div>

                <div className="input-group">
                  <label htmlFor="signup-name">{t.labelName}</label>
                  <input
                    id="signup-name"
                    type="text"
                    placeholder="Kartik Thorat"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                  />
                </div>

                <div className="input-group">
                  <label htmlFor="signup-phone">{t.labelPhone}</label>
                  <input
                    id="signup-phone"
                    type="tel"
                    placeholder="+91 98765 43210"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    required
                  />
                </div>

                <div className="input-group">
                  <label htmlFor="signup-email">{t.labelEmail}</label>
                  <input
                    id="signup-email"
                    type="email"
                    placeholder="teacher@myschool.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>

                <div className="input-group">
                  <label htmlFor="signup-password">{t.labelPassword}</label>
                  <input
                    id="signup-password"
                    type="password"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                </div>

                <div className="input-group">
                  <label htmlFor="signup-confirm-password">{t.labelConfirmPass}</label>
                  <input
                    id="signup-confirm-password"
                    type="password"
                    placeholder="••••••••"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                  />
                </div>

                <button 
                  type="submit" 
                  className="btn btn-primary auth-submit-btn"
                  disabled={submitting}
                >
                  {submitting ? t.btnRegistering : t.btnRegister}
                </button>
              </form>
            )}
          </div>
        </section>
      </main>

      {/* Interactive Walkthrough Section */}
      <section className="how-it-works-section reveal-fade-in-up">
        <div className="section-header">
          <h2>{t.howItWorksTitle}</h2>
          <p>{t.howItWorksSubtitle}</p>
        </div>

        <div className="interactive-walkthrough">
          {/* Steps panel */}
          <div className="walkthrough-steps">
            <button 
              className={`step-card ${activeStep === 0 ? 'active' : ''}`}
              onClick={() => handleStepClick(0)}
              type="button"
            >
              <h4>{t.step1Title}</h4>
              <p>{t.step1Desc}</p>
            </button>
            <button 
              className={`step-card ${activeStep === 1 ? 'active' : ''}`}
              onClick={() => handleStepClick(1)}
              type="button"
            >
              <h4>{t.step2Title}</h4>
              <p>{t.step2Desc}</p>
            </button>
            <button 
              className={`step-card ${activeStep === 2 ? 'active' : ''}`}
              onClick={() => handleStepClick(2)}
              type="button"
            >
              <h4>{t.step3Title}</h4>
              <p>{t.step3Desc}</p>
            </button>
            <button 
              className={`step-card ${activeStep === 3 ? 'active' : ''}`}
              onClick={() => handleStepClick(3)}
              type="button"
            >
              <h4>{t.step4Title}</h4>
              <p>{t.step4Desc}</p>
            </button>
          </div>

          {/* Interactive display output */}
          <div className="walkthrough-display glass-panel">
            {renderStepMockup()}
            <div className="virtual-pointer" style={getPointerCoords()} />
          </div>
        </div>
      </section>

      {/* Video Demonstration Section */}
      <section className="demo-video-section reveal-fade-in-up">
        <div className="section-header">
          <h2>{t.videoTitle}</h2>
          <p>{t.videoSubtitle}</p>
        </div>

        <div className="video-player-container glass-panel">
          <div className="video-mockup-wrapper" onClick={() => setIsModalOpen(true)}>
            <div className="play-ripple-container">
              <div className="ripple-wave r1" />
              <div className="ripple-wave r2" />
              <div className="ripple-wave r3" />
              <button className="play-mockup-btn" aria-label="Play video demo" onClick={(e) => { e.stopPropagation(); setIsModalOpen(true); }}>
                ▶
              </button>
            </div>
            <div className="mock-video-tags">
              <span>{t.videoTag1}</span>
              <span>{t.videoTag2}</span>
              <span>{t.videoTag3}</span>
            </div>
            <strong className="video-mockup-title">{t.videoPlaceholder}</strong>
          </div>
        </div>
      </section>

      <footer className="landing-footer">
        <p>{t.footerCopy}</p>
        <div className="footer-links">
          <span>{t.footerVersion}</span>
          <span>•</span>
          <span onClick={() => navigate('/admin-login')} className="admin-hidden-trigger">
            {t.footerOperator}
          </span>
        </div>
      </footer>

      {isModalOpen && (
        <div className="emulator-modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="emulator-modal-content glass-panel" onClick={(e) => e.stopPropagation()}>
            <button className="close-emulator-btn" onClick={() => setIsModalOpen(false)}>×</button>
            
            <div className="emulator-phone-wrapper">
              <div className="phone-notch" />
              <div className="phone-screen">
                {/* Simulated StatusBar */}
                <div className="app-status-bar">
                  <span>12:30 PM</span>
                  <div className="status-icons">📶 🔋 100%</div>
                </div>

                {/* Simulated App Content */}
                <div className="emulator-app-body">
                  <div className="emulator-header">
                    <div className="avatar">KT</div>
                    <div>
                      <h4>Kartik Thorat</h4>
                      <span className="sync-badge">🟢 Cloud Synced</span>
                    </div>
                  </div>

                  <div className="emulator-stats-row">
                    <div className="stat-card">
                      <span>Standards</span>
                      <strong>8</strong>
                    </div>
                    <div className="stat-card">
                      <span>Offline Logs</span>
                      <strong>240+</strong>
                    </div>
                  </div>

                  <div className="emulator-simulated-workflow">
                    <div className="workflow-screen-title">Simulated Grade Book</div>
                    <div className="simulated-grades-grid">
                      <div className="grade-item">
                        <span>Mathematics</span>
                        <div className="grade-pill a1">A1</div>
                      </div>
                      <div className="grade-item">
                        <span>Science</span>
                        <div className="grade-pill a1">A1</div>
                      </div>
                      <div className="grade-item">
                        <span>Social Studies</span>
                        <div className="grade-pill a2">A2</div>
                      </div>
                    </div>
                    
                    <div className="offline-alert-box">
                      <span className="icon">💾</span>
                      <div>
                        <strong>Database Saved Locally</strong>
                        <p>No internet required. Logs will sync next time you are online.</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="emulator-info-sidebar">
              <h3>{lang === 'en' ? 'MySchool Mobile App Simulator' : 'मायस्कूल मोबाईल ॲप सिम्युलेटर'}</h3>
              <p>
                {lang === 'en' 
                  ? 'Explore the high-fidelity features of our mobile interface directly from your web browser:' 
                  : 'थेट तुमच्या वेब ब्राउझरवरून आमच्या मोबाईल इंटरफेसच्या वैशिष्ट्यांचा अनुभव घ्या:'}
              </p>
              <ul>
                <li>
                  <strong>{lang === 'en' ? 'Local Database Engine' : 'स्थानिक डेटाबेस इंजिन'}:</strong>{' '}
                  {lang === 'en' ? 'Works in areas with zero cell coverage.' : 'कव्हरेज नसलेल्या भागातही व्यवस्थित काम करते.'}
                </li>
                <li>
                  <strong>{lang === 'en' ? 'Offline SQLite Storage' : 'ऑफलाईन SQLite स्टोरेज'}:</strong>{' '}
                  {lang === 'en' ? 'Write operations take less than 1ms.' : 'गुण नोंदणी प्रक्रिया १ मिलीसेकंदापेक्षा कमी वेळात होते.'}
                </li>
                <li>
                  <strong>{lang === 'en' ? 'Automated PDF Generation' : 'स्वयंचलित पीडीएफ निर्मिती'}:</strong>{' '}
                  {lang === 'en' ? 'Creates progress cards offline inside the classroom.' : 'वर्गात बसून ऑफलाईन प्रगतीपत्रके तयार करा.'}
                </li>
              </ul>
              <button className="btn btn-primary" onClick={() => setIsModalOpen(false)}>
                {lang === 'en' ? 'Got It' : 'समजले'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
