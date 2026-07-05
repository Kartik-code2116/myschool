import React from 'react';
import useLanguage from '../../../utils/useLanguage';

export default function CoverPageT1({ school, classInfo, year, students }) {
    const { language } = useLanguage();
    const isEn = language === 'en';

    // Translations matching CoverPageGenerator.java
    const labelGovtSchool = isEn ? "ZILLA PARISHAD PRIMARY SCHOOL" : "जिल्हा परिषद प्राथमिक शाळा";
    const labelSchoolNameFallback = isEn ? "SCHOOL NAME" : "शाळेचे नाव";
    const labelUdise = isEn ? "UDISE : " : "युडायस : ";
    const labelTitle1 = isEn ? "CONTINUOUS & COMPREHENSIVE" : "सातत्यपूर्ण सर्वंकष";
    const labelTitle2 = isEn ? "EVALUATION" : "मूल्यमापन";
    
    const labelYear = isEn ? "Academic Year" : "शैक्षणिक वर्ष";
    const labelClass = isEn ? "Standard" : "इयत्ता";
    const labelDiv = isEn ? "Division" : "तुकडी";
    const labelUdiseCode = isEn ? "UDISE Code" : "युडायस कोड";
    const labelTeacher = isEn ? "Class Teacher" : "वर्गशिक्षक";
    const labelPrincipal = isEn ? "Headmaster" : "मुख्याध्यापक";

    const schoolName = school?.name ? school.name.toUpperCase() : labelSchoolNameFallback;
    const udiseVal = school?.udiseCode || "—";
    const yearLabel = classInfo?.academicYearLabel || "2025-26";
    const className = classInfo?.className || "-";
    const division = classInfo?.division || "-";
    const teacher = classInfo?.teacherName || "-";
    const principal = school?.principalName || "-";

    // Replicate Dot Grid
    const renderDotGrid = (left, right) => {
        const dots = [];
        for (let row = 0; row < 8; row++) {
            for (let col = 0; col < 3; col++) {
                dots.push(
                    <div 
                        key={`dot-${row}-${col}`}
                        style={{
                            position: 'absolute',
                            width: '4px',
                            height: '4px',
                            borderRadius: '50%',
                            backgroundColor: '#C5CAE9', // C_INDIGO_LIGHT
                            top: `${350 + row * 22}px`,
                            [left !== undefined ? 'left' : 'right']: `${(left !== undefined ? left : right) + col * 18}px`
                        }}
                    />
                );
            }
        }
        return dots;
    };

    return (
        <div className="report-cover-wrapper">
            {students.map((student, idx) => (
                <div key={student.id} className="rc-cover-page" style={{ 
                    pageBreakAfter: idx < students.length - 1 ? 'always' : 'auto'
                }}>
                    
                    {/* BACKGROUND ARTWORK (Replicating iText PDF cb) */}
                    <div className="rc-bg-layer">
                        {/* Pale Background */}
                        <div style={{ position: 'absolute', inset: 0, backgroundColor: '#F5F5FF', zIndex: -1 }}></div>
                        
                        {/* Top Indigo Band */}
                        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '160px', backgroundColor: '#283593' }}></div>
                        <div style={{ position: 'absolute', top: '40px', left: 0, right: 0, height: '120px', backgroundColor: '#3F51B5' }}></div>
                        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '10px', backgroundColor: '#FFC107' }}></div>

                        {/* Top Right Circles */}
                        <div className="rc-circle" style={{ top: '-10px', right: '-20px', width: '120px', height: '120px', backgroundColor: 'rgba(255,255,255,0.18)' }}></div>
                        <div className="rc-circle" style={{ top: '30px', right: '10px', width: '80px', height: '80px', backgroundColor: 'rgba(255,255,255,0.12)' }}></div>
                        <div className="rc-circle" style={{ top: '90px', right: '40px', width: '60px', height: '60px', backgroundColor: 'rgba(255,255,255,0.15)' }}></div>

                        {/* Top Left Circles */}
                        <div className="rc-circle" style={{ top: '60px', left: '-10px', width: '110px', height: '110px', backgroundColor: 'rgba(255,255,255,0.14)' }}></div>
                        <div className="rc-circle" style={{ top: '100px', left: '40px', width: '60px', height: '60px', backgroundColor: 'rgba(255,255,255,0.10)' }}></div>

                        {/* White Logo Circle */}
                        <div style={{ 
                            position: 'absolute', top: '105px', left: '50%', transform: 'translateX(-50%)',
                            width: '110px', height: '110px', backgroundColor: '#FFF', borderRadius: '50%',
                            border: '4px solid #FFC107', display: 'flex', alignItems: 'center', justifyContent: 'center',
                            zIndex: 10
                        }}>
                            <img src="/vite.svg" alt="Logo" style={{ width: '60px', height: '60px' }} />
                        </div>

                        {/* Bottom Footer Band */}
                        <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: '50px', backgroundColor: '#283593' }}></div>
                        <div style={{ position: 'absolute', bottom: '50px', left: 0, right: 0, height: '6px', backgroundColor: '#FFC107' }}></div>

                        {/* Dot Grids */}
                        {renderDotGrid(30, undefined)}
                        {renderDotGrid(undefined, 30)}
                    </div>

                    {/* CONTENT LAYER */}
                    <div className="rc-content-layer">
                        <div style={{ height: '240px' }}></div> {/* Spacer for Logo/Header */}

                        {/* GOVT LABEL */}
                        <div style={{ textAlign: 'center', color: '#5A5A64', fontSize: '18px', fontWeight: 'bold', letterSpacing: '1px' }}>
                            {labelGovtSchool}
                        </div>

                        {/* SCHOOL NAME */}
                        <div style={{ textAlign: 'center', color: '#283593', fontSize: '32px', fontWeight: 'bold', marginTop: '15px' }}>
                            {schoolName}
                        </div>

                        {/* UDISE */}
                        <div style={{ textAlign: 'center', color: '#5A5A64', fontSize: '14px', marginTop: '5px' }}>
                            {labelUdise}{udiseVal}
                        </div>

                        {/* DECORATIVE AMBER DIVIDER */}
                        <div style={{ display: 'flex', justifyContent: 'center', margin: '25px 0' }}>
                            <div style={{ display: 'flex', width: '60%', height: '8px' }}>
                                <div style={{ flex: 1, backgroundColor: '#FF8F00' }}></div>
                                <div style={{ flex: 3, backgroundColor: '#FFC107' }}></div>
                                <div style={{ flex: 1, backgroundColor: '#FF8F00' }}></div>
                            </div>
                        </div>

                        {/* BIG TITLE */}
                        <div style={{ textAlign: 'center', color: '#3F51B5', fontSize: '42px', fontWeight: 'bold', lineHeight: '1.2' }}>
                            <div>{labelTitle1}</div>
                            <div>{labelTitle2}</div>
                        </div>
                        <div style={{ textAlign: 'center', color: '#5A5A64', fontSize: '18px', marginTop: '10px' }}>
                            ( CCE )
                        </div>

                        {/* INFO CARD */}
                        <div style={{ display: 'flex', justifyContent: 'center', marginTop: '50px' }}>
                            <div className="rc-info-card">
                                <div className="rc-info-bar"></div>
                                <table className="rc-info-table">
                                    <tbody>
                                        {[
                                            [labelYear, yearLabel],
                                            [labelClass, className],
                                            [labelDiv, division],
                                            [labelUdiseCode, udiseVal],
                                            ["Student Name", student.name], // Added for web to show student name on cover
                                            ["Roll No", student.rollNo],
                                            [labelTeacher, teacher],
                                            [labelPrincipal, principal]
                                        ].map((row, i) => (
                                            <tr key={i}>
                                                <td className="rc-info-label">{row[0]}</td>
                                                <td className="rc-info-value">{row[1]}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        {/* FOOTER TEXT */}
                        <div style={{ position: 'absolute', bottom: '15px', left: 0, right: 0, textAlign: 'center', color: '#A0A0B4', fontSize: '12px', fontStyle: 'italic' }}>
                            Edu Report App — CCE Report
                        </div>
                    </div>

                </div>
            ))}
        </div>
    );
}
