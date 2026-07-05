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

export default function ProgressFirstSemT18({ school, classInfo, semester, students, marksSem1, marksSem2 }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    const MONTHS_EN = ["JUN", "JUL", "AUG", "SEP", "OCT", "NOV"];
    const MONTHS_MR = ["जून", "जुलै", "ऑगस्ट", "सप्टें", "ऑक्टो", "नोव्हे"];
    const activeMonths = isEn ? MONTHS_EN : MONTHS_MR;

    // Default Subjects
    const defaultSubsMR = ["मराठी", "हिंदी", "इंग्रजी", "गणित", "सा.वि./ प.अ.", "स.शा.", "कला", "कार्यानुभव", "शा.शि."];
    const defaultSubsEN = ["Marathi", "Hindi", "English", "Maths", "Science", "Social Sci.", "Art", "Work Exp.", "P.E."];
    let subjects = classInfo?.subjects?.map(s => s.name) || [];
    if (subjects.length === 0) {
        subjects = isEn ? defaultSubsEN : defaultSubsMR;
    } else {
        // Ensure we have 9 subjects for the layout by padding if needed
        while(subjects.length < 9) subjects.push("");
        // Or truncate to 9
        if (subjects.length > 9) subjects = subjects.slice(0, 9);
    }

    const calcAge = (dobStr) => {
        if (!dobStr) return "-";
        try {
            const parts = dobStr.split('-');
            if (parts.length === 3) {
                const dob = new Date(parts[2], parts[1] - 1, parts[0]);
                if (!isNaN(dob.getTime())) {
                    const diffMs = Date.now() - dob.getTime();
                    const ageDt = new Date(diffMs);
                    const years = Math.abs(ageDt.getUTCFullYear() - 1970);
                    const months = ageDt.getUTCMonth();
                    return `${years}${isEn ? " Y., " : " व., "}${months}${isEn ? " M." : " म."}`;
                }
            }
        } catch (e) {}
        return "-";
    };

    const findRemark = (rec, subName) => {
        if (!rec?.detailedMarks) return "";
        let rem = rec.detailedMarks[sanitizeKey(subName)]?.remark;
        if (rem) return rem.replace(/\|\|/g, ", ").trim();
        
        const safeName = sanitizeKey(subName).toLowerCase();
        for (const [k, v] of Object.entries(rec.detailedMarks)) {
            if (v?.remark) {
                const safeK = sanitizeKey(k).toLowerCase();
                if (safeK === safeName || safeK.includes(safeName) || safeName.includes(safeK)) {
                    return v.remark.replace(/\|\|/g, ", ").trim();
                }
            }
        }
        return "";
    };

    return (
        <div className="report-progress1sem-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 landscape; margin: 10mm; }
                }
                .p1s-page {
                    width: 297mm;
                    min-height: 210mm;
                    padding: 15px;
                    margin: 0 auto;
                    box-sizing: border-box;
                    page-break-after: always;
                    border: 3px solid #6096B4;
                    border-radius: 15px;
                    display: flex;
                    justify-content: space-between;
                }
                .p1s-left {
                    width: 48%;
                    padding-right: 15px;
                    border-right: 1px dashed #ccc;
                }
                .p1s-right {
                    width: 50%;
                    padding-left: 15px;
                }
                .p1s-pill {
                    background-color: #FCE4EC;
                    color: #D81B60;
                    font-size: 16px;
                    font-weight: bold;
                    padding: 6px 30px;
                    border-radius: 15px;
                    display: inline-block;
                    margin-bottom: 15px;
                }
                .p1s-block {
                    background-color: #F0F2F5;
                    border-radius: 10px;
                    padding: 10px;
                    margin-bottom: 15px;
                }
                .p1s-detail-table {
                    width: 100%;
                    font-size: 10px;
                }
                .p1s-detail-table td {
                    padding: 2px;
                    vertical-align: top;
                }
                .p1s-bullet {
                    color: #2980B9;
                    margin-right: 4px;
                }
                .p1s-label { color: #444; }
                .p1s-value { font-weight: bold; color: #111; }
                .p1s-att-table {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 9px;
                }
                .p1s-att-table th, .p1s-att-table td {
                    border: 1px solid #6096B4;
                    padding: 4px 2px;
                    text-align: center;
                }
                .p1s-marks-table {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 10px;
                }
                .p1s-marks-table th {
                    background-color: #DAE6F3;
                    border: 1px solid #6096B4;
                    padding: 6px;
                    text-align: center;
                    font-weight: bold;
                }
                .p1s-marks-table td {
                    border: 1px solid #6096B4;
                    padding: 4px;
                    vertical-align: middle;
                }
                `}
            </style>
            
            {students.map((student, idx) => {
                const rec = marksSem1?.[student.id]; // Strict Sem 1

                const wd = new Array(6).fill(0);
                const pd = new Array(6).fill(0);
                if (student.monthlyAttendance) {
                    for (let i = 0; i < 6; i++) {
                        const att = student.monthlyAttendance[MONTHS_MR[i]] || student.monthlyAttendance[MONTHS_EN[i]];
                        if (att) {
                            if (att.includes("/")) {
                                const parts = att.split("/");
                                pd[i] = parseInt(parts[0].trim()) || 0;
                                wd[i] = parseInt(parts[1].trim()) || 0;
                            } else {
                                pd[i] = parseInt(att.trim()) || 0;
                            }
                        }
                    }
                }

                const Label = ({ text }) => <><span className="p1s-bullet">●</span><span className="p1s-label">{text}</span></>;

                return (
                    <div key={student.id} className="p1s-page rc-standard-page">
                        
                        {/* LEFT PANEL */}
                        <div className="p1s-left">
                            <div style={{ textAlign: 'center', marginBottom: '10px' }}>
                                <div style={{ fontSize: '10px', fontWeight: 'bold' }}>
                                    {isEn ? "UDISE: " : "युडायस: "}{school?.udiseCode || "-"}
                                </div>
                                <div style={{ fontSize: '16px', fontWeight: 'bold', margin: '2px 0' }}>
                                    {isEn ? "Zilla Parishad Primary School " : "जिल्हा परिषद प्राथमिक शाळा "}{school?.name || ""}
                                </div>
                                <div style={{ fontSize: '10px' }}>
                                    {school?.address || ""}
                                </div>
                                <div style={{ fontSize: '10px', fontWeight: 'bold' }}>
                                    {isEn ? "Year: " : "सन: "}{classInfo?.academicYearLabel || "-"}
                                </div>
                            </div>

                            <div style={{ textAlign: 'center' }}>
                                <div className="p1s-pill">{isEn ? "PROGRESS CARD" : "प्रगती पत्रक"}</div>
                            </div>

                            <div className="p1s-block">
                                <table className="p1s-detail-table">
                                    <tbody>
                                        <tr>
                                            <td style={{ width: '25%' }}><Label text={isEn ? "Name" : "नाव"} /></td>
                                            <td colSpan="3" className="p1s-value">: {student.name || "-"}</td>
                                        </tr>
                                        <tr>
                                            <td><Label text={isEn ? "Student ID" : "स्टुडंट ID"} /></td>
                                            <td colSpan="3" className="p1s-value">: {student.studentIdNumber || "-"}</td>
                                        </tr>
                                        <tr>
                                            <td><Label text={isEn ? "Roll No." : "हजेरी क्रमांक"} /></td>
                                            <td style={{ width: '25%' }} className="p1s-value">: {student.rollNo || "-"}</td>
                                            <td style={{ width: '25%' }}><Label text={isEn ? "Reg. No." : "रजि.नंबर"} /></td>
                                            <td style={{ width: '25%' }} className="p1s-value">: {student.registrationNo || "-"}</td>
                                        </tr>
                                        <tr>
                                            <td><Label text={isEn ? "Class" : "इयत्ता"} /></td>
                                            <td className="p1s-value">: {classInfo?.className || "-"}</td>
                                            <td><Label text={isEn ? "Division" : "तुकडी"} /></td>
                                            <td className="p1s-value">: {classInfo?.division || "-"}</td>
                                        </tr>
                                        <tr>
                                            <td><Label text={isEn ? "Medium" : "माध्यम"} /></td>
                                            <td className="p1s-value">: {student.medium || "-"}</td>
                                            <td><Label text={isEn ? "Date of Birth" : "जन्मतारीख"} /></td>
                                            <td className="p1s-value">: {student.dob || "-"}</td>
                                        </tr>
                                        <tr>
                                            <td><Label text={isEn ? "Mother Tongue" : "मातृभाषा"} /></td>
                                            <td className="p1s-value">: {student.motherTongue || "-"}</td>
                                            <td><Label text={isEn ? "Age" : "वय"} /></td>
                                            <td className="p1s-value">: {calcAge(student.dob)}</td>
                                        </tr>
                                        <tr>
                                            <td><Label text={isEn ? "Mother's Name" : "आईचे नाव"} /></td>
                                            <td colSpan="3" className="p1s-value">: {student.motherName || "-"}</td>
                                        </tr>
                                        <tr>
                                            <td><Label text={isEn ? "Father's Name" : "वडिलांचे नाव"} /></td>
                                            <td colSpan="3" className="p1s-value">: {student.fatherName || "-"}</td>
                                        </tr>
                                        <tr>
                                            <td><Label text={isEn ? "Address" : "पत्ता"} /></td>
                                            <td colSpan="3" className="p1s-value">: {student.address || "-"}</td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>

                            <div style={{ color: '#D81B60', fontSize: '11px', fontWeight: 'bold', marginBottom: '5px' }}>
                                {isEn ? "Attendance :" : "उपस्थिती :"}
                            </div>
                            <table className="p1s-att-table">
                                <thead>
                                    <tr>
                                        <th style={{ width: '25%', backgroundColor: '#F0F2F5' }}>{isEn ? "Month" : "महिना"}</th>
                                        {activeMonths.map((m, i) => <td key={i} style={{ fontWeight: 'bold' }}>{m}</td>)}
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <th style={{ backgroundColor: '#F0F2F5' }}>{isEn ? "Working Days" : "कामाचे दिवस"}</th>
                                        {wd.map((w, i) => <td key={`w-${i}`}>{w > 0 ? w : ""}</td>)}
                                    </tr>
                                    <tr>
                                        <th style={{ backgroundColor: '#F0F2F5' }}>{isEn ? "Present Days" : "हजर दिवस"}</th>
                                        {pd.map((p, i) => <td key={`p-${i}`}>{p > 0 ? p : (wd[i] > 0 ? "0" : "")}</td>)}
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        {/* RIGHT PANEL */}
                        <div className="p1s-right">
                            <div style={{ color: '#D81B60', fontSize: '12px', fontWeight: 'bold', marginBottom: '10px' }}>
                                {isEn ? "First Semester: Grades & Remarks" : "प्रथम सत्र : श्रेणी व नोंदी"}
                            </div>
                            
                            <table className="p1s-marks-table">
                                <thead>
                                    <tr>
                                        <th style={{ width: '6%' }}>{isEn ? "Sr.No." : "अ.नं."}</th>
                                        <th style={{ width: '24%' }}>{isEn ? "Subject" : "विषय"}</th>
                                        <th style={{ width: '15%' }}>{isEn ? "Grade" : "श्रेणी"}</th>
                                        <th style={{ width: '55%' }}>{isEn ? "Descriptive Remarks" : "वर्णनात्मक नोंदी"}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {subjects.map((sub, i) => {
                                        let grade = "-";
                                        if (rec && sub) {
                                            const d = rec.detailedMarks?.[sanitizeKey(sub)];
                                            if (d?.grade) grade = normalizeGrade(d.grade);
                                        }
                                        const remark = sub ? findRemark(rec, sub) : "";

                                        return (
                                            <tr key={i}>
                                                <td style={{ textAlign: 'center' }}>{i + 1}</td>
                                                <td style={{ textAlign: 'center' }}>{sub}</td>
                                                <td style={{ textAlign: 'center', fontWeight: 'bold' }}>{grade}</td>
                                                <td>{remark || "-"}</td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>

                            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '30px', fontSize: '10px' }}>
                                <div style={{ textAlign: 'center', width: '33%' }}>
                                    <div>{isEn ? "Teacher Signature" : "शिक्षक स्वाक्षरी"}</div>
                                </div>
                                <div style={{ textAlign: 'center', width: '33%' }}>
                                    <div>{isEn ? "Parent Signature" : "पालक स्वाक्षरी"}</div>
                                </div>
                                <div style={{ textAlign: 'center', width: '33%' }}>
                                    <div>{isEn ? "Headmaster Signature" : "मुख्याध्यापक स्वाक्षरी"}</div>
                                </div>
                            </div>
                        </div>

                    </div>
                );
            })}
        </div>
    );
}
