import React, { useState, useEffect } from 'react';
import { useTeacherContext } from '../context/TeacherContext';
import { db } from '../firebase';
import { doc, getDoc, updateDoc } from 'firebase/firestore';
import useLanguage from '../utils/useLanguage';
import './AppSubjects.css'; // Use new beautiful CSS

export default function AppSubjects() {
  const { activeClass } = useTeacherContext();
  const { t } = useLanguage();
  
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function fetchClassSubjects() {
      if (!activeClass) {
        setLoading(false);
        return;
      }
      setLoading(true);
      try {
        const docRef = doc(db, 'classes', activeClass.id);
        const snap = await getDoc(docRef);
        if (snap.exists() && snap.data().subjects && snap.data().subjects.length > 0) {
          setSubjects(snap.data().subjects);
        } else {
          setSubjects([]);
        }
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    fetchClassSubjects();
  }, [activeClass]);

  const handleAddSubject = () => {
    const name = prompt(t("Enter new subject name:", "नवीन विषयाचे नाव टाका:"));
    if (name) {
      const newSub = {
        name: name,
        subjectCode: "",
        maxMarks: 100,
        // Formative
        maxNirikhshan: 0,
        maxTondiKam: 10,
        maxPratyakshik: 10,
        maxUpkram: 10,
        maxPrakalp: 0,
        maxChachani: 20,
        maxSwadhyay: 0,
        maxItar: 0,
        // Summative
        maxTondi: 10,
        maxPratyakshikB: 10,
        maxLekhi: 30
      };
      setSubjects([...subjects, newSub]);
    }
  };

  const handleDeleteSubject = (index) => {
    if (window.confirm(t("Remove this subject?", "हा विषय काढायचा का?"))) {
      const newSubs = [...subjects];
      newSubs.splice(index, 1);
      setSubjects(newSubs);
    }
  };

  const handleFieldChange = (index, field, value) => {
    let finalValue = value;
    if (field !== 'name' && field !== 'subjectCode') {
      finalValue = parseInt(value) || 0;
    }
    
    const newSubs = [...subjects];
    newSubs[index] = { ...newSubs[index], [field]: finalValue };
    setSubjects(newSubs);
  };

  const handleSave = async () => {
    if (!activeClass) return;
    setSaving(true);
    try {
      const docRef = doc(db, 'classes', activeClass.id);
      await updateDoc(docRef, { subjects: subjects });
      alert(t("Subjects saved successfully!", "विषय यशस्वीरित्या सेव्ह झाले!"));
    } catch (err) {
      console.error(err);
      alert(t("Failed to save.", "सेव्ह करण्यात त्रुटी."));
    } finally {
      setSaving(false);
    }
  };

  if (!activeClass) {
    return (
      <div className="app-settings">
        <div className="warning-banner">{t("Please select an Active Class from the Dashboard.", "कृपया डॅशबोर्डवरून सक्रिय वर्ग निवडा.")}</div>
      </div>
    );
  }

  return (
    <div className="app-settings">
      <div className="settings-header">
        <h2>{t("Subject Configuration", "विषय सेटिंग्ज")}</h2>
        <p>{t(`Manage the subjects and their respective max marks for ${activeClass.name}.`, `${activeClass.name} साठी विषय आणि त्यांचे कमाल गुण व्यवस्थापित करा.`)}</p>
      </div>

      <div className="settings-grid">
        <div className="settings-card card-panel" style={{ gridColumn: '1 / -1' }}>
          <div className="card-header-with-action">
            <h3>{t("Class Subjects", "वर्गाचे विषय")}</h3>
            <div>
              <button className="btn-secondary" onClick={handleAddSubject} style={{ marginRight: '10px' }}>
                + {t("Add Subject", "विषय जोडा")}
              </button>
              <button className="btn-primary" onClick={handleSave} disabled={saving}>
                {saving ? t('Saving...', 'सेव्ह होत आहे...') : t('Save Configuration', 'सेव्ह करा')}
              </button>
            </div>
          </div>

          {loading ? (
            <p>{t("Loading subjects...", "विषय लोड होत आहेत...")}</p>
          ) : subjects.length === 0 ? (
            <div className="empty-state">
              {t("No subjects found for this class. Add one to begin.", "या वर्गासाठी कोणतेही विषय आढळले नाहीत. सुरुवात करण्यासाठी नवीन विषय जोडा.")}
            </div>
          ) : (
            <div className="subject-grid">
              {subjects.map((sub, idx) => (
                <div key={idx} className="subject-card">
                  
                  {/* Card Header (Name & Delete) */}
                  <div className="subject-header">
                    <input 
                      type="text" 
                      value={sub.name} 
                      onChange={e => handleFieldChange(idx, 'name', e.target.value)}
                      placeholder={t("Subject Name", "विषयाचे नाव")}
                    />
                    <button 
                      className="subject-delete-btn"
                      onClick={() => handleDeleteSubject(idx)}
                      title={t("Delete Subject", "विषय काढा")}
                    >
                      ✕
                    </button>
                  </div>

                  {/* Formative Marks Section */}
                  <div className="marks-section">
                    <h4 className="formative-header">{t("Formative (आकारिक)", "आकारिक (Formative)")}</h4>
                    <div className="marks-grid">
                      <div className="mark-input-group">
                        <label>{t("Observation", "निरीक्षण")}</label>
                        <input type="number" value={sub.maxNirikhshan} onChange={e => handleFieldChange(idx, 'maxNirikhshan', e.target.value)} />
                      </div>
                      <div className="mark-input-group">
                        <label>{t("Oral", "तोंडी काम")}</label>
                        <input type="number" value={sub.maxTondiKam} onChange={e => handleFieldChange(idx, 'maxTondiKam', e.target.value)} />
                      </div>
                      <div className="mark-input-group">
                        <label>{t("Practical", "प्रात्यक्षिक")}</label>
                        <input type="number" value={sub.maxPratyakshik} onChange={e => handleFieldChange(idx, 'maxPratyakshik', e.target.value)} />
                      </div>
                      <div className="mark-input-group">
                        <label>{t("Activity", "उपक्रम")}</label>
                        <input type="number" value={sub.maxUpkram} onChange={e => handleFieldChange(idx, 'maxUpkram', e.target.value)} />
                      </div>
                      <div className="mark-input-group">
                        <label>{t("Project", "प्रकल्प")}</label>
                        <input type="number" value={sub.maxPrakalp} onChange={e => handleFieldChange(idx, 'maxPrakalp', e.target.value)} />
                      </div>
                      <div className="mark-input-group">
                        <label>{t("Test", "चाचणी")}</label>
                        <input type="number" value={sub.maxChachani} onChange={e => handleFieldChange(idx, 'maxChachani', e.target.value)} />
                      </div>
                      <div className="mark-input-group">
                        <label>{t("Homework", "स्वाध्याय")}</label>
                        <input type="number" value={sub.maxSwadhyay} onChange={e => handleFieldChange(idx, 'maxSwadhyay', e.target.value)} />
                      </div>
                      <div className="mark-input-group">
                        <label>{t("Other", "इतर")}</label>
                        <input type="number" value={sub.maxItar} onChange={e => handleFieldChange(idx, 'maxItar', e.target.value)} />
                      </div>
                    </div>
                  </div>

                  {/* Summative Marks Section */}
                  <div className="marks-section">
                    <h4 className="summative-header">{t("Summative (संकलित)", "संकलित (Summative)")}</h4>
                    <div className="marks-grid">
                      <div className="mark-input-group">
                        <label>{t("Oral", "तोंडी")}</label>
                        <input type="number" value={sub.maxTondi} onChange={e => handleFieldChange(idx, 'maxTondi', e.target.value)} />
                      </div>
                      <div className="mark-input-group">
                        <label>{t("Practical", "प्रात्यक्षिक")}</label>
                        <input type="number" value={sub.maxPratyakshikB} onChange={e => handleFieldChange(idx, 'maxPratyakshikB', e.target.value)} />
                      </div>
                      <div className="mark-input-group">
                        <label>{t("Written", "लेखी")}</label>
                        <input type="number" value={sub.maxLekhi} onChange={e => handleFieldChange(idx, 'maxLekhi', e.target.value)} />
                      </div>
                    </div>
                  </div>

                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
