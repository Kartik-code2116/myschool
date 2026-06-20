/**
 * Firebase Cloud Function: verifyPurchase
 * 
 * MySchool App — Billing Server-Side Verification
 * 
 * Audit fix #1 (BILLING SECURITY): Subscription was being granted client-side.
 * This Cloud Function provides the required server-side verification.
 * 
 * HOW TO DEPLOY:
 *   1. Install Firebase CLI: npm install -g firebase-tools
 *   2. Login: firebase login
 *   3. Init functions: firebase init functions (select JavaScript)
 *   4. Copy this file to functions/index.js
 *   5. Add service account key (see step 6 below)
 *   6. In Firebase Console > Project Settings > Service Accounts,
 *      generate a new private key and save as functions/serviceAccountKey.json
 *   7. Deploy: firebase deploy --only functions
 * 
 * ENVIRONMENT SETUP:
 *   - Set your Google Play package name:
 *     firebase functions:config:set googleplay.package="com.kartik.myschool"
 *   - Ensure Google Play Android Developer API is enabled in Google Cloud Console
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { google } = require("googleapis");

admin.initializeApp();

// Initialize Google Play Developer API
const androidpublisher = google.androidpublisher("v3");

/**
 * Triggered when a document is created in the "subscriptions" collection.
 * The client (BillingHelper.java) creates this document with subscriptionVerified: false.
 * This function verifies the purchase token and sets subscriptionVerified: true if valid.
 */
exports.verifyPurchase = functions.firestore
  .document("subscriptions/{uid}")
  .onWrite(async (change, context) => {
    const uid = context.params.uid;
    const data = change.after.data();

    // Only process unverified subscriptions
    if (!data || data.subscriptionVerified === true) {
      console.log(`Skipping uid=${uid}: already verified or no data`);
      return null;
    }

    const purchaseToken = data.purchaseToken;
    const productId = data.productId || "premium_monthly";
    const packageName = functions.config().googleplay.package;

    try {
      // Authenticate with Google Play Developer API using service account
      const auth = new google.auth.GoogleAuth({
        keyFile: "./serviceAccountKey.json",
        scopes: ["https://www.googleapis.com/auth/androidpublisher"],
      });
      const authClient = await auth.getClient();
      google.options({ auth: authClient });

      // Verify the subscription purchase token
      const response = await androidpublisher.purchases.subscriptions.get({
        packageName: packageName,
        subscriptionId: productId,
        token: purchaseToken,
      });

      const subscription = response.data;
      const isValid =
        subscription.paymentState === 1 || // Payment received
        subscription.paymentState === 2;   // Free trial

      if (isValid) {
        // ✅ Purchase verified — grant premium access
        await admin.firestore().collection("users").doc(uid).set(
          {
            subscriptionStatus: "active",
            subscriptionVerified: true,
            subscriptionExpiryTime: subscription.expiryTimeMillis,
            googlePlayPurchaseToken: purchaseToken,
            verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
          },
          { merge: true }
        );

        // Update the subscription document
        await admin.firestore().collection("subscriptions").doc(uid).update({
          subscriptionVerified: true,
          verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        console.log(`✅ Subscription verified for uid=${uid}`);
      } else {
        // ❌ Invalid purchase
        await admin.firestore().collection("subscriptions").doc(uid).update({
          subscriptionVerified: false,
          verificationError: "Payment not received (paymentState=" + subscription.paymentState + ")",
          verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        console.log(`❌ Invalid subscription for uid=${uid}`);
      }
    } catch (error) {
      console.error(`Error verifying purchase for uid=${uid}:`, error);
      await admin.firestore().collection("subscriptions").doc(uid).update({
        verificationError: error.message,
        verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    return null;
  });
