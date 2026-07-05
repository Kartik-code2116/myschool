import React from 'react';
import useLanguage from '../../../utils/useLanguage';

export default function IndexPageT2({ school, classInfo, year, semester, students }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    const title = isEn ? "Index" : "अनुक्रमणिका";
    const lblUdise = isEn ? "UDISE: " : "युडायस: ";
    const lblYear = isEn ? "Year: " : "सन: ";
    const lblSchool = isEn ? "School: " : "शाळा: ";
    const lblClass = isEn ? "Class: " : "इयत्ता: ";
    const lblDiv = isEn ? ", Division: " : ", तुकडी: ";
    
    const semText = semester?.number === 2 
        ? (isEn ? "Second Semester" : "द्वितीय सत्र")
        : (isEn ? "First Semester" : "प्रथम सत्र");

    const headers = [
        isEn ? "Sr.No." : "अ.क्र.",
        isEn ? "Student Name" : "विद्यार्थ्याचे नाव",
        isEn ? "Roll No." : "हजेरी क्र.",
        isEn ? "Birth Date" : "जन्मतारीख",
        isEn ? "Page No." : "पान क्र."
    ];

    // Splitting students into pages if there are too many for one A4 sheet
    const ROWS_PER_PAGE = 30; // Standard for A4
    const pages = [];
    for (let i = 0; i < students.length; i += ROWS_PER_PAGE) {
        pages.push(students.slice(i, i + ROWS_PER_PAGE));
    }

    if (pages.length === 0) {
        pages.push([]); // ensure at least one page renders
    }

    return (
        <div className="report-index-wrapper">
            {pages.map((pageStudents, pageIdx) => (
                <div key={pageIdx} className="rc-standard-page" style={{ 
                    pageBreakAfter: pageIdx < pages.length - 1 ? 'always' : 'auto',
                    padding: '20px 30px'
                }}>
                    
                    {/* Header: Title */}
                    <div style={{ textAlign: 'center', fontSize: '24px', fontWeight: 'bold', color: '#28285A', marginBottom: '20px' }}>
                        {title}
                    </div>

                    {/* Header Info Table */}
                    <div style={{ marginBottom: '20px', fontSize: '15px', fontWeight: 'bold' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                            <div style={{ width: '40%' }}>{lblUdise} {school?.udiseCode || ""}</div>
                            <div style={{ width: '20%', textAlign: 'center' }}>{semText}</div>
                            <div style={{ width: '40%', textAlign: 'right' }}>{lblYear} {year?.name || "2025-26"}</div>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                            <div style={{ width: '60%' }}>{lblSchool} {school?.name || ""}</div>
                            <div style={{ width: '40%', textAlign: 'right' }}>
                                {lblClass} {classInfo?.className || ""} {lblDiv} {classInfo?.division || "-"}
                            </div>
                        </div>
                    </div>

                    {/* Table */}
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
                        <thead>
                            <tr>
                                {headers.map((h, i) => (
                                    <th key={i} style={{
                                        backgroundColor: '#DAE9F5',
                                        border: '1px solid #333',
                                        padding: '10px 8px',
                                        textAlign: 'center',
                                        verticalAlign: 'middle',
                                        color: '#000',
                                        width: i === 0 ? '8%' : i === 1 ? '45%' : i === 2 ? '15%' : i === 3 ? '17%' : '15%'
                                    }}>
                                        {h}
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {pageStudents.map((s, idx) => {
                                const globalIdx = pageIdx * ROWS_PER_PAGE + idx;
                                const isAlt = globalIdx % 2 !== 0;
                                const bg = isAlt ? '#F5F7FA' : '#FFFFFF';
                                return (
                                    <tr key={s.id || idx} style={{ backgroundColor: bg }}>
                                        <td style={{ border: '1px solid #333', padding: '8px', textAlign: 'center' }}>{globalIdx + 1}</td>
                                        <td style={{ border: '1px solid #333', padding: '8px 12px' }}>{s.name}</td>
                                        <td style={{ border: '1px solid #333', padding: '8px', textAlign: 'center' }}>{s.rollNo}</td>
                                        <td style={{ border: '1px solid #333', padding: '8px', textAlign: 'center' }}>{s.dob || "-"}</td>
                                        <td style={{ border: '1px solid #333', padding: '8px', textAlign: 'center' }}>{globalIdx + 1}</td>
                                    </tr>
                                );
                            })}
                            
                            {/* Fill empty rows if needed to make it look full */}
                            {pageStudents.length < ROWS_PER_PAGE && Array.from({ length: Math.min(5, ROWS_PER_PAGE - pageStudents.length) }).map((_, idx) => {
                                const globalIdx = pageIdx * ROWS_PER_PAGE + pageStudents.length + idx;
                                const isAlt = globalIdx % 2 !== 0;
                                const bg = isAlt ? '#F5F7FA' : '#FFFFFF';
                                return (
                                    <tr key={`empty-${idx}`} style={{ backgroundColor: bg }}>
                                        <td style={{ border: '1px solid #333', padding: '8px', height: '35px' }}></td>
                                        <td style={{ border: '1px solid #333', padding: '8px' }}></td>
                                        <td style={{ border: '1px solid #333', padding: '8px' }}></td>
                                        <td style={{ border: '1px solid #333', padding: '8px' }}></td>
                                        <td style={{ border: '1px solid #333', padding: '8px' }}></td>
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
