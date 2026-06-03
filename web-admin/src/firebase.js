import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getAnalytics } from "firebase/analytics";

const firebaseConfig = {
  apiKey: "AIzaSyAqfGkJxCRkdVnx519DgyTuvG4h0URJYdc",
  authDomain: "kartik-28deb.firebaseapp.com",
  projectId: "kartik-28deb",
  storageBucket: "kartik-28deb.firebasestorage.app",
  messagingSenderId: "323006914636",
  appId: "1:323006914636:web:5824af661dc297ec8114e9",
  measurementId: "G-4P8P0579FC"
};

const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);

export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
