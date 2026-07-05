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
    return s.includes("कला") || s.includes("कार्यानुभव") || s.includes("शा.शि.") || s.includes("शारीरिक")
        || s.includes("art") || s.includes("work experience") || s.includes("p.e.") || s.includes("physical education") || s.includes("craft");
};

export default function AnnualMarksheetT12({ school, classInfo, year, students, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    const allSubjects = classInfo?.subjects || [];
    const academicSubs = [];
    const nonAcSubs = [];
    
    allSubjects.forEach(s => {
        if (isNonAcademic(s.name)) nonAcSubs.push(s);
        else academicSubs.push(s);
    });

    const acaMax = (academicSubs.length > 0 && academicSubs[0].maxMarks > 0) ? academicSubs[0].maxMarks : 50;

    const academicYear = classInfo?.academicYearLabel || "2025-26";
    let nextYear = "";
    if (academicYear.includes("-")) {
        const parts = academicYear.split("-");
        if (parts.length > 1) {
            const y2 = parts[1].trim();
            nextYear = y2.length === 2 ? "20" + y2 : y2;
        }
    }
    if (!nextYear) {
        nextYear = new Date().getFullYear().toString();
    }
    const defaultDate = `01-05-${nextYear}`;
    const resultDate = school?.resultDate || defaultDate;

    return (
        <div className="report-annualmarksheet-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 portrait; margin: 10mm; }
                }
                .am-page {
                    width: 210mm;
                    min-height: 297mm;
                    padding: 15px;
                    margin: 0 auto;
                    box-sizing: border-box;
                    page-break-after: always;
                    border: 2px solid #111;
                    border-radius: 15px;
                    position: relative;
                }
                .am-tbl th {
                    background-color: #DAE6F3;
                    color: #111;
                    border: 1px solid #111;
                    padding: 6px;
                    text-align: center;
                    font-weight: bold;
                    vertical-align: middle;
                }
                .am-tbl td {
                    border: 1px solid #111;
                    padding: 6px;
                    color: #111;
                    vertical-align: middle;
                }
                `}
            </style>
            
            {students.map((st, idx) => {
                const rec = marksSem2?.[st.id];
                
                let totalObtained = 0;
                let maxTotal = 0;
                let hasFailed = false;

                return (
                    <div key={st.id} className="am-page rc-standard-page">
                        
                        {/* Title */}
                        <div style={{ textAlign: 'center', marginBottom: '15px' }}>
                            <div style={{ fontSize: '18px', fontWeight: 'bold', color: '#D81B60' }}>
                                {isEn ? "● MARKSHEET ●" : "● गुणपत्रक ●"}
                            </div>
                            <div style={{ fontSize: '14px', fontWeight: 'bold' }}>
                                {isEn ? "Annual Examination" : "वार्षिक परीक्षा"}
                            </div>
                        </div>

                        {/* Header Info */}
                        <table style={{ width: '100%', marginBottom: '10px', fontSize: '12px' }}>
                            <tbody>
                                <tr>
                                    <td style={{ textAlign: 'left', width: '60%', paddingBottom: '4px' }}>
                                        {isEn ? "School Name: " : "शाळेचे नाव : "}{school?.name || "-"}
                                    </td>
                                    <td style={{ textAlign: 'right', width: '40%', paddingBottom: '4px' }}>
                                        {isEn ? "UDISE : " : "युडायस : "}{school?.udiseCode || "-"}
                                    </td>
                                </tr>
                                <tr>
                                    <td style={{ textAlign: 'left', paddingBottom: '4px' }}>
                                        {isEn ? "Name: " : "नाव : "}{st.name || "-"}
                                    </td>
                                    <td style={{ textAlign: 'right', paddingBottom: '4px' }}>
                                        {isEn ? "Year: " : "सन : "}{classInfo?.academicYearLabel || "-"}
                                    </td>
                                </tr>
                                <tr>
                                    <td style={{ textAlign: 'left', paddingBottom: '4px' }}>
                                        {isEn ? "Roll No: " : "हजेरी क्रमांक : "}{st.rollNo || "-"}
                                    </td>
                                    <td style={{ textAlign: 'right', paddingBottom: '4px' }}>
                                        {isEn ? "Class: " : "इयत्ता: "}{classInfo?.className || "-"}
                                        {isEn ? ", Division: " : ", तुकडी: "}{classInfo?.division || "-"}
                                    </td>
                                </tr>
                            </tbody>
                        </table>

                        {/* Marks Table */}
                        <table className="am-tbl" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '11px', marginBottom: '15px' }}>
                            <thead>
                                <tr>
                                    <th style={{ width: '8%' }}>{isEn ? "Sr.No." : "अ.नं."}</th>
                                    <th style={{ width: '25%' }}>{isEn ? "Subject" : "विषय"}</th>
                                    <th style={{ width: '15%' }}>{isEn ? "Min\nRequired Marks" : "किमान\nआवश्यक गुण"}</th>
                                    <th style={{ width: '15%' }}>{isEn ? `Total Marks\n(Out of ${acaMax})\nObtained` : `एकूण गुण\n(${acaMax} पैकी)\nप्राप्त`}</th>
                                    <th style={{ width: '12%' }}>{isEn ? "Grace\nMarks" : "सवलतीचे\nगुण"}</th>
                                    <th style={{ width: '15%' }}>{isEn ? "Remark\n(Pass/\nFail)" : "शेरा\n(उत्तीर्ण/\nअनुत्तीर्ण)"}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {/* Academic Subjects */}
                                {academicSubs.map((sub, sIdx) => {
                                    const d = rec?.detailedMarks?.[sanitizeKey(sub.name)];
                                    const obt = d?.grandTotal || 0;
                                    totalObtained += obt;
                                    const subMax = sub.maxMarks > 0 ? sub.maxMarks : 50;
                                    maxTotal += subMax;
                                    
                                    const minPass = subMax > 0 ? Math.ceil(subMax * 0.35) : 18;
                                    const passed = obt >= minPass;
                                    if (!passed) hasFailed = true;

                                    return (
                                        <tr key={sIdx}>
                                            <td style={{ textAlign: 'center' }}>{sIdx + 1}</td>
                                            <td style={{ textAlign: 'left' }}>{sub.name}</td>
                                            <td style={{ textAlign: 'center' }}>{minPass}</td>
                                            <td style={{ textAlign: 'center', fontWeight: 'bold' }}>{obt}</td>
                                            <td style={{ textAlign: 'center' }}>-</td>
                                            <td style={{ textAlign: 'center' }}>{passed ? (isEn ? "Pass" : "उत्तीर्ण") : (isEn ? "Fail" : "अनुत्तीर्ण")}</td>
                                        </tr>
                                    );
                                })}
                                
                                {/* Non-Academic Subjects */}
                                {nonAcSubs.map((sub, i) => {
                                    const d = rec?.detailedMarks?.[sanitizeKey(sub.name)];
                                    const grade = normalizeGrade(d?.grade) || "-";
                                    const rowIdx = academicSubs.length + i + 1;
                                    
                                    let extraBorder = {};
                                    if (nonAcSubs.length === 1) { extraBorder = { borderTop: '1px solid #111', borderBottom: '1px solid #111' }; }
                                    else if (i === 0) { extraBorder = { borderTop: '1px solid #111', borderBottom: 'none' }; }
                                    else if (i === nonAcSubs.length - 1) { extraBorder = { borderTop: 'none', borderBottom: '1px solid #111' }; }
                                    else { extraBorder = { borderTop: 'none', borderBottom: 'none' }; }

                                    return (
                                        <tr key={sub.name}>
                                            <td style={{ textAlign: 'center' }}>{rowIdx}</td>
                                            <td style={{ textAlign: 'left' }}>{sub.name}</td>
                                            <td style={{ textAlign: 'center' }}>{isEn ? "Grade" : "श्रेणी"}</td>
                                            <td style={{ textAlign: 'center', fontWeight: 'bold' }}>{grade}</td>
                                            <td colSpan="2" style={{ borderLeft: '1px solid #111', borderRight: '1px solid #111', ...extraBorder }}></td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>

                        {/* Summary Totals */}
                        <table style={{ width: '100%', marginBottom: '40px', fontSize: '12px' }}>
                            <tbody>
                                <tr>
                                    <td style={{ textAlign: 'left', fontWeight: 'bold', paddingTop: '5px' }}>
                                        {isEn ? "Total Marks: " : "एकूण गुण : "}{totalObtained}
                                    </td>
                                    <td style={{ textAlign: 'right', fontWeight: 'bold', paddingTop: '5px' }}>
                                        {isEn ? "Percentage: " : "शेकडा गुण : "}{maxTotal > 0 ? ((totalObtained * 100) / maxTotal).toFixed(1) : "0.0"}%
                                    </td>
                                </tr>
                                <tr>
                                    <td style={{ textAlign: 'left', paddingTop: '5px' }}>
                                        {isEn ? "Result Date: " : "निकाल दिनांक : "}{resultDate}
                                    </td>
                                    <td style={{ textAlign: 'right', fontWeight: 'bold', paddingTop: '5px' }}>
                                        {isEn ? "Remark: " : "शेरा : "}{hasFailed ? (isEn ? "Fail" : "अनुत्तीर्ण") : (isEn ? "Pass" : "उत्तीर्ण")}
                                    </td>
                                </tr>
                            </tbody>
                        </table>

                        {/* Signatures */}
                        <table style={{ width: '100%', fontSize: '11px' }}>
                            <tbody>
                                <tr>
                                    <td style={{ textAlign: 'center', width: '50%' }}>
                                        <div>{isEn ? "Class Teacher Signature" : "वर्गशिक्षक स्वाक्षरी"}</div>
                                        <div>{isEn ? "" : "श्रीम . "}{classInfo?.teacherName || ""}</div>
                                    </td>
                                    <td style={{ textAlign: 'center', width: '50%' }}>
                                        <div>{isEn ? "Headmaster Signature" : "मुख्याध्यापक स्वाक्षरी"}</div>
                                        <div>{isEn ? "" : "श्री "}{school?.principalName || ""}</div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>

                    </div>
                );
            })}
        </div>
    );
}
