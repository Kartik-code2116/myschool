import React from 'react';
import useLanguage from '../../../utils/useLanguage';

export default function HPCT19({ school, classInfo, students }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    const dimensions = isEn ? [
        "Cognitive Development (Academic)",
        "Socio-Emotional Development",
        "Physical & Motor Development",
        "Creative & Artistic Development",
        "Communication & Language",
        "Values & Disposition"
    ] : [
        "बौद्धिक विकास (शैक्षणिक)",
        "सामाजिक-भावनिक विकास",
        "शारीरिक आणि कारक विकास",
        "सर्जनशील व कलात्मक विकास",
        "संभाषण आणि भाषा विकास",
        "मूल्ये आणि वृत्ती"
    ];

    const hardcodedRemark = isEn 
        ? "Student is showing excellent progress in cognitive skills and works well with peers. Continues to display strong leadership qualities during group activities. Need to encourage more participation in physical activities."
        : "विद्यार्थी बौद्धिक कौशल्यांमध्ये उत्तम प्रगती दर्शवत आहे आणि सहकाऱ्यांसोबत चांगले काम करतो. शारीरिक उपक्रमांमध्ये अधिक सहभाग घेण्यास प्रोत्साहित करणे आवश्यक आहे.";

    return (
        <div className="report-hpc-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 portrait; margin: 15mm; }
                }
                .hpc-page {
                    width: 210mm;
                    min-height: 297mm;
                    padding: 20px;
                    margin: 0 auto;
                    box-sizing: border-box;
                    page-break-after: always;
                    font-family: sans-serif;
                }
                .hpc-title {
                    font-size: 18px;
                    font-weight: bold;
                    text-align: center;
                    margin-bottom: 20px;
                    color: #111;
                }
                .hpc-header {
                    width: 100%;
                    margin-bottom: 20px;
                    font-size: 12px;
                    font-weight: bold;
                }
                .hpc-section-title {
                    font-size: 14px;
                    font-weight: bold;
                    color: #1976D2;
                    margin-bottom: 10px;
                }
                .hpc-tbl {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 11px;
                    margin-bottom: 20px;
                }
                .hpc-tbl th {
                    background-color: #DAE6F3;
                    border: 1px solid #111;
                    padding: 8px;
                    text-align: center;
                }
                .hpc-tbl td {
                    border: 1px solid #111;
                    padding: 8px;
                }
                .hpc-remark-box {
                    border: 1px solid #111;
                    padding: 15px;
                    font-size: 12px;
                    min-height: 60px;
                    margin-bottom: 40px;
                }
                .hpc-sig {
                    display: flex;
                    justify-content: space-between;
                    font-size: 12px;
                    font-weight: bold;
                }
                .hpc-sig div {
                    text-align: center;
                    width: 30%;
                }
                `}
            </style>
            
            {students.map((student, idx) => (
                <div key={student.id} className="hpc-page rc-standard-page">
                    
                    <div className="hpc-title">
                        {isEn ? "Holistic Progress Card (HPC)" : "सर्वांगीण प्रगतीपत्रक (HPC)"}
                    </div>

                    <table className="hpc-header">
                        <tbody>
                            <tr>
                                <td style={{ width: '50%', textAlign: 'left', paddingBottom: '5px' }}>
                                    {isEn ? "Student Name: " : "विद्यार्थ्याचे नाव: "}{student.name || "-"}
                                </td>
                                <td style={{ width: '50%', textAlign: 'right', paddingBottom: '5px' }}>
                                    {isEn ? "Class: " : "इयत्ता: "}{classInfo?.className || "-"} - {classInfo?.division || "-"}
                                </td>
                            </tr>
                            <tr>
                                <td style={{ textAlign: 'left', paddingBottom: '5px' }}>
                                    {isEn ? "Roll No: " : "हजेरी क्र.: "}{student.rollNo || "-"}
                                </td>
                                <td style={{ textAlign: 'right', paddingBottom: '5px' }}>
                                    {isEn ? "Year: " : "वर्ष: "}{classInfo?.academicYearLabel || "-"}
                                </td>
                            </tr>
                            <tr>
                                <td style={{ textAlign: 'left' }}>
                                    {isEn ? "School: " : "शाळेचे नाव: "}{school?.name || "-"}
                                </td>
                                <td style={{ textAlign: 'right' }}>
                                    {isEn ? "UDISE: " : "युडायस: "}{school?.udiseCode || "-"}
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    <div className="hpc-section-title">
                        {isEn ? "NEP 2020: 360 Degree Multidimensional Assessment" : "राष्ट्रीय शैक्षणिक धोरण २०२०: ३६० अंश बहुआयामी मूल्यमापन"}
                    </div>

                    <table className="hpc-tbl">
                        <thead>
                            <tr>
                                <th style={{ width: '6%' }}>{isEn ? "Sr." : "अ.क्र."}</th>
                                <th style={{ width: '38%' }}>{isEn ? "Dimensions of Development" : "विकासाची क्षेत्रे"}</th>
                                <th style={{ width: '14%' }}>{isEn ? "Self\nAssessment" : "स्वयं\nमूल्यमापन"}</th>
                                <th style={{ width: '14%' }}>{isEn ? "Peer\nAssessment" : "सहपाठी\nमूल्यमापन"}</th>
                                <th style={{ width: '14%' }}>{isEn ? "Teacher\nAssessment" : "शिक्षक\nमूल्यमापन"}</th>
                                <th style={{ width: '14%' }}>{isEn ? "Parent\nAssessment" : "पालक\nमूल्यमापन"}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {dimensions.map((dim, i) => {
                                const bg = i % 2 !== 0 ? '#F8F9FA' : '#FFF';
                                return (
                                    <tr key={i} style={{ backgroundColor: bg }}>
                                        <td style={{ textAlign: 'center' }}>{i + 1}</td>
                                        <td>{dim}</td>
                                        <td style={{ textAlign: 'center', fontWeight: 'bold' }}>✓</td>
                                        <td style={{ textAlign: 'center', fontWeight: 'bold' }}>✓</td>
                                        <td style={{ textAlign: 'center', fontWeight: 'bold' }}>✓</td>
                                        <td style={{ textAlign: 'center', fontWeight: 'bold' }}>✓</td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>

                    <div className="hpc-section-title">
                        {isEn ? "Teacher's Qualitative Remarks" : "शिक्षकांचा अभिप्राय"}
                    </div>
                    
                    <div className="hpc-remark-box">
                        {hardcodedRemark}
                    </div>

                    <div className="hpc-sig">
                        <div>
                            <div>{isEn ? "Class Teacher" : "वर्गशिक्षक"}</div>
                            <div style={{ marginTop: '2px', fontWeight: 'normal' }}>{classInfo?.teacherName || ""}</div>
                        </div>
                        <div>
                            <div>{isEn ? "Parent/Guardian" : "पालक"}</div>
                        </div>
                        <div>
                            <div>{isEn ? "Headmaster" : "मुख्याध्यापक"}</div>
                            <div style={{ marginTop: '2px', fontWeight: 'normal' }}>{school?.principalName || ""}</div>
                        </div>
                    </div>

                </div>
            ))}
        </div>
    );
}
