import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";

const firebaseConfig = {
  apiKey: "AIzaSyAQGltN_97tRW0-dhqJFvxL_c3IvJvR6xM",
  authDomain: "kartik-28deb.firebaseapp.com",
  projectId: "kartik-28deb",
  storageBucket: "kartik-28deb.firebasestorage.app",
  messagingSenderId: "323006914636",
  appId: "1:323006914636:web:YOUR_WEB_APP_ID" // For pure web access, any valid structure works or we can omit it if not strictly required, but Firestore/Auth typically just need apiKey/projectId.
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
