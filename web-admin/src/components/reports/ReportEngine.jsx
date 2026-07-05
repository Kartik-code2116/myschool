import React, { useState, useEffect, useRef } from 'react';
import { useTeacherContext } from '../../context/TeacherContext';
import { db, auth } from '../../firebase';
import { collection, query, where, getDocs } from 'firebase/firestore';
import { useReactToPrint } from 'react-to-print';
import useLanguage from '../../utils/useLanguage';

// Templates
import CoverPageT1 from './templates/CoverPageT1';
import IndexPageT2 from './templates/IndexPageT2';
import ProgressCardCoverT9 from './templates/ProgressCardCoverT9';
import MarksRegisterT3 from './templates/MarksRegisterT3';
import DescriptiveRemarksT4 from './templates/DescriptiveRemarksT4';
import GradeChartT5 from './templates/GradeChartT5';
import ProgressBookT6 from './templates/ProgressBookT6';
import RosterGradeTableT7 from './templates/RosterGradeTableT7';
import MarksGradeLedgerT8 from './templates/MarksGradeLedgerT8';
import ProgressCardInnerT10 from './templates/ProgressCardInnerT10';
import SubjectRegisterT11 from './templates/SubjectRegisterT11';
import AnnualMarksheetT12 from './templates/AnnualMarksheetT12';
import ResultSheetT13 from './templates/ResultSheetT13';
import ProgressPortraitT14 from './templates/ProgressPortraitT14';
import ProgressBookCombinedT16 from './templates/ProgressBookCombinedT16';
import CasteGradeTableT17 from './templates/CasteGradeTableT17';
import ProgressFirstSemT18 from './templates/ProgressFirstSemT18';
import HPCT19 from './templates/HPCT19';
// (More will be imported here as we build them)

export default function ReportEngine({ reportTemplate, onBack }) {
    const { activeSchool, activeClass, activeAcademicYear, activeSemester } = useTeacherContext();
    const { t } = useLanguage();
    
    const [students, setStudents] = useState([]);
    const [selectedStudent, setSelectedStudent] = useState(null);
    const [allMarksSem1, setAllMarksSem1] = useState({});
    const [allMarksSem2, setAllMarksSem2] = useState({});
    
    const [loading, setLoading] = useState(true);
    const componentRef = useRef();

    useEffect(() => {
        async function loadData() {
            if (!activeClass || !auth.currentUser) return;
            setLoading(true);
            try {
                // 1. Load Students
                const qStu = query(collection(db, 'students'), 
                    where('classId', '==', activeClass.id),
                    where('teacherId', '==', auth.currentUser.uid)
                );
                const snapStu = await getDocs(qStu);
                const stuList = snapStu.docs.map(doc => ({ id: doc.id, ...doc.data() }));
                stuList.sort((a,b) => parseInt(a.rollNo || 0) - parseInt(b.rollNo || 0));
                setStudents(stuList);

                // 2. We need Semester IDs. For robust reports, we pull marks for Sem 1 and Sem 2.
                // In a perfect system, we query marks by semesterNumber or semesterId.
                const qMarks = query(collection(db, 'marks'), 
                    where('classId', '==', activeClass.id),
                    where('teacherId', '==', auth.currentUser.uid)
                );
                const snapMarks = await getDocs(qMarks);
                
                const s1 = {};
                const s2 = {};
                
                snapMarks.docs.forEach(doc => {
                    const data = { id: doc.id, ...doc.data() };
                    let recSemNum = parseInt(data.semesterNumber) || 1;
                    // If no semester ID but data exists, fallback to Sem 1
                    if (!data.semesterId && !data.semesterNumber) recSemNum = 1;
                    
                    if (recSemNum === 1) {
                        if (!s1[data.studentId] || data.updatedAt > s1[data.studentId].updatedAt) {
                            s1[data.studentId] = data;
                        }
                    } else if (recSemNum === 2) {
                        if (!s2[data.studentId] || data.updatedAt > s2[data.studentId].updatedAt) {
                            s2[data.studentId] = data;
                        }
                    }
                });

                setAllMarksSem1(s1);
                setAllMarksSem2(s2);
                
            } catch(err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        }
        loadData();
    }, [activeClass]);

    const handlePrint = useReactToPrint({
        contentRef: componentRef,
        documentTitle: `Report_${reportTemplate.id}_${activeClass?.className}`,
    });

    // Decide which template to render
    const renderTemplate = () => {
        const props = {
            school: activeSchool,
            classInfo: activeClass,
            year: activeAcademicYear,
            semester: activeSemester,
            students: selectedStudent ? [selectedStudent] : students, // Array of target students
            marksSem1: allMarksSem1,
            marksSem2: allMarksSem2
        };

        switch (reportTemplate.id) {
            case 1: return <CoverPageT1 {...props} />;
            case 2: return <IndexPageT2 {...props} />;
            case 3: return <MarksRegisterT3 {...props} />;
            case 4: return <DescriptiveRemarksT4 {...props} />;
            case 5: return <GradeChartT5 {...props} />;
            case 6: return <ProgressBookT6 {...props} />;
            case 7: return <RosterGradeTableT7 {...props} />;
            case 8: return <MarksGradeLedgerT8 {...props} />;
            case 9: return <ProgressCardCoverT9 {...props} />;
            case 10: return <ProgressCardInnerT10 {...props} />;
            case 11: return <SubjectRegisterT11 {...props} />;
            case 12: return <AnnualMarksheetT12 {...props} />;
            case 13: return <ResultSheetT13 {...props} />;
            case 14: return <ProgressPortraitT14 {...props} />;
            case 15: return <ProgressBookT6 {...props} />; // 15 uses same structure as 6 (CCE Alt)
            case 16: return <ProgressBookCombinedT16 {...props} />;
            case 17: return <CasteGradeTableT17 {...props} />;
            case 18: return <ProgressFirstSemT18 {...props} />;
            case 19: return <HPCT19 {...props} />;
            // Add more cases as we build them...
            default: 
                return (
                    <div style={{ padding: '50px', textAlign: 'center', color: 'red' }}>
                        <h3>Template {reportTemplate.id} ({reportTemplate.title}) Not Implemented Yet</h3>
                        <p>Currently in development.</p>
                    </div>
                );
        }
    };

    return (
        <div className="report-engine animate-fade-in">
            <div className="engine-header">
                <button className="btn-back" onClick={onBack}>← Back</button>
                <h2 style={{ margin: 0 }}>{reportTemplate.title}</h2>
            </div>

            <div className="engine-layout">
                <div className="engine-sidebar">
                    <h3 style={{ marginTop: 0 }}>Select Target</h3>
                    <ul className="student-list-compact">
                        <li 
                            className={selectedStudent === null ? 'active' : ''}
                            onClick={() => setSelectedStudent(null)}
                        >
                            📚 Whole Class (All Students)
                        </li>
                    </ul>
                    
                    <h4 style={{ margin: '15px 0 10px 0', fontSize: '13px', color: 'var(--text-secondary)' }}>Specific Student</h4>
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

                <div className="engine-viewer">
                    <div className="viewer-header">
                        <div>
                            <h3 style={{ margin: '0 0 5px 0' }}>Preview</h3>
                            <p style={{ margin: 0, fontSize: '13px', color: 'var(--text-secondary)' }}>
                                Targeting: {selectedStudent ? selectedStudent.name : 'Whole Class'}
                            </p>
                        </div>
                        <button className="btn btn-primary" onClick={handlePrint} disabled={loading}>
                            🖨️ Print to PDF
                        </button>
                    </div>

                    <div className="printable-wrapper">
                        {loading ? <p>Loading data...</p> : (() => {
                            const landscapeIds = [3, 4, 5, 6, 7, 8, 9, 10, 13, 15, 16, 17, 18];
                            const isLandscape = landscapeIds.includes(reportTemplate.id);
                            
                            return (
                                <div className={`printable-a4 ${isLandscape ? 'landscape' : ''}`} ref={componentRef}>
                                    {renderTemplate()}
                                </div>
                            );
                        })()}
                    </div>
                </div>
            </div>
        </div>
    );
}
