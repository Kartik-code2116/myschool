import React from 'react';
import useLanguage from '../../../utils/useLanguage';

const sanitizeKey = (key) => {
    if (!key) return "unknown";
    return key.replace(/[\.#\$\[\]\/\\~\*]/g, "_");
};

const getGrade = (percentage) => {
    if (percentage >= 91) return "A1";
    if (percentage >= 81) return "A2";
    if (percentage >= 71) return "B1";
    if (percentage >= 61) return "B2";
    if (percentage >= 51) return "C1";
    if (percentage >= 41) return "C2";
    if (percentage >= 33) return "D";
    if (percentage >= 21) return "E1";
    return "E2";
};

export default function MarksRegisterT3({ school, classInfo, year, semester, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    // Ensure we only show one student per page, or iterate if multiple
    const subjects = classInfo?.subjects || [];

    return (
        <div className="report-marks-reg-wrapper">
            {students.map((student, idx) => {
                const marksData = (semester?.number === 2 ? marksSem2[student.id] : marksSem1[student.id])?.detailedMarks || {};
                
                let grandObtained = 0;
                let grandMax = 0;

                return (
                    <div key={student.id} className="rc-standard-page" style={{ 
                        pageBreakAfter: idx < students.length - 1 ? 'always' : 'auto',
                        padding: '20px'
                    }}>
                        
                        {/* Title */}
                        <div style={{ textAlign: 'center', fontSize: '18px', fontWeight: 'bold', color: '#111', marginBottom: '15px' }}>
                            {isEn ? "Continuous Comprehensive Evaluation" : "सातत्यपूर्ण सर्वंकष मूल्यमापन"}
                        </div>

                        {/* Meta Header */}
                        <div style={{ marginBottom: '15px', fontSize: '12px', fontWeight: 'bold' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
                                <div>{isEn ? "Name: " : "नाव: "}{student.name}</div>
                                <div>{isEn ? "Year: " : "सन : "}{classInfo?.academicYearLabel || "2025-26"}</div>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <div style={{ width: '40%' }}>
                                    {isEn ? "Class: " : "इयत्ता: "}{classInfo?.className} {isEn ? ", Div: " : ", तुकडी: "}{classInfo?.division || "-"}
                                </div>
                                <div style={{ width: '20%', textAlign: 'center' }}>
                                    {isEn ? "Roll No.: " : "रोल नं.: "}{student.rollNo}
                                </div>
                                <div style={{ width: '40%', textAlign: 'right' }}>
                                    {semester?.number === 2 ? (isEn ? "Second Semester" : "द्वितीय सत्र") : (isEn ? "First Semester" : "प्रथम सत्र")}
                                </div>
                            </div>
                        </div>

                        {/* Main Table */}
                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '10px' }}>
                            <thead>
                                <tr>
                                    <th rowSpan="3" style={thStyle}>{isEn ? "Sr." : "अ.नं"}</th>
                                    <th rowSpan="3" style={{...thStyle, width: '12%'}}>{isEn ? "Subject" : "विषय"}</th>
                                    <th rowSpan="3" style={thStyle}>{isEn ? "Detail" : "तपशील"}</th>
                                    <th colSpan="9" style={thStyle}>{isEn ? "Formative (A)" : "आकारिक (अ)"}</th>
                                    <th colSpan="4" style={thStyle}>{isEn ? "Summative (B)" : "संकलित (ब)"}</th>
                                    <th rowSpan="3" style={thStyle}>{isEn ? "A+B" : "अ+ब"}</th>
                                    <th rowSpan="3" style={thStyle}>{isEn ? "Total" : "श्रे.गुण"}</th>
                                    <th rowSpan="3" style={thStyle}>{isEn ? "Grade" : "श्रेणी"}</th>
                                </tr>
                                <tr>
                                    <th style={thStyle2}>{isEn ? "Obs." : "निरीक्षण"}</th>
                                    <th style={thStyle2}>{isEn ? "Oral" : "तोंडीकाम"}</th>
                                    <th style={thStyle2}>{isEn ? "Pract." : "प्रात्यक्षिक"}</th>
                                    <th style={thStyle2}>{isEn ? "Activity" : "उपक्रम"}</th>
                                    <th style={thStyle2}>{isEn ? "Project" : "प्रकल्प"}</th>
                                    <th style={thStyle2}>{isEn ? "Test" : "चाचणी"}</th>
                                    <th style={thStyle2}>{isEn ? "Assign." : "स्वाध्याय"}</th>
                                    <th style={thStyle2}>{isEn ? "Other" : "इतर"}</th>
                                    <th rowSpan="2" style={thStyle}>{isEn ? "Total" : "एकूण"}</th>
                                    
                                    <th style={thStyle2}>{isEn ? "Oral" : "तोंडी"}</th>
                                    <th style={thStyle2}>{isEn ? "Pract." : "प्रात्य."}</th>
                                    <th style={thStyle2}>{isEn ? "Written" : "लेखी"}</th>
                                    <th rowSpan="2" style={thStyle}>{isEn ? "Total" : "एकूण"}</th>
                                </tr>
                                <tr>
                                    <th style={thStyle3}>1</th><th style={thStyle3}>2</th><th style={thStyle3}>3</th>
                                    <th style={thStyle3}>4</th><th style={thStyle3}>5</th><th style={thStyle3}>6</th>
                                    <th style={thStyle3}>7</th><th style={thStyle3}>8</th>
                                    <th style={thStyle3}>1</th><th style={thStyle3}>2</th><th style={thStyle3}>3</th>
                                </tr>
                            </thead>
                            <tbody>
                                {subjects.map((sub, i) => {
                                    const d = marksData[sanitizeKey(sub.name)] || {};
                                    
                                    let formativeMax = (sub.maxNirikhshan||0) + (sub.maxTondiKam||0) + (sub.maxPratyakshik||0) + (sub.maxUpkram||0) + (sub.maxPrakalp||0) + (sub.maxChachani||0) + (sub.maxSwadhyay||0) + (sub.maxItar||0);
                                    let summativeMax = (sub.maxTondi||0) + (sub.maxPratyakshikB||0) + (sub.maxLekhi||0);
                                    let totalMax = formativeMax + summativeMax;
                                    if (totalMax === 0 && sub.maxMarks > 0) {
                                        formativeMax = Math.floor(sub.maxMarks / 2);
                                        summativeMax = sub.maxMarks - formativeMax;
                                        totalMax = sub.maxMarks;
                                    }

                                    const obtFormative = parseInt(d.akarikTotal) || 0;
                                    const obtSummative = parseInt(d.sanklit) || 0;
                                    const obtTotal = parseInt(d.grandTotal) || 0;
                                    
                                    const isNonAcademic = (sub.maxTondi||0) + (sub.maxPratyakshikB||0) + (sub.maxLekhi||0) === 0 && sub.maxMarks > 0;
                                    if (!isNonAcademic) {
                                        grandObtained += obtTotal;
                                        grandMax += totalMax;
                                    }

                                    return (
                                        <React.Fragment key={sub.name}>
                                            {/* ROW 1: OBTAINED */}
                                            <tr>
                                                <td rowSpan="2" style={tdStyleCenter}>{i + 1}</td>
                                                <td rowSpan="2" style={tdStyleLeft}>{sub.name}</td>
                                                <td style={tdStyleCenter}>{isEn ? "Obt." : "प्राप्त"}</td>
                                                
                                                <td style={tdStyleCenter}>{d.nirikhshan||""}</td>
                                                <td style={tdStyleCenter}>{d.tondiKam||""}</td>
                                                <td style={tdStyleCenter}>{d.pratyakshik||""}</td>
                                                <td style={tdStyleCenter}>{d.upkram||""}</td>
                                                <td style={tdStyleCenter}>{d.prakalp||""}</td>
                                                <td style={tdStyleCenter}>{d.chachani||""}</td>
                                                <td style={tdStyleCenter}>{d.swadhyay||""}</td>
                                                <td style={tdStyleCenter}>{d.itar||""}</td>
                                                <td style={{...tdStyleCenter, fontWeight: 'bold'}}>{obtFormative||""}</td>
                                                
                                                <td style={tdStyleCenter}>{d.tondi||""}</td>
                                                <td style={tdStyleCenter}>{d.pratyakshikB||""}</td>
                                                <td style={tdStyleCenter}>{d.lekhi||""}</td>
                                                <td style={{...tdStyleCenter, fontWeight: 'bold'}}>{obtSummative||""}</td>
                                                
                                                <td rowSpan="2" style={{...tdStyleCenter, fontWeight: 'bold'}}>{obtTotal||""}</td>
                                                <td rowSpan="2" style={tdStyleCenter}>{obtTotal||""}</td>
                                                <td rowSpan="2" style={{...tdStyleCenter, fontWeight: 'bold'}}>{totalMax ? getGrade((obtTotal/totalMax)*100) : "—"}</td>
                                            </tr>
                                            {/* ROW 2: MAX */}
                                            <tr style={{ backgroundColor: '#F8F9FA' }}>
                                                <td style={tdStyleCenter}>{isEn ? "Max" : "पैकी"}</td>
                                                <td style={tdStyleCenter}>{sub.maxNirikhshan||""}</td>
                                                <td style={tdStyleCenter}>{sub.maxTondiKam||""}</td>
                                                <td style={tdStyleCenter}>{sub.maxPratyakshik||""}</td>
                                                <td style={tdStyleCenter}>{sub.maxUpkram||""}</td>
                                                <td style={tdStyleCenter}>{sub.maxPrakalp||""}</td>
                                                <td style={tdStyleCenter}>{sub.maxChachani||""}</td>
                                                <td style={tdStyleCenter}>{sub.maxSwadhyay||""}</td>
                                                <td style={tdStyleCenter}>{sub.maxItar||""}</td>
                                                <td style={{...tdStyleCenter, fontWeight: 'bold'}}>{formativeMax||""}</td>
                                                
                                                <td style={tdStyleCenter}>{sub.maxTondi||""}</td>
                                                <td style={tdStyleCenter}>{sub.maxPratyakshikB||""}</td>
                                                <td style={tdStyleCenter}>{sub.maxLekhi||""}</td>
                                                <td style={{...tdStyleCenter, fontWeight: 'bold'}}>{summativeMax||""}</td>
                                            </tr>
                                        </React.Fragment>
                                    );
                                })}
                            </tbody>
                        </table>

                        {/* Summary */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '15px', fontWeight: 'bold', fontSize: '13px' }}>
                            <div>{isEn ? "Total Marks: " : "एकूण गुण : "}{grandObtained} / {grandMax}</div>
                            <div>{isEn ? "Percent: " : "शे.गुण : "}{grandMax > 0 ? ((grandObtained/grandMax)*100).toFixed(1) : "0.0"} %</div>
                            <div>{isEn ? "Overall Grade: " : "सर्वसाधारण श्रेणी : "}{grandMax > 0 ? getGrade((grandObtained/grandMax)*100) : "-"}</div>
                        </div>

                        {/* Signatures */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '50px', fontSize: '12px', fontWeight: 'bold' }}>
                            <div>{isEn ? "Class Teacher : " : "वर्गशिक्षक स्वाक्षरी : "}{classInfo?.teacherName || ""}</div>
                            <div>{isEn ? "Principal : " : "मुख्याध्यापक स्वाक्षरी : "}{school?.principalName || ""}</div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
}

const thStyle = {
    backgroundColor: '#DAE9F5',
    border: '1px solid #333',
    padding: '4px',
    textAlign: 'center',
    color: '#000'
};
const thStyle2 = { ...thStyle, fontSize: '9px', padding: '2px' };
const thStyle3 = { backgroundColor: '#E3F2FD', border: '1px solid #333', padding: '2px', textAlign: 'center', fontSize: '8px' };

const tdStyleCenter = {
    border: '1px solid #333',
    padding: '4px',
    textAlign: 'center',
    color: '#111'
};
const tdStyleLeft = { ...tdStyleCenter, textAlign: 'left' };
