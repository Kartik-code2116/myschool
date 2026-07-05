import React from 'react';
import useLanguage from '../../../utils/useLanguage';

const sanitizeKey = (key) => {
    if (!key) return "unknown";
    return key.replace(/[\.#\$\[\]\/\\~\*]/g, "_");
};

// Normalize grade matching logic from PdfGenerator
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

export default function RosterGradeTableT7({ school, classInfo, year, semester, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    const subjects = classInfo?.subjects || [];
    
    const GRADES = ["A-1", "A-2", "B-1", "B-2", "C-1", "C-2", "D", "E-1", "E-2"];
    const GRADE_LABELS_MR = ["अ-1", "अ-2", "ब-1", "ब-2", "क-1", "क-2", "ड", "इ-1", "इ-2"];

    // Compute distribution
    // gradeData[subjectIndex][gradeIndex] = { boys: 0, girls: 0 }
    const gradeData = subjects.map(() => GRADES.map(() => ({ boys: 0, girls: 0 })));
    
    let patBoys = 0, patGirls = 0;
    let attBoys = 0, attGirls = 0;

    students.forEach(st => {
        const isFemale = st.gender === 'Female' || st.gender === 'मुलगी' || st.gender === 'स्त्री' || st.gender === '2';
        
        if (isFemale) patGirls++;
        else patBoys++;

        const marksRecord = semester?.number === 2 ? marksSem2[st.id] : marksSem1[st.id];
        
        if (marksRecord && marksRecord.presentDays > 0) {
            if (isFemale) attGirls += parseInt(marksRecord.presentDays);
            else attBoys += parseInt(marksRecord.presentDays);
        }

        const details = marksRecord?.detailedMarks || {};

        subjects.forEach((sub, si) => {
            const d = details[sanitizeKey(sub.name)];
            if (d && d.grade) {
                const normG = normalizeGrade(d.grade);
                for (let gi = 0; gi < GRADES.length; gi++) {
                    if (GRADES[gi] === normG) {
                        if (isFemale) gradeData[si][gi].girls++;
                        else gradeData[si][gi].boys++;
                        break;
                    }
                }
            }
        });
    });

    return (
        <div className="report-roster-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 landscape; margin: 10mm; }
                }
                `}
            </style>
            
            <div className="rc-standard-page" style={{ padding: '15px', minHeight: '210mm', width: '297mm', margin: '0 auto' }}>
                
                {/* Title */}
                <div style={{ textAlign: 'center', fontSize: '16px', fontWeight: 'bold', color: '#111', marginBottom: '15px' }}>
                    {isEn ? "Continuous Comprehensive Evaluation" : "सातत्यपूर्ण सर्वंकष मूल्यमापन"}
                </div>

                {/* Header Info Table */}
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '15px', fontSize: '11px', fontWeight: 'bold', color: '#111' }}>
                    <div style={{ width: '33%' }}>
                        <div>{isEn ? "UDISE: " : "युडायस: "}{school?.udiseCode || "-"}</div>
                        <div>{isEn ? "School: " : "शाळा: "}{school?.name || "-"}</div>
                    </div>
                    <div style={{ width: '33%', textAlign: 'center', display: 'flex', alignItems: 'flex-end', justifyContent: 'center' }}>
                        <div style={{ fontSize: '15px' }}>
                            {semester?.number === 2 ? (isEn ? "Second Semester" : "द्वितीय सत्र") : (isEn ? "First Semester" : "प्रथम सत्र")}
                        </div>
                    </div>
                    <div style={{ width: '33%', textAlign: 'right' }}>
                        <div>{isEn ? "Year: " : "सन: "}{classInfo?.academicYearLabel || "-"}</div>
                        <div>{isEn ? "Class: " : "इयत्ता: "}{classInfo?.className} {isEn ? ", Div: " : ", तुकडी: "}{classInfo?.division || "-"}</div>
                    </div>
                </div>

                {/* Main Table */}
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '10px' }}>
                    <thead>
                        <tr>
                            <th rowSpan="2" style={{...thStyle, width: '4%'}}>{isEn ? "Sr.No." : "अ.नं"}</th>
                            <th rowSpan="2" style={{...thStyle, width: '12%'}}>{isEn ? "Subject" : "विषय"}</th>
                            {GRADES.map((g, gi) => (
                                <th key={gi} colSpan="2" style={thStyle}>{isEn ? g : GRADE_LABELS_MR[gi]}</th>
                            ))}
                            <th colSpan="2" style={thStyle}>{isEn ? "Total" : "एकूण"}</th>
                        </tr>
                        <tr>
                            {GRADES.map((_, gi) => (
                                <React.Fragment key={`sub-hdr-${gi}`}>
                                    <th style={thStyle2}>{isEn ? "Boys" : "मुले"}</th>
                                    <th style={thStyle2}>{isEn ? "Girls" : "मुली"}</th>
                                </React.Fragment>
                            ))}
                            <th style={thStyle2}>{isEn ? "Boys" : "मुले"}</th>
                            <th style={thStyle2}>{isEn ? "Girls" : "मुली"}</th>
                        </tr>
                    </thead>
                    <tbody>
                        {subjects.map((sub, si) => {
                            let rowBoys = 0, rowGirls = 0;
                            const bg = si % 2 === 0 ? '#F8F9FA' : '#FFF';
                            
                            return (
                                <tr key={sub.name} style={{ backgroundColor: bg }}>
                                    <td style={{...tdStyle, textAlign: 'center'}}>{si + 1}</td>
                                    <td style={tdStyle}>{sub.name}</td>
                                    
                                    {GRADES.map((_, gi) => {
                                        const b = gradeData[si][gi].boys;
                                        const g = gradeData[si][gi].girls;
                                        rowBoys += b;
                                        rowGirls += g;
                                        return (
                                            <React.Fragment key={`data-${si}-${gi}`}>
                                                <td style={{...tdStyle, textAlign: 'center'}}>{b > 0 ? b : "0"}</td>
                                                <td style={{...tdStyle, textAlign: 'center'}}>{g > 0 ? g : "0"}</td>
                                            </React.Fragment>
                                        );
                                    })}
                                    
                                    {/* Row Totals */}
                                    <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{rowBoys}</td>
                                    <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{rowGirls}</td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>

                {/* Bottom Summary & Signatures */}
                <div style={{ display: 'flex', marginTop: '30px' }}>
                    <div style={{ width: '40%' }}>
                        <table style={{ width: '80%', borderCollapse: 'collapse', fontSize: '10px' }}>
                            <thead>
                                <tr>
                                    <th style={thStyle}>{isEn ? "Detail" : "तपशील"}</th>
                                    <th style={thStyle}>{isEn ? "Boys" : "मुले"}</th>
                                    <th style={thStyle}>{isEn ? "Girls" : "मुली"}</th>
                                    <th style={thStyle}>{isEn ? "Total" : "एकूण"}</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td style={tdStyle}>{isEn ? "Enrolled" : "पट"}</td>
                                    <td style={{...tdStyle, textAlign: 'center'}}>{patBoys}</td>
                                    <td style={{...tdStyle, textAlign: 'center'}}>{patGirls}</td>
                                    <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{patBoys + patGirls}</td>
                                </tr>
                                <tr style={{ backgroundColor: '#F8F9FA' }}>
                                    <td style={tdStyle}>{isEn ? "Attendance" : "उपस्थिती"}</td>
                                    <td style={{...tdStyle, textAlign: 'center'}}>{attBoys}</td>
                                    <td style={{...tdStyle, textAlign: 'center'}}>{attGirls}</td>
                                    <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{attBoys + attGirls}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <div style={{ width: '60%', display: 'flex', justifyContent: 'space-around', alignItems: 'flex-end', paddingBottom: '10px' }}>
                        <div style={{ width: '40%', borderTop: '1px solid #111', textAlign: 'center', paddingTop: '5px', fontSize: '11px', fontWeight: 'bold' }}>
                            {isEn ? "Class Teacher Signature" : "वर्गशिक्षक स्वाक्षरी"} : {classInfo?.teacherName || ""}
                        </div>
                        <div style={{ width: '40%', borderTop: '1px solid #111', textAlign: 'center', paddingTop: '5px', fontSize: '11px', fontWeight: 'bold' }}>
                            {isEn ? "Headmaster Signature" : "मुख्याध्यापक स्वाक्षरी"} : {school?.principalName || ""}
                        </div>
                    </div>
                </div>

            </div>
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

const thStyle2 = { ...thStyle, backgroundColor: '#E8F5E9' };

const tdStyle = {
    border: '1px solid #111',
    padding: '4px',
    color: '#111',
    verticalAlign: 'middle'
};
