import React from 'react';
import useLanguage from '../../../utils/useLanguage';

const sanitizeKey = (key) => {
    if (!key) return "unknown";
    return key.replace(/[\.#\$\[\]\/\\~\*]/g, "_");
};

export default function GradeChartT5({ school, classInfo, year, semester, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    // The Android app limits to 9 subjects for Portrait A4 fit
    const allSubjects = classInfo?.subjects || [];
    const subjects = allSubjects.slice(0, 9); 

    // Split students for pagination (approx 35 per page for A4 Portrait)
    const ROWS_PER_PAGE = 35; 
    const pages = [];
    for (let i = 0; i < students.length; i += ROWS_PER_PAGE) {
        pages.push(students.slice(i, i + ROWS_PER_PAGE));
    }
    if (pages.length === 0) pages.push([]);

    return (
        <div className="report-gradechart-wrapper">
            {pages.map((pageStudents, pageIdx) => (
                <div key={pageIdx} className="rc-standard-page" style={{ 
                    pageBreakAfter: pageIdx < pages.length - 1 ? 'always' : 'auto',
                    padding: '20px 15px' // Narrow margins
                }}>
                    
                    {/* Title */}
                    <div style={{ textAlign: 'center', fontSize: '18px', fontWeight: 'bold', color: '#111', marginBottom: '15px' }}>
                        {isEn ? "Continuous Comprehensive Evaluation" : "सातत्यपूर्ण सर्वंकष मूल्यमापन"}
                    </div>

                    {/* Header Info Table */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '15px', fontSize: '12px', fontWeight: 'bold', color: '#111' }}>
                        <div style={{ width: '33%' }}>
                            <div>{isEn ? "UDISE: " : "युडायस: "}{school?.udiseCode || "-"}</div>
                            <div>{school?.name || "-"}</div>
                        </div>
                        <div style={{ width: '33%', textAlign: 'center', display: 'flex', alignItems: 'flex-end', justifyContent: 'center' }}>
                            <div style={{ fontSize: '14px' }}>
                                {semester?.number === 2 ? (isEn ? "Second Semester" : "द्वितीय सत्र") : (isEn ? "First Semester" : "प्रथम सत्र")}
                            </div>
                        </div>
                        <div style={{ width: '33%', textAlign: 'right' }}>
                            <div>{isEn ? "Year: " : "सन : "}{classInfo?.academicYearLabel || "-"}</div>
                            <div>{isEn ? "Class: " : "इयत्ता: "}{classInfo?.className} {isEn ? ", Div: " : ", तुकडी: "}{classInfo?.division || "-"}</div>
                        </div>
                    </div>

                    {/* Main Table */}
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '10px' }}>
                        <thead>
                            <tr>
                                <th rowSpan="2" style={{...thStyle, width: '4%'}}>{isEn ? "Sr.No" : "अ.नं"}</th>
                                <th rowSpan="2" style={{...thStyle, width: '16%'}}>{isEn ? "Student Name" : "विद्यार्थ्याचे नाव"}</th>
                                {subjects.map((sub, i) => (
                                    <th key={i} colSpan="2" style={{...thStyle, width: `${80/subjects.length}%`}}>{sub.name}</th>
                                ))}
                            </tr>
                            <tr>
                                {subjects.map((sub, i) => (
                                    <React.Fragment key={`sub-hdr-${i}`}>
                                        <th style={thStyle}>{isEn ? "Marks" : "गुण"}</th>
                                        <th style={thStyle}>{isEn ? "Grade" : "श्रेणी"}</th>
                                    </React.Fragment>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {pageStudents.map((student, idx) => {
                                const globalIdx = pageIdx * ROWS_PER_PAGE + idx;
                                const marksRecord = semester?.number === 2 ? marksSem2[student.id] : marksSem1[student.id];
                                const marksData = marksRecord?.detailedMarks || {};
                                const bg = globalIdx % 2 === 0 ? '#F8F9FA' : '#FFF';

                                return (
                                    <tr key={student.id} style={{ backgroundColor: bg }}>
                                        <td style={{...tdStyle, textAlign: 'center'}}>{globalIdx + 1}</td>
                                        <td style={tdStyle}>{student.name}</td>
                                        
                                        {subjects.map((sub, i) => {
                                            const d = marksData[sanitizeKey(sub.name)];
                                            if (d) {
                                                return (
                                                    <React.Fragment key={`data-${i}`}>
                                                        <td style={{...tdStyle, textAlign: 'center'}}>{d.grandTotal || "-"}</td>
                                                        <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{d.grade || "-"}</td>
                                                    </React.Fragment>
                                                );
                                            } else {
                                                return (
                                                    <React.Fragment key={`data-${i}`}>
                                                        <td style={{...tdStyle, textAlign: 'center', color: '#757575'}}>-</td>
                                                        <td style={{...tdStyle, textAlign: 'center', color: '#757575'}}>-</td>
                                                    </React.Fragment>
                                                );
                                            }
                                        })}
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>

                </div>
            ))}
        </div>
    );
}

const thStyle = {
    backgroundColor: '#ECEFF1',
    color: '#111',
    border: '1px solid #111',
    padding: '4px',
    textAlign: 'center',
    fontWeight: 'bold'
};

const tdStyle = {
    border: '1px solid #111',
    padding: '4px',
    color: '#111',
    verticalAlign: 'middle'
};
