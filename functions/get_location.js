const { google } = require('googleapis');
const auth = new google.auth.GoogleAuth({
  keyFile: './serviceAccountKey.json',
  scopes: ['https://www.googleapis.com/auth/datastore'],
});

async function main() {
  const authClient = await auth.getClient();
  const firestore = google.firestore({
    version: 'v1',
    auth: authClient,
  });

  const res = await firestore.projects.databases.get({
    name: 'projects/kartik-28deb/databases/(default)',
  });
  console.log('Database Info:', JSON.stringify(res.data, null, 2));
}

main().catch(console.error);
