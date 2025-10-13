// Import necessary modules from the Firebase SDK.
// These URLs point to specific versions of the Firebase libraries hosted on Google's CDN.
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js"; // Core Firebase app initialization
import {
    getDatabase,     // Function to get the Realtime Database service
    ref,             // Function to create a reference to a specific location in the database
    query,           // Function to construct database queries
    orderByChild,    // Query clause to order results by a specific child key
    onValue,         // Listener for real-time data changes
    get              // Function to get data once
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-database.js"; // Firebase Realtime Database
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js"; // Firebase Authentication
import { getStorage, ref as storageRef, getDownloadURL } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-storage.js"; // Firebase Cloud Storage

// Firebase configuration object.
// These are unique keys and URLs for your specific Firebase project.
const firebaseConfig = {
    apiKey: "AIzaSyCJSkL4MsVRVwb9sWGvkFQHz-RaGUY-2xo", // Your Firebase project API key
    databaseURL: "https://sai-synapse-default-rtdb.firebaseio.com", // URL of your Realtime Database
    projectId: "sai-synapse",     // Your Firebase project ID
    authDomain: "sai-synapse.firebasestorage.app",
    storageBucket: "synapse-social-sai.firebasestorage.app", // Your Firebase Storage bucket URL
    appId: "1:308269400761:android:91c2e7415671cc49ed9168" // Your Firebase App ID (often for Android/iOS)
};

// Initialize Firebase with the provided configuration.
const app = initializeApp(firebaseConfig);
// Get instances of various Firebase services.
const database = getDatabase(app); // Realtime Database instance
const auth = getAuth(app);         // Authentication instance
const storage = getStorage(app);   // Storage instance

// Get references to key HTML DOM elements by their IDs.
// These elements will be manipulated by the JavaScript to display dynamic content.
const userInfoDiv = document.getElementById('user-info');             // Div to display user login status
const conversationsList = document.getElementById('conversations-list'); // Ul/div to display conversation list
const loadingDiv = document.getElementById('loading');                 // General loading indicator

// NEW: Get reference to the loading overlay element.
const loadingOverlay = document.getElementById('loadingOverlay');      // Full-screen loading overlay

let currentUser = null; // Variable to store the UID (User ID) of the currently logged-in user. Initialized to null.

// NEW: Functions to show and hide the loading overlay and loading spinner.
function showLoading() {
    // If the overlay element exists, set its display style to 'flex' to show it.
    if (loadingOverlay) loadingOverlay.style.display = 'flex';
    // If the loading spinner element exists, set its display style to 'block' to show it.
    if (loadingDiv) loadingDiv.style.display = 'block';
}

function hideLoading() {
    // If the overlay element exists, set its display style to 'none' to hide it.
    if (loadingOverlay) loadingOverlay.style.display = 'none';
    // If the loading spinner element exists, set its display style to 'none' to hide it.
    if (loadingDiv) loadingDiv.style.display = 'none';
}

// ---
// User Authentication State Listener
// ---

// Listens for changes in the user's authentication state (e.g., login, logout).
onAuthStateChanged(auth, (user) => {
    if (user) {
        // If a user is logged in:
        currentUser = user.uid; // Store the user's UID.
        // Display the logged-in user's email or display name.
        userInfoDiv.textContent = `Logged in as: ${user.email || user.displayName || 'User'}`;
        showLoading(); // NEW: Show loading indicator while data is being fetched for the authenticated user.
        loadConversations(); // Load the user's conversations.
        loadUserProfile(user.uid); // Load and display the user's profile information.
    } else {
        // If no user is logged in (logged out or not authenticated):
        currentUser = null; // Clear the current user UID.
        userInfoDiv.textContent = "Please log in to view messages"; // Prompt the user to log in.
        // Display a message in the conversations list indicating that login is required.
        conversationsList.innerHTML = '<div class="conversation">Please log in to view your messages</div>';
        clearUserProfile(); // Clear any displayed user profile information.
        hideLoading(); // NEW: Hide loading indicator if no user is authenticated.
    }
});

// ---
// Functions for Conversations/Inbox
// ---

/**
 * Loads and displays the current user's conversations from Firebase.
 * This function is called when a user successfully logs in.
 */
function loadConversations() {
    // If no user is logged in, exit the function.
    if (!currentUser) return;

    showLoading(); // NEW: Show loading indicator before starting to load conversations.

    // Create a reference to the current user's inbox in the Realtime Database.
    const inboxRef = ref(database, `skyline/inbox/${currentUser}`);
    // Create a query to order conversations by the 'push_date' child.
    const inboxQuery = query(inboxRef, orderByChild('push_date'));

    // Listen for real-time changes to the inbox data.
    onValue(inboxQuery, (snapshot) => {
        conversationsList.innerHTML = ''; // Clear any previously displayed conversations.
        hideLoading(); // NEW: Hide loading indicator once conversation data starts arriving.

        const conversations = []; // Array to store conversation objects.
        // Iterate over each child (conversation) in the snapshot.
        snapshot.forEach((childSnapshot) => {
            const conversation = childSnapshot.val(); // Get the conversation data.
            conversation.contactId = childSnapshot.key; // Add the contact's UID (which is the key) to the conversation object.
            conversations.push(conversation); // Add the conversation to the array.
        });

        // Sort conversations by 'push_date' in descending order (most recent first).
        conversations.sort((a, b) => b.push_date - a.push_date);

        // Check if there are any conversations.
        if (conversations.length === 0) {
            // If no conversations, display a "No messages yet" message.
            conversationsList.innerHTML = '<div class="conversation">No messages yet</div>';
        } else {
            // If conversations exist, display each one.
            conversations.forEach(conversation => {
                displayConversation(conversation);
            });
        }
    }, (error) => {
        // Handle any errors during conversation loading.
        console.error("Error loading conversations:", error);
        hideLoading(); // NEW: Hide loading indicator on error.
        // Display an error message in the conversations list.
        conversationsList.innerHTML = '<div class="conversation">Error loading messages</div>';
    });
}

/**
 * Displays a single conversation in the conversations list.
 * This function dynamically creates HTML elements for each conversation.
 * @param {object} conversation - The conversation object from Firebase, containing details like last message, push date, and contact ID.
 */
async function displayConversation(conversation) {
    const contactId = conversation.contactId; // The UID of the other person in the conversation.
    // Fetch detailed information about the contact (e.g., nickname, username, avatar).
    const userDetails = await getUserDetails(contactId);
    // Determine the user's name for display, preferring nickname, then username, then a generic "User".
    const userName = userDetails?.nickname || userDetails?.username || `User ${contactId.substring(0, 4)}`;
    const firstLetter = userName.charAt(0).toUpperCase(); // Get the first letter for avatar fallback.

    // Create an anchor tag (<a>) to make the conversation clickable and link to the chat page.
    const conversationElement = document.createElement('a');
    // Set the href to the chat.html page, passing the contact's UID and name as URL parameters.
    conversationElement.href = `chat.html?uid=${contactId}&name=${encodeURIComponent(userName)}`;
    conversationElement.className = 'conversation'; // Add a CSS class for styling.

    // Format the message time for display.
    const messageTime = new Date(parseInt(conversation.push_date)); // Convert push_date (timestamp) to Date object.
    const timeString = messageTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }); // Format as "HH:MM".
    // If the message is from today, show only time; otherwise, show full date.
    const dateString = isToday(messageTime) ? timeString : messageTime.toLocaleDateString();

    // Create a div for the user avatar.
    const avatarContainer = document.createElement('div');
    avatarContainer.className = 'user-avatar';

    // Try to load the contact's avatar image.
    // Check if userDetails exists, and if 'avatar' property is not null or empty.
    if (userDetails?.avatar && userDetails.avatar !== "null" && userDetails.avatar !== "") {
        const img = document.createElement('img'); // Create an image element.
        img.src = userDetails.avatar; // Set the image source.
        img.alt = userName; // Set alt text for accessibility.
        img.onerror = () => {
            // If the image fails to load, fallback to displaying the first letter of the name.
            avatarContainer.textContent = firstLetter;
        };
        avatarContainer.appendChild(img); // Add the image to the avatar container.
    } else {
        // If no avatar is available or valid, display the first letter of the name.
        avatarContainer.textContent = firstLetter;
    }

    // Clear existing content and build the inner HTML of the conversation element.
    conversationElement.innerHTML = ''; // Clear previous content.
    conversationElement.appendChild(avatarContainer); // Add the avatar container.
    conversationElement.innerHTML += `
        <div class="conversation-details">
            <div class="conversation-name">${userName}</div>              <div class="conversation-preview">${conversation.last_message_text}</div> </div>
        <div class="conversation-time">${dateString}</div>                 `;

    conversationsList.appendChild(conversationElement); // Add the completed conversation element to the list.
}

/**
 * Fetches user details (e.g., nickname, username, avatar) from Firebase for a given UID.
 * @param {string} uid - The user ID to fetch details for.
 * @returns {Promise<object|null>} - A promise that resolves with the user details object if found, or null if not.
 */
async function getUserDetails(uid) {
    try {
        const userRef = ref(database, `skyline/users/${uid}`); // Reference to the user's profile data.
        const snapshot = await get(userRef); // Get the data once.
        return snapshot.val(); // Return the data as a JavaScript object.
    } catch (error) {
        console.error("Error fetching user details:", error); // Log any errors.
        return null; // Return null on error.
    }
}

/**
 * Checks if a given Date object represents the current day.
 * @param {Date} date - The date to check.
 * @returns {boolean} - True if the date is today, false otherwise.
 */
function isToday(date) {
    const today = new Date(); // Get the current date.
    // Compare year, month, and day to determine if it's today.
    return date.getDate() === today.getDate() &&
           date.getMonth() === today.getMonth() &&
           date.getFullYear() === today.getFullYear();
}

// ---
// Functions for User Profile
// ---

/**
 * Loads and displays the current user's profile information in the UI.
 * This includes banner image, avatar, nickname, username, and biography.
 * @param {string} uid - The UID of the user whose profile is to be loaded.
 */
async function loadUserProfile(uid) {
    const userRef = ref(database, 'skyline/users/' + uid); // Reference to the user's profile data.
    try {
        const snapshot = await get(userRef); // Get the user's profile data once.
        if (snapshot.exists()) {
            // If user data exists:
            const data = snapshot.val(); // Get the profile data as an object.

            // Handle avatar loading, including potential fallbacks.
            await loadProfileImage(data, uid);

            // Update the profile banner image.
            const profileBanner = document.querySelector('.profile-banner');
            if (profileBanner) {
                // If profile_cover_image exists and is not "null", set it as background image.
                if (data.profile_cover_image && data.profile_cover_image !== "null") {
                    profileBanner.style.backgroundImage = `url(${data.profile_cover_image})`;
                } else {
                    // Otherwise, clear any existing background image.
                    profileBanner.style.backgroundImage = '';
                }
            }

            // Update username and name.
            const profileName = document.querySelector('.profile-name');
            if (profileName) profileName.textContent = data.nickname || "No Name"; // Display nickname or "No Name".

            const profileUsername = document.querySelector('.profile-username');
            if (profileUsername) profileUsername.textContent = '' + (data.username || "unknown"); // Display username with '@'.

            // Update biography.
            const profileBio = document.querySelector('.profile-bio');
            if (profileBio) profileBio.textContent = data.biography || "No bio added."; // Display biography or default text.

        } else {
            console.log("No user data found for profile.");
            clearUserProfile(); // Clear profile UI if data is not found.
        }
    } catch (error) {
        console.error("Error fetching user profile:", error); // Log any errors during fetching.
        clearUserProfile(); // Clear profile UI on error.
    }
}

/**
 * Loads the user's profile image (avatar) with proper fallback handling.
 * This function tries to load from a URL or Firebase Storage and falls back to initials.
 * It targets two potential avatar display areas: '#user1-info .profile-avatar' and '.dialog-avatar'.
 * @param {object} userData - The user's profile data object.
 * @param {string} uid - The user's UID.
 */
async function loadProfileImage(userData, uid) {
    // Get references to the avatar elements.
    const profileAvatar = document.querySelector('#user1-info .profile-avatar');
    const dialogAvatar = document.querySelector('.dialog-avatar');

    // Helper function to set the fallback avatar (initials).
    const setFallbackAvatar = () => {
        const name = userData?.nickname || userData?.username || 'User'; // Get a name for the initial.
        const initial = name.charAt(0).toUpperCase(); // Get the first letter, capitalized.
        if (profileAvatar) {
            profileAvatar.innerHTML = ''; // Clear any existing image.
            profileAvatar.textContent = initial; // Set text content to the initial.
            profileAvatar.style.backgroundImage = ''; // Clear any background image.
        }
        if (dialogAvatar) {
            dialogAvatar.innerHTML = '';
            dialogAvatar.textContent = initial;
            dialogAvatar.style.backgroundImage = '';
        }
    };

    try {
        // Check if avatar data exists and is not null or empty.
        if (userData?.avatar && userData.avatar !== "null" && userData.avatar !== "") {
            let imageUrl = userData.avatar; // Assume the avatar value is initially a URL.
            // If avatar_history_type is not "url", it implies the avatar is stored in Firebase Storage.
            if (userData.avatar_history_type !== "url") {
                try {
                    // Try to get the download URL from Firebase Storage.
                    const storageReference = storageRef(storage, `profile_pictures/${uid}`);
                    imageUrl = await getDownloadURL(storageReference);
                } catch (storageError) {
                    console.log("Profile picture not found in storage, trying direct URL:", storageError);
                    // If storage fails, revert to the original `userData.avatar` value, assuming it might be a direct URL fallback.
                    imageUrl = userData.avatar;
                }
            }

            const img = new Image(); // Create a new Image object.
            img.src = imageUrl; // Set the image source.
            img.onload = () => {
                // When the image loads successfully:
                if (profileAvatar) {
                    profileAvatar.innerHTML = ''; // Clear any existing content.
                    profileAvatar.appendChild(img.cloneNode()); // Append a clone of the loaded image.
                }
                if (dialogAvatar) {
                    dialogAvatar.innerHTML = '';
                    dialogAvatar.appendChild(img.cloneNode());
                }
            };
            // If the image fails to load from the URL, call the fallback function.
            img.onerror = setFallbackAvatar;
        } else {
            // If no avatar data or invalid data, use the fallback.
            setFallbackAvatar();
        }
    } catch (error) {
        console.error("Error loading profile image:", error); // Log any errors.
        setFallbackAvatar(); // Use fallback on any unhandled error.
    }
}

/**
 * Clears the displayed user profile information from the UI.
 * This function is called when a user logs out or if profile data isn't found.
 */
function clearUserProfile() {
    // Clear Avatar display.
    const profileAvatar = document.querySelector('#user1-info .profile-avatar');
    if (profileAvatar) {
        profileAvatar.innerHTML = ''; // Remove any image element.
        profileAvatar.style.backgroundImage = ''; // Clear any background image set via CSS.
        profileAvatar.textContent = ''; // Clear any initials displayed.
    }

    const dialogAvatar = document.querySelector('.dialog-avatar');
    if (dialogAvatar) {
        dialogAvatar.innerHTML = '';
        dialogAvatar.style.backgroundImage = '';
        dialogAvatar.textContent = '';
    }

    // Clear Banner display.
    const profileBanner = document.querySelector('.profile-banner');
    if (profileBanner) profileBanner.style.backgroundImage = ''; // Clear background image.

    // Clear Username & Name display.
    const profileName = document.querySelector('.profile-name');
    if (profileName) profileName.textContent = '';

    const profileUsername = document.querySelector('.profile-username');
    if (profileUsername) profileUsername.textContent = '';

    // Clear Bio display.
    const profileBio = document.querySelector('.profile-bio');
    if (profileBio) profileBio.textContent = '';
}

// ---
// Event Listeners
// ---

// Event listener for clicking the profile trigger button.
document.getElementById('profile-trigger').addEventListener('click', function() {
    // When clicked, display the profile dialog by setting its display style to 'flex'.
    document.getElementById('profile-dialog').style.display = 'flex';
});

// Event listener for clicking the dialog close button within the profile dialog.
document.getElementById('dialog-close').addEventListener('click', function() {
    // When clicked, hide the profile dialog by setting its display style to 'none'.
    document.getElementById('profile-dialog').style.display = 'none';
});

// ---
// Theme Toggle Functionality
// ---

// Ensures the DOM is fully loaded before running the theme logic.
document.addEventListener('DOMContentLoaded', function() {
    const themeToggle = document.getElementById('theme-toggle'); // The button/element to toggle theme.
    const themeIcon = document.getElementById('theme-icon');     // The icon that changes with the theme.
    const html = document.documentElement;                       // Reference to the <html> tag (for applying classes).

    // Define the cycle of themes and corresponding Material Icons.
    const themes = ['light', 'dark', 'auto'];         // Possible themes
    const icons = ['light_mode', 'dark_mode', 'brightness_auto']; // Icons for each theme

    // Get the saved theme from localStorage, or default to 'auto' if no theme is saved.
    let currentTheme = localStorage.getItem('theme') || 'auto';
    // Ensure the retrieved theme is one of the valid options.
    if (!themes.includes(currentTheme)) currentTheme = 'auto';

    // Set the initial theme when the page loads.
    setTheme(currentTheme);

    // Add event listener to the theme toggle button.
    themeToggle.addEventListener('click', function() {
        // Find the current theme's index in the `themes` array.
        const currentIndex = themes.indexOf(currentTheme);
        // Calculate the index for the next theme in the cycle (loops back to start).
        const nextIndex = (currentIndex + 1) % themes.length;
        currentTheme = themes[nextIndex]; // Update `currentTheme` to the next theme.

        setTheme(currentTheme); // Apply the new theme.
        localStorage.setItem('theme', currentTheme); // Save the new theme to localStorage.
    });

    /**
     * Applies the specified theme to the HTML document.
     * @param {string} theme - The theme to apply ('light', 'dark', or 'auto').
     */
    function setTheme(theme) {
        // Remove any existing theme classes ('light', 'dark') from the <html> element.
        html.classList.remove('light', 'dark');

        if (theme === 'auto') {
            // If 'auto' theme is selected, use the system's color scheme preference.
            if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
                // If system prefers dark mode, add 'dark' class.
                html.classList.add('dark');
            } else {
                // Otherwise (system prefers light mode or no preference), add 'light' class.
                html.classList.add('light');
            }
            // Set the icon for auto mode.
            themeIcon.textContent = 'brightness_auto';
        } else {
            // If a specific theme ('light' or 'dark') is selected, apply that class directly.
            html.classList.add(theme);
            // Set the appropriate icon based on the chosen theme.
            themeIcon.textContent = theme === 'dark' ? 'dark_mode' : 'light_mode';
        }
    }

    // Watch for system theme changes when in 'auto' mode.
    // This listener ensures that if the user changes their system's theme preference,
    // and our app is set to 'auto' theme, the UI will update accordingly without a reload.
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
        if (localStorage.getItem('theme') === 'auto') {
            setTheme('auto'); // Re-apply 'auto' theme to reflect the system change.
        }
    });
});
