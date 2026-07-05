import React, { createContext, useContext, useState, useEffect } from 'react';

const TeacherContext = createContext();

export function TeacherProvider({ children }) {
  const [activeAcademicYear, setActiveAcademicYear] = useState(null);
  const [activeSemester, setActiveSemester] = useState(null);
  const [activeClass, setActiveClass] = useState(null);
  
  // Load saved state from local storage on init
  useEffect(() => {
    const savedYear = localStorage.getItem('teacher_active_year');
    const savedSem = localStorage.getItem('teacher_active_semester');
    const savedClass = localStorage.getItem('teacher_active_class');
    
    if (savedYear) setActiveAcademicYear(JSON.parse(savedYear));
    if (savedSem) setActiveSemester(JSON.parse(savedSem));
    if (savedClass) setActiveClass(JSON.parse(savedClass));
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
      activeClass, setActiveClass: setClass
    }}>
      {children}
    </TeacherContext.Provider>
  );
}

export function useTeacherContext() {
  return useContext(TeacherContext);
}
