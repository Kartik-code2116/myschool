import React, { createContext, useContext, useState, useEffect } from 'react';
import { auth, db } from '../firebase';
import { onAuthStateChanged } from 'firebase/auth';
import { collection, query, where, getDocs, doc, getDoc } from 'firebase/firestore';

const TeacherContext = createContext();

export function TeacherProvider({ children }) {
  const [activeAcademicYear, setActiveAcademicYear] = useState(null);
  const [activeSemester, setActiveSemester] = useState(null);
  const [activeClass, setActiveClass] = useState(null);
  const [activeSchool, setActiveSchool] = useState(null);
  const [teacherProfile, setTeacherProfile] = useState(null);

  const [academicYearsList, setAcademicYearsList] = useState([]);
  const [semestersList, setSemestersList] = useState([]);
  
  const [loadingContext, setLoadingContext] = useState(true);

  // Load saved state from local storage on init
  useEffect(() => {
    const savedYear = localStorage.getItem('teacher_active_year');
    const savedSem = localStorage.getItem('teacher_active_semester');
    const savedClass = localStorage.getItem('teacher_active_class');
    const savedSchool = localStorage.getItem('teacher_active_school');
    
    if (savedYear) setActiveAcademicYear(JSON.parse(savedYear));
    if (savedSem) setActiveSemester(JSON.parse(savedSem));
    if (savedClass) setActiveClass(JSON.parse(savedClass));
    if (savedSchool) setActiveSchool(JSON.parse(savedSchool));
  }, []);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (user) {
        setLoadingContext(true);
        try {
          // 1. Fetch School
          const qSchool = query(collection(db, 'schools'), where('teacherId', '==', user.uid));
          const snapSchool = await getDocs(qSchool);
          if (!snapSchool.empty) {
             const school = { id: snapSchool.docs[0].id, ...snapSchool.docs[0].data() };
             setActiveSchool(school);
             localStorage.setItem('teacher_active_school', JSON.stringify(school));
          }

          // 1.5 Fetch Teacher Profile (for subscription status)
          const docRef = doc(db, 'teachers', user.uid);
          const docSnap = await getDoc(docRef);
          if (docSnap.exists()) {
            setTeacherProfile({ id: docSnap.id, ...docSnap.data() });
          }

          // 2. Fetch Academic Years
          const qYears = query(collection(db, 'academic_years'), where('teacherId', '==', user.uid));
          const snapYears = await getDocs(qYears);
          const years = snapYears.docs.map(d => ({ id: d.id, name: d.data().label || d.data().year, ...d.data() }));
          years.sort((a,b) => (b.startYear || 0) - (a.startYear || 0)); // Descending
          setAcademicYearsList(years);

          // 3. Fetch Semesters
          const qSems = query(collection(db, 'semesters'), where('teacherId', '==', user.uid));
          const snapSems = await getDocs(qSems);
          const sems = snapSems.docs.map(d => ({ id: d.id, name: d.data().name, ...d.data() }));
          sems.sort((a,b) => (a.number || 0) - (b.number || 0)); // Ascending
          setSemestersList(sems);
          
        } catch(err) {
          console.error("Error loading teacher context:", err);
        } finally {
          setLoadingContext(false);
        }
      } else {
        setActiveSchool(null);
        setTeacherProfile(null);
        setAcademicYearsList([]);
        setSemestersList([]);
        setLoadingContext(false);
      }
    });
    return () => unsubscribe();
  }, []);

  const setYear = (year) => {
    setActiveAcademicYear(year);
    if (year) {
      localStorage.setItem('teacher_active_year', JSON.stringify(year));
    } else {
      localStorage.removeItem('teacher_active_year');
    }
  };

  const setSemester = (semester) => {
    setActiveSemester(semester);
    if (semester) {
      localStorage.setItem('teacher_active_semester', JSON.stringify(semester));
    } else {
      localStorage.removeItem('teacher_active_semester');
    }
  };

  const setClass = (cls) => {
    setActiveClass(cls);
    if (cls) {
      localStorage.setItem('teacher_active_class', JSON.stringify(cls));
    } else {
      localStorage.removeItem('teacher_active_class');
    }
  };

  return (
    <TeacherContext.Provider value={{
      activeAcademicYear, setActiveAcademicYear: setYear,
      activeSemester, setActiveSemester: setSemester,
      activeClass, setActiveClass: setClass,
      activeSchool, teacherProfile, academicYearsList, semestersList, loadingContext
    }}>
      {children}
    </TeacherContext.Provider>
  );
}

export function useTeacherContext() {
  return useContext(TeacherContext);
}
