import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js";
import {
  getDatabase,
  ref,
  push,
  set,
  onChildAdded,
  serverTimestamp,
  query,
  orderByChild,
  limitToLast
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-database.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";

// Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyCJSkL4MsVRVwb9sWGvkFQHz-RaGUY-2xo",
  authDomain: "https://sai-synapse-default-rtdb.firebaseio.com",
  projectId: "sai-synapse",
  storageBucket: "sai-synapse.firebasestorage.app",
  messagingSenderId: "269633434355",
  appId: "1:308269400761:android:91c2e7415671cc49ed9168",
  databaseURL: "https://sai-synapse-default-rtdb.firebaseio.com/"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const database = getDatabase(app);
const auth = getAuth(app);

// DOM elements
const chatBox = document.getElementById('chat-box');
const messageInput = document.getElementById('message-input');
const sendButton = document.querySelector('button');
const userInfoDiv = document.getElementById('user-info');

// Current user and chat participants
let currentUser = null;
let otherUser = null;

// Initialize auth state listener
onAuthStateChanged(auth, (user) => {
  if (user) {
    // User is signed in
    currentUser = user.uid;
    userInfoDiv.textContent = `Logged in as: ${user.email || user.uid}`;

    // Here you would typically load the chat interface
    // For now we'll just enable the input field
    messageInput.disabled = false;

    // You'll need to implement how to select who to chat with
    // For now, we'll assume otherUser is set elsewhere in your app
    if (otherUser) {
      loadMessages();
    }
  } else {
    // User is signed out
    currentUser = null;
    userInfoDiv.textContent = "Please log in";
    messageInput.disabled = true;
  }
});

// Function to set the other user (the one you're chatting with)
function setChatPartner(userId) {
  otherUser = userId;
  if (currentUser) {
    loadMessages();
  }
}

// Function to send message
async function sendMessage() {
  if (!currentUser || !otherUser) {
    alert("Please log in and select a chat partner first");
    return;
  }

  const messageText = messageInput.value.trim();
  if (messageText === '') return;

  try {
    // Create a new message reference
    const newMessageRef = push(ref(database, `skyline/chats/${currentUser}/${otherUser}`));
    const timestamp = Date.now().toString();

    // Set the message data
    await set(newMessageRef, {
      TYPE: "MESSAGE",
      key: newMessageRef.key,
      message_state: "sended",
      message_text: messageText,
      push_date: timestamp,
      uid: currentUser
    });

    // Also add to the recipient's chat node
    const recipientMessageRef = push(ref(database, `skyline/chats/${otherUser}/${currentUser}`));
    await set(recipientMessageRef, {
      TYPE: "MESSAGE",
      key: recipientMessageRef.key,
      message_state: "sended",
      message_text: messageText,
      push_date: timestamp,
      uid: currentUser
    });

    // Update inbox for both users
    await updateInbox(currentUser, otherUser, messageText, timestamp);
    await updateInbox(otherUser, currentUser, messageText, timestamp);

    messageInput.value = '';
  } catch (error) {
    console.error("Error sending message:", error);
    alert("Failed to send message!");
  }
}

// Update inbox with last message
async function updateInbox(userId, contactId, lastMessage, timestamp) {
  const inboxRef = ref(database, `skyline/inbox/${userId}/${contactId}`);
  await set(inboxRef, {
    TYPE: "MESSAGE",
    last_message_state: "sended",
    last_message_text: lastMessage,
    last_message_uid: userId,
    push_date: timestamp,
    uid: contactId
  });
}

// Load messages for the current chat
function loadMessages() {
  if (!currentUser || !otherUser) return;

  // Clear existing messages
  chatBox.innerHTML = '';

  const messagesRef = ref(database, `skyline/chats/${currentUser}/${otherUser}`);
  const messagesQuery = query(messagesRef, orderByChild('push_date'), limitToLast(100));

  onChildAdded(messagesQuery, (snapshot) => {
    const message = snapshot.val();
    displayMessage(message);
  });
}

// Display a message in the chat box
function displayMessage(messageData) {
  if (!messageData || messageData.TYPE !== "MESSAGE") return;

  const messageElement = document.createElement('div');
  messageElement.classList.add('message');

  // Format timestamp
  const timestamp = parseInt(messageData.push_date);
  const timeString = isNaN(timestamp) ? 'now' : new Date(timestamp).toLocaleTimeString();

  // Check if message is from current user
  const isCurrentUser = messageData.uid === currentUser;

  messageElement.innerHTML = `
    <span class="sender">${isCurrentUser ? 'You' : getUserName(messageData.uid)}</span>
    <span class="text">${messageData.message_text}</span>
    <span class="time">${timeString}</span>
    <span class="status">${messageData.message_state}</span>
  `;

  // Style differently based on sender
  messageElement.classList.add(isCurrentUser ? 'own-message' : 'other-message');

  chatBox.appendChild(messageElement);
  chatBox.scrollTop = chatBox.scrollHeight;
}

// Helper function to get username from UID (you'll need to implement this properly)
function getUserName(uid) {
  // In a real app, you would fetch this from your users database
  // For now, we'll just return "User" plus first 4 chars of UID
  return `User ${uid.substring(0, 4)}...`;
}

// Event listeners
sendButton.addEventListener('click', sendMessage);
messageInput.addEventListener('keypress', (e) => {
  if (e.key === 'Enter') {
    sendMessage();
  }
});

// Export functions you might need from other parts of your app
window.chatFunctions = {
  setChatPartner
};