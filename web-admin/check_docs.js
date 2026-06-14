import { initializeApp } from 'firebase/app';
import { getFirestore, collection, getDocs } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: "AIzaSyAqfGkJxCRkdVnx519DgyTuvG4h0URJYdc",
  authDomain: "kartik-28deb.firebaseapp.com",
  projectId: "kartik-28deb",
  storageBucket: "kartik-28deb.firebasestorage.app",
  messagingSenderId: "323006914636",
  appId: "1:323006914636:web:5824af661dc297ec8114e9"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function checkDocs() {
  console.log("Fetching documents from remarkBanks...");
  const snap = await getDocs(collection(db, 'remarkBanks'));
  snap.forEach(doc => {
    console.log("DOC ID:", doc.id);
  });
  process.exit(0);
}
checkDocs();
