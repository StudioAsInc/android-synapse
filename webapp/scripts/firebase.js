// firebase.js
// Firebase App (the core Firebase SDK) is always required
import firebase from "https://www.gstatic.com/firebasejs/9.22.2/firebase-app.js";
import "https://www.gstatic.com/firebasejs/9.22.2/firebase-auth.js";
import "https://www.gstatic.com/firebasejs/9.22.2/firebase-database.js";

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "YOUR_API_KEY", // Your Firebase project API key
    databaseURL: "YOUR_DATABASE_URL", // URL of your Realtime Database
    projectId: "sai-synapse",     // Your Firebase project ID
    authDomain: "sai-synapse.firebasestorage.app",
    storageBucket: "synapse-social-sai.firebasestorage.app", // Your Firebase Storage bucket URL
    appId: "1:308269400761:android:91c2e7415671cc49ed9168" // Your Firebase App ID (often for Android/iOS)
};

// Initialize Firebase
const app = firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
const database = firebase.database();

export { app, auth, database };