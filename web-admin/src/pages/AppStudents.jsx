import React, { useState, useEffect } from 'react';
import { collection, query, where, getDocs, addDoc, doc, updateDoc, deleteDoc, onSnapshot } from 'firebase/firestore';
import { db, auth } from '../firebase';
import { useTeacherContext } from '../context/TeacherContext';
import useLanguage from '../utils/useLanguage';
import './AppStudents.css';

export default function AppStudents() {
  const { activeClass, activeSchool } = useTeacherContext();
  const { t } = useLanguage();
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [isAdding, setIsAdding] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [isPromoteModalOpen, setIsPromoteModalOpen] = useState(false);
  
  const [activeTab, setActiveTab] = useState('basic');

  const emptyStudent = {
    name: '', rollNo: '', registrationNo: '', dob: '', gender: 'Male', cast: '', religion: '', birthPlace: '', bloodGroup: '', height: '', weight: '', height2: '', weight2: '',
    motherName: '', fatherName: '', address: '', parentPhone: '', parentPhone2: '', fatherOccupation: '', motherOccupation: '',
    bankName: '', bankAccount: '', bankIfsc: '', bankUid: '',
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
    
    try {
      const docData = {
        ...formData,
        classId: activeClass.id,
        teacherId: auth.currentUser.uid,
        className: activeClass.name,
        standard: activeClass.className || activeClass.name,
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
              <div className="input-group"><label>{t('Height (Sem 1) cm', 'उंची (सत्र १) cm')}</label><input type="text" name="height" value={formData.height} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Weight (Sem 1) kg', 'वजन (सत्र १) kg')}</label><input type="text" name="weight" value={formData.weight} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Height (Sem 2) cm', 'उंची (सत्र २) cm')}</label><input type="text" name="height2" value={formData.height2} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Weight (Sem 2) kg', 'वजन (सत्र २) kg')}</label><input type="text" name="weight2" value={formData.weight2} onChange={handleChange} /></div>
            </div>
          )}
          
          {activeTab === 'family' && (
            <div className="form-grid">
              <div className="input-group"><label>{t('Mother\'s Name', 'आईचे नाव')}</label><input type="text" name="motherName" value={formData.motherName} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Father\'s Name', 'वडिलांचे नाव')}</label><input type="text" name="fatherName" value={formData.fatherName} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Address', 'पत्ता')}</label><input type="text" name="address" value={formData.address} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Phone Number 1', 'फोन नंबर १')}</label><input type="tel" name="parentPhone" value={formData.parentPhone} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Phone Number 2', 'फोन नंबर २')}</label><input type="tel" name="parentPhone2" value={formData.parentPhone2} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Mother\'s Occupation', 'आईचा व्यवसाय')}</label><input type="text" name="motherOccupation" value={formData.motherOccupation} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Father\'s Occupation', 'वडिलांचा व्यवसाय')}</label><input type="text" name="fatherOccupation" value={formData.fatherOccupation} onChange={handleChange} /></div>
            </div>
          )}

          {activeTab === 'bank' && (
            <div className="form-grid">
              <div className="input-group"><label>{t('Bank Name', 'बँकेचे नाव')}</label><input type="text" name="bankName" value={formData.bankName} onChange={handleChange} /></div>
              <div className="input-group"><label>{t('Account Number', 'खाते क्रमांक')}</label><input type="text" name="bankAccount" value={formData.bankAccount} onChange={handleChange} /></div>
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
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
          <div className="profile-section card-panel">
            <h4>{t('Basic Info', 'मूलभूत माहिती')}</h4>
            <p><strong>{t('Registration No:', 'रजिस्टर नं:')}</strong> {selectedStudent.registrationNo || 'N/A'}</p>
            <p><strong>{t('DOB:', 'जन्म तारीख:')}</strong> {selectedStudent.dob || 'N/A'}</p>
            <p><strong>{t('Caste:', 'जात:')}</strong> {selectedStudent.cast || 'N/A'}</p>
            <p><strong>{t('Religion:', 'धर्म:')}</strong> {selectedStudent.religion || 'N/A'}</p>
            <p><strong>{t('Blood Group:', 'रक्तगट:')}</strong> {selectedStudent.bloodGroup || 'N/A'}</p>
            <div style={{ display: 'flex', gap: '15px', marginTop: '10px' }}>
              <p><strong>{t('Height:', 'उंची:')}</strong> {selectedStudent.height || '--'} cm</p>
              <p><strong>{t('Weight:', 'वजन:')}</strong> {selectedStudent.weight || '--'} kg</p>
            </div>
          </div>
          <div className="profile-section card-panel">
            <h4>{t('Family & Contact', 'कुटुंब आणि संपर्क')}</h4>
            <p><strong>{t('Mother:', 'आई:')}</strong> {selectedStudent.motherName || 'N/A'}</p>
            <p><strong>{t('Father:', 'वडील:')}</strong> {selectedStudent.fatherName || 'N/A'}</p>
            <p><strong>{t('Address:', 'पत्ता:')}</strong> {selectedStudent.address || 'N/A'}</p>
            <p><strong>{t('Phone 1:', 'फोन १:')}</strong> {selectedStudent.parentPhone || 'N/A'}</p>
            <p><strong>{t('Phone 2:', 'फोन २:')}</strong> {selectedStudent.parentPhone2 || 'N/A'}</p>
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
        </div>
      </div>
    );
  };

  return (
    <div className="app-students">
      <div className="students-header">
        <h2>{t('Students Database', 'विद्यार्थी माहिती')}</h2>
        <div className="header-actions">
          <button className="btn-secondary" onClick={() => setIsPromoteModalOpen(true)}>
            {t('Promote Students', 'विद्यार्थी प्रमोट करा')}
          </button>
          <button className="btn-primary" onClick={openAddModal}>
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
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                    <div className="stu-avatar" style={{ width: '50px', height: '50px', fontSize: '24px', background: 'var(--primary-color)', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '50%' }}>
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

                <div className="modal-actions" style={{ justifyContent: 'space-between', marginTop: '20px' }}>
                  <button type="button" className="btn-danger" onClick={handleDeleteStudent} style={{ background: 'var(--danger-color)', color: 'white', border: 'none', padding: '10px 16px', borderRadius: '8px', cursor: 'pointer' }}>{t('Delete Student', 'डिलीट करा')}</button>
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
              {t('Select students from this class to promote them to a new class and academic year.', 'या वर्गातील विद्यार्थ्यांना नवीन वर्ग आणि नवीन शैक्षणिक वर्षात प्रमोट करा.')}
            </p>
            
            <div className="warning-banner" style={{ marginBottom: '20px' }}>
              {t('Feature coming soon in Phase 4.', 'हे फीचर टप्पा ४ मध्ये येत आहे.')} 
            </div>

            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={() => setIsPromoteModalOpen(false)}>{t('Close', 'बंद करा')}</button>
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
