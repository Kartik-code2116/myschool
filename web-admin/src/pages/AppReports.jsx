import React, { useState } from 'react';
import { REPORT_TEMPLATES } from '../components/reports/ReportTemplates';
import ReportEngine from '../components/reports/ReportEngine';
import useLanguage from '../utils/useLanguage';
import './AppReports.css';

export default function AppReports() {
  const { t } = useLanguage();
  const [selectedReport, setSelectedReport] = useState(null);

  // If a report is selected, render the Engine (which shows the preview modal)
  if (selectedReport) {
      return <ReportEngine reportTemplate={selectedReport} onBack={() => setSelectedReport(null)} />;
  }

  return (
    <div className="app-reports animate-fade-in">
      <div className="reports-header">
        <h2>{t("Report Cards & Printables", "रिपोर्ट आणि प्रिंट्स")}</h2>
        <p>{t("Select a report template to generate and print.", "प्रिंट करण्यासाठी रिपोर्ट निवडा.")}</p>
      </div>

      <div className="reports-grid">
          {REPORT_TEMPLATES.map((report) => (
              <div 
                  key={report.id} 
                  className="report-card-item"
                  onClick={() => setSelectedReport(report)}
              >
                  <div 
                      className="report-category-pill" 
                      style={{ 
                          color: report.category.color, 
                          backgroundColor: `${report.category.color}15` 
                      }}
                  >
                      {report.category.label}
                  </div>
                  <h3>{report.title}</h3>
                  <p>{report.desc}</p>
                  <button 
                      className="btn-print-action"
                      style={{ color: report.category.color }}
                  >
                      Generate ➜
                  </button>
              </div>
          ))}
      </div>
    </div>
  );
}
