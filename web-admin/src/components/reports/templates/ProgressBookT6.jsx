import React from 'react';
import useLanguage from '../../../utils/useLanguage';

const sanitizeKey = (key) => {
    if (!key) return "unknown";
    return key.replace(/[\.#\$\[\]\/\\~\*]/g, "_");
};

export default function ProgressBookT6({ school, classInfo, year, semester, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    const subjects = classInfo?.subjects || [];

    // Split students for pagination (approx 25 per page for A4 Landscape)
    const ROWS_PER_PAGE = 25; 
    const pages = [];
    for (let i = 0; i < students.length; i += ROWS_PER_PAGE) {
        pages.push(students.slice(i, i + ROWS_PER_PAGE));
    }
    if (pages.length === 0) pages.push([]);

    return (
        <div className="report-progressbook-wrapper">
            {/* Adding landscape print style explicitly for this report */}
            <style>
                {`
                @media print {
                    @page { size: A4 landscape; margin: 10mm; }
                }
                `}
            </style>
            
            {pages.map((pageStudents, pageIdx) => (
                <div key={pageIdx} className="rc-standard-page" style={{ 
                    pageBreakAfter: pageIdx < pages.length - 1 ? 'always' : 'auto',
                    padding: '15px',
                    minHeight: '210mm', // Landscape A4 height
                    width: '297mm', // Landscape A4 width
                    margin: '0 auto'
                }}>
                    
                    {/* Title */}
                    <div style={{ textAlign: 'center', fontSize: '18px', fontWeight: 'bold', color: '#111', marginBottom: '15px' }}>
                        {isEn ? "Continuous Comprehensive Evaluation" : "सातत्यपूर्ण सर्वंकष मूल्यमापन"}
                    </div>

                    {/* Header Info Table */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '15px', fontSize: '12px', fontWeight: 'bold', color: '#111' }}>
                        <div style={{ width: '33%' }}>
                            <div>{isEn ? "UDISE: " : "युडायस: "}{school?.udiseCode || "-"}</div>
                            <div>{isEn ? "School: " : "शाळेचे नाव: "}{school?.name || "-"}</div>
                        </div>
                        <div style={{ width: '33%', textAlign: 'center', display: 'flex', alignItems: 'flex-end', justifyContent: 'center' }}>
                            <div style={{ fontSize: '16px' }}>
                                {semester?.number === 2 ? (isEn ? "Second Semester" : "द्वितीय सत्र") : (isEn ? "First Semester" : "प्रथम सत्र")}
                            </div>
                        </div>
                        <div style={{ width: '33%', textAlign: 'right' }}>
                            <div>{isEn ? "Year: " : "शैक्षणिक वर्ष: "}{classInfo?.academicYearLabel || "-"}</div>
                            <div>{isEn ? "Class: " : "इयत्ता: "}{classInfo?.className} {isEn ? ", Div: " : ", तुकडी: "}{classInfo?.division || "-"}</div>
                        </div>
                    </div>

                    {/* Main Table */}
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '9px' }}>
                        <thead>
                            <tr>
                                <th rowSpan="3" style={{...thStyle, width: '3%'}}>{isEn ? "Sr." : "अ.नं."}</th>
                                <th rowSpan="3" style={{...thStyle, width: '12%'}}>{isEn ? "Name of Student" : "विद्यार्थ्याचे नाव"}</th>
                                {subjects.map((sub, i) => {
                                    const summativeMax = (sub.maxTondi||0) + (sub.maxPratyakshikB||0) + (sub.maxLekhi||0);
                                    const isNonAcademic = (summativeMax === 0 && sub.maxMarks > 0);
                                    return (
                                        <th key={i} colSpan={isNonAcademic ? 2 : 4} style={thStyle}>{sub.name}</th>
                                    );
                                })}
                            </tr>
                            <tr>
                                {subjects.map((sub, i) => {
                                    const summativeMax = (sub.maxTondi||0) + (sub.maxPratyakshikB||0) + (sub.maxLekhi||0);
                                    const isNonAcademic = (summativeMax === 0 && sub.maxMarks > 0);
                                    if (isNonAcademic) {
                                        return (
                                            <React.Fragment key={`sub-hdr-${i}`}>
                                                <th style={thStyle}>{isEn ? "Formative" : "आकारिक"}</th>
                                                <th style={thStyle}>{isEn ? "Grade" : "श्रेणी"}</th>
                                            </React.Fragment>
                                        );
                                    } else {
                                        return (
                                            <React.Fragment key={`sub-hdr-${i}`}>
                                                <th style={thStyle}>{isEn ? "Formative" : "आकारिक"}</th>
                                                <th style={thStyle}>{isEn ? "Summative" : "संकलित"}</th>
                                                <th style={thStyle}>{isEn ? "Total" : "एकूण (अ+ब)"}</th>
                                                <th style={thStyle}>{isEn ? "Grade" : "श्रेणी"}</th>
                                            </React.Fragment>
                                        );
                                    }
                                })}
                            </tr>
                            <tr>
                                {subjects.map((sub, i) => {
                                    const summativeMax = (sub.maxTondi||0) + (sub.maxPratyakshikB||0) + (sub.maxLekhi||0);
                                    const isNonAcademic = (summativeMax === 0 && sub.maxMarks > 0);
                                    let akarikMaxVal = Math.floor((sub.maxMarks||0) / 2);
                                    let sanklitMaxVal = (sub.maxMarks||0) - akarikMaxVal;
                                    
                                    if (isNonAcademic) {
                                        return (
                                            <React.Fragment key={`sub-max-${i}`}>
                                                <th style={thStyle2}>{sub.maxMarks}</th>
                                                <th style={thStyle2}></th>
                                            </React.Fragment>
                                        );
                                    } else {
                                        return (
                                            <React.Fragment key={`sub-max-${i}`}>
                                                <th style={thStyle2}>{akarikMaxVal}</th>
                                                <th style={thStyle2}>{sanklitMaxVal}</th>
                                                <th style={thStyle2}>{sub.maxMarks}</th>
                                                <th style={thStyle2}></th>
                                            </React.Fragment>
                                        );
                                    }
                                })}
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
                                            const summativeMax = (sub.maxTondi||0) + (sub.maxPratyakshikB||0) + (sub.maxLekhi||0);
                                            const isNonAcademic = (summativeMax === 0 && sub.maxMarks > 0);
                                            
                                            const d = marksData[sanitizeKey(sub.name)];
                                            
                                            if (d) {
                                                const aStr = d.akarikTotal > 0 ? d.akarikTotal : "";
                                                const sStr = d.sanklit > 0 ? d.sanklit : "";
                                                const gTot = d.grandTotal > 0 ? d.grandTotal : ((d.akarikTotal > 0 || d.sanklit > 0) ? (d.akarikTotal + d.sanklit) : "");
                                                const grade = d.grade || "";

                                                if (isNonAcademic) {
                                                    return (
                                                        <React.Fragment key={`data-${i}`}>
                                                            <td style={{...tdStyle, textAlign: 'center'}}>{aStr}</td>
                                                            <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{grade}</td>
                                                        </React.Fragment>
                                                    );
                                                } else {
                                                    return (
                                                        <React.Fragment key={`data-${i}`}>
                                                            <td style={{...tdStyle, textAlign: 'center'}}>{aStr}</td>
                                                            <td style={{...tdStyle, textAlign: 'center'}}>{sStr}</td>
                                                            <td style={{...tdStyle, textAlign: 'center'}}>{gTot}</td>
                                                            <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{grade}</td>
                                                        </React.Fragment>
                                                    );
                                                }
                                            } else {
                                                if (isNonAcademic) {
                                                    return (
                                                        <React.Fragment key={`data-${i}`}>
                                                            <td style={{...tdStyle, textAlign: 'center', color: '#aaa'}}>-</td>
                                                            <td style={{...tdStyle, textAlign: 'center', color: '#aaa'}}>-</td>
                                                        </React.Fragment>
                                                    );
                                                } else {
                                                    return (
                                                        <React.Fragment key={`data-${i}`}>
                                                            <td style={{...tdStyle, textAlign: 'center', color: '#aaa'}}>-</td>
                                                            <td style={{...tdStyle, textAlign: 'center', color: '#aaa'}}>-</td>
                                                            <td style={{...tdStyle, textAlign: 'center', color: '#aaa'}}>-</td>
                                                            <td style={{...tdStyle, textAlign: 'center', color: '#aaa'}}>-</td>
                                                        </React.Fragment>
                                                    );
                                                }
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
    padding: '3px',
    textAlign: 'center',
    fontWeight: 'bold'
};

const thStyle2 = { ...thStyle, backgroundColor: '#E8F5E9' };

const tdStyle = {
    border: '1px solid #111',
    padding: '4px',
    color: '#111',
    verticalAlign: 'middle'
};
