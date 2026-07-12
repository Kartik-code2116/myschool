import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { initializeFirestore, persistentLocalCache, persistentMultipleTabManager } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getAnalytics } from "firebase/analytics";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID
};

const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);

export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
export const db = initializeFirestore(app, {
  localCache: persistentLocalCache({tabManager: persistentMultipleTabManager()})
});
export const storage = getStorage(app);

export const ADMIN_EMAILS = ['admin@myschool.com', 'kartikthorat2116@gmail.com'];

// Check if user has admin privileges
export const checkIsAdmin = (userOrEmail) => {
  if (!userOrEmail) return false;
  const email = typeof userOrEmail === 'string' ? userOrEmail : userOrEmail.email;
  return email ? ADMIN_EMAILS.includes(email.trim().toLowerCase()) : false;
};

// Log admin login to Firestore
export const logAdminLogin = async (user, provider) => {
  if (checkIsAdmin(user)) {
    try {
      let locationStr = 'Unknown Location';
      try {
        // Try DB-IP first (free, generous limits, HTTPS)
        const res1 = await fetch('https://api.db-ip.com/v2/free/self');
        if (res1.ok) {
          const data = await res1.json();
          if (data && data.ipAddress) {
            locationStr = `${data.city || 'Unknown'}, ${data.stateProv || 'Unknown'} (${data.countryName || 'Unknown'}) - IP: ${data.ipAddress}`;
          }
        } else {
          // Fallback to ipapi.co
          const res2 = await fetch('https://ipapi.co/json/');
          if (res2.ok) {
            const data = await res2.json();
            if (data && data.ip) {
              locationStr = `${data.city || 'Unknown'}, ${data.region || 'Unknown'} (${data.country_name || 'Unknown'}) - IP: ${data.ip}`;
            }
          } else {
            // Fallback to ipwho.is
            const res3 = await fetch('https://ipwho.is/');
            if (res3.ok) {
              const data = await res3.json();
              if (data && data.ip) {
                locationStr = `${data.city || 'Unknown'}, ${data.region || 'Unknown'} (${data.country || 'Unknown'}) - IP: ${data.ip}`;
              }
            }
          }
        }
      } catch (e) {
        console.error("Failed to fetch location data:", e);
      }

      const { collection, addDoc, serverTimestamp } = await import('firebase/firestore');
      await addDoc(collection(db, 'admin_logs'), {
        uid: user.uid,
        email: user.email,
        provider: provider,
        timestamp: serverTimestamp(),
        userAgent: navigator.userAgent,
        location: locationStr
      });
    } catch (err) {
      console.error("Failed to log admin login:", err);
    }
  }
};
