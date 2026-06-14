import { useState, useEffect } from 'react';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { ref, uploadBytesResumable, getDownloadURL } from 'firebase/storage';
import { db, storage } from '../firebase';
import './AdminProfile.css';

export default function AdminProfile() {
  const [upiId, setUpiId] = useState('');
  const [upgradeMessage, setUpgradeMessage] = useState('You have reached the free limit of 3 students. Upgrade for ₹100/year to add unlimited students.');
  const [qrFile, setQrFile] = useState(null);
  const [currentQrUrl, setCurrentQrUrl] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '' });
  const [uploadProgress, setUploadProgress] = useState(0);
  const [activeTab, setActiveTab] = useState('payment'); // 'payment' or 'app_config'

  const [appConfig, setAppConfig] = useState({
    devName: 'Sanjay Gore',
    devEmail: 'sanjaygore@myschool.com',
    devPhone: '',
    devWebsite: '',
    appVersion: '24.04.14',
    aboutText: 'Developed with ❤️ to make result management easy and effortless for teachers.'
  });

  useEffect(() => {
    const fetchAdminSettings = async () => {
      try {
        const paymentDocRef = doc(db, 'admin_settings', 'payment_info');
        const paymentSnap = await getDoc(paymentDocRef);
        if (paymentSnap.exists()) {
          const data = paymentSnap.data();
          setUpiId(data.upi_id || '');
          setUpgradeMessage(data.upgrade_message || 'You have reached the free limit of 3 students. Upgrade for ₹100/year to add unlimited students.');
          setCurrentQrUrl(data.upi_qr_url || '');
        }

        const configDocRef = doc(db, 'admin_settings', 'app_config');
        const configSnap = await getDoc(configDocRef);
        if (configSnap.exists()) {
          const data = configSnap.data();
          setAppConfig({
            devName: data.devName || 'Sanjay Gore',
            devEmail: data.devEmail || 'sanjaygore@myschool.com',
            devPhone: data.devPhone || '',
            devWebsite: data.devWebsite || '',
            appVersion: data.appVersion || '24.04.14',
            aboutText: data.aboutText || 'Developed with ❤️ to make result management easy and effortless for teachers.'
          });
        }
      } catch (err) {
        console.error('Error fetching admin settings:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchAdminSettings();
  }, []);

  const handleFileChange = (e) => {
    if (e.target.files[0]) {
      setQrFile(e.target.files[0]);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage({ text: '', type: '' });
    setUploadProgress(0);

    try {
      let finalQrUrl = currentQrUrl;

      // Upload new QR code if selected
      if (qrFile) {
        const storageRef = ref(storage, `admin_qrs/${Date.now()}_${qrFile.name}`);
        const uploadTask = uploadBytesResumable(storageRef, qrFile);

        finalQrUrl = await new Promise((resolve, reject) => {
          uploadTask.on(
            'state_changed',
            (snapshot) => {
              const progress = (snapshot.bytesTransferred / snapshot.totalBytes) * 100;
              setUploadProgress(progress);
            },
            (error) => reject(error),
            async () => {
              const downloadURL = await getDownloadURL(uploadTask.snapshot.ref);
              resolve(downloadURL);
            }
          );
        });
        setCurrentQrUrl(finalQrUrl);
        setQrFile(null); // Clear selected file after successful upload
      }

      // Update Firestore document
      const docRef = doc(db, 'admin_settings', 'payment_info');
      await setDoc(docRef, {
        upi_id: upiId,
        upgrade_message: upgradeMessage,
        upi_qr_url: finalQrUrl
      }, { merge: true });

      setMessage({ text: 'Payment info updated successfully. Users will now see this in the app.', type: 'success' });
    } catch (err) {
      console.error('Error saving admin settings:', err);
      setMessage({ text: `Failed to save: ${err.message}`, type: 'error' });
    } finally {
      setSaving(false);
      setUploadProgress(0);
    }
  };

  const handleSaveAppConfig = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage({ text: '', type: '' });

    try {
      const docRef = doc(db, 'admin_settings', 'app_config');
      await setDoc(docRef, appConfig, { merge: true });
      setMessage({ text: 'App configuration updated successfully. Users will see these details in the app.', type: 'success' });
    } catch (err) {
      console.error('Error saving app config settings:', err);
      setMessage({ text: `Failed to save: ${err.message}`, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleAppConfigChange = (field, value) => {
    setAppConfig(prev => ({ ...prev, [field]: value }));
  };

  if (loading) {
    return <div className="loading">Loading admin profile...</div>;
  }

  return (
    <main className="dashboard-content profile-page">
      <div className="page-kicker">Settings</div>
      <div className="header-actions">
        <div>
          <h1>Admin Settings</h1>
          <p>Configure app settings, developer information, and payment settings.</p>
        </div>
      </div>

      <div className="tabs-container">
        <button
          className={`tab-btn ${activeTab === 'payment' ? 'active' : ''}`}
          onClick={() => { setActiveTab('payment'); setMessage({ text: '', type: '' }); }}
          type="button"
        >
          💳 Payment Setup
        </button>
        <button
          className={`tab-btn ${activeTab === 'app_config' ? 'active' : ''}`}
          onClick={() => { setActiveTab('app_config'); setMessage({ text: '', type: '' }); }}
          type="button"
        >
          ⚙️ App &amp; Developer Config
        </button>
      </div>

      <div className="glass-panel profile-card animate-fade-in">
        {message.text && (
          <div className={`alert alert-${message.type}`}>
            {message.text}
          </div>
        )}

        {activeTab === 'payment' ? (
          <form onSubmit={handleSave} className="profile-form">
            <div className="form-group">
              <label htmlFor="upiId">UPI ID</label>
              <input
                type="text"
                id="upiId"
                value={upiId}
                onChange={(e) => setUpiId(e.target.value)}
                placeholder="e.g. myschool@upi"
                required
              />
            </div>

            <div className="form-group full-width">
              <label htmlFor="upgradeMessage">Subscription Upgrade Message</label>
              <textarea
                id="upgradeMessage"
                value={upgradeMessage}
                onChange={(e) => setUpgradeMessage(e.target.value)}
                placeholder="e.g. You have reached the free limit of 3 students. Upgrade for ₹100/year to add unlimited students."
                rows={3}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="qrUpload">UPI QR Code Screenshot</label>
              <input
                type="file"
                id="qrUpload"
                accept="image/*"
                onChange={handleFileChange}
              />
              {qrFile && <span className="file-selected">Selected: {qrFile.name}</span>}
            </div>

            {(currentQrUrl || qrFile) && (
              <div className="current-qr-preview">
                {qrFile ? (
                  <div>
                    <h3>New QR Code Preview</h3>
                    <img src={URL.createObjectURL(qrFile)} alt="New UPI QR" className="qr-image" />
                    <p className="helper-text">This will replace the current QR code once saved.</p>
                  </div>
                ) : (
                  <div>
                    <h3>Currently Uploaded QR Code</h3>
                    <img 
                      src={currentQrUrl} 
                      alt="Current UPI QR" 
                      className="qr-image" 
                      onError={(e) => console.error("Admin QR Code image failed to load. URL: " + currentQrUrl, e)}
                    />
                  </div>
                )}
              </div>
            )}

            {uploadProgress > 0 && uploadProgress < 100 && (
              <div className="progress-bar-container">
                <div className="progress-bar" style={{ width: `${uploadProgress}%` }}></div>
              </div>
            )}

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving...' : 'Save Settings'}
              </button>
            </div>
          </form>
        ) : (
          <form onSubmit={handleSaveAppConfig} className="profile-form">
            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="devName">Developer Name</label>
                <input
                  type="text"
                  id="devName"
                  value={appConfig.devName}
                  onChange={(e) => handleAppConfigChange('devName', e.target.value)}
                  placeholder="e.g. Sanjay Gore"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="appVersion">App Version</label>
                <input
                  type="text"
                  id="appVersion"
                  value={appConfig.appVersion}
                  onChange={(e) => handleAppConfigChange('appVersion', e.target.value)}
                  placeholder="e.g. 24.04.14"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="devEmail">Developer Email</label>
                <input
                  type="text"
                  id="devEmail"
                  value={appConfig.devEmail}
                  onChange={(e) => handleAppConfigChange('devEmail', e.target.value)}
                  placeholder="e.g. developer@myschool.com"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="devPhone">Developer Phone</label>
                <input
                  type="text"
                  id="devPhone"
                  value={appConfig.devPhone}
                  onChange={(e) => handleAppConfigChange('devPhone', e.target.value)}
                  placeholder="e.g. +91 9876543210"
                />
              </div>

              <div className="form-group full-width">
                <label htmlFor="devWebsite">Developer Website / Portfolio URL</label>
                <input
                  type="text"
                  id="devWebsite"
                  value={appConfig.devWebsite}
                  onChange={(e) => handleAppConfigChange('devWebsite', e.target.value)}
                  placeholder="e.g. https://sanjaygore.com"
                />
              </div>

              <div className="form-group full-width">
                <label htmlFor="aboutText">About App Details</label>
                <textarea
                  id="aboutText"
                  value={appConfig.aboutText}
                  onChange={(e) => handleAppConfigChange('aboutText', e.target.value)}
                  placeholder="Describe the application features and developer's mission..."
                  rows={4}
                  required
                />
              </div>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving...' : 'Save Config'}
              </button>
            </div>
          </form>
        )}
      </div>
    </main>
  );
}
