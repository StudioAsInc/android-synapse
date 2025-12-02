import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getDatabase } from "firebase/database";
import { getAnalytics, isSupported } from "firebase/analytics";

const firebaseConfig = {
  apiKey: "your-api-key-here",
  authDomain: "your-auth-domain.firebaseapp.com",
  databaseURL: "https://your-project-default-rtdb.region.firebasedatabase.app",
  projectId: "your-project-id",
  storageBucket: "your-project.firebasestorage.app",
  messagingSenderId: "your-messaging-sender-id",
  appId: "1:your-app-id:web:your-web-id",
  measurementId: "G-YOUR-MEASUREMENT-ID"
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getDatabase(app);

export let analytics = null;
isSupported().then(supported => {
  if (supported) {
    analytics = getAnalytics(app);
  }
}).catch(() => {
  console.warn('Firebase Analytics not supported in this environment.');
});
