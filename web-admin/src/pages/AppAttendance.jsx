import React, { useState, useEffect } from 'react';
import { useTeacherContext } from '../context/TeacherContext';
import { db, auth } from '../firebase';
import { collection, query, where, getDocs, doc, setDoc } from 'firebase/firestore';
import './AppAttendance.css';

const MONTHS = ["जून", "जुलै", "ऑगस्ट", "सप्टें", "ऑक्टो", "नोव्हे", "डिसें", "जाने", "फेब्रु", "मार्च", "एप्रिल", "मे"];

export default function AppAttendance() {
  const { activeClass, activeAcademicYear } = useTeacherContext();
  const [students, setStudents] = useState([]);
  const [attendanceRecords, setAttendanceRecords] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  
  const [selectedMonth, setSelectedMonth] = useState(MONTHS[0]);
  const [workingDays, setWorkingDays] = useState(24);

  useEffect(() => {
    async function loadData() {
      if (!activeClass || !activeAcademicYear) {
        setLoading(false);
        return;
      }
      setLoading(true);
      try {
        // Load Students
        const qStu = query(collection(db, 'students'), where('classId', '==', activeClass.id));
        const snapStu = await getDocs(qStu);
        const stuList = snapStu.docs.map(d => ({ id: d.id, ...d.data() }));
        stuList.sort((a,b) => parseInt(a.rollNo||0) - parseInt(b.rollNo||0));
        setStudents(stuList);

        // Load Attendance Records
        const qAtt = query(collection(db, 'attendance_records'), 
            where('classId', '==', activeClass.id),
            where('academicYear', '==', activeAcademicYear.name || activeAcademicYear.id)
        );
        const snapAtt = await getDocs(qAtt);
        const attMap = {};
        snapAtt.forEach(d => {
            const data = d.data();
            attMap[data.studentId] = data;
        });
        setAttendanceRecords(attMap);

      } catch(e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [activeClass, activeAcademicYear]);

  const handlePresentChange = (studentId, presentVal) => {
    const val = parseInt(presentVal) || 0;
    setAttendanceRecords(prev => {
        const record = prev[studentId] || {
            studentId, classId: activeClass.id, academicYear: activeAcademicYear.name || activeAcademicYear.id, monthlyData: {}
        };
        const newData = { ...record.monthlyData, [selectedMonth]: `${val}/${workingDays}` };
        return { ...prev, [studentId]: { ...record, monthlyData: newData } };
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
        for (const stu of students) {
            const record = attendanceRecords[stu.id];
            if (record) {
                // Ensure default values for other months exist if new record
                if (!record.id) {
                    MONTHS.forEach(m => {
                        if (!record.monthlyData[m]) record.monthlyData[m] = "0/0";
                    });
                }
                const docRef = record.id ? doc(db, 'attendance_records', record.id) : doc(collection(db, 'attendance_records'));
                await setDoc(docRef, {
                    id: docRef.id,
                    studentId: stu.id,
                    classId: activeClass.id,
                    academicYear: activeAcademicYear.name || activeAcademicYear.id,
                    monthlyData: record.monthlyData
                }, { merge: true });
                record.id = docRef.id;
            }
        }
        alert("Attendance saved successfully!");
    } catch (e) {
        console.error(e);
        alert("Failed to save.");
    } finally {
        setSaving(false);
    }
  };

  if (!activeClass || !activeAcademicYear) {
      return <div className="app-attendance"><div className="warning-banner">Please select an Active Class and Academic Year from Settings.</div></div>;
  }

  return (
    <div className="app-attendance">
      <div className="attendance-header">
        <h2>Attendance</h2>
        <button className="btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving...' : 'Save All'}
        </button>
      </div>
      
      <div className="attendance-controls card-panel">
        <div className="form-group">
            <label>Select Month</label>
            <select value={selectedMonth} onChange={e => setSelectedMonth(e.target.value)}>
                {MONTHS.map(m => <option key={m} value={m}>{m}</option>)}
            </select>
        </div>
        <div className="form-group">
            <label>Total Working Days</label>
            <input type="number" value={workingDays} onChange={e => setWorkingDays(parseInt(e.target.value)||0)} />
        </div>
      </div>

      <div className="attendance-list">
        {loading ? <p>Loading...</p> : (
            students.map(stu => {
                const record = attendanceRecords[stu.id];
                const monthVal = record?.monthlyData?.[selectedMonth] || `0/${workingDays}`;
                const presentDays = monthVal.split('/')[0] || "0";
                
                return (
                    <div key={stu.id} className="attendance-item card-panel">
                        <div className="stu-info">
                            <strong>{stu.rollNo}. {stu.name}</strong>
                        </div>
                        <div className="input-group">
                            <input 
                                type="number" 
                                value={presentDays} 
                                onChange={e => handlePresentChange(stu.id, e.target.value)} 
                                max={workingDays}
                                min="0"
                            />
                            <span>/ {workingDays}</span>
                        </div>
                    </div>
                );
            })
        )}
      </div>
    </div>
  );
}
