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

const isNonAcademic = (sub) => {
    if (!sub) return false;
    const s = sub.trim().toLowerCase();
    return s.includes("कला") || s.includes("कार्यानुभव") || s.includes("शा.शि.") || s.includes("श शारीरिक")
        || s.includes("art") || s.includes("work experience") || s.includes("p.e.") || s.includes("physical education") || s.includes("craft");
};

export default function ResultSheetT13({ school, classInfo, year, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    // Result sheet relies on sem2 marks as per Android logic (Option 13)
    const allSubjects = classInfo?.subjects || [];
    const acaSubs = [];
    const nonAcaSubs = [];
    let maxTotal = 0;
    
    allSubjects.forEach(s => {
        if (isNonAcademic(s.name)) {
            nonAcaSubs.push(s);
        } else {
            acaSubs.push(s);
            maxTotal += (s.maxMarks > 0) ? s.maxMarks : 50;
        }
    });

    // Split students for pagination (approx 20 per page for landscape)
    const ROWS_PER_PAGE = 20; 
    const pages = [];
    for (let i = 0; i < students.length; i += ROWS_PER_PAGE) {
        pages.push(students.slice(i, i + ROWS_PER_PAGE));
    }
    if (pages.length === 0) pages.push([]);

    return (
        <div className="report-resultsheet-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 landscape; margin: 10mm; }
                }
                .rs-page {
                    width: 297mm;
                    min-height: 210mm;
                    padding: 15px;
                    margin: 0 auto;
                    box-sizing: border-box;
                    page-break-after: always;
                    border: 1px solid #111;
                }
                .rs-tbl th {
                    background-color: #DAE6F3;
                    color: #111;
                    border: 1px solid #111;
                    padding: 4px;
                    text-align: center;
                    font-weight: bold;
                    vertical-align: middle;
                }
                .rs-tbl td {
                    border: 1px solid #111;
                    padding: 4px;
                    color: #111;
                    vertical-align: middle;
                    text-align: center;
                }
                `}
            </style>
            
            {pages.map((pageStudents, pageIdx) => (
                <div key={pageIdx} className="rs-page rc-standard-page">
                    
                    {/* Titles */}
                    <div style={{ textAlign: 'center', marginBottom: '8px' }}>
                        <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#D81B60' }}>
                            {isEn ? "● RESULT SHEET ●" : "● निकालपत्रक ●"}
                        </div>
                        <div style={{ fontSize: '12px', fontWeight: 'bold', marginTop: '4px' }}>
                            {isEn ? "Annual Examination " : "वार्षिक परीक्षा "}{classInfo?.className || ""}
                        </div>
                    </div>

                    {/* Header Info */}
                    <table style={{ width: '100%', marginBottom: '10px', fontSize: '11px', fontWeight: 'bold' }}>
                        <tbody>
                            <tr>
                                <td style={{ textAlign: 'left', width: '60%' }}>
                                    {isEn ? "School Name: " : "शाळेचे नाव : "}{school?.name || "-"}
                                </td>
                                <td style={{ textAlign: 'right', width: '40%' }}>
                                    {isEn ? "UDISE : " : "युडायस : "}{school?.udiseCode || "-"}
                                </td>
                            </tr>
                            <tr>
                                <td style={{ textAlign: 'left' }}>
                                    {isEn ? "Class: " : "इयत्ता: "}{classInfo?.className || "-"}
                                    {isEn ? ", Division: " : ", तुकडी: "}{classInfo?.division || "-"}
                                </td>
                                <td style={{ textAlign: 'right' }}>
                                    {isEn ? "Year: " : "सन : "}{classInfo?.academicYearLabel || "-"}
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    {/* Table */}
                    <table className="rs-tbl" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '9px' }}>
                        <thead>
                            {/* Row 1 */}
                            <tr>
                                <th rowSpan="2" style={{ width: '4%' }}>{isEn ? "Sr.No." : "अ.नं."}</th>
                                <th rowSpan="2" style={{ width: '12%' }}>{isEn ? "Student Name" : "नाव"}</th>
                                <th rowSpan="2" style={{ width: '5%' }}>{isEn ? "Attd." : "हजर दिन"}</th>
                                
                                {acaSubs.map((sub, i) => (
                                    <th key={`aca-${i}`} colSpan="3">{sub.name}</th>
                                ))}
                                {nonAcaSubs.map((sub, i) => (
                                    <th key={`non-${i}`} rowSpan="2">{sub.name}</th>
                                ))}
                                
                                <th rowSpan="2" style={{ width: '6%' }}>{isEn ? "Total " : "एकूण "}{maxTotal}</th>
                                <th rowSpan="2" style={{ width: '6%' }}>{isEn ? "Percentage" : "शेकडा गुण"}</th>
                                <th rowSpan="2" style={{ width: '6%' }}>{isEn ? "Remark" : "शेरा"}</th>
                            </tr>
                            {/* Row 2 */}
                            <tr>
                                {acaSubs.map((_, i) => (
                                    <React.Fragment key={`subcol-${i}`}>
                                        <th>{isEn ? "Marks" : "गुण"}</th>
                                        <th>{isEn ? "Grace" : "ग्रेस"}</th>
                                        <th>{isEn ? "Rem." : "शेरा"}</th>
                                    </React.Fragment>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {pageStudents.map((st, idx) => {
                                const globalIdx = pageIdx * ROWS_PER_PAGE + idx;
                                const rec = marksSem2?.[st.id];
                                const bg = globalIdx % 2 === 0 ? '#F8F9FA' : '#FFF';
                                
                                let totalObtained = 0;
                                let hasFailed = false;

                                return (
                                    <tr key={st.id} style={{ backgroundColor: bg }}>
                                        <td>{globalIdx + 1}</td>
                                        <td style={{ textAlign: 'left', fontWeight: 'bold' }}>{st.name}</td>
                                        <td>{rec?.presentDays > 0 ? rec.presentDays : "-"}</td>

                                        {/* Academic Subjects */}
                                        {acaSubs.map((sub, i) => {
                                            const d = rec?.detailedMarks?.[sanitizeKey(sub.name)];
                                            const obt = d?.grandTotal || 0;
                                            totalObtained += obt;
                                            
                                            const minPass = sub.maxMarks > 0 ? Math.ceil(sub.maxMarks * 0.35) : 18;
                                            const passed = obt >= minPass;
                                            if (!passed) hasFailed = true;

                                            return (
                                                <React.Fragment key={`aca-data-${i}`}>
                                                    <td style={{ fontWeight: 'bold' }}>{obt > 0 ? obt : "-"}</td>
                                                    <td style={{ color: '#aaa' }}>-</td>
                                                    <td style={{ fontWeight: passed ? 'normal' : 'bold', color: passed ? '#111' : '#D32F2F' }}>
                                                        {passed ? "P" : "F"}
                                                    </td>
                                                </React.Fragment>
                                            );
                                        })}

                                        {/* Non-Academic Subjects */}
                                        {nonAcaSubs.map((sub, i) => {
                                            const d = rec?.detailedMarks?.[sanitizeKey(sub.name)];
                                            const grade = normalizeGrade(d?.grade) || "-";
                                            return (
                                                <td key={`non-data-${i}`} style={{ fontWeight: 'bold' }}>{grade}</td>
                                            );
                                        })}

                                        {/* Totals */}
                                        <td style={{ fontWeight: 'bold' }}>{totalObtained}</td>
                                        <td style={{ fontWeight: 'bold' }}>
                                            {maxTotal > 0 ? ((totalObtained * 100) / maxTotal).toFixed(1) : "0.0"}
                                        </td>
                                        <td style={{ fontWeight: 'bold', color: hasFailed ? '#D32F2F' : '#111' }}>
                                            {hasFailed ? (isEn ? "Fail" : "अनुत्तीर्ण") : (isEn ? "Pass" : "उत्तीर्ण")}
                                        </td>
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
