import React from 'react';
import useLanguage from '../../../utils/useLanguage';

export default function ProgressPortraitT14({ school, classInfo, year, students }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    const MONTHS_EN = ["JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC", "JAN", "FEB", "MAR", "APR", "MAY"];
    const MONTHS_MR = ["जून", "जुलै", "ऑगस्ट", "सप्टें", "ऑक्टो", "नोव्हे", "डिसें", "जाने", "फेब्रु", "मार्च", "एप्रिल", "मे"];
    const activeMonths = isEn ? MONTHS_EN : MONTHS_MR;

    const calcAge = (dobStr) => {
        if (!dobStr) return "-";
        try {
            // DD-MM-YYYY format
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

    return (
        <div className="report-progressportrait-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 portrait; margin: 10mm; }
                }
                .pp-page {
                    width: 210mm;
                    min-height: 297mm;
                    padding: 20px;
                    margin: 0 auto;
                    box-sizing: border-box;
                    page-break-after: always;
                    border: 3px solid #6096B4;
                    border-radius: 15px;
                    position: relative;
                }
                .pp-pill {
                    background-color: #FCE4EC;
                    color: #D81B60;
                    font-size: 20px;
                    font-weight: bold;
                    padding: 8px 30px;
                    border-radius: 20px;
                    display: inline-block;
                    margin-bottom: 20px;
                }
                .pp-block {
                    background-color: #F0F2F5;
                    border-radius: 10px;
                    padding: 15px;
                    margin-bottom: 20px;
                }
                .pp-detail-table {
                    width: 100%;
                    font-size: 11px;
                }
                .pp-detail-table td {
                    padding: 4px 2px;
                    vertical-align: top;
                }
                .pp-label {
                    color: #444;
                }
                .pp-value {
                    font-weight: bold;
                    color: #111;
                }
                .pp-bullet {
                    color: #2980B9;
                    margin-right: 4px;
                }
                .pp-att-table {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 10px;
                }
                .pp-att-table th, .pp-att-table td {
                    border: 1px solid #6096B4;
                    padding: 6px 2px;
                    text-align: center;
                }
                .pp-att-table th {
                    background-color: #F0F2F5;
                    font-weight: bold;
                }
                `}
            </style>
            
            {students.map((student, idx) => {
                
                // Parse attendance
                const wd = new Array(12).fill(0);
                const pd = new Array(12).fill(0);
                if (student.monthlyAttendance) {
                    for (let i = 0; i < 12; i++) {
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

                const Label = ({ text }) => <><span className="pp-bullet">●</span><span className="pp-label">{text}</span></>;

                return (
                    <div key={student.id} className="pp-page rc-standard-page">
                        
                        {/* Header */}
                        <div style={{ textAlign: 'center', marginBottom: '15px' }}>
                            <div style={{ fontSize: '11px', fontWeight: 'bold' }}>
                                {isEn ? "School UDISE: " : "युडायस क्रमांक: "}{school?.udiseCode || "-"}
                            </div>
                            <div style={{ fontSize: '18px', fontWeight: 'bold', margin: '4px 0' }}>
                                {isEn ? "Zilla Parishad Primary School " : "जिल्हा परिषद प्राथमिक शाळा "}{school?.name || ""}
                            </div>
                            <div style={{ fontSize: '10px', marginBottom: '4px' }}>
                                {school?.address || ""}
                            </div>
                            <div style={{ fontSize: '11px', fontWeight: 'bold' }}>
                                {isEn ? "Year: " : "सन: "}{classInfo?.academicYearLabel || "-"}
                            </div>
                        </div>

                        {/* Title Pill */}
                        <div style={{ textAlign: 'center' }}>
                            <div className="pp-pill">{isEn ? "PROGRESS CARD" : "प्रगती पत्रक"}</div>
                        </div>

                        {/* Details Block */}
                        <div className="pp-block">
                            <table className="pp-detail-table">
                                <tbody>
                                    <tr>
                                        <td style={{ width: '25%' }}><Label text={isEn ? "Name" : "नाव"} /></td>
                                        <td colSpan="3" className="pp-value">: {student.name || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Student ID" : "स्टुडंट ID"} /></td>
                                        <td style={{ width: '25%' }} className="pp-value">: {student.studentIdNumber || "-"}</td>
                                        <td style={{ width: '25%' }}><Label text={isEn ? "Reg. No." : "रजि.नंबर"} /></td>
                                        <td style={{ width: '25%' }} className="pp-value">: {student.registrationNo || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Class" : "इयत्ता"} /></td>
                                        <td className="pp-value">: {classInfo?.className || "-"}</td>
                                        <td><Label text={isEn ? "Division" : "तुकडी"} /></td>
                                        <td className="pp-value">: {classInfo?.division || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Roll No." : "हजेरी क्रमांक"} /></td>
                                        <td className="pp-value">: {student.rollNo || "-"}</td>
                                        <td><Label text={isEn ? "Medium" : "माध्यम"} /></td>
                                        <td className="pp-value">: {student.medium || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Mother Tongue" : "मातृभाषा"} /></td>
                                        <td className="pp-value">: {student.motherTongue || "-"}</td>
                                        <td><Label text={isEn ? "Age" : "वय"} /></td>
                                        <td className="pp-value">: {calcAge(student.dob)}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Date of Birth" : "जन्मतारीख"} /></td>
                                        <td className="pp-value">: {student.dob || "-"}</td>
                                        <td><Label text={isEn ? "Birth Place" : "जन्मस्थान"} /></td>
                                        <td className="pp-value">: {student.birthPlace || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Caste" : "जात"} /></td>
                                        <td className="pp-value">: {student.cast || "-"}</td>
                                        <td><Label text={isEn ? "Religion" : "धर्म"} /></td>
                                        <td className="pp-value">: {student.religion || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Aadhaar / UID" : "आधार / UID"} /></td>
                                        <td className="pp-value">: {student.uid || "-"}</td>
                                        <td><Label text={isEn ? "Blood Group" : "रक्तगट"} /></td>
                                        <td className="pp-value">: {student.bloodGroup || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Date of Adm." : "प्रवेश तारीख"} /></td>
                                        <td className="pp-value">: {student.dateOfAdmission || "-"}</td>
                                        <td><Label text={isEn ? "Account No." : "खाते क्रमांक"} /></td>
                                        <td className="pp-value">: {student.bankAccount || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Branch" : "शाखा"} /></td>
                                        <td className="pp-value">: {student.bankBranch || "-"}</td>
                                        <td><Label text={isEn ? "IFSC Code" : "IFSC कोड"} /></td>
                                        <td className="pp-value">: {student.bankIfsc || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Mother's Name" : "आईचे नाव"} /></td>
                                        <td colSpan="3" className="pp-value">: {student.motherName || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Father's Name" : "वडिलांचे नाव"} /></td>
                                        <td colSpan="3" className="pp-value">: {student.fatherName || "-"}</td>
                                    </tr>
                                    <tr>
                                        <td><Label text={isEn ? "Address" : "पत्ता"} /></td>
                                        <td colSpan="3" className="pp-value">: {student.address || "-"}</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        {/* Notice */}
                        <div style={{ fontSize: '10px', marginBottom: '20px', paddingLeft: '10px' }}>
                            {isEn ? "School will reopen on June 15, 2026 after summer vacation." : "उन्हाळी सुट्टीनंतर शाळा १५ जून २०२६ रोजी सुरू होईल ."}
                        </div>

                        {/* Attendance */}
                        <div style={{ color: '#D81B60', fontSize: '12px', fontWeight: 'bold', marginBottom: '5px' }}>
                            {isEn ? "Attendance :" : "उपस्थिती :"}
                        </div>
                        <table className="pp-att-table">
                            <thead>
                                <tr>
                                    <th style={{ width: '22%' }}>{isEn ? "Month" : "महिना"}</th>
                                    {activeMonths.map((m, i) => <td key={i} style={{ fontWeight: 'bold' }}>{m}</td>)}
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <th>{isEn ? "Working Days" : "कामाचे दिवस"}</th>
                                    {wd.map((w, i) => <td key={`w-${i}`}>{w > 0 ? w : ""}</td>)}
                                </tr>
                                <tr>
                                    <th>{isEn ? "Present Days" : "हजर दिवस"}</th>
                                    {pd.map((p, i) => <td key={`p-${i}`}>{p > 0 ? p : (wd[i] > 0 ? "0" : "")}</td>)}
                                </tr>
                            </tbody>
                        </table>
                        
                    </div>
                );
            })}
        </div>
    );
}
