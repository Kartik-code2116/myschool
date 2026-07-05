import { useState, useEffect } from 'react';
import { doc, getDoc, collection, addDoc, query, where, getDocs } from 'firebase/firestore';
import { ref, uploadBytesResumable, getDownloadURL } from 'firebase/storage';
import { db, storage, auth } from '../firebase';
import { useTeacherContext } from '../context/TeacherContext';
import useLanguage from '../utils/useLanguage';
import './AppSubscription.css';

export default function AppSubscription() {
  const { teacherProfile, loadingContext } = useTeacherContext();
  const { t } = useLanguage();
  
  const [adminPaymentInfo, setAdminPaymentInfo] = useState(null);
  const [pendingRequest, setPendingRequest] = useState(null);
  const [screenshotFile, setScreenshotFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [message, setMessage] = useState({ text: '', type: '' });

  useEffect(() => {
    const fetchData = async () => {
      try {
        // Fetch Admin Payment Config
        const paymentDocRef = doc(db, 'admin_settings', 'payment_info');
        const paymentSnap = await getDoc(paymentDocRef);
        if (paymentSnap.exists()) {
          setAdminPaymentInfo(paymentSnap.data());
        }

        // Fetch User's Pending Requests
        if (auth.currentUser) {
          const q = query(
            collection(db, 'subscriptions'),
            where('teacherId', '==', auth.currentUser.uid),
            where('status', '==', 'pending')
          );
          const snap = await getDocs(q);
          if (!snap.empty) {
            setPendingRequest({ id: snap.docs[0].id, ...snap.docs[0].data() });
          }
        }
      } catch (err) {
        console.error('Error fetching subscription data:', err);
      }
    };
    fetchData();
  }, []);

  const handleFileChange = (e) => {
    if (e.target.files[0]) {
      setScreenshotFile(e.target.files[0]);
    }
  };

  const handleSubmitProof = async (e) => {
    e.preventDefault();
    if (!screenshotFile) {
      setMessage({ text: t("Please select a screenshot file.", "कृपया स्क्रीनशॉट फाईल निवडा."), type: 'error' });
      return;
    }
    
    setUploading(true);
    setMessage({ text: '', type: '' });
    setUploadProgress(0);

    try {
      const storageRef = ref(storage, `subscription_proofs/${auth.currentUser.uid}_${Date.now()}_${screenshotFile.name}`);
      const uploadTask = uploadBytesResumable(storageRef, screenshotFile);

      const finalUrl = await new Promise((resolve, reject) => {
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

      const newRequest = {
        teacherId: auth.currentUser.uid,
        status: 'pending',
        timestamp: Date.now(),
        screenshotUrl: finalUrl
      };

      const docRef = await addDoc(collection(db, 'subscriptions'), newRequest);
      setPendingRequest({ id: docRef.id, ...newRequest });
      setMessage({ text: t("Payment proof submitted! Awaiting admin approval.", "पेमेंट पुरावा सबमिट केला! ॲडमिन मंजुरीच्या प्रतीक्षेत."), type: 'success' });
      setScreenshotFile(null);
    } catch (err) {
      console.error('Error uploading proof:', err);
      setMessage({ text: err.message, type: 'error' });
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  };

  if (loadingContext) {
    return <div className="loading">{t("Loading subscription details...", "सबस्क्रिप्शन माहिती लोड होत आहे...")}</div>;
  }

  const isActive = teacherProfile?.subscriptionStatus === 'active';
  const isPending = !!pendingRequest;

  return (
    <main className="dashboard-content subscription-page">
      <div className="page-kicker">{t("Monetization", "मोनेटायझेशन")}</div>
      <div className="header-actions">
        <div>
          <h1>{t("My Subscription", "माझे सबस्क्रिप्शन")}</h1>
          <p>{t("Manage your active plan and renew your subscription.", "तुमचा सक्रिय प्लॅन पहा आणि सबस्क्रिप्शन नूतनीकरण करा.")}</p>
        </div>
      </div>

      <div className="subscription-content">
        {/* Current Status Card */}
        <div className={`glass-panel status-card ${isActive ? 'status-active' : 'status-inactive'}`}>
          <h2>{t("Current Plan Status", "सद्य प्लॅन स्थिती")}</h2>
          <div className="status-badge-large">
            {isActive ? t("Active (Premium)", "सक्रिय (प्रीमियम)") : t("Free Tier (Limited)", "फ्री टियर (मर्यादित)")}
          </div>
          
          {isActive ? (
            <div className="status-details">
              <p>✅ {t("Unlimited students & reports enabled.", "अमर्यादित विद्यार्थी आणि प्रगतीपत्रके सुरू.")}</p>
              {teacherProfile?.subscriptionExpiry && (
                <p>{t("Valid until:", "वैधता:")} <strong>{new Date(teacherProfile.subscriptionExpiry).toLocaleDateString()}</strong></p>
              )}
            </div>
          ) : (
            <div className="status-details">
              <p>⚠️ {t("You are on the free tier (limited to 3 students). Upgrade to unlock all features.", "तुम्ही फ्री टियरवर आहात (फक्त ३ विद्यार्थ्यांसाठी मर्यादित). सर्व सुविधांसाठी प्रीमियममध्ये श्रेणीसुधारित करा.")}</p>
            </div>
          )}
        </div>

        {/* Upgrade / Payment Section */}
        {!isActive && (
          <div className="glass-panel payment-card animate-fade-in-up">
            <h2>{t("Upgrade to Premium", "प्रीमियममध्ये श्रेणीसुधारित करा")}</h2>
            <p className="upgrade-msg">{adminPaymentInfo?.upgrade_message || t("Upgrade for ₹100/year to add unlimited students.", "अमर्यादित विद्यार्थ्यांसाठी फक्त ₹१००/वर्ष मध्ये अपग्रेड करा.")}</p>
            
            {adminPaymentInfo ? (
              <div className="payment-details-container">
                <div className="qr-section">
                  {adminPaymentInfo.upi_qr_url ? (
                    <img src={adminPaymentInfo.upi_qr_url} alt="Admin UPI QR Code" className="payment-qr" />
                  ) : (
                    <div className="no-qr-placeholder">{t("No QR Code Available", "QR कोड उपलब्ध नाही")}</div>
                  )}
                  <div className="upi-id-box">
                    <span>{t("UPI ID:", "युपीआय (UPI) आयडी:")}</span>
                    <strong>{adminPaymentInfo.upi_id || t("Not provided", "दिलेला नाही")}</strong>
                  </div>
                </div>
                
                <div className="upload-section">
                  {isPending ? (
                    <div className="pending-state">
                      <span className="pending-icon">⏳</span>
                      <h3>{t("Payment Under Review", "पेमेंट तपासणी सुरू आहे")}</h3>
                      <p>{t("We have received your payment screenshot. It will be activated shortly by the administrator.", "आम्हाला तुमचा पेमेंट स्क्रीनशॉट प्राप्त झाला आहे. ॲडमिनकडून लवकरच ते सक्रिय केले जाईल.")}</p>
                    </div>
                  ) : (
                    <form onSubmit={handleSubmitProof} className="upload-form">
                      <h3>{t("Submit Payment Proof", "पेमेंट पुरावा सबमिट करा")}</h3>
                      <p>{t("Scan the QR code, complete the payment, and upload the screenshot here.", "QR कोड स्कॅन करा, पेमेंट पूर्ण करा आणि येथे स्क्रीनशॉट अपलोड करा.")}</p>
                      
                      {message.text && (
                        <div className={`alert alert-${message.type}`}>{message.text}</div>
                      )}

                      <div className="form-group">
                        <input 
                          type="file" 
                          accept="image/*" 
                          onChange={handleFileChange} 
                          required
                        />
                      </div>

                      {uploadProgress > 0 && uploadProgress < 100 && (
                        <div className="progress-bar-container">
                          <div className="progress-bar" style={{ width: `${uploadProgress}%` }}></div>
                        </div>
                      )}

                      <button type="submit" className="btn btn-primary" disabled={uploading || !screenshotFile}>
                        {uploading ? t("Uploading...", "अपलोड होत आहे...") : t("Submit Screenshot", "स्क्रीनशॉट सबमिट करा")}
                      </button>
                    </form>
                  )}
                </div>
              </div>
            ) : (
              <div className="admin-setup-missing">
                <p>{t("The administrator has not configured payment details yet.", "ॲडमिनिस्ट्रेटरने अद्याप पेमेंट तपशील कॉन्फिगर केलेले नाहीत.")}</p>
              </div>
            )}
          </div>
        )}
      </div>
    </main>
  );
}
