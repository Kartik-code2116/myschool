import React from 'react';
import useLanguage from '../../../utils/useLanguage';

const MONTHS_MR = ["जून", "जुलै", "ऑगस्ट", "सप्टेंबर", "ऑक्टोबर", "नोव्हेंबर", "डिसेंबर", "जानेवारी", "फेब्रुवारी", "मार्च", "एप्रिल", "मे"];
const MONTHS_EN = ["June", "July", "August", "September", "October", "November", "December", "January", "February", "March", "April", "May"];

const GRADE_SCALE = [
    { range: "91-100", gMr: "अ-1", gEn: "A-1" },
    { range: "81-90", gMr: "अ-2", gEn: "A-2" },
    { range: "71-80", gMr: "ब-1", gEn: "B-1" },
    { range: "61-70", gMr: "ब-2", gEn: "B-2" },
    { range: "51-60", gMr: "क-1", gEn: "C-1" },
    { range: "41-50", gMr: "क-2", gEn: "C-2" },
    { range: "33-40", gMr: "ड", gEn: "D" },
    { range: "21-32", gMr: "इ-1", gEn: "E-1" },
    { range: "0-20", gMr: "इ-2", gEn: "E-2" }
];

const lookupKeys = ["जून", "जुलै", "ऑगस्ट", "सप्टें", "ऑक्टो", "नोव्हे", "डिसें", "जाने", "फेब्रु", "मार्च", "एप्रिल", "मे"];

const calculateAge = (dobString, isEn) => {
    if (!dobString) return "-";
    try {
        const parts = dobString.split('-');
        if (parts.length === 3) {
            const dob = new Date(parts[2], parts[1] - 1, parts[0]);
            const diffMs = Date.now() - dob.getTime();
            const ageDt = new Date(diffMs); 
            const years = Math.abs(ageDt.getUTCFullYear() - 1970);
            const months = ageDt.getUTCMonth();
            return isEn ? `${years} Y. ${months} M.` : `${years} व. ${months} म.`;
        }
    } catch (e) {}
    return "-";
};

export default function ProgressCardCoverT9({ school, classInfo, year, students }) {
    const { language } = useLanguage();
    const isEn = language === 'en';
    const displayMonths = isEn ? MONTHS_EN : MONTHS_MR;

    return (
        <div className="report-progresscard-wrapper">
            <style>
                {`
                @media print {
                    @page { size: A4 landscape; margin: 10mm; }
                }
                .pcc-page {
                    display: flex;
                    width: 297mm;
                    min-height: 210mm;
                    margin: 0 auto;
                    box-sizing: border-box;
                    page-break-after: always;
                }
                .pcc-panel {
                    width: 50%;
                    padding: 15px;
                    box-sizing: border-box;
                    display: flex;
                    flex-direction: column;
                }
                .pcc-inner-panel {
                    border: 2px solid #96A0AA;
                    border-radius: 8px;
                    flex: 1;
                    padding: 15px;
                    position: relative;
                }
                .pcc-right-bg {
                    background: #FFF;
                    position: relative;
                    overflow: hidden;
                }
                .pcc-right-bg::before {
                    content: '';
                    position: absolute;
                    bottom: 5%;
                    right: 5%;
                    width: 90%;
                    height: 50%;
                    background: #FEDD65;
                    border-radius: 20px;
                    z-index: 0;
                }
                .pcc-right-content {
                    position: relative;
                    z-index: 1;
                }
                `}
            </style>
            
            {students.map((student, idx) => (
                <div key={student.id} className="pcc-page rc-standard-page">
                    
                    {/* LEFT PANEL (Attendance, Health, Grade Scale) */}
                    <div className="pcc-panel">
                        <div className="pcc-inner-panel">
                            
                            <h4 style={{ color: '#0097A7', margin: '0 0 10px 0', fontSize: '14px' }}>
                                {isEn ? "Attendance :" : "उपस्थिती :"}
                            </h4>
                            
                            {/* Attendance Table */}
                            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '10px' }}>
                                <thead>
                                    <tr>
                                        <th rowSpan="2" style={thLeft}>{isEn ? "Month" : "महिना"}</th>
                                        <th rowSpan="2" style={thLeft}>{isEn ? "Working\nDays" : "कामाचे\nदिवस"}</th>
                                        <th rowSpan="2" style={thLeft}>{isEn ? "Present\nDays" : "हजर\nदिवस"}</th>
                                        <th colSpan="3" style={thLeft}>{isEn ? "Signature" : "स्वाक्षरी"}</th>
                                    </tr>
                                    <tr>
                                        <th style={thLeft}>{isEn ? "Class Teacher" : "वर्गशिक्षक"}</th>
                                        <th style={thLeft}>{isEn ? "Parent" : "पालक"}</th>
                                        <th style={thLeft}>{isEn ? "Headmaster" : "मुख्याध्यापक"}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {displayMonths.map((m, i) => {
                                        const key = lookupKeys[i];
                                        const bg = i % 2 === 0 ? '#F8F9FA' : '#FFF';
                                        let wd = "", pd = "";
                                        
                                        if (student.monthlyAttendance) {
                                            const att = student.monthlyAttendance[key] || student.monthlyAttendance[m];
                                            if (att && att.includes('/')) {
                                                const parts = att.split('/');
                                                pd = parts[0].trim();
                                                wd = parts[1].trim();
                                            } else if (att) {
                                                pd = att.trim();
                                            }
                                        }

                                        return (
                                            <tr key={i} style={{ backgroundColor: bg }}>
                                                <td style={tdLeftAlign}>{m}</td>
                                                <td style={tdLeftCenter}>{wd}</td>
                                                <td style={tdLeftCenter}>{pd}</td>
                                                <td style={tdLeft}></td>
                                                <td style={tdLeft}></td>
                                                <td style={tdLeft}></td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                            
                            <div style={{ fontSize: '9px', marginTop: '10px', textAlign: 'center', fontWeight: 'bold' }}>
                                {isEn ? "School will reopen on June 15 after summer vacation." : "उन्हाळी सुट्टीनंतर शाळा १५ जून रोजी सुरू होईल."}
                            </div>
                            
                            {/* Health Info */}
                            <h4 style={{ color: '#0097A7', margin: '15px 0 5px 0', fontSize: '12px' }}>
                                {isEn ? "Health Information" : "आरोग्य विषयक माहिती"}
                            </h4>
                            <table style={{ width: '80%', borderCollapse: 'collapse', fontSize: '10px' }}>
                                <thead>
                                    <tr>
                                        <th style={thHealth}>-</th>
                                        <th style={thHealth}>{isEn ? "First Semester" : "प्रथम सत्र"}</th>
                                        <th style={thHealth}>{isEn ? "Second Semester" : "द्वितीय सत्र"}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td style={tdLeftAlign}>{isEn ? "Weight" : "वजन"}</td>
                                        <td style={tdLeftCenter}>{student.weightSem1 || "-"}</td>
                                        <td style={tdLeftCenter}>{student.weightSem2 || "-"}</td>
                                    </tr>
                                    <tr style={{ backgroundColor: '#F8F9FA' }}>
                                        <td style={tdLeftAlign}>{isEn ? "Height" : "उंची"}</td>
                                        <td style={tdLeftCenter}>{student.heightSem1 || "-"}</td>
                                        <td style={tdLeftCenter}>{student.heightSem2 || "-"}</td>
                                    </tr>
                                </tbody>
                            </table>
                            
                            {/* Grade Scale */}
                            <div style={{ marginTop: 'auto', paddingTop: '15px' }}>
                                <div style={{ display: 'inline-block', backgroundColor: '#FEDD65', padding: '2px 8px', borderRadius: '4px', fontSize: '10px', fontWeight: 'bold', marginBottom: '5px' }}>
                                    {isEn ? "Grade Scale :" : "श्रेणी तक्ता :"}
                                </div>
                                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '9px' }}>
                                    <tbody>
                                        <tr>
                                            {GRADE_SCALE.map((g, i) => <td key={i} style={{ border: '1px solid #111', padding: '2px', textAlign: 'center' }}>{g.range}</td>)}
                                        </tr>
                                        <tr style={{ backgroundColor: '#F0F5FA' }}>
                                            {GRADE_SCALE.map((g, i) => <td key={i} style={{ border: '1px solid #111', padding: '2px', textAlign: 'center', fontWeight: 'bold' }}>{isEn ? g.gEn : g.gMr}</td>)}
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                            
                        </div>
                    </div>
                    
                    {/* RIGHT PANEL (Cover & Student Details) */}
                    <div className="pcc-panel">
                        <div className="pcc-inner-panel pcc-right-bg">
                            <div className="pcc-right-content">
                                
                                <div style={{ textAlign: 'center', marginTop: '10px' }}>
                                    <div style={{ fontSize: '12px', fontWeight: 'bold', marginBottom: '5px' }}>
                                        {isEn ? "UDISE Code: " : "युडायस क्रमांक: "}{school?.udiseCode || "-"}
                                    </div>
                                    <div style={{ fontSize: '16px', fontWeight: 'bold', marginBottom: '5px' }}>
                                        {isEn ? "Zilla Parishad Primary School" : "जिल्हा परिषद प्राथमिक शाळा"}
                                    </div>
                                    <div style={{ fontSize: '22px', fontWeight: 'bold', marginBottom: '5px', color: '#111' }}>
                                        {school?.name || (isEn ? "SCHOOL NAME" : "शाळेचे नाव")}
                                    </div>
                                    <div style={{ fontSize: '12px', marginBottom: '5px' }}>
                                        {school?.address || ""}
                                    </div>
                                    <div style={{ fontSize: '12px', fontWeight: 'bold', marginBottom: '15px' }}>
                                        {isEn ? "Year: " : "सन: "}{classInfo?.academicYearLabel || "-"}
                                    </div>
                                    
                                    <div style={{ fontSize: '26px', fontWeight: 'bold', marginBottom: '20px', letterSpacing: '1px' }}>
                                        {isEn ? "PROGRESS CARD" : "प्रगती पत्रक"}
                                    </div>
                                </div>
                                
                                <div style={{ marginTop: '20px', padding: '0 10px' }}>
                                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '11px', color: '#111' }}>
                                        <tbody>
                                            <DetailRow full={true} lbl1={isEn ? "• Name" : "• नाव"} val1={student.name} />
                                            <DetailRow lbl1={isEn ? "• Student ID" : "• स्टुडंट ID"} val1={student.studentIdNumber} lbl2={isEn ? "Reg. No." : "रजि.नंबर"} val2={student.registrationNo} />
                                            <DetailRow lbl1={isEn ? "• Roll No." : "• हजेरी क्रमांक"} val1={student.rollNo} lbl2={isEn ? "Medium" : "माध्यम"} val2={student.medium} />
                                            <DetailRow lbl1={isEn ? "• Class" : "• इयत्ता"} val1={classInfo?.className || student.standard} lbl2={isEn ? "Division" : "तुकडी"} val2={classInfo?.division || student.division} />
                                            <DetailRow lbl1={isEn ? "• Date of Birth" : "• जन्मतारीख"} val1={student.dob} lbl2={isEn ? "Birth Place" : "जन्मस्थान"} val2={student.birthPlace} />
                                            <DetailRow lbl1={isEn ? "• Caste" : "• जात"} val1={student.cast} lbl2={isEn ? "Religion" : "धर्म"} val2={student.religion} />
                                            <DetailRow lbl1={isEn ? "• Aadhaar / UID" : "• आधार / UID"} val1={student.uid} lbl2={isEn ? "Blood Group" : "रक्तगट"} val2={student.bloodGroup} />
                                            <DetailRow lbl1={isEn ? "• Date of Adm." : "• प्रवेश तारीख"} val1={student.dateOfAdmission} lbl2={isEn ? "Account No." : "खाते क्रमांक"} val2={student.bankAccount} />
                                            <DetailRow lbl1={isEn ? "• Branch" : "• शाखा"} val1={student.bankBranch} lbl2={isEn ? "IFSC Code" : "IFSC कोड"} val2={student.bankIfsc} />
                                            <DetailRow lbl1={isEn ? "• Mother Tongue" : "• मातृभाषा"} val1={student.motherTongue} lbl2={isEn ? "Age" : "वय"} val2={calculateAge(student.dob, isEn)} />
                                            <DetailRow full={true} lbl1={isEn ? "• Mother's Name" : "• आईचे नाव"} val1={student.motherName} />
                                            <DetailRow full={true} lbl1={isEn ? "• Father's Name" : "• वडिलांचे नाव"} val1={student.fatherName} />
                                            <DetailRow full={true} lbl1={isEn ? "• Address" : "• पत्ता"} val1={student.address} />
                                        </tbody>
                                    </table>
                                </div>
                                
                            </div>
                        </div>
                    </div>

                </div>
            ))}
        </div>
    );
}

const DetailRow = ({ full, lbl1, val1, lbl2, val2 }) => {
    if (full) {
        return (
            <tr>
                <td style={{ padding: '4px 0', fontWeight: 'bold', width: '30%' }}>{lbl1}</td>
                <td colSpan="3" style={{ padding: '4px 0' }}>: {val1 || "-"}</td>
            </tr>
        );
    }
    return (
        <tr>
            <td style={{ padding: '4px 0', fontWeight: 'bold', width: '30%' }}>{lbl1}</td>
            <td style={{ padding: '4px 0', width: '20%' }}>: {val1 || "-"}</td>
            <td style={{ padding: '4px 0 4px 15px', fontWeight: 'bold', width: '25%' }}>{lbl2}</td>
            <td style={{ padding: '4px 0', width: '25%' }}>: {val2 || "-"}</td>
        </tr>
    );
};

const thLeft = { backgroundColor: '#FEEB96', border: '1px solid #111', padding: '3px', textAlign: 'center' };
const tdLeft = { border: '1px solid #111', padding: '4px' };
const tdLeftCenter = { ...tdLeft, textAlign: 'center' };
const tdLeftAlign = { ...tdLeft, textAlign: 'left' };

const thHealth = { backgroundColor: '#0097A7', color: '#FFF', border: '1px solid #111', padding: '3px', textAlign: 'center' };
