import React from 'react';
import useLanguage from '../../../utils/useLanguage';

const sanitizeKey = (key) => {
    if (!key) return "unknown";
    return key.replace(/[\.#\$\[\]\/\\~\*]/g, "_");
};

export default function DescriptiveRemarksT4({ school, classInfo, year, semester, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    const subjects = classInfo?.subjects || [];

    const getExtraRemark = (marksData, key1, key2) => {
        if (!marksData) return "";
        // First check specialRemarks object which we built into the web app earlier
        if (marksData.specialRemarks) {
            if (key1 === 'vishesh' && marksData.specialRemarks.progress) return marksData.specialRemarks.progress;
            if (key1 === 'aavad' && marksData.specialRemarks.interests) return marksData.specialRemarks.interests;
            if (key1 === 'sudharna' && marksData.specialRemarks.needsImprovement) return marksData.specialRemarks.needsImprovement;
            if (key1 === 'vyaktimatva' && marksData.specialRemarks.personality) return marksData.specialRemarks.personality;
        }

        // Fallback: search detailedMarks keys like Android app does
        if (marksData.detailedMarks) {
            for (const [k, v] of Object.entries(marksData.detailedMarks)) {
                if (v && v.remark) {
                    const sk = k.toLowerCase();
                    if (sk.includes(key1) || sk.includes(key2)) {
                        return v.remark.replace(/\|\|/g, ", ").trim();
                    }
                }
            }
        }
        return "";
    };

    return (
        <div className="report-desc-wrapper">
            {students.map((student, idx) => {
                const marksRecord = semester?.number === 2 ? marksSem2[student.id] : marksSem1[student.id];
                const marksData = marksRecord?.detailedMarks || {};
                
                let rowIdx = 1;

                return (
                    <div key={student.id} className="rc-standard-page" style={{ 
                        pageBreakAfter: idx < students.length - 1 ? 'always' : 'auto',
                        padding: '28px 28px 32px 28px'
                    }}>
                        
                        {/* Title */}
                        <div style={{ textAlign: 'center', fontSize: '20px', fontWeight: 'bold', color: '#111', marginBottom: '15px' }}>
                            {isEn ? "Continuous Comprehensive Evaluation" : "सातत्यपूर्ण सर्वंकष मूल्यमापन"}
                        </div>

                        {/* Student Info Header */}
                        <div style={{ marginBottom: '15px', fontSize: '13px', fontWeight: 'normal', color: '#111' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                                <div style={{ fontWeight: 'bold' }}>{isEn ? "Name: " : "नाव: "}{student.name}</div>
                                <div></div>
                                <div style={{ fontWeight: 'bold' }}>{isEn ? "Year: " : "सन: "}{classInfo?.academicYearLabel || "-"}</div>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <div>{isEn ? "Class: " : "इयत्ता: "}{classInfo?.className} {isEn ? ", Division: " : ", तुकडी: "}{classInfo?.division || "-"}</div>
                                <div style={{ textAlign: 'center' }}>{isEn ? "Roll No.: " : "रोल नं.: "}{student.rollNo}</div>
                                <div style={{ fontWeight: 'bold', textAlign: 'right' }}>
                                    {semester?.number === 2 ? (isEn ? "Second Semester" : "द्वितीय सत्र") : (isEn ? "First Semester" : "प्रथम सत्र")}
                                </div>
                            </div>
                        </div>

                        {/* Table */}
                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px', marginTop: '10px' }}>
                            <thead>
                                <tr>
                                    <th style={{...thStyle, width: '10%'}}>{isEn ? "Sr.No." : "अ.नं"}</th>
                                    <th style={{...thStyle, width: '30%'}}>{isEn ? "Subject" : "विषय"}</th>
                                    <th style={{...thStyle, width: '60%'}}>{isEn ? "Subject-wise Descriptive Remark" : "विषयवार वर्णनात्मक नोंद"}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {/* Class Subjects */}
                                {subjects.map((sub) => {
                                    const sName = (sub.name || "").toLowerCase();
                                    if (sName.includes("vishesh") || sName.includes("aavad") || sName.includes("sudharna") || sName.includes("vyaktimatva") ||
                                        sName.includes("विशेष") || sName.includes("आवड") || sName.includes("सुधारणा") || sName.includes("व्यक्तिमत्व")) {
                                        return null; // Skip these as they go at the bottom
                                    }

                                    const d = marksData[sanitizeKey(sub.name)];
                                    const remark = d?.remark ? d.remark.replace(/\|\|/g, ", ").trim() : "";
                                    const bg = rowIdx % 2 === 0 ? '#F8F9FA' : '#FFF';
                                    
                                    return (
                                        <tr key={sub.name} style={{ backgroundColor: bg }}>
                                            <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{rowIdx++}</td>
                                            <td style={tdStyle}>{sub.name}</td>
                                            <td style={{...tdStyle, minHeight: '32px'}}>{remark}</td>
                                        </tr>
                                    );
                                })}

                                {/* Extra Rows */}
                                {[
                                    { k1: 'vishesh', k2: 'विशेष', lblEn: 'Special Progress', lblMr: 'विशेष प्रगती' },
                                    { k1: 'aavad', k2: 'आवड', lblEn: 'Interests & Hobbies (Arts, Sports, Literature etc.)', lblMr: 'आवड, छंद कला, क्रीडा, साहित्य इ.' },
                                    { k1: 'sudharna', k2: 'सुधारणा', lblEn: 'Improvement Needed', lblMr: 'सुधारणा आवश्यक' },
                                    { k1: 'vyaktimatva', k2: 'व्यक्तिमत्व', lblEn: 'Personality Traits\n(Attitude, Aptitude, Values, Personality Details)', lblMr: 'व्यक्तिमत्व गुण विशेष\n(अभिवृत्ती, कल, मूल्ये, स्वभाव गुणविशेष)' },
                                ].map((extra) => {
                                    const remark = getExtraRemark(marksRecord, extra.k1, extra.k2);
                                    const bg = rowIdx % 2 === 0 ? '#F8F9FA' : '#FFF';
                                    
                                    return (
                                        <tr key={extra.k1} style={{ backgroundColor: bg }}>
                                            <td style={{...tdStyle, textAlign: 'center', fontWeight: 'bold'}}>{rowIdx++}</td>
                                            <td style={{...tdStyle, whiteSpace: 'pre-wrap'}}>{isEn ? extra.lblEn : extra.lblMr}</td>
                                            <td style={{...tdStyle, minHeight: '40px'}}>{remark}</td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>

                        {/* Signatures */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '60px', fontSize: '13px', fontWeight: 'bold' }}>
                            <div>{isEn ? "Class Teacher" : "वर्गशिक्षक स्वाक्षरी"}</div>
                            <div>{isEn ? "Principal" : "मुख्याध्यापक स्वाक्षरी"}</div>
                        </div>

                    </div>
                );
            })}
        </div>
    );
}

const thStyle = {
    backgroundColor: '#3F51B5', // C_PRIMARY
    color: '#FFF',
    border: '1px solid #111',
    padding: '8px',
    textAlign: 'center',
    fontWeight: 'bold'
};

const tdStyle = {
    border: '1px solid #111',
    padding: '8px',
    color: '#111',
    verticalAlign: 'middle'
};
