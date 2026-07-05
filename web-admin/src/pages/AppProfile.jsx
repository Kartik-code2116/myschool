import React, { useState, useEffect } from 'react';
import { db, auth } from '../firebase';
import { doc, getDoc, updateDoc } from 'firebase/firestore';
import useLanguage from '../utils/useLanguage';
import './AppProfile.css';

export default function AppProfile() {
  const { t } = useLanguage();
  const [profile, setProfile] = useState({
    name: '',
    phone: '',
    schoolName: '',
    udiseCode: ''
  });
  const [subscription, setSubscription] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function loadProfile() {
      if (!auth.currentUser) return;
      try {
        const docRef = doc(db, 'teachers', auth.currentUser.uid);
        const snap = await getDoc(docRef);
        if (snap.exists()) {
          const data = snap.data();
          setProfile({
            name: data.name || '',
            phone: data.phone || '',
            schoolName: data.schoolName || '',
            udiseCode: data.udiseCode || ''
          });
          
          if (data.subscription) {
            setSubscription(data.subscription);
          } else {
            setSubscription({
              plan: 'Free Basic',
              status: 'active',
              expiryDate: null
            });
          }
        }
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    loadProfile();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!auth.currentUser) return;
    setSaving(true);
    try {
      const docRef = doc(db, 'teachers', auth.currentUser.uid);
      await updateDoc(docRef, {
        name: profile.name,
        phone: profile.phone,
        schoolName: profile.schoolName,
        udiseCode: profile.udiseCode
      });
      alert(t("Profile updated successfully!", "प्रोफाइल यशस्वीरित्या अपडेट झाले!"));
    } catch (err) {
      console.error(err);
      alert(t("Failed to update profile.", "प्रोफाइल अपडेट करण्यात त्रुटी."));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="app-profile"><div className="loading">{t("Loading Profile...", "प्रोफाइल लोड होत आहे...")}</div></div>;

  return (
    <div className="app-profile animate-fade-in">
      <div className="profile-header">
        <div className="page-kicker">{t("Account", "खाते")}</div>
        <h2>{t("Teacher Profile & Subscription", "शिक्षक प्रोफाइल आणि सबस्क्रिप्शन")}</h2>
        <p>{t("Manage your personal details, school information, and subscription plan.", "तुमची वैयक्तिक माहिती, शाळेची माहिती आणि सबस्क्रिप्शन प्लॅन व्यवस्थापित करा.")}</p>
      </div>

      <div className="profile-grid">
        <div className="profile-card card-panel">
          <h3>{t("Personal Details", "वैयक्तिक माहिती")}</h3>
          <form onSubmit={handleSave} className="auth-form">
            <div className="input-group">
              <label>{t("Full Name", "पूर्ण नाव")}</label>
              <input 
                type="text" 
                value={profile.name} 
                onChange={e => setProfile({...profile, name: e.target.value})} 
                required 
              />
            </div>
            <div className="input-group">
              <label>{t("Phone Number", "फोन नंबर")}</label>
              <input 
                type="tel" 
                value={profile.phone} 
                onChange={e => setProfile({...profile, phone: e.target.value})} 
                required 
              />
            </div>
            <div className="input-group">
              <label>{t("School Name", "शाळेचे नाव")}</label>
              <input 
                type="text" 
                value={profile.schoolName} 
                onChange={e => setProfile({...profile, schoolName: e.target.value})} 
              />
            </div>
            <div className="input-group">
              <label>{t("UDISE Code", "युडायस कोड")}</label>
              <input 
                type="text" 
                value={profile.udiseCode} 
                onChange={e => setProfile({...profile, udiseCode: e.target.value})} 
              />
            </div>
            <div className="form-actions" style={{ marginTop: '20px' }}>
              <button type="submit" className="btn-primary" disabled={saving}>
                {saving ? t('Saving...', 'सेव्ह होत आहे...') : t('Save Profile', 'प्रोफाइल सेव्ह करा')}
              </button>
            </div>
          </form>
        </div>

        <div className="profile-card card-panel subscription-card">
          <h3>{t("Subscription Status", "सबस्क्रिप्शन स्थिती")}</h3>
          {subscription ? (
            <div className="subscription-details">
              <div className="sub-badge" style={{ 
                background: subscription.status === 'active' ? 'var(--success-color)' : 'var(--warning-color)',
                color: 'white', padding: '6px 12px', borderRadius: '20px', display: 'inline-block', fontWeight: 'bold', fontSize: '14px', marginBottom: '15px'
              }}>
                {subscription.status.toUpperCase()}
              </div>
              <div className="sub-info">
                <p><strong>{t("Current Plan:", "सध्याचा प्लॅन:")}</strong> {subscription.plan}</p>
                {subscription.expiryDate && (
                  <p>
                    <strong>{t("Expires On:", "अंतिम मुदत:")}</strong> {new Date(subscription.expiryDate).toLocaleDateString()}
                  </p>
                )}
              </div>
              
              <div className="sub-actions" style={{ marginTop: '30px', padding: '20px', background: 'var(--bg-color)', borderRadius: '12px' }}>
                <h4 style={{ margin: '0 0 10px 0' }}>{t("Need to upgrade?", "अपग्रेड करायचे आहे का?")}</h4>
                <p style={{ margin: '0 0 15px 0', fontSize: '14px', color: 'var(--text-secondary)' }}>
                  {t("To upgrade to the Premium plan and unlock unlimited student capacity, please contact the administrator or upgrade via the Android app.", "प्रीमियम प्लॅनमध्ये अपग्रेड करण्यासाठी आणि अमर्यादित विद्यार्थी क्षमता मिळवण्यासाठी, कृपया ॲडमिनिस्ट्रेटरशी संपर्क साधा किंवा अँड्रॉइड ॲपवरून अपग्रेड करा.")}
                </p>
                <button className="btn-secondary" style={{ width: '100%' }} disabled>{t("Upgrade Option Unavailable", "अपग्रेड पर्याय उपलब्ध नाही")}</button>
              </div>
            </div>
          ) : (
            <p>{t("No subscription details found.", "कोणतेही सबस्क्रिप्शन तपशील आढळले नाहीत.")}</p>
          )}
        </div>
      </div>
    </div>
  );
}
