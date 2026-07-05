import React from 'react';
import useLanguage from '../../../utils/useLanguage';

const sanitizeKey = (key) => {
    if (!key) return "unknown";
    return key.replace(/[\.#\$\[\]\/\\~\*]/g, "_");
};

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

export default function ProgressBookCombinedT16({ school, classInfo, semester, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';
    const selSem = semester?.number || 0; // 0 = both, 1 = sem1, 2 = sem2

    const allSubs = (classInfo?.subjects || []).filter(sub => {
        const s = sub.name.toLowerCase();
        return !(s.includes("vishesh") || s.includes("aavad") || s.includes("sudharna") || s.includes("vyaktimatva") ||
                 s.includes("विशेष") || s.includes("आवड") || s.includes("सुधारणा") || s.includes("व्यक्तिमत्व"));
    });

    // We can assume attendance is always shown in this variant for simplicity, or we can just render it conditionally.
    const showAttendance = true;

    // Split students for pagination (landscape, combined rows might mean fewer students per page)
    const ROWS_PER_PAGE = selSem === 0 ? 10 : 20; 
    const pages = [];
    for (let i = 0; i < students.length; i += ROWS_PER_PAGE) {
        pages.push(students.slice(i, i + ROWS_PER_PAGE));
    }
    if (pages.length === 0) pages.push([]);

    return (
        <div className="report-pb-combined-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 landscape; margin: 10mm; }
                }
                .pbc-page {
                    width: 297mm;
                    min-height: 210mm;
                    padding: 10px;
                    margin: 0 auto;
                    box-sizing: border-box;
                    page-break-after: always;
                }
                .pbc-tbl th {
                    background-color: #DAE6F3;
                    color: #111;
                    border: 1px solid #111;
                    padding: 4px;
                    text-align: center;
                    font-weight: bold;
                    vertical-align: middle;
                    font-size: 10px;
                }
                .pbc-tbl td {
                    border: 1px solid #111;
                    padding: 4px;
                    color: #111;
                    vertical-align: middle;
                    text-align: center;
                    font-size: 10px;
                }
                `}
            </style>
            
            {pages.map((pageStudents, pageIdx) => (
                <div key={pageIdx} className="pbc-page rc-standard-page">
                    
                    {/* Header */}
                    <table style={{ width: '100%', marginBottom: '10px', fontSize: '11px', fontWeight: 'bold' }}>
                        <tbody>
                            <tr>
                                <td style={{ textAlign: 'left', width: '33%' }}>
                                    <div>{isEn ? "UDISE: " : "युडायस क्रमांक: "}{school?.udiseCode || "-"}</div>
                                    <div style={{ marginTop: '2px' }}>{isEn ? "School Name: " : "शाळेचे नाव: "}{school?.name || "-"}</div>
                                </td>
                                <td style={{ textAlign: 'center', width: '34%' }}>
                                    <div style={{ fontSize: '16px' }}>
                                        {isEn ? "Continuous Comprehensive Evaluation" : "सातत्यपूर्ण सर्वंकष मूल्यमापन"}
                                    </div>
                                    <div style={{ marginTop: '2px', fontSize: '12px' }}>
                                        {selSem === 1 ? (isEn ? "First Semester" : "प्रथम सत्र") :
                                         selSem === 2 ? (isEn ? "Second Semester" : "द्वितीय सत्र") :
                                         (isEn ? "First & Second Semester" : "प्रथम व द्वितीय सत्र")}
                                    </div>
                                </td>
                                <td style={{ textAlign: 'right', width: '33%' }}>
                                    <div>{isEn ? "Year: " : "सन: "}{classInfo?.academicYearLabel || "-"}</div>
                                    <div style={{ marginTop: '2px' }}>
                                        {isEn ? "Class: " : "इयत्ता: "}{classInfo?.className || "-"} , 
                                        {isEn ? " Div: " : " तुकडी: "}{classInfo?.division || "-"}
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    {/* Table */}
                    <table className="pbc-tbl" style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr>
                                <th rowSpan="2" style={{ width: '4%' }}>{isEn ? "Sr.No." : "अ.नं"}</th>
                                <th rowSpan="2" style={{ width: '15%' }}>{isEn ? "Student Name" : "विद्यार्थ्याचे नाव"}</th>
                                {showAttendance && <th rowSpan="2" style={{ width: '5%' }}>{isEn ? "Attd." : "उपस्थिती"}</th>}
                                <th rowSpan="2" style={{ width: '4%' }}>{isEn ? "Term" : "सत्र"}</th>
                                
                                {allSubs.map((sub, i) => (
                                    <th key={i} colSpan="2">{sub.name}</th>
                                ))}

                                <th rowSpan="2" style={{ width: '5%' }}>{isEn ? "Total" : "एकूण"}</th>
                                <th rowSpan="2" style={{ width: '6%' }}>{isEn ? "Percentage" : "शेकडा गुण"}</th>
                                <th rowSpan="2" style={{ width: '5%' }}>{isEn ? "Grade" : "श्रेणी"}</th>
                            </tr>
                            <tr>
                                {allSubs.map((_, i) => (
                                    <React.Fragment key={`subcol-${i}`}>
                                        <th style={{ width: '4%' }}>{isEn ? "Marks" : "गुण"}</th>
                                        <th style={{ width: '4%' }}>{isEn ? "Grade" : "श्रेणी"}</th>
                                    </React.Fragment>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {pageStudents.map((st, idx) => {
                                const globalIdx = pageIdx * ROWS_PER_PAGE + idx;
                                const bg = globalIdx % 2 === 0 ? '#F8F9FA' : '#FFF';
                                
                                const s1 = marksSem1?.[st.id];
                                const s2 = marksSem2?.[st.id];

                                // Attendance
                                let attDays = 0;
                                if (st.monthlyAttendance) {
                                    Object.values(st.monthlyAttendance).forEach(v => {
                                        if (v) {
                                            if (v.includes('/')) {
                                                attDays += (parseInt(v.split('/')[0].trim()) || 0);
                                            } else {
                                                attDays += (parseInt(v.trim()) || 0);
                                            }
                                        }
                                    });
                                }
                                if (attDays === 0 && s1?.presentDays) attDays = s1.presentDays;
                                if (attDays === 0 && s2?.presentDays) attDays = s2.presentDays;

                                const rowspan = selSem === 0 ? 2 : 1;

                                const renderSemRow = (semNum, rec, termLabel) => {
                                    return (
                                        <tr style={{ backgroundColor: bg }}>
                                            <td>{termLabel}</td>
                                            {allSubs.map((sub, i) => {
                                                const d = rec?.detailedMarks?.[sanitizeKey(sub.name)];
                                                let marks = "";
                                                if (d?.grandTotal > 0) marks = d.grandTotal;
                                                else if (d && (d.akarikTotal > 0 || d.sanklit > 0)) marks = (d.akarikTotal || 0) + (d.sanklit || 0);
                                                else if (rec?.subjectMarks?.[sanitizeKey(sub.name)] > 0) marks = rec.subjectMarks[sanitizeKey(sub.name)];
                                                
                                                const grade = normalizeGrade(d?.grade) || "";

                                                return (
                                                    <React.Fragment key={`data-${i}`}>
                                                        <td>{marks}</td>
                                                        <td style={{ fontWeight: 'bold' }}>{grade}</td>
                                                    </React.Fragment>
                                                );
                                            })}
                                            <td style={{ fontWeight: 'bold' }}>{rec?.totalObtained > 0 ? rec.totalObtained : ""}</td>
                                            <td style={{ fontWeight: 'bold' }}>{rec?.percentage > 0 ? rec.percentage.toFixed(1) : ""}</td>
                                            <td style={{ fontWeight: 'bold' }}>{normalizeGrade(rec?.grade) || ""}</td>
                                        </tr>
                                    );
                                };

                                return (
                                    <React.Fragment key={st.id}>
                                        {/* Sem 1 Row (Main row with Sr.No, Name, Attd) */}
                                        {(selSem === 0 || selSem === 1) && (
                                            <tr style={{ backgroundColor: bg }}>
                                                <td rowSpan={rowspan}>{globalIdx + 1}</td>
                                                <td rowSpan={rowspan} style={{ textAlign: 'left', fontWeight: 'bold' }}>{st.name}</td>
                                                {showAttendance && <td rowSpan={rowspan}>{attDays > 0 ? attDays : "-"}</td>}
                                                
                                                <td>I</td>
                                                {allSubs.map((sub, i) => {
                                                    const d = s1?.detailedMarks?.[sanitizeKey(sub.name)];
                                                    let marks = "";
                                                    if (d?.grandTotal > 0) marks = d.grandTotal;
                                                    else if (d && (d.akarikTotal > 0 || d.sanklit > 0)) marks = (d.akarikTotal || 0) + (d.sanklit || 0);
                                                    else if (s1?.subjectMarks?.[sanitizeKey(sub.name)] > 0) marks = s1.subjectMarks[sanitizeKey(sub.name)];
                                                    
                                                    const grade = normalizeGrade(d?.grade) || "";

                                                    return (
                                                        <React.Fragment key={`data-s1-${i}`}>
                                                            <td>{marks}</td>
                                                            <td style={{ fontWeight: 'bold' }}>{grade}</td>
                                                        </React.Fragment>
                                                    );
                                                })}
                                                <td style={{ fontWeight: 'bold' }}>{s1?.totalObtained > 0 ? s1.totalObtained : ""}</td>
                                                <td style={{ fontWeight: 'bold' }}>{s1?.percentage > 0 ? s1.percentage.toFixed(1) : ""}</td>
                                                <td style={{ fontWeight: 'bold' }}>{normalizeGrade(s1?.grade) || ""}</td>
                                            </tr>
                                        )}

                                        {/* Sem 2 Row (If selSem is 2, this is the main row. If 0, it's the second row) */}
                                        {(selSem === 0 || selSem === 2) && (
                                            selSem === 2 ? (
                                                <tr style={{ backgroundColor: bg }}>
                                                    <td rowSpan={rowspan}>{globalIdx + 1}</td>
                                                    <td rowSpan={rowspan} style={{ textAlign: 'left', fontWeight: 'bold' }}>{st.name}</td>
                                                    {showAttendance && <td rowSpan={rowspan}>{attDays > 0 ? attDays : "-"}</td>}
                                                    
                                                    <td>II</td>
                                                    {allSubs.map((sub, i) => {
                                                        const d = s2?.detailedMarks?.[sanitizeKey(sub.name)];
                                                        let marks = "";
                                                        if (d?.grandTotal > 0) marks = d.grandTotal;
                                                        else if (d && (d.akarikTotal > 0 || d.sanklit > 0)) marks = (d.akarikTotal || 0) + (d.sanklit || 0);
                                                        else if (s2?.subjectMarks?.[sanitizeKey(sub.name)] > 0) marks = s2.subjectMarks[sanitizeKey(sub.name)];
                                                        
                                                        const grade = normalizeGrade(d?.grade) || "";
    
                                                        return (
                                                            <React.Fragment key={`data-s2-${i}`}>
                                                                <td>{marks}</td>
                                                                <td style={{ fontWeight: 'bold' }}>{grade}</td>
                                                            </React.Fragment>
                                                        );
                                                    })}
                                                    <td style={{ fontWeight: 'bold' }}>{s2?.totalObtained > 0 ? s2.totalObtained : ""}</td>
                                                    <td style={{ fontWeight: 'bold' }}>{s2?.percentage > 0 ? s2.percentage.toFixed(1) : ""}</td>
                                                    <td style={{ fontWeight: 'bold' }}>{normalizeGrade(s2?.grade) || ""}</td>
                                                </tr>
                                            ) : (
                                                renderSemRow(2, s2, "II")
                                            )
                                        )}
                                    </React.Fragment>
                                );
                            })}
                        </tbody>
                    </table>

                </div>
            ))}
        </div>
    );
}
