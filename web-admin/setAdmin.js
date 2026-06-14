const admin = require('firebase-admin');

// 1. You need to download a service account key from Firebase Console
//    (Project Settings -> Service Accounts -> Generate New Private Key)
// 2. Save it in this directory as 'serviceAccountKey.json'
// 3. Run: npm install firebase-admin
// 4. Run: node setAdmin.js

const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const adminEmails = [
  'admin@myschool.com',
  'kartikthorat2431@gmail.com'
];

async function setAdminClaims() {
  for (const email of adminEmails) {
    try {
      const user = await admin.auth().getUserByEmail(email);
      await admin.auth().setCustomUserClaims(user.uid, { admin: true });
      console.log(`Successfully set admin claim for ${email}`);
    } catch (error) {
      if (error.code === 'auth/user-not-found') {
        console.log(`User not found for email: ${email}`);
      } else {
        console.error(`Error setting admin claim for ${email}:`, error);
      }
    }
  }
  process.exit();
}

setAdminClaims();
