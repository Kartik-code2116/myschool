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

// Mirroring the Java remarkContaining logic
const findGlobalRemark = (rec, keyword1, keyword2) => {
    if (!rec || !rec.detailedMarks) return "";
    for (const [key, detail] of Object.entries(rec.detailedMarks)) {
        if (detail && detail.remark && detail.remark.trim() !== "") {
            const safeKey = sanitizeKey(key).toLowerCase();
            if (safeKey.includes(keyword1.toLowerCase()) || safeKey.includes(keyword2.toLowerCase())) {
                return detail.remark.replace(/\|\|/g, ", ").trim();
            }
        }
    }
    return "";
};

export default function ProgressCardInnerT10({ school, classInfo, year, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    const allSubjects = classInfo?.subjects || [];
    // Filter out pseudo-subjects
    const subjects = allSubjects.filter(sub => {
        const sName = (sub.name || "").toLowerCase();
        return !(sName.includes("vishesh") || sName.includes("aavad") || sName.includes("sudharna") || sName.includes("vyaktimatva") ||
                 sName.includes("विशेष") || sName.includes("आवड") || sName.includes("सुधारणा") || sName.includes("व्यक्तिमत्व"));
    });

    // Calculate row spans for the 3 descriptive remarks blocks
    const totalSubjects = subjects.length;
    let span1 = 1, span2 = 1, span3 = 1;
    if (totalSubjects > 0) {
        span1 = Math.ceil(totalSubjects / 3);
        const remaining1 = totalSubjects - span1;
        if (remaining1 > 0) {
            span2 = Math.ceil(remaining1 / 2);
            span3 = remaining1 - span2;
        } else {
            span2 = 0;
            span3 = 0;
        }
    }

    const renderPanel = (student, isSem2) => {
        const title = isSem2 ? (isEn ? "● Second Semester ●" : "● द्वितीय सत्र ●") : (isEn ? "● First Semester ●" : "● प्रथम सत्र ●");
        const rec = isSem2 ? marksSem2?.[student.id] : marksSem1?.[student.id];

        const vishesh = findGlobalRemark(rec, "vishesh", "विशेष");
        const aavad = findGlobalRemark(rec, "aavad", "आवड");
        const sudharna = findGlobalRemark(rec, "sudharna", "सुधारणा");

        return (
            <div className="pci-panel">
                <div className="pci-title">{title}</div>
                
                {/* Header Table */}
                <table style={{ width: '100%', marginBottom: '5px', fontSize: '10px' }}>
                    <tbody>
                        <tr>
                            <td style={{ textAlign: 'left', fontWeight: 'bold' }}>{isEn ? "Name: " : "नाव: "}{student.name || "-"}</td>
                            <td style={{ textAlign: 'center' }}></td>
                            <td style={{ textAlign: 'right', fontWeight: 'bold' }}>{isEn ? "Year: " : "सन: "}{classInfo?.academicYearLabel || "-"}</td>
                        </tr>
                        <tr>
                            <td style={{ textAlign: 'left' }}>
                                {isEn ? "Class: " : "इयत्ता: "}{classInfo?.className || "-"}
                                {isEn ? ", Div: " : ", तुकडी: "}{classInfo?.division || "-"}
                            </td>
                            <td style={{ textAlign: 'center' }}>{isEn ? "Roll No: " : "हजेरी क्रमांक: "}{student.rollNo || "-"}</td>
                            <td style={{ textAlign: 'right' }}>{isEn ? "UDISE: " : "युडायस: "}{school?.udiseCode || "-"}</td>
                        </tr>
                    </tbody>
                </table>

                {/* Main Table */}
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '10px', flex: 1 }}>
                    <thead>
                        <tr>
                            <th style={{...thStyle, width: '8%'}}>{isEn ? "Sr.No." : "अ.नं"}</th>
                            <th style={{...thStyle, width: '25%'}}>{isEn ? "Subject" : "विषय"}</th>
                            <th style={{...thStyle, width: '12%'}}>{isEn ? "Grade" : "श्रेणी"}</th>
                            <th style={{...thStyle, width: '55%'}}>{isEn ? "Descriptive Remarks" : "वर्णनात्मक नोंदी"}</th>
                        </tr>
                    </thead>
                    <tbody>
                        {subjects.map((sub, idx) => {
                            const rowIdx = idx + 1;
                            const d = rec?.detailedMarks?.[sanitizeKey(sub.name)];
                            const grade = normalizeGrade(d?.grade);
                            const bg = idx % 2 === 0 ? '#F8F9FA' : '#FFF';

                            let remarkCell = null;
                            if (rowIdx === 1 && span1 > 0) {
                                remarkCell = (
                                    <td rowSpan={span1} style={{...tdStyle, backgroundColor: '#FFF', verticalAlign: 'top', position: 'relative'}}>
                                        <div style={{ fontWeight: 'bold', borderBottom: '1px solid #ddd', paddingBottom: '2px', marginBottom: '4px' }}>
                                            {isEn ? "Special Development :" : "विशेष प्रगती :"}
                                        </div>
                                        <div style={{ minHeight: '30px' }}>{vishesh}</div>
                                    </td>
                                );
                            } else if (rowIdx === 1 + span1 && span2 > 0) {
                                remarkCell = (
                                    <td rowSpan={span2} style={{...tdStyle, backgroundColor: '#FFF', verticalAlign: 'top', position: 'relative'}}>
                                        <div style={{ fontWeight: 'bold', borderBottom: '1px solid #ddd', paddingBottom: '2px', marginBottom: '4px' }}>
                                            {isEn ? "Interest, Hobby :" : "आवड, छंद :"}
                                        </div>
                                        <div style={{ minHeight: '30px' }}>{aavad}</div>
                                    </td>
                                );
                            } else if (rowIdx === 1 + span1 + span2 && span3 > 0) {
                                remarkCell = (
                                    <td rowSpan={span3} style={{...tdStyle, backgroundColor: '#FFF', verticalAlign: 'top', position: 'relative'}}>
                                        <div style={{ fontWeight: 'bold', borderBottom: '1px solid #ddd', paddingBottom: '2px', marginBottom: '4px' }}>
                                            {isEn ? "Necessary Improvement :" : "सुधारणा आवश्यक :"}
                                        </div>
                                        <div style={{ minHeight: '30px' }}>{sudharna}</div>
                                    </td>
                                );
                            }

                            return (
                                <tr key={idx} style={{ backgroundColor: bg }}>
                                    <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{rowIdx}</td>
                                    <td style={tdStyle}>{sub.name}</td>
                                    <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{grade || "-"}</td>
                                    {remarkCell}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>

                {/* Signatures */}
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '30px', fontSize: '11px', fontWeight: 'bold' }}>
                    <div style={{ width: '45%', textAlign: 'center' }}>
                        {isEn ? "Teacher Signature : " : "शिक्षक स्वाक्षरी : "}{classInfo?.teacherName || ""}
                    </div>
                    <div style={{ width: '45%', textAlign: 'center' }}>
                        {isEn ? "Headmaster Signature : " : "मुख्याध्यापक स्वाक्षरी : "}{school?.principalName || ""}
                    </div>
                </div>
            </div>
        );
    };

    return (
        <div className="report-pci-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 landscape; margin: 10mm; }
                }
                .pci-page {
                    display: flex;
                    width: 297mm;
                    min-height: 210mm;
                    margin: 0 auto;
                    box-sizing: border-box;
                    page-break-after: always;
                    padding: 10px;
                }
                .pci-panel {
                    width: 50%;
                    padding: 8px 15px;
                    box-sizing: border-box;
                    display: flex;
                    flex-direction: column;
                }
                .pci-title {
                    text-align: center;
                    font-size: 14px;
                    font-weight: bold;
                    color: #FFF;
                    background-color: #0097A7;
                    padding: 4px;
                    border-radius: 4px;
                    margin-bottom: 8px;
                }
                `}
            </style>
            
            {students.map((student, idx) => (
                <div key={student.id} className="pci-page rc-standard-page">
                    {renderPanel(student, false)}
                    <div style={{ width: '1px', backgroundColor: '#ddd', margin: '0 5px' }}></div>
                    {renderPanel(student, true)}
                </div>
            ))}
        </div>
    );
}

const thStyle = {
    backgroundColor: '#0097A7',
    color: '#FFF',
    border: '1px solid #111',
    padding: '4px',
    textAlign: 'center',
    fontWeight: 'bold'
};

const tdStyle = {
    border: '1px solid #111',
    padding: '4px',
    color: '#111',
    verticalAlign: 'middle'
};
