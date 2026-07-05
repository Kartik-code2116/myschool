import React from 'react';
import useLanguage from '../../../utils/useLanguage';

const getCasteCategoryIndex = (caste) => {
    if (!caste) return 5; // OPEN
    const c = caste.toUpperCase().trim();
    if (c.includes("SC") || c.includes("अनु.जाती") || c.includes("अनुसुचित जाती") || c === "1" || c === "0") return 0;
    if (c.includes("ST") || c.includes("अनु.जमाती") || c.includes("अनुसुचित जमाती") || c === "2") return 1;
    if (c.includes("VJ") || c.includes("विमुक्त") || c === "3") return 2;
    if (c.includes("NT") || c.includes("भटक्या") || c === "4") return 3;
    if (c.includes("OBC") || c.includes("SBC") || c.includes("इतर") || c.includes("मागास") || c === "5" || c === "6") return 4;
    return 5;
};

const getGradeIndex = (grade) => {
    if (!grade) return -1;
    const g = grade.toUpperCase().trim();
    if (g === "A-1" || g === "A1" || g === "अ-1" || g === "अ१") return 0;
    if (g === "A-2" || g === "A2" || g === "अ-2" || g === "अ२") return 1;
    if (g === "B-1" || g === "B1" || g === "ब-1" || g === "ब१") return 2;
    if (g === "B-2" || g === "B2" || g === "ब-2" || g === "ब२") return 3;
    if (g === "C-1" || g === "C1" || g === "क-1" || g === "क१") return 4;
    if (g === "C-2" || g === "C2" || g === "क-2" || g === "क२") return 5;
    if (g === "D" || g === "ड") return 6;
    if (g === "E-1" || g === "E1" || g === "इ-1" || g === "इ१" || g === "ई-1") return 7;
    if (g === "E-2" || g === "E2" || g === "इ-2" || g === "इ२" || g === "ई-2") return 8;
    return -1;
};

export default function CasteGradeTableT17({ school, classInfo, semester, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';
    const isSem2 = semester?.number === 2;

    const marksMap = isSem2 ? marksSem2 : marksSem1;

    // Data structure: counts[caste][grade][gender]
    // caste: 0-SC, 1-ST, 2-VJ, 3-NT, 4-OBC, 5-OPEN, 6-TOTAL
    // grade: 0..8 (A1..E2), 9-TOTAL
    // gender: 0-Boys, 1-Girls
    const counts = Array(7).fill(0).map(() => Array(10).fill(0).map(() => Array(2).fill(0)));
    let enrolledBoys = 0, enrolledGirls = 0;
    let presentBoys = 0, presentGirls = 0;

    students.forEach(s => {
        const isGirl = s.gender && (s.gender.toLowerCase() === 'female' || s.gender === 'मुली' || s.gender === 'मुलगी' || s.gender === '2');
        const gIdx = isGirl ? 1 : 0;
        const cIdx = getCasteCategoryIndex(s.cast);

        if (isGirl) enrolledGirls++;
        else enrolledBoys++;

        let pDays = 0;
        if (s.monthlyAttendance) {
            Object.values(s.monthlyAttendance).forEach(v => {
                if (v) {
                    if (v.includes('/')) {
                        pDays += (parseInt(v.split('/')[0].trim()) || 0);
                    } else {
                        pDays += (parseInt(v.trim()) || 0);
                    }
                }
            });
        }
        if (pDays > 0) {
            if (isGirl) presentGirls++;
            else presentBoys++;
        }

        const rec = marksMap?.[s.id];
        const gradeIdx = getGradeIndex(rec?.grade);
        
        if (gradeIdx !== -1) {
            counts[cIdx][gradeIdx][gIdx]++;
            counts[cIdx][9][gIdx]++; // Caste Total
            counts[6][gradeIdx][gIdx]++; // Grade Total
            counts[6][9][gIdx]++; // Grand Total
        }
    });

    const casteNames = isEn 
        ? ["SC", "ST", "VJ", "NT", "OBC", "OPEN", "Total"] 
        : ["अनु.जाती", "अनु.जमाती", "विमुक्त जाती", "भटक्या जाती", "इतर मागासवर्ग", "बिगर मागास", "एकूण"];
    
    const gradeHeaders = isEn 
        ? ["A-1", "A-2", "B-1", "B-2", "C-1", "C-2", "D", "E-1", "E-2", "Total"]
        : ["अ-1", "अ-2", "ब-1", "ब-2", "क-1", "क-2", "ड", "इ-1", "इ-2", "एकूण"];

    const blank = (v) => v > 0 ? v : "-";

    return (
        <div className="report-castegrade-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 landscape; margin: 10mm; }
                }
                .cg-page {
                    width: 297mm;
                    min-height: 210mm;
                    padding: 15px;
                    margin: 0 auto;
                    box-sizing: border-box;
                    page-break-after: always;
                }
                .cg-tbl {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 11px;
                    margin-bottom: 20px;
                }
                .cg-tbl th {
                    background-color: #DAE6F3;
                    color: #111;
                    border: 1px solid #111;
                    padding: 6px;
                    text-align: center;
                    font-weight: bold;
                    vertical-align: middle;
                }
                .cg-tbl td {
                    border: 1px solid #111;
                    padding: 6px;
                    color: #111;
                    vertical-align: middle;
                    text-align: center;
                }
                .cg-stat {
                    width: 40%;
                    border-collapse: collapse;
                    font-size: 11px;
                }
                .cg-stat th {
                    background-color: #DAE6F3;
                    color: #111;
                    border: 1px solid #111;
                    padding: 6px;
                }
                .cg-stat td {
                    border: 1px solid #111;
                    padding: 6px;
                    text-align: center;
                }
                `}
            </style>
            
            <div className="cg-page rc-standard-page">
                {/* Header Info */}
                <table style={{ width: '100%', marginBottom: '15px', fontSize: '13px', fontWeight: 'bold' }}>
                    <tbody>
                        <tr>
                            <td style={{ textAlign: 'left', width: '33%' }}>
                                <div>{isEn ? "UDISE: " : "युडायस: "}{school?.udiseCode || "-"}</div>
                                <div style={{ marginTop: '2px' }}>{isEn ? "School: " : "शाळा: "}{school?.name || "-"}</div>
                            </td>
                            <td style={{ textAlign: 'center', width: '34%' }}>
                                <div style={{ fontSize: '16px' }}>{isEn ? "Continuous Comprehensive Evaluation" : "सातत्यपूर्ण सर्वंकष मूल्यमापन"}</div>
                                <div style={{ marginTop: '2px', fontSize: '13px' }}>
                                    {isSem2 ? (isEn ? "Second Semester" : "द्वितीय सत्र") : (isEn ? "First Semester" : "प्रथम सत्र")}
                                </div>
                            </td>
                            <td style={{ textAlign: 'right', width: '33%' }}>
                                <div>{isEn ? "Year : " : "सन : "}{classInfo?.academicYearLabel || "-"}</div>
                                <div style={{ marginTop: '2px' }}>
                                    {isEn ? "Class: " : "इयत्ता: "}{classInfo?.className || "-"} 
                                    {isEn ? ", Division: " : ", तुकडी: "}{classInfo?.division || "-"}
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>

                {/* Main Table */}
                <table className="cg-tbl">
                    <thead>
                        <tr>
                            <th rowSpan="2" style={{ width: '5%' }}>{isEn ? "Sr.No." : "अ.नं"}</th>
                            <th rowSpan="2" style={{ width: '15%' }}>{isEn ? "Caste Category" : "जात संवर्ग"}</th>
                            {gradeHeaders.map((g, i) => <th key={i} colSpan="2">{g}</th>)}
                        </tr>
                        <tr>
                            {gradeHeaders.map((_, i) => (
                                <React.Fragment key={`sub-${i}`}>
                                    <th style={{ backgroundColor: '#E3F2FD' }}>{isEn ? "Boys" : "मुले"}</th>
                                    <th style={{ backgroundColor: '#FCE4EC' }}>{isEn ? "Girls" : "मुली"}</th>
                                </React.Fragment>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {casteNames.map((cName, cIdx) => {
                            const bg = cIdx % 2 !== 0 ? '#F8F9FA' : '#FFF';
                            return (
                                <tr key={cIdx} style={{ backgroundColor: bg, fontWeight: cIdx === 6 ? 'bold' : 'normal' }}>
                                    <td>{cIdx + 1}</td>
                                    <td style={{ textAlign: 'left' }}>{cName}</td>
                                    {Array(10).fill(0).map((_, gIdx) => (
                                        <React.Fragment key={`val-${gIdx}`}>
                                            <td>{blank(counts[cIdx][gIdx][0])}</td>
                                            <td>{blank(counts[cIdx][gIdx][1])}</td>
                                        </React.Fragment>
                                    ))}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginTop: '20px' }}>
                    {/* Stat Table */}
                    <table className="cg-stat">
                        <thead>
                            <tr>
                                <th>{isEn ? "Details" : "तपशील"}</th>
                                <th>{isEn ? "Boys" : "मुले"}</th>
                                <th>{isEn ? "Girls" : "मुली"}</th>
                                <th>{isEn ? "Total" : "एकूण"}</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td style={{ textAlign: 'left', fontWeight: 'bold' }}>{isEn ? "Roll" : "पट"}</td>
                                <td>{blank(enrolledBoys)}</td>
                                <td>{blank(enrolledGirls)}</td>
                                <td>{blank(enrolledBoys + enrolledGirls)}</td>
                            </tr>
                            <tr>
                                <td style={{ textAlign: 'left', fontWeight: 'bold' }}>{isEn ? "Attendance" : "उपस्थिती"}</td>
                                <td>{blank(presentBoys)}</td>
                                <td>{blank(presentGirls)}</td>
                                <td>{blank(presentBoys + presentGirls)}</td>
                            </tr>
                        </tbody>
                    </table>

                    {/* Signatures */}
                    <div style={{ width: '40%', fontSize: '13px', fontWeight: 'bold' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '40px' }}>
                            <div style={{ textAlign: 'center' }}>
                                <div>{isEn ? "Class Teacher Signature" : "वर्गशिक्षक स्वाक्षरी"}</div>
                                <div>{classInfo?.teacherName || ""}</div>
                            </div>
                            <div style={{ textAlign: 'center' }}>
                                <div>{isEn ? "Headmaster Signature" : "मुख्याध्यापक स्वाक्षरी"}</div>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    );
}
