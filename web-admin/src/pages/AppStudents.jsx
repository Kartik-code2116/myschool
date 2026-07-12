import React, { useState, useEffect } from 'react';
import { collection, query, where, getDocs, addDoc, doc, updateDoc, deleteDoc, onSnapshot, setDoc, getDoc, serverTimestamp } from 'firebase/firestore';
import { db, auth } from '../firebase';
import { useTeacherContext } from '../context/TeacherContext';
import useLanguage from '../utils/useLanguage';
import './AppStudents.css';

export default function AppStudents() {
  const { activeClass, activeSchool, academicYearsList, semestersList, teacherProfile } = useTeacherContext();
  const { t } = useLanguage();
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [isAdding, setIsAdding] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [isPromoteModalOpen, setIsPromoteModalOpen] = useState(false);
  
  const [parentLinkCode, setParentLinkCode] = useState(null);
  const [generatingLink, setGeneratingLink] = useState(false);
  
  const [targetYearId, setTargetYearId] = useState('');
  const [targetClassNum, setTargetClassNum] = useState('1');
  const [targetDiv, setTargetDiv] = useState('-');
  const [promoting, setPromoting] = useState(false);
  const [promoteStatus, setPromoteStatus] = useState('');

  const [activeTab, setActiveTab] = useState('basic');

  const emptyStudent = {
    name: '', rollNo: '', registrationNo: '', dob: '', gender: 'Male', cast: '', religion: '', birthPlace: '', bloodGroup: '', heightSem1: '', weightSem1: '', heightSem2: '', weightSem2: '',
    motherName: '', fatherName: '', address: '', fatherPhone: '', motherPhone: '', fatherOccupation: '', motherOccupation: '',
    bankName: '', bankAccount: '', bankBranch: '', bankIfsc: '', bankUid: '',
    medium: '', motherTongue: '', dateOfAdmission: '', studentIdNumber: '', uid: ''
  };

  const [formData, setFormData] = useState(emptyStudent);

  useEffect(() => {
    if (!activeClass || !auth.currentUser) {
      setStudents([]);
      setLoading(false);
      return;
    }
    
    setLoading(true);
    const q = query(collection(db, 'students'), 
        where('classId', '==', activeClass.id),
        where('teacherId', '==', auth.currentUser.uid)
    );
    
    const unsubscribe = onSnapshot(q, (snap) => {
        const stuList = snap.docs.map(doc => ({ id: doc.id, ...emptyStudent, ...doc.data() }));
        stuList.sort((a,b) => parseInt(a.rollNo || 0) - parseInt(b.rollNo || 0));
        setStudents(stuList);
        setLoading(false);
    }, (err) => {
        console.error(err);
        setLoading(false);
    });

    return () => unsubscribe();
  }, [activeClass]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!activeClass || !auth.currentUser) return;
    
    if (teacherProfile?.subscriptionStatus !== 'active' && students.length >= 3) {
      alert("You have reached the free limit of 3 students. Please upgrade to premium to add more.");
      return;
    }
    
    try {
      const docData = {
        ...formData,
        classId: activeClass.id,
        teacherId: auth.currentUser.uid,
        className: activeClass.name,
        standard: activeClass.className || activeClass.name,
        division: activeClass.division || '-',
        schoolId: activeSchool?.id || '',
        schoolName: activeSchool?.name || ''
      };
      await addDoc(collection(db, 'students'), docData);
      setIsAdding(false);
      setFormData(emptyStudent);
      setActiveTab('basic');
      fetchStudents();
    } catch (err) {
      console.error("Error adding student:", err);
    }
  };

  const handleStudentClick = (stu) => {
    setSelectedStudent(stu);
    setFormData(stu);
    setIsEditing(false);
    setActiveTab('basic');
    fetchParentLink(stu.id);
  };

  const fetchParentLink = async (studentId) => {
    try {
      const docRef = doc(db, 'parent_links', studentId);
      const snap = await getDoc(docRef);
      if (snap.exists()) {
        setParentLinkCode(snap.data().code);
      } else {
        setParentLinkCode(null);
      }
    } catch (err) {
      console.error("Failed to fetch parent link", err);
    }
  };

  const generateParentLink = async () => {
    if (!selectedStudent || !activeClass) return;
    setGeneratingLink(true);
    try {
      const code = String(100000 + Math.floor(Math.random() * 900000));
      const linkData = {
        id: selectedStudent.id,
        studentId: selectedStudent.id,
        teacherId: auth.currentUser.uid,
        code: code,
        studentName: selectedStudent.name,
        className: activeClass.className || activeClass.name,
        schoolName: activeSchool?.name || 'My School',
        createdAt: Date.now()
      };
      await setDoc(doc(db, 'parent_links', selectedStudent.id), linkData);
      setParentLinkCode(code);
    } catch (err) {
      console.error("Failed to generate parent link", err);
    } finally {
      setGeneratingLink(false);
    }
  };

  const handleSaveEdit = async (e) => {
    e.preventDefault();
    if (!selectedStudent) return;
    
    try {
      const stuRef = doc(db, 'students', selectedStudent.id);
      
      const updateData = { ...formData };
      delete updateData.id;

      await updateDoc(stuRef, updateData);
      
      setSelectedStudent({ ...selectedStudent, ...updateData });
      setIsEditing(false);
      fetchStudents();
    } catch (err) {
      console.error("Error updating student:", err);
    }
  };

  const handleDeleteStudent = async () => {
    if (!selectedStudent) return;
    if (window.confirm(t(`Are you sure you want to delete ${selectedStudent.name}?`, `तुम्हाला खात्री आहे की ${selectedStudent.name} ला डिलीट करायचे आहे?`))) {
      try {
        await deleteDoc(doc(db, 'students', selectedStudent.id));
        setSelectedStudent(null);
        fetchStudents();
      } catch (err) {
        console.error("Error deleting student:", err);
      }
    }
  };

  const handlePromoteStudents = async () => {
    if (!targetYearId || students.length === 0) return;
    setPromoting(true);
    setPromoteStatus(t('Finding or creating target class...', 'लक्ष्य वर्ग शोधत आहे किंवा तयार करत आहे...'));

    try {
      const selectedYear = academicYearsList.find(y => y.id === targetYearId);
      let targetClassObj = null;

      // 1. Check if class exists
      const qClass = query(collection(db, 'classes'), 
        where('teacherId', '==', auth.currentUser.uid),
        where('yearId', '==', targetYearId),
        where('className', '==', targetClassNum),
        where('division', '==', targetDiv)
      );
      const snapClass = await getDocs(qClass);

      if (!snapClass.empty) {
        targetClassObj = { id: snapClass.docs[0].id, ...snapClass.docs[0].data() };
      } else {
        // Create it
        setPromoteStatus(t('Creating new class...', 'नवीन वर्ग तयार करत आहे...'));
        let targetSemId = '';
        if (semestersList.length > 0) {
          const semsForYear = semestersList.filter(s => s.yearId === targetYearId);
          if (semsForYear.length > 0) {
            targetSemId = semsForYear[0].id;
          }
        }
        
        const newClassData = {
          schoolId: activeSchool?.id || '',
          yearId: targetYearId,
          academicYearLabel: selectedYear?.label || selectedYear?.year || '',
          semesterId: targetSemId,
          className: targetClassNum,
          division: targetDiv,
          examName: 'First Semester',
          teacherName: auth.currentUser.email?.split('@')[0] || '',
          teacherPhone: '',
          teacherEmail: auth.currentUser.email || '',
          studentCount: 0,
          subjects: [],
          monthlyWorkingDays: {},
          teacherId: auth.currentUser.uid,
          createdAt: serverTimestamp()
        };
        const docRef = await addDoc(collection(db, 'classes'), newClassData);
        targetClassObj = { id: docRef.id, ...newClassData };
      }

      setPromoteStatus(t('Copying students...', 'विद्यार्थ्यांची माहिती कॉपी करत आहे...'));
      
      let successCount = 0;
      for (const stu of students) {
        const newStudent = { ...stu };
        delete newStudent.id;
        newStudent.classId = targetClassObj.id;
        newStudent.className = `${t('Class', 'इयत्ता')} ${targetClassNum} ${targetDiv}`;
        newStudent.standard = targetClassNum;
        newStudent.division = targetDiv;
        newStudent.schoolId = targetClassObj.schoolId;
        newStudent.schoolName = activeSchool?.name || 'My School';
        newStudent.teacherId = auth.currentUser.uid;
        newStudent.marksEntered = false;

        await addDoc(collection(db, 'students'), newStudent);
        successCount++;
      }

      setPromoteStatus('');
      alert(t(`Successfully promoted ${successCount} students!`, `यशस्वीरित्या ${successCount} विद्यार्थ्यांना प्रमोट केले!`));
      setIsPromoteModalOpen(false);
    } catch (err) {
      console.error(err);
      setPromoteStatus(t('Failed to promote students.', 'विद्यार्थ्यांना प्रमोट करण्यात त्रुटी आली.'));
    } finally {
      setPromoting(false);
    }
  };

  const openAddModal = () => {
    setFormData(emptyStudent);
    setActiveTab('basic');
    setIsAdding(true);
  };

  const renderFormFields = () => {
    return (
      <div className="tabbed-form">
        <div className="form-tabs">
          <button type="button" className={activeTab === 'basic' ? 'active' : ''} onClick={() => setActiveTab('basic')}>{t('Basic', 'मूलभूत')}</button>
          <button type="button" className={activeTab === 'family' ? 'active' : ''} onClick={() => setActiveTab('family')}>{t('Family', 'कुटुंब')}</button>
          <button type="button" className={activeTab === 'bank' ? 'active' : ''} onClick={() => setActiveTab('bank')}>{t('Bank', 'बँक')}</button>
          <button type="button" className={activeTab === 'academic' ? 'active' : ''} onClick={() => setActiveTab('academic')}>{t('Academic', 'शैक्षणिक')}</button>
        </div>
        
        <div className="tab-content" style={{ maxHeight: '60vh', overflowY: 'auto', padding: '10px 5px' }}>
          {activeTab === 'basic' && (
            <div className="form-grid">
              <div className="input-group">
                <label>{t('Full Name *', 'पूर्ण नाव *')}</label>
                <input required type="text" name="name" value={formData.name} onChange={handleChange} />
              </div>
              <div className="input-group">
                <label>{t('Roll Number *', 'हजेरी क्रमांक *')}</label>
                <input required type="number" name="rollNo" value={formData.rollNo} onChange={handleChange} />
              </div>
              <div className="input-group">
                <label>{t('Gender *', 'लिंग *')}</label>
                <select name="gender" value={formData.gender} onChange={handleChange}>
                  <option value="Male">{t('Male', 'मुलगा')}</option>
                  <option value="Female">{t('Female', 'मुलगी')}</option>
                </select>
              </div>
              <div className="input-group"><label>{t('Registration No', 'रजिस्टर नं')}</label><input type="text" name="registrationNo" value={formData.registrationNo} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Date of Birth', 'जन्म तारीख')}</label><input type="date" name="dob" value={formData.dob} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Caste', 'जात')}</label><input type="text" name="cast" value={formData.cast} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Religion', 'धर्म')}</label><input type="text" name="religion" value={formData.religion} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Birth Place', 'जन्म ठिकाण')}</label><input type="text" name="birthPlace" value={formData.birthPlace} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Blood Group', 'रक्तगट')}</label><input type="text" name="bloodGroup" value={formData.bloodGroup} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Height (Sem 1) cm', 'उंची (सत्र १) cm')}</label><input type="text" name="heightSem1" value={formData.heightSem1} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Weight (Sem 1) kg', 'वजन (सत्र १) kg')}</label><input type="text" name="weightSem1" value={formData.weightSem1} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Height (Sem 2) cm', 'उंची (सत्र २) cm')}</label><input type="text" name="heightSem2" value={formData.heightSem2} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Weight (Sem 2) kg', 'वजन (सत्र २) kg')}</label><input type="text" name="weightSem2" value={formData.weightSem2} onChange={handleChange} /></div>
            </div>
          )}
          
          {activeTab === 'family' && (
            <div className="form-grid">
              <div className="input-group"><label>{t('Mother\'s Name', 'आईचे नाव')}</label><input type="text" name="motherName" value={formData.motherName} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Father\'s Name', 'वडिलांचे नाव')}</label><input type="text" name="fatherName" value={formData.fatherName} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Address', 'पत्ता')}</label><input type="text" name="address" value={formData.address} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Phone Number 1 (Father)', 'फोन नंबर १ (वडील)')}</label><input type="tel" name="fatherPhone" value={formData.fatherPhone} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Phone Number 2 (Mother)', 'फोन नंबर २ (आई)')}</label><input type="tel" name="motherPhone" value={formData.motherPhone} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Mother\'s Occupation', 'आईचा व्यवसाय')}</label><input type="text" name="motherOccupation" value={formData.motherOccupation} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Father\'s Occupation', 'वडिलांचा व्यवसाय')}</label><input type="text" name="fatherOccupation" value={formData.fatherOccupation} onChange={handleChange} /></div>
            </div>
          )}

          {activeTab === 'bank' && (
            <div className="form-grid">
              <div className="input-group"><label>{t('Bank Name', 'बँकेचे नाव')}</label><input type="text" name="bankName" value={formData.bankName} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Account Number', 'खाते क्रमांक')}</label><input type="text" name="bankAccount" value={formData.bankAccount} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Bank Branch', 'बँकेची शाखा')}</label><input type="text" name="bankBranch" value={formData.bankBranch} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('IFSC Code', 'आयएफएससी कोड')}</label><input type="text" name="bankIfsc" value={formData.bankIfsc} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Aadhar UID', 'आधार क्र.')}</label><input type="text" name="bankUid" value={formData.bankUid} onChange={handleChange} /></div>
            </div>
          )}

          {activeTab === 'academic' && (
            <div className="form-grid">
              <div className="input-group"><label>{t('Medium', 'माध्यम')}</label><input type="text" name="medium" value={formData.medium} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Mother Tongue', 'मातृभाषा')}</label><input type="text" name="motherTongue" value={formData.motherTongue} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Date of Admission', 'प्रवेश दिनांक')}</label><input type="date" name="dateOfAdmission" value={formData.dateOfAdmission} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Student ID Number', 'विद्यार्थी आयडी क्र.')}</label><input type="text" name="studentIdNumber" value={formData.studentIdNumber} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('UID', 'यूआयडी')}</label><input type="text" name="uid" value={formData.uid} onChange={handleChange} /></div>
            </div>
          )}
        </div>
      </div>
    );
  };

  const renderProfileView = () => {
    return (
      <div className="profile-view-details">
        <div className="profile-grid">
          <div className="profile-section card-panel">
            <h4>{t('Basic Info', 'मूलभूत माहिती')}</h4>
            <p><strong>{t('Registration No:', 'रजिस्टर नं:')}</strong> {selectedStudent.registrationNo || 'N/A'}</p>
            <p><strong>{t('DOB:', 'जन्म तारीख:')}</strong> {selectedStudent.dob || 'N/A'}</p>
            <p><strong>{t('Caste:', 'जात:')}</strong> {selectedStudent.cast || 'N/A'}</p>
            <p><strong>{t('Religion:', 'धर्म:')}</strong> {selectedStudent.religion || 'N/A'}</p>
            <p><strong>{t('Blood Group:', 'रक्तगट:')}</strong> {selectedStudent.bloodGroup || 'N/A'}</p>
            <div style={{ display: 'flex', gap: '15px', marginTop: '10px' }}>
              <p><strong>{t('Height:', 'उंची:')}</strong> {selectedStudent.heightSem1 || '--'} cm</p>
              <p><strong>{t('Weight:', 'वजन:')}</strong> {selectedStudent.weightSem1 || '--'} kg</p>
            </div>
          </div>
          <div className="profile-section card-panel">
            <h4>{t('Family & Contact', 'कुटुंब आणि संपर्क')}</h4>
            <p><strong>{t('Mother:', 'आई:')}</strong> {selectedStudent.motherName || 'N/A'}</p>
            <p><strong>{t('Father:', 'वडील:')}</strong> {selectedStudent.fatherName || 'N/A'}</p>
            <p><strong>{t('Address:', 'पत्ता:')}</strong> {selectedStudent.address || 'N/A'}</p>
            <p><strong>{t('Phone 1 (Father):', 'फोन १ (वडील):')}</strong> {selectedStudent.fatherPhone || 'N/A'}</p>
            <p><strong>{t('Phone 2 (Mother):', 'फोन २ (आई):')}</strong> {selectedStudent.motherPhone || 'N/A'}</p>
          </div>
          <div className="profile-section card-panel">
            <h4>{t('Bank & Identity', 'बँक आणि ओळख')}</h4>
            <p><strong>{t('Bank:', 'बँक:')}</strong> {selectedStudent.bankName || 'N/A'}</p>
            <p><strong>{t('Account:', 'खाते:')}</strong> {selectedStudent.bankAccount || 'N/A'}</p>
            <p><strong>{t('IFSC:', 'आयएफएससी:')}</strong> {selectedStudent.bankIfsc || 'N/A'}</p>
            <p><strong>{t('Aadhar UID:', 'आधार क्र.:')}</strong> {selectedStudent.bankUid || 'N/A'}</p>
          </div>
          <div className="profile-section card-panel">
            <h4>{t('Academic', 'शैक्षणिक')}</h4>
            <p><strong>{t('Medium:', 'माध्यम:')}</strong> {selectedStudent.medium || 'N/A'}</p>
            <p><strong>{t('Mother Tongue:', 'मातृभाषा:')}</strong> {selectedStudent.motherTongue || 'N/A'}</p>
            <p><strong>{t('Admitted:', 'प्रवेश दिनांक:')}</strong> {selectedStudent.dateOfAdmission || 'N/A'}</p>
            <p><strong>{t('Student ID:', 'विद्यार्थी आयडी:')}</strong> {selectedStudent.studentIdNumber || 'N/A'}</p>
          </div>
          <div className="profile-section card-panel" style={{ gridColumn: '1 / -1' }}>
            <h4>{t('Parent Portal Access', 'पालक पोर्टल प्रवेश')}</h4>
            <div className="parent-link-box">
              <div className="parent-link-info">
                <p style={{ margin: '0 0 5px 0', fontWeight: '600' }}>{t('Parent Link Code', 'पालक लिंक कोड')}</p>
                <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: '13px' }}>
                  {t('Share this 6-digit code with parents so they can view the student\'s marks and reports in the Parent Portal.', 'हा ६ अंकी कोड पालकांसोबत शेअर करा जेणेकरून ते पालक पोर्टलमध्ये विद्यार्थ्याचे गुण आणि निकाल पाहू शकतील.')}
                </p>
              </div>
              <div className="parent-link-actions">
                {parentLinkCode ? (
                  <div style={{ padding: '8px 16px', background: 'var(--surface-color)', border: '2px dashed var(--primary-color)', borderRadius: '6px', fontSize: '20px', fontWeight: '800', letterSpacing: '4px', color: 'var(--primary-color)' }}>
                    {parentLinkCode}
                  </div>
                ) : (
                  <span style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>{t('Not generated', 'तयार नाही')}</span>
                )}
                <button 
                  className="btn-primary" 
                  onClick={generateParentLink}
                  disabled={generatingLink}
                >
                  {generatingLink ? '...' : (parentLinkCode ? t('Regenerate', 'पुन्हा तयार करा') : t('Generate Code', 'कोड तयार करा'))}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="app-students">
      <div className="students-header">
        <h2>{t('Students Database', 'विद्यार्थी माहिती')}</h2>
        <div className="header-actions">
          <button 
            className="btn btn-primary" 
            onClick={() => setIsPromoteModalOpen(true)}
            disabled={teacherProfile?.subscriptionStatus !== 'active'}
            title={teacherProfile?.subscriptionStatus !== 'active' ? "Upgrade to premium to use rollover feature" : ""}
          >
            {t('Promote Students', 'पुढील वर्गात प्रमोट करा')}
          </button>
          <button 
            className="btn btn-add" 
            onClick={() => {
              if (teacherProfile?.subscriptionStatus !== 'active' && students.length >= 3) {
                alert("You have reached the free limit of 3 students. Please upgrade to premium from the Subscription menu.");
              } else {
                setFormData(emptyStudent); 
                setIsAdding(true);
              }
            }}
          >
            + {t('Add Student', 'विद्यार्थी जोडा')}
          </button>
        </div>
      </div>

      {!activeClass && (
        <div className="warning-banner">{t('Please select an Active Class from the Dashboard.', 'कृपया डॅशबोर्डवरून सक्रिय वर्ग निवडा.')}</div>
      )}

      {/* ADD STUDENT MODAL */}
      {isAdding && (
        <div className="modal-overlay">
          <div className="modal-content large-modal">
            <h3>{t('Add New Student', 'नवीन विद्यार्थी जोडा')}</h3>
            <form onSubmit={handleAdd} className="auth-form">
              {renderFormFields()}
              <div className="modal-actions" style={{ marginTop: '20px' }}>
                <button type="button" className="btn-secondary" onClick={() => setIsAdding(false)}>{t('Cancel', 'रद्द करा')}</button>
                <button type="submit" className="btn-primary">{t('Save Student', 'सेव्ह करा')}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* STUDENT PROFILE / EDIT MODAL */}
      {selectedStudent && (
        <div className="modal-overlay">
          <div className="modal-content large-modal">
            {!isEditing ? (
              <>
                <div className="student-modal-header">
                  <div className="student-modal-title">
                    <div className="stu-avatar" style={{ width: '50px', height: '50px', fontSize: '24px' }}>
                      {selectedStudent.name.charAt(0)}
                    </div>
                    <div>
                      <h2 style={{ margin: '0 0 5px 0' }}>{selectedStudent.name}</h2>
                      <p style={{ margin: 0, color: 'var(--text-secondary)' }}>{t('Roll:', 'हजेरी क्र:')} {selectedStudent.rollNo} | {selectedStudent.gender === 'Female' ? t('Female', 'मुलगी') : t('Male', 'मुलगा')}</p>
                    </div>
                  </div>
                  <button className="btn-secondary" onClick={() => setIsEditing(true)}>{t('Edit Profile', 'माहिती बदला')}</button>
                </div>
                
                {renderProfileView()}

                <div className="modal-actions split-actions">
                  <button type="button" className="btn-danger" onClick={handleDeleteStudent}>{t('Delete Student', 'डिलीट करा')}</button>
                  <button type="button" className="btn-secondary" onClick={() => setSelectedStudent(null)}>{t('Close', 'बंद करा')}</button>
                </div>
              </>
            ) : (
              <>
                <h3>{t('Edit Student', 'विद्यार्थी माहिती बदला')}</h3>
                <form onSubmit={handleSaveEdit} className="auth-form">
                  {renderFormFields()}
                  <div className="modal-actions" style={{ marginTop: '20px' }}>
                    <button type="button" className="btn-secondary" onClick={() => setIsEditing(false)}>{t('Cancel', 'रद्द करा')}</button>
                    <button type="submit" className="btn-primary">{t('Save Changes', 'सेव्ह करा')}</button>
                  </div>
                </form>
              </>
            )}
          </div>
        </div>
      )}

      {/* PROMOTE STUDENTS MODAL */}
      {isPromoteModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>{t('Promote Students', 'विद्यार्थी प्रमोट करा')}</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '20px' }}>
              {t('Select target academic year and class to copy all students from the current roster.', 'वर्तमान सूचीतील सर्व विद्यार्थ्यांना कॉपी करण्यासाठी लक्ष्य शैक्षणिक वर्ष आणि वर्ग निवडा.')}
            </p>
            
            <div className="form-grid" style={{ marginBottom: '20px' }}>
              <div className="input-group">
                <label>{t('Target Academic Year', 'नवीन शैक्षणिक वर्ष')}</label>
                <select value={targetYearId} onChange={e => setTargetYearId(e.target.value)}>
                  <option value="">-- {t('Select Year', 'वर्ष निवडा')} --</option>
                  {academicYearsList.map(y => (
                    <option key={y.id} value={y.id}>{y.name}</option>
                  ))}
                </select>
              </div>
              <div className="input-group">
                <label>{t('Target Class', 'नवीन इयत्ता')}</label>
                <select value={targetClassNum} onChange={e => setTargetClassNum(e.target.value)}>
                  {[...Array(12)].map((_, i) => (
                    <option key={i+1} value={`${i+1}`}>{i+1}</option>
                  ))}
                </select>
              </div>
              <div className="input-group">
                <label>{t('Target Division', 'नवीन तुकडी')}</label>
                <select value={targetDiv} onChange={e => setTargetDiv(e.target.value)}>
                  <option value="-">{t('No Division', 'तुकडी नाही')}</option>
                  <option value="A">A</option>
                  <option value="B">B</option>
                  <option value="C">C</option>
                  <option value="D">D</option>
                </select>
              </div>
            </div>

            {promoteStatus && <div className="info-banner" style={{ background: 'var(--soft-panel)', padding: '10px', borderRadius: '6px', color: 'var(--primary-color)', marginBottom: '20px' }}>{promoteStatus}</div>}

            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={() => setIsPromoteModalOpen(false)} disabled={promoting}>{t('Cancel', 'रद्द करा')}</button>
              <button type="button" className="btn-primary" onClick={handlePromoteStudents} disabled={promoting || !targetYearId || students.length === 0}>
                {promoting ? t('Processing...', 'प्रक्रिया चालू आहे...') : t('Promote All', 'सर्व प्रमोट करा')}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="students-list">
        {loading ? (
          <p>{t('Loading students...', 'विद्यार्थी लोड होत आहेत...')}</p>
        ) : students.length === 0 ? (
          <div className="empty-state">{t('No students found. Add one to get started.', 'विद्यार्थी आढळले नाहीत. नवीन विद्यार्थी जोडा.')}</div>
        ) : (
          students.map(stu => (
            <div key={stu.id} className="student-card" onClick={() => handleStudentClick(stu)} style={{ cursor: 'pointer' }}>
              <div className="stu-avatar">{stu.name.charAt(0)}</div>
              <div className="stu-info">
                <h4>{stu.name}</h4>
                <p>{t('Roll No:', 'हजेरी क्र:')} {stu.rollNo} | {stu.gender === 'Female' ? t('Female', 'मुलगी') : t('Male', 'मुलगा')}</p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
