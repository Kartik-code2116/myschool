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

export default function SubjectRegisterT11({ school, classInfo, year, semester, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';
    const isSem2 = semester?.number === 2;

    const subjects = classInfo?.subjects || [];
    
    // Formative & Summative Labels
    const formLabels = isEn ? ["Obs.", "Oral", "Pract.", "Activity", "Project", "Test", "Assign.", "Other", "Total"] 
                            : ["निरीक्षण", "तोंडीकाम", "प्रात्यक्षिक", "उपक्रम", "प्रकल्प", "चाचणी", "स्वाध्याय", "इतर", "एकूण"];
    const summLabels = isEn ? ["Oral", "Pract.", "Written", "Total"] 
                            : ["तोंडी", "प्रात्य.", "लेखी", "एकूण"];

    const blank = (v) => (v > 0 ? v : "-");

    return (
        <div className="report-subjectreg-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 portrait; margin: 10mm; }
                }
                .sreg-page {
                    width: 210mm;
                    min-height: 297mm;
                    padding: 10px;
                    margin: 0 auto;
                    box-sizing: border-box;
                    page-break-after: always;
                    font-size: 10px;
                }
                .sreg-tbl th {
                    background-color: #ECEFF1;
                    color: #111;
                    border: 1px solid #111;
                    padding: 3px;
                    text-align: center;
                    font-weight: bold;
                }
                .sreg-tbl td {
                    border: 1px solid #111;
                    padding: 3px;
                    color: #111;
                    vertical-align: middle;
                    text-align: center;
                }
                .sreg-tbl td.text-left {
                    text-align: left;
                }
                `}
            </style>
            
            {subjects.map((sub, sIdx) => {
                
                // Calculate max marks for header row 3
                let formMax = (sub.maxNirikhshan||0) + (sub.maxTondiKam||0) + (sub.maxPratyakshik||0) + (sub.maxUpkram||0) + (sub.maxPrakalp||0) + (sub.maxChachani||0) + (sub.maxSwadhyay||0) + (sub.maxItar||0);
                let summMax = (sub.maxTondi||0) + (sub.maxPratyakshikB||0) + (sub.maxLekhi||0);
                if (formMax === 0 && sub.maxMarks > 0) formMax = sub.maxMarks / 2;
                if (summMax === 0 && sub.maxMarks > 0) summMax = sub.maxMarks / 2;

                return (
                    <div key={sIdx} className="sreg-page rc-standard-page">
                        
                        {/* Title */}
                        <div style={{ textAlign: 'center', fontSize: '14px', fontWeight: 'bold', marginBottom: '8px' }}>
                            {isEn ? "Continuous Comprehensive Evaluation" : "सातत्यपूर्ण सर्वंकष मूल्यमापन"}
                        </div>

                        {/* Headers */}
                        <table style={{ width: '100%', marginBottom: '5px', fontSize: '11px', fontWeight: 'bold' }}>
                            <tbody>
                                <tr>
                                    <td style={{ textAlign: 'left', width: '40%' }}>{school?.name || "-"}</td>
                                    <td style={{ textAlign: 'center', width: '20%' }}></td>
                                    <td style={{ textAlign: 'right', width: '40%' }}>{isEn ? "Year : " : "सन : "}{classInfo?.academicYearLabel || "-"}</td>
                                </tr>
                                <tr>
                                    <td style={{ textAlign: 'left' }}>
                                        {isEn ? "Class: " : "इयत्ता: "}{classInfo?.className || "-"} 
                                        {isEn ? ", Division: " : ", तुकडी: "}{classInfo?.division || "-"}
                                    </td>
                                    <td style={{ textAlign: 'center', fontSize: '13px' }}>{sub.name}</td>
                                    <td style={{ textAlign: 'right' }}>{isSem2 ? (isEn ? "Second Semester" : "द्वितीय सत्र") : (isEn ? "First Semester" : "प्रथम सत्र")}</td>
                                </tr>
                            </tbody>
                        </table>

                        {/* Main Table */}
                        <table className="sreg-tbl" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '9px' }}>
                            <thead>
                                {/* Row 1 */}
                                <tr>
                                    <th rowSpan="3" style={{ width: '4%' }}>{isEn ? "Sr.No." : "अ. नं"}</th>
                                    <th rowSpan="3" style={{ width: '18%' }}>{isEn ? "Student Name" : "विद्यार्थ्याचे नाव"}</th>
                                    <th colSpan="9">{isEn ? "Formative (A)" : "आकारिक (अ)"}</th>
                                    <th colSpan="4">{isEn ? "Summative (B)" : "संकलित (ब)"}</th>
                                    <th rowSpan="3" style={{ width: '5%' }}>{isEn ? "A+B" : "अ+ब"}</th>
                                    <th rowSpan="3" style={{ width: '5%' }}>{isEn ? "Total" : "श्रे.गुण"}</th>
                                    <th rowSpan="3" style={{ width: '5%' }}>{isEn ? "Grade" : "श्रेणी"}</th>
                                </tr>
                                {/* Row 2 */}
                                <tr>
                                    {formLabels.map((lbl, i) => <th key={`f-${i}`}>{lbl}</th>)}
                                    {summLabels.map((lbl, i) => <th key={`s-${i}`}>{lbl}</th>)}
                                </tr>
                                {/* Row 3 - Max marks */}
                                <tr style={{ backgroundColor: '#E3F2FD' }}>
                                    <th>{blank(sub.maxNirikhshan)}</th>
                                    <th>{blank(sub.maxTondiKam)}</th>
                                    <th>{blank(sub.maxPratyakshik)}</th>
                                    <th>{blank(sub.maxUpkram)}</th>
                                    <th>{blank(sub.maxPrakalp)}</th>
                                    <th>{blank(sub.maxChachani)}</th>
                                    <th>{blank(sub.maxSwadhyay)}</th>
                                    <th>{blank(sub.maxItar)}</th>
                                    <th>{blank(formMax)}</th>

                                    <th>{blank(sub.maxTondi)}</th>
                                    <th>{blank(sub.maxPratyakshikB)}</th>
                                    <th>{blank(sub.maxLekhi)}</th>
                                    <th>{blank(summMax)}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {students.map((st, i) => {
                                    const bg = i % 2 === 0 ? '#F8F9FA' : '#FFF';
                                    const rec = isSem2 ? marksSem2?.[st.id] : marksSem1?.[st.id];
                                    const d = rec?.detailedMarks?.[sanitizeKey(sub.name)];

                                    if (d) {
                                        return (
                                            <tr key={st.id} style={{ backgroundColor: bg }}>
                                                <td>{i + 1}</td>
                                                <td className="text-left">{st.name}</td>
                                                
                                                {/* Formative */}
                                                <td>{blank(d.nirikhshan)}</td>
                                                <td>{blank(d.tondiKam)}</td>
                                                <td>{blank(d.pratyakshik)}</td>
                                                <td>{blank(d.upkram)}</td>
                                                <td>{blank(d.prakalp)}</td>
                                                <td>{blank(d.chachani)}</td>
                                                <td>{blank(d.swadhyay)}</td>
                                                <td>{blank(d.itar)}</td>
                                                <td style={{ fontWeight: 'bold' }}>{blank(d.akarikTotal)}</td>
                                                
                                                {/* Summative */}
                                                <td>{blank(d.tondi)}</td>
                                                <td>{blank(d.pratyakshikB)}</td>
                                                <td>{blank(d.lekhi)}</td>
                                                <td style={{ fontWeight: 'bold' }}>{blank(d.sanklit)}</td>
                                                
                                                {/* Final */}
                                                <td style={{ fontWeight: 'bold' }}>{blank(d.grandTotal)}</td>
                                                <td>{blank(d.grandTotal)}</td>
                                                <td style={{ fontWeight: 'bold' }}>{normalizeGrade(d.grade) || "-"}</td>
                                            </tr>
                                        );
                                    } else {
                                        return (
                                            <tr key={st.id} style={{ backgroundColor: bg }}>
                                                <td>{i + 1}</td>
                                                <td className="text-left">{st.name}</td>
                                                {[...Array(16)].map((_, emptyI) => <td key={emptyI} style={{ color: '#aaa' }}>-</td>)}
                                            </tr>
                                        );
                                    }
                                })}
                            </tbody>
                        </table>
                    </div>
                );
            })}
        </div>
    );
}
