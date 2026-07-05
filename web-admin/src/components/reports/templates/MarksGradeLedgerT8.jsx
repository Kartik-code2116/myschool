import React from 'react';
import useLanguage from '../../../utils/useLanguage';

const sanitizeKey = (key) => {
    if (!key) return "unknown";
    return key.replace(/[\.#\$\[\]\/\\~\*]/g, "_");
};

// Normalize grade matching logic
const normalizeGrade = (g) => {
    if (!g) return "";
    let s = g.trim().toUpperCase().replace(/ /g, "").replace(/-/g, "");
    s = s.replace("१", "1").replace("२", "2");

    const mapping = {
        "अ1": "A-1", "अ2": "A-2", "ब1": "B-1", "ब2": "B-2", 
        "क1": "C-1", "क2": "C-2", "ड": "D", "इ1": "E-1", "ई1": "E-1", "इ2": "E-2", "ई2": "E-2",
        "A1": "A-1", "A2": "A-2", "B1": "B-1", "B2": "B-2",
        "C1": "C-1", "C2": "C-2", "D": "D", "E1": "E-1", "E2": "E-2"
    };
    return mapping[s] || g;
};

export default function MarksGradeLedgerT8({ school, classInfo, year, semester, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    const allSubjects = classInfo?.subjects || [];
    const subjects = allSubjects.slice(0, 9); // Max 9 subjects to fit layout
    
    const GRADES = ["A-1", "A-2", "B-1", "B-2", "C-1", "C-2", "D", "E-1", "E-2"];
    const GRADE_LABELS_MR = ["अ-1", "अ-2", "ब-1", "ब-2", "क-1", "क-2", "ड", "इ-1", "इ-2"];

    // Split students for pagination (approx 20 per page for A4 Landscape with bottom table)
    const ROWS_PER_PAGE = 20; 
    const pages = [];
    for (let i = 0; i < students.length; i += ROWS_PER_PAGE) {
        pages.push(students.slice(i, i + ROWS_PER_PAGE));
    }
    if (pages.length === 0) pages.push([]);

    // We calculate the grade counts globally for the class (to show at the end of the report)
    const globalGradeCounts = subjects.map(() => GRADES.map(() => 0));

    students.forEach(st => {
        const marksRecord = semester?.number === 2 ? marksSem2[st.id] : marksSem1[st.id];
        const details = marksRecord?.detailedMarks || {};
        
        subjects.forEach((sub, si) => {
            const d = details[sanitizeKey(sub.name)];
            if (d && d.grade) {
                const normG = normalizeGrade(d.grade);
                for (let gi = 0; gi < GRADES.length; gi++) {
                    if (GRADES[gi] === normG) {
                        globalGradeCounts[si][gi]++;
                        break;
                    }
                }
            }
        });
    });

    return (
        <div className="report-ledger-wrapper">
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
                    padding: '12px', minHeight: '210mm', width: '297mm', margin: '0 auto' 
                }}>
                    
                    {/* Title */}
                    <div style={{ textAlign: 'center', fontSize: '15px', fontWeight: 'bold', color: '#111', marginBottom: '10px' }}>
                        {isEn ? "Continuous Comprehensive Evaluation" : "सातत्यपूर्ण सर्वंकष मूल्यमापन"}
                    </div>

                    {/* Header Info Table */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '11px', fontWeight: 'bold', color: '#111' }}>
                        <div style={{ width: '33%' }}>
                            <div>{isEn ? "UDISE: " : "युडायस: "}{school?.udiseCode || "-"}</div>
                            <div>{isEn ? "School: " : "शाळा: "}{school?.name || "-"}</div>
                        </div>
                        <div style={{ width: '33%', textAlign: 'center', display: 'flex', alignItems: 'flex-end', justifyContent: 'center' }}>
                            <div style={{ fontSize: '14px' }}>
                                {semester?.number === 2 ? (isEn ? "Second Semester" : "द्वितीय सत्र") : (isEn ? "First Semester" : "प्रथम सत्र")}
                            </div>
                        </div>
                        <div style={{ width: '33%', textAlign: 'right' }}>
                            <div>{isEn ? "Year: " : "सन: "}{classInfo?.academicYearLabel || "-"}</div>
                            <div>{isEn ? "Class: " : "इयत्ता: "}{classInfo?.className} {isEn ? ", Div: " : ", तुकडी: "}{classInfo?.division || "-"}</div>
                        </div>
                    </div>

                    {/* Top Main Table */}
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '10px' }}>
                        <thead>
                            <tr>
                                <th rowSpan="2" style={{...thStyle, width: '3%'}}>{isEn ? "Sr" : "अ.नं"}</th>
                                <th rowSpan="2" style={{...thStyle, width: '14%'}}>{isEn ? "Student Name" : "विद्यार्थ्यांचे नाव"}</th>
                                <th rowSpan="2" style={{...thStyle, width: '4%'}}>{isEn ? "Gender" : "लिंग"}</th>
                                <th rowSpan="2" style={{...thStyle, width: '5%'}}>{isEn ? "Attd." : "हजर दिवस"}</th>
                                {subjects.map((sub, i) => (
                                    <th key={i} colSpan="2" style={thStyle}>{sub.name}</th>
                                ))}
                            </tr>
                            <tr>
                                {subjects.map((_, i) => (
                                    <React.Fragment key={`sub-hdr2-${i}`}>
                                        <th style={thStyle2}>{isEn ? "Obt." : "प्र."}</th>
                                        <th style={thStyle2}>{isEn ? "Grd." : "श्रे."}</th>
                                    </React.Fragment>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {pageStudents.map((st, idx) => {
                                const globalIdx = pageIdx * ROWS_PER_PAGE + idx;
                                const marksRecord = semester?.number === 2 ? marksSem2[st.id] : marksSem1[st.id];
                                const details = marksRecord?.detailedMarks || {};
                                const bg = globalIdx % 2 === 0 ? '#F8F9FA' : '#FFF';

                                let genderLabel = "-";
                                if (st.gender) {
                                    const g = st.gender.toLowerCase();
                                    if (g.includes("female") || g.includes("girl") || g.includes("मुलगी") || g.includes("स्त्री")) {
                                        genderLabel = "G";
                                    } else {
                                        genderLabel = "B";
                                    }
                                }

                                const presentDays = marksRecord?.presentDays > 0 ? marksRecord.presentDays : "0";

                                return (
                                    <tr key={st.id} style={{ backgroundColor: bg }}>
                                        <td style={{...tdStyle, textAlign: 'center'}}>{globalIdx + 1}</td>
                                        <td style={tdStyle}>{st.name}</td>
                                        <td style={{...tdStyle, textAlign: 'center'}}>{genderLabel}</td>
                                        <td style={{...tdStyle, textAlign: 'center'}}>{presentDays}</td>
                                        
                                        {subjects.map((sub, si) => {
                                            const d = details[sanitizeKey(sub.name)];
                                            if (d) {
                                                const gt = d.grandTotal > 0 ? d.grandTotal : "0";
                                                let gradeStr = "-";
                                                if (d.grade) {
                                                    const normG = normalizeGrade(d.grade);
                                                    gradeStr = normG;
                                                    for (let gi = 0; gi < GRADES.length; gi++) {
                                                        if (GRADES[gi] === normG) {
                                                            gradeStr = isEn ? GRADES[gi] : GRADE_LABELS_MR[gi];
                                                            break;
                                                        }
                                                    }
                                                }
                                                return (
                                                    <React.Fragment key={`data-${si}`}>
                                                        <td style={{...tdStyle, textAlign: 'center'}}>{gt}</td>
                                                        <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{gradeStr}</td>
                                                    </React.Fragment>
                                                );
                                            } else {
                                                return (
                                                    <React.Fragment key={`data-${si}`}>
                                                        <td style={{...tdStyle, textAlign: 'center', color: '#aaa'}}>-</td>
                                                        <td style={{...tdStyle, textAlign: 'center', color: '#aaa'}}>-</td>
                                                    </React.Fragment>
                                                );
                                            }
                                        })}
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>

                    {/* Bottom Table and Signatures (Only on the last page) */}
                    {pageIdx === pages.length - 1 && (
                        <div style={{ display: 'flex', marginTop: '15px' }}>
                            <div style={{ width: '55%' }}>
                                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '9px' }}>
                                    <thead>
                                        <tr>
                                            <th style={thStyle}>{isEn ? "Sr" : "अ.नं"}</th>
                                            <th style={thStyle}>{isEn ? "Subject" : "विषय"}</th>
                                            {GRADES.map((g, gi) => (
                                                <th key={gi} style={thStyle2}>{isEn ? g : GRADE_LABELS_MR[gi]}</th>
                                            ))}
                                            <th style={thStyle}>{isEn ? "Total" : "एकूण"}</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {subjects.map((sub, si) => {
                                            const bg = si % 2 === 0 ? '#F8F9FA' : '#FFF';
                                            let rowTotal = 0;
                                            return (
                                                <tr key={sub.name} style={{ backgroundColor: bg }}>
                                                    <td style={{...tdStyle, textAlign: 'center'}}>{si + 1}</td>
                                                    <td style={tdStyle}>{sub.name}</td>
                                                    {GRADES.map((_, gi) => {
                                                        const cnt = globalGradeCounts[si][gi];
                                                        rowTotal += cnt;
                                                        return (
                                                            <td key={gi} style={{...tdStyle, textAlign: 'center'}}>{cnt > 0 ? cnt : "0"}</td>
                                                        );
                                                    })}
                                                    <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{rowTotal}</td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                            <div style={{ width: '45%', display: 'flex', justifyContent: 'space-around', alignItems: 'flex-end', paddingBottom: '10px' }}>
                                <div style={{ width: '40%', borderTop: '1px solid #111', textAlign: 'center', paddingTop: '5px', fontSize: '11px', fontWeight: 'bold' }}>
                                    {isEn ? "Class Teacher Signature" : "वर्गशिक्षक स्वाक्षरी"} : {classInfo?.teacherName || ""}
                                </div>
                                <div style={{ width: '40%', borderTop: '1px solid #111', textAlign: 'center', paddingTop: '5px', fontSize: '11px', fontWeight: 'bold' }}>
                                    {isEn ? "Headmaster Signature" : "मुख्याध्यापक स्वाक्षरी"} : {school?.principalName || ""}
                                </div>
                            </div>
                        </div>
                    )}
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
    padding: '3px',
    color: '#111',
    verticalAlign: 'middle'
};
