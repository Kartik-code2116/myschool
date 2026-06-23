const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const { google } = require("googleapis");

admin.initializeApp();

const androidpublisher = google.androidpublisher("v3");

exports.verifyPurchase = onDocumentCreated(
  {
    document: "subscriptions/{uid}",
    region: "asia-south1",
  },
  async (event) => {
    const uid = event.params.uid;
    const data = event.data ? event.data.data() : null;

    // Only process if data exists
    if (!data) {
      console.log(`No data for uid=${uid}`);
      return null;
    }

    const purchaseToken = data.purchaseToken;
    const productId = data.productId || "yearly_pro_access";
    const packageName = "com.kartik.myschool";

    console.log(`Verifying subscription for uid=${uid}, product=${productId}, package=${packageName}`);

    try {
      // Authenticate with Google Play Developer API using service account
      // Note: serviceAccountKey.json must be present in functions/ directory
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
      
      // paymentState: 0 (Payment pending), 1 (Payment received), 2 (Free trial), 3 (Pending deferred)
      const isValid =
        subscription.paymentState === 1 || // Payment received
        subscription.paymentState === 2;   // Free trial

      if (isValid) {
        // ✅ Purchase verified — grant premium access in teachers collection
        const expiryTime = parseInt(subscription.expiryTimeMillis || 0);
        await admin.firestore().collection("teachers").doc(uid).set(
          {
            subscriptionStatus: "active",
            subscriptionExpiry: expiryTime,
            subscriptionVerified: true,
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

        console.log(`✅ Subscription verified and granted for uid=${uid}`);
      } else {
        // ❌ Invalid purchase
        await admin.firestore().collection("subscriptions").doc(uid).update({
          subscriptionVerified: false,
          verificationError: `Payment not received (paymentState=${subscription.paymentState})`,
          verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        await admin.firestore().collection("teachers").doc(uid).set(
          {
            subscriptionStatus: "inactive",
            subscriptionVerified: false,
          },
          { merge: true }
        );
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
  }
);
