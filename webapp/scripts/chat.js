import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js";
import {
    getDatabase,
    ref,
    push,
    set,
    onChildAdded,
    query,
    orderByChild,
    serverTimestamp,
    onValue,
    update,
    get
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-database.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { getStorage, ref as storageRef, getDownloadURL } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-storage.js";

const firebaseConfig = {
    apiKey: "AIzaSyCJSkL4MsVRVwb9sWGvkFQHz-RaGUY-2xo", // Your Firebase project API key
    databaseURL: "https://sai-synapse-default-rtdb.firebaseio.com", // URL of your Realtime Database
    projectId: "sai-synapse",     // Your Firebase project ID
    storageBucket: "synapse-social-sai.firebasestorage.app", // Your Firebase Storage bucket URL
    appId: "1:308269400761:android:91c2e7415671cc49ed9168" // Your Firebase App ID (often for Android/iOS)
};

const app = initializeApp(firebaseConfig);
const database = getDatabase(app);
const auth = getAuth(app);
const storage = getStorage(app);

// DOM elements
const backButton = document.getElementById('back-button');
const partnerAvatar = document.getElementById('partner-avatar');
const partnerName = document.getElementById('partner-name');
const partnerStatus = document.getElementById('partner-status');
const chatMessages = document.getElementById('chat-messages');
const messageInput = document.getElementById('message-input');
const sendButton = document.getElementById('send-button');
const typingIndicator = document.getElementById('typing-indicator');

// Get URL parameters
const urlParams = new URLSearchParams(window.location.search);
const partnerUid = urlParams.get('uid');
const partnerNameFromUrl = urlParams.get('name');

// Current user and partner data
let currentUser = null;
let partnerData = null;

// Initialize auth state listener
onAuthStateChanged(auth, async (user) => {
    if (user) {
        currentUser = user.uid;
        await loadPartnerData();
        loadMessages();
        setupTypingIndicator();
    } else {
        // Redirect to login if not authenticated
        window.location.href = 'login.html';
    }
});

// Load partner data from Firebase
async function loadPartnerData() {
    try {
        // Set name from URL first
        partnerName.textContent = decodeURIComponent(partnerNameFromUrl) || 'User';

        // Get partner details from database
        const partnerRef = ref(database, `skyline/users/${partnerUid}`);
        const snapshot = await get(partnerRef);
        partnerData = snapshot.val();

        // Update name if available in database
        if (partnerData?.nickname || partnerData?.username) {
            partnerName.textContent = partnerData.nickname || partnerData.username;
        }

        // Load profile picture
        try {
            if (partnerData?.avatar && partnerData.avatar !== "null") {
                if (partnerData.avatar_history_type === "url") {
                    partnerAvatar.innerHTML = `<img src="${partnerData.avatar}" alt="${partnerName.textContent}" />`;
                } else {
                    const profilePicRef = storageRef(storage, `profile_pictures/${partnerUid}`);
                    const profilePicUrl = await getDownloadURL(profilePicRef);
                    partnerAvatar.innerHTML = `<img src="${profilePicUrl}" alt="${partnerName.textContent}" />`;
                }
            } else {
                // Fallback to initial
                partnerAvatar.textContent = partnerName.textContent.charAt(0).toUpperCase();
            }
        } catch (error) {
            console.error("Error loading profile picture:", error);
            partnerAvatar.textContent = partnerName.textContent.charAt(0).toUpperCase();
        }

    } catch (error) {
        console.error("Error loading partner data:", error);
    }
}

// Load messages for the current chat
function loadMessages() {
    if (!currentUser || !partnerUid) return;

    chatMessages.innerHTML = '';

    const messagesRef = ref(database, `skyline/chats/${currentUser}/${partnerUid}`);
    const messagesQuery = query(messagesRef, orderByChild('push_date'));

    onChildAdded(messagesQuery, (snapshot) => {
        const message = snapshot.val();
        if (message.TYPE === "MESSAGE") {
            displayMessage(message);
            scrollToBottom();
        }
    });
}

// Display a message in the chat
function displayMessage(messageData) {
    if (!messageData || messageData.TYPE !== "MESSAGE") return;

    const messageElement = document.createElement('div');
    const isCurrentUser = messageData.uid === currentUser;
    messageElement.classList.add('message', isCurrentUser ? 'message-sent' : 'message-received');

    // Format timestamp
    const timestamp = parseInt(messageData.push_date);
    const timeString = isNaN(timestamp) ? 'now' :
        new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    // Show sender name only for received messages in group chats (future enhancement)
    const showSender = !isCurrentUser;

    messageElement.innerHTML = `
        ${showSender ? `<div class="message-sender">${partnerName.textContent}</div>` : ''}
        <div class="message-text">${messageData.message_text}</div>
        <div class="message-time">${timeString}</div>
    `;

    chatMessages.appendChild(messageElement);
}

// Send a new message
async function sendMessage() {
    const messageText = messageInput.value.trim();
    if (!messageText || !currentUser || !partnerUid) return;

    try {
        const timestamp = Date.now().toString();
        const messageId = push(ref(database)).key;

        // Create message object
        const message = {
            TYPE: "MESSAGE",
            key: messageId,
            message_state: "sended",
            message_text: messageText,
            push_date: timestamp,
            uid: currentUser
        };

        // Save message to both users' chat nodes
        await set(ref(database, `skyline/chats/${currentUser}/${partnerUid}/${messageId}`), message);
        await set(ref(database, `skyline/chats/${partnerUid}/${currentUser}/${messageId}`), message);

        // Update inbox for both users
        await updateInbox(currentUser, partnerUid, messageText, timestamp);
        await updateInbox(partnerUid, currentUser, messageText, timestamp);

        // Clear input and scroll to bottom
        messageInput.value = '';
        scrollToBottom();

    } catch (error) {
        console.error("Error sending message:", error);
        alert("Failed to send message. Please try again.");
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

// Setup typing indicator listener
function setupTypingIndicator() {
    const typingRef = ref(database, `skyline/chats/${partnerUid}/${currentUser}/typing-message`);
    onValue(typingRef, (snapshot) => {
        const typingData = snapshot.val();
        if (typingData && typingData.typingMessageStatus === "true") {
            typingIndicator.textContent = `${partnerName.textContent} is typing...`;
            typingIndicator.style.display = 'block';
        } else {
            typingIndicator.style.display = 'none';
        }
    });
}

// Handle typing indicator
let typingTimeout;
messageInput.addEventListener('input', () => {
    // Set typing status
    update(ref(database, `skyline/chats/${currentUser}/${partnerUid}/typing-message`), {
        typingMessageStatus: "true",
        uid: currentUser
    });

    // Clear previous timeout
    if (typingTimeout) clearTimeout(typingTimeout);

    // Set timeout to clear typing status after 2 seconds of inactivity
    typingTimeout = setTimeout(() => {
        update(ref(database, `skyline/chats/${currentUser}/${partnerUid}/typing-message`), {
            typingMessageStatus: "false",
            uid: currentUser
        });
    }, 2000);
});

// Scroll to bottom of chat
function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// Event listeners
sendButton.addEventListener('click', sendMessage);
messageInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault(); // Prevent default behavior (new line)
        sendMessage();
    }
    // Shift+Enter will create a new line as normal
});

backButton.addEventListener('click', () => {
    window.history.back();
});

// Initial scroll to bottom
window.addEventListener('load', () => {
    setTimeout(scrollToBottom, 500);
});

// Prevent zooming
document.addEventListener('wheel', function(e) {
    if (e.ctrlKey) e.preventDefault();
}, { passive: false });

document.addEventListener('touchstart', function (e) {
    if (e.touches.length > 1) {
        e.preventDefault(); // Prevent pinch zoom
    }
}, { passive: false });

document.addEventListener('touchmove', function (e) {
    if (e.touches.length > 1) {
        e.preventDefault(); // Prevent pinch zoom
    }
}, { passive: false });

document.addEventListener('gesturestart', function (e) {
    e.preventDefault();
});