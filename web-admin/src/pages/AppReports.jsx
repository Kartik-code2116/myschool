import React, { useState, useEffect, useRef } from 'react';
import { useTeacherContext } from '../context/TeacherContext';
import { db, auth } from '../firebase';
import { collection, query, where, getDocs, doc, getDoc } from 'firebase/firestore';
import { useReactToPrint } from 'react-to-print';
import './AppReports.css';

const DEFAULT_SUBJECTS = ["Marathi", "Hindi", "English", "Mathematics", "Science", "Social Science", "Art", "Physical Education"];

export default function AppReports() {
  const { activeClass, activeAcademicYear, activeSemester } = useTeacherContext();
  const [students, setStudents] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);
  
  const [marksRecord, setMarksRecord] = useState(null);
  const [detailedMarks, setDetailedMarks] = useState({});
  const [classSubjects, setClassSubjects] = useState(null);

  const [loading, setLoading] = useState(true);
  const componentRef = useRef();

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
    async function fetchSubjects() {
      if (!activeClass) return;
      try {
        const docRef = doc(db, 'class_subjects', activeClass.id);
        const snap = await getDoc(docRef);
        if (snap.exists() && snap.data().subjects) {
          setClassSubjects(snap.data().subjects);
        }
      } catch(err) {
        console.error(err);
      }
    }
    fetchSubjects();
  }, [activeClass]);

  useEffect(() => {
    async function loadMarks() {
      if (!selectedStudent || !activeAcademicYear) return;
      
      try {
        const q = query(collection(db, 'marks'), 
            where('studentId', '==', selectedStudent.id),
            where('semesterId', '==', activeSemester?.id || "1")
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

  const handlePrint = useReactToPrint({
    content: () => componentRef.current,
    documentTitle: `ReportCard_${selectedStudent?.name || 'Student'}`,
  });

  const displaySubjects = classSubjects || DEFAULT_SUBJECTS.map(name => ({
    id: name, name, formativeWeight: 40, summativeWeight: 60
  }));

  // Calculate totals
  let totalGrand = 0;
  let totalMax = 0;
  displaySubjects.forEach(sub => {
    const data = detailedMarks[sub.name] || {};
    totalGrand += parseInt(data.grandTotal || 0);
    totalMax += (sub.formativeWeight + sub.summativeWeight);
  });
  const percentage = totalMax > 0 ? ((totalGrand / totalMax) * 100).toFixed(2) : 0;

  if (!activeClass) {
      return <div className="app-reports"><div className="warning-banner">Please select an Active Class from the Dashboard.</div></div>;
  }

  return (
    <div className="app-reports">
      <div className="reports-header">
        <h2>Report Cards (Marksheets)</h2>
        <p>Select a student to generate and print their report card.</p>
      </div>

      <div className="reports-layout">
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

        <div className="report-viewer card-panel">
          {!selectedStudent ? (
              <div className="empty-state">Select a student from the list to view their report card.</div>
          ) : (
            <>
              <div className="viewer-header">
                <h3>Preview: {selectedStudent.name}</h3>
                <button className="btn-primary" onClick={handlePrint}>🖨️ Print to PDF</button>
              </div>

              <div className="printable-container" style={{ background: '#e0e0e0', padding: '20px', borderRadius: '8px', marginTop: '20px', overflowX: 'auto' }}>
                {/* The actual printable area */}
                <div 
                  className="printable-report-card" 
                  ref={componentRef}
                  style={{ background: '#fff', width: '210mm', minHeight: '297mm', padding: '20mm', margin: '0 auto', boxShadow: '0 0 10px rgba(0,0,0,0.1)', fontFamily: 'Arial, sans-serif' }}
                >
                  <div style={{ textAlign: 'center', borderBottom: '2px solid #000', paddingBottom: '10px', marginBottom: '20px' }}>
                    <h1 style={{ margin: '0 0 5px 0', fontSize: '24px' }}>EduReport Web Portal</h1>
                    <h2 style={{ margin: '0 0 5px 0', fontSize: '18px' }}>Student Marksheet / Report Card</h2>
                    <p style={{ margin: 0, fontSize: '14px' }}>Academic Year: {activeAcademicYear?.name} | Semester: {activeSemester?.name}</p>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px', fontSize: '14px', lineHeight: '1.6' }}>
                    <div>
                      <div><strong>Student Name:</strong> {selectedStudent.name}</div>
                      <div><strong>Class:</strong> {activeClass.name}</div>
                    </div>
                    <div>
                      <div><strong>Roll No:</strong> {selectedStudent.rollNo}</div>
                      <div><strong>Gender:</strong> {selectedStudent.gender}</div>
                    </div>
                  </div>

                  <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: '20px', fontSize: '14px' }}>
                    <thead>
                      <tr>
                        <th style={{ border: '1px solid #000', padding: '8px', textAlign: 'left' }}>Subject</th>
                        <th style={{ border: '1px solid #000', padding: '8px', textAlign: 'center' }}>Formative (Max)</th>
                        <th style={{ border: '1px solid #000', padding: '8px', textAlign: 'center' }}>Formative (Obt)</th>
                        <th style={{ border: '1px solid #000', padding: '8px', textAlign: 'center' }}>Summative (Max)</th>
                        <th style={{ border: '1px solid #000', padding: '8px', textAlign: 'center' }}>Summative (Obt)</th>
                        <th style={{ border: '1px solid #000', padding: '8px', textAlign: 'center' }}>Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      {displaySubjects.map(sub => {
                        const data = detailedMarks[sub.name] || {};
                        return (
                          <tr key={sub.name}>
                            <td style={{ border: '1px solid #000', padding: '8px' }}>{sub.name}</td>
                            <td style={{ border: '1px solid #000', padding: '8px', textAlign: 'center' }}>{sub.formativeWeight}</td>
                            <td style={{ border: '1px solid #000', padding: '8px', textAlign: 'center' }}>{data.akarikTotal || '-'}</td>
                            <td style={{ border: '1px solid #000', padding: '8px', textAlign: 'center' }}>{sub.summativeWeight}</td>
                            <td style={{ border: '1px solid #000', padding: '8px', textAlign: 'center' }}>{data.sanklit || '-'}</td>
                            <td style={{ border: '1px solid #000', padding: '8px', textAlign: 'center', fontWeight: 'bold' }}>{data.grandTotal || '-'}</td>
                          </tr>
                        );
                      })}
                      <tr style={{ background: '#f5f5f5', fontWeight: 'bold' }}>
                        <td style={{ border: '1px solid #000', padding: '8px' }} colSpan="5">Grand Total</td>
                        <td style={{ border: '1px solid #000', padding: '8px', textAlign: 'center' }}>{totalGrand} / {totalMax}</td>
                      </tr>
                    </tbody>
                  </table>

                  <div style={{ marginBottom: '20px', fontSize: '14px' }}>
                    <div style={{ fontWeight: 'bold', fontSize: '16px' }}>Percentage: {percentage}%</div>
                  </div>

                  <div style={{ marginTop: '30px' }}>
                    <h3 style={{ fontSize: '16px', borderBottom: '1px solid #ccc', paddingBottom: '5px', marginBottom: '10px' }}>Descriptive Remarks</h3>
                    <ul style={{ listStyleType: 'none', padding: 0, margin: 0, fontSize: '14px', lineHeight: '1.6' }}>
                      {displaySubjects.map(sub => {
                        const data = detailedMarks[sub.name] || {};
                        if (data.remark && data.remark.trim() !== '') {
                          return (
                            <li key={`rem-${sub.name}`} style={{ marginBottom: '5px' }}>
                              <strong>{sub.name}:</strong> {data.remark}
                            </li>
                          );
                        }
                        return null;
                      })}
                    </ul>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '80px', fontSize: '14px' }}>
                    <div style={{ borderTop: '1px solid #000', paddingTop: '5px', width: '200px', textAlign: 'center' }}>Class Teacher Signature</div>
                    <div style={{ borderTop: '1px solid #000', paddingTop: '5px', width: '200px', textAlign: 'center' }}>Principal Signature</div>
                  </div>

                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
