import { initializeApp } from 'firebase/app';
import { getFirestore, collection, getDocs } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: "AIzaSyAQGltN_97tRW0-dhqJFvxL_c3IvJvR6xM",
  authDomain: "kartik-28deb.firebaseapp.com",
  projectId: "kartik-28deb"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function test() {
  try {
    const snap = await getDocs(collection(db, 'subscriptions'));
    console.log("Found " + snap.size + " subscriptions.");
    snap.forEach(doc => console.log(doc.id, doc.data()));
  } catch (e) {
    console.error("Error:", e);
  }
}
test();
