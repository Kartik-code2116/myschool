import React, { useState, useEffect } from 'react';
import { collection, query, where, getDocs, addDoc } from 'firebase/firestore';
import { db, auth } from '../firebase';
import { useTeacherContext } from '../context/TeacherContext';
import './AppStudents.css';

export default function AppStudents() {
  const { activeClass } = useTeacherContext();
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isAdding, setIsAdding] = useState(false);
  
  // Form State
  const [newStudent, setNewStudent] = useState({
    name: '', rollNo: '', gender: 'Male'
  });

  const fetchStudents = async () => {
    if (!activeClass) {
      setStudents([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const q = query(collection(db, 'students'), where('classId', '==', activeClass.id));
      const snap = await getDocs(q);
      const stuData = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      // Sort by rollNo
      stuData.sort((a, b) => parseInt(a.rollNo || 0) - parseInt(b.rollNo || 0));
      setStudents(stuData);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStudents();
  }, [activeClass]);

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!activeClass || !auth.currentUser) return;
    
    try {
      const docData = {
        name: newStudent.name,
        rollNo: newStudent.rollNo,
        gender: newStudent.gender,
        classId: activeClass.id,
        teacherId: auth.currentUser.uid,
        className: activeClass.name,
      };
      await addDoc(collection(db, 'students'), docData);
      setIsAdding(false);
      setNewStudent({ name: '', rollNo: '', gender: 'Male' });
      fetchStudents();
    } catch (err) {
      console.error("Error adding student:", err);
    }
  };

  return (
    <div className="app-students">
      <div className="students-header">
        <h2>Students</h2>
        <button className="btn-add" onClick={() => setIsAdding(true)}>+ Add</button>
      </div>

      {!activeClass && (
        <div className="warning-banner">Please select an Active Class from the Dashboard.</div>
      )}

      {isAdding && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Add New Student</h3>
            <form onSubmit={handleAdd}>
              <div className="form-group">
                <label>Full Name</label>
                <input required type="text" value={newStudent.name} onChange={e => setNewStudent({...newStudent, name: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Roll Number</label>
                <input required type="number" value={newStudent.rollNo} onChange={e => setNewStudent({...newStudent, rollNo: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Gender</label>
                <select value={newStudent.gender} onChange={e => setNewStudent({...newStudent, gender: e.target.value})}>
                  <option>Male</option>
                  <option>Female</option>
                </select>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-cancel" onClick={() => setIsAdding(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Save Student</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="students-list">
        {loading ? (
          <p>Loading students...</p>
        ) : students.length === 0 ? (
          <div className="empty-state">No students found. Add one to get started.</div>
        ) : (
          students.map(stu => (
            <div key={stu.id} className="student-card">
              <div className="stu-avatar">{stu.name.charAt(0)}</div>
              <div className="stu-info">
                <h4>{stu.name}</h4>
                <p>Roll No: {stu.rollNo} | {stu.gender}</p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
