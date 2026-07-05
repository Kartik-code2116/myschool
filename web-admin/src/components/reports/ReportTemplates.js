export const REPORT_CATEGORIES = {
    REGISTER: { id: 'register', label: 'गुणनोंदी (Registers)', color: '#5A4FCF' },
    RESULT: { id: 'result', label: 'निकालपत्रक (Result Sheets)', color: '#D32F2F' },
    MARKSHEET: { id: 'marksheet', label: 'गुणपत्रक (Marksheets)', color: '#6A5ACD' },
    TABLES: { id: 'tables', label: 'तक्ते (Tables)', color: '#757575' },
    PROGRESSBOOK: { id: 'progressbook', label: 'प्रगती पुस्तक (Progress Books)', color: '#388E3C' },
    PROGRESSCARD: { id: 'progresscard', label: 'प्रगतीपत्रक (Progress Cards)', color: '#6A5ACD' }
};

export const REPORT_TEMPLATES = [
    { id: 1, type: 'COVER', category: REPORT_CATEGORIES.REGISTER, title: '1. मुखपृष्ठ', desc: 'सत्राचे मुखपृष्ठ' },
    { id: 2, type: 'INDEX', category: REPORT_CATEGORIES.REGISTER, title: '2. अनुक्रमणिका', desc: 'विद्यार्थ्यांची अनुक्रमणिका / व्यक्तिमत्व नोंद' },
    { id: 3, type: 'MARKS_REGISTER', category: REPORT_CATEGORIES.REGISTER, title: '3. गुणनोंदी', desc: 'सर्व विषयांचे एकत्रित गुण' },
    { id: 4, type: 'DESCRIPTIVE', category: REPORT_CATEGORIES.REGISTER, title: '4. वर्णनात्मक नोंदी', desc: 'वर्णनात्मक नोंदींचा तक्ता' },
    { id: 5, type: 'GRADE_CHART', category: REPORT_CATEGORIES.REGISTER, title: '5. श्रेणी तक्ता', desc: 'श्रेणीनिहाय तक्ता' },
    { id: 6, type: 'CCE', category: REPORT_CATEGORIES.RESULT, title: '6. सर्वसामावेशक निकाल', desc: 'सर्वंकष प्रगती' },
    { id: 7, type: 'ROSTER_GRADE', category: REPORT_CATEGORIES.RESULT, title: '7. श्रेणी तक्ता', desc: 'रोस्टर श्रेणी' },
    { id: 8, type: 'MARKS_GRADE_LEDGER', category: REPORT_CATEGORIES.RESULT, title: '8. गुण-श्रेणीयुक्त निकालपत्रक', desc: 'लेजर निकालपत्रक' },
    { id: 9, type: 'PROGRESS_COVER', category: REPORT_CATEGORIES.MARKSHEET, title: '9. प्रगतीपत्रक मुखपृष्ठ', desc: 'प्रगतीपत्रकाचे मुख्य पान' },
    { id: 10, type: 'PROGRESS_INNER', category: REPORT_CATEGORIES.MARKSHEET, title: '10. प्रगतीपत्रक पृष्ठ', desc: 'दोन्ही सत्रांचे गुण' },
    { id: 11, type: 'SUBJECT_REGISTER', category: REPORT_CATEGORIES.TABLES, title: '11. विषयवार गुणनोंदी', desc: 'प्रत्येक विषयाची स्वतंत्र नोंद' },
    { id: 12, type: 'ANNUAL_MARKSHEET', category: REPORT_CATEGORIES.PROGRESSBOOK, title: '12. पाचवी आठवी गुणपत्रक', desc: 'वार्षिक परीक्षा गुणपत्रक' },
    { id: 13, type: 'RESULT_SHEET', category: REPORT_CATEGORIES.TABLES, title: '13. पाचवी आठवी वार्षिक तक्ते', desc: 'निकालपत्रक' },
    { id: 14, type: 'PROGRESS_PORTRAIT', category: REPORT_CATEGORIES.MARKSHEET, title: '14. प्रगतीपत्रक (Portrait)', desc: 'उभे प्रगतीपत्रक' },
    { id: 15, type: 'CCE_ALT', category: REPORT_CATEGORIES.RESULT, title: '15. वार्षिक निकालपत्रक', desc: 'पर्यायी निकालपत्रक' },
    { id: 16, type: 'CCE_ALT2', category: REPORT_CATEGORIES.RESULT, title: '16. वार्षिक निकालपत्रक (CCE)', desc: 'CCE निकालपत्रक' },
    { id: 17, type: 'CASTE_GRADE', category: REPORT_CATEGORIES.RESULT, title: '17. जात श्रेणी तक्ता', desc: 'जात आणि श्रेणी' },
    { id: 18, type: 'PROGRESS_FIRST_SEM', category: REPORT_CATEGORIES.PROGRESSCARD, title: '18. प्रगतीपत्रक प्रथम सत्र', desc: 'फक्त प्रथम सत्राचे गुण' },
    { id: 19, type: 'HPC', category: REPORT_CATEGORIES.PROGRESSCARD, title: '19. सर्वंकष प्रगती पत्रक (HPC)', desc: 'Holistic Progress Card' }
];
