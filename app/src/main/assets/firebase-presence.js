/**
 * Firebase Presence System for Synapse Social Media App
 * 
 * This module provides a comprehensive presence system that:
 * - Monitors user connection status using Firebase Realtime Database
 * - Automatically sets online/offline status based on connection
 * - Tracks last activity timestamps
 * - Provides real-time status updates for other users
 * - Handles graceful disconnections and reconnections
 * 
 * @author StudioAs Inc.
 * @version 1.0.0
 * @license Custom
 */

// Firebase configuration and initialization
class FirebasePresenceSystem {
    constructor(config) {
        this.config = {
            databaseURL: config.databaseURL || 'https://synapse-app-default-rtdb.firebaseio.com',
            presencePath: config.presencePath || 'presence',
            userStatusPath: config.userStatusPath || 'user_status',
            connectionTimeout: config.connectionTimeout || 30000, // 30 seconds
            heartbeatInterval: config.heartbeatInterval || 25000, // 25 seconds
            ...config
        };
        
        this.currentUser = null;
        this.isConnected = false;
        this.connectionRef = null;
        this.userStatusRef = null;
        this.heartbeatInterval = null;
        this.statusListeners = new Map();
        this.connectionListeners = [];
        
        // Initialize Firebase if not already initialized
        this.initializeFirebase();
    }

    /**
     * Initialize Firebase SDK
     */
    initializeFirebase() {
        // Check if Firebase is already initialized
        if (typeof firebase === 'undefined') {
            console.error('Firebase SDK not loaded. Please include Firebase in your project.');
            return;
        }

        // Initialize Firebase if not already done
        if (!firebase.apps.length) {
            firebase.initializeApp({
                databaseURL: this.config.databaseURL
            });
        }

        this.database = firebase.database();
        this.connectionRef = this.database.ref('.info/connected');
        this.setupConnectionMonitoring();
    }

    /**
     * Set up connection monitoring using Firebase's .info/connected
     */
    setupConnectionMonitoring() {
        this.connectionRef.on('value', (snapshot) => {
            const connected = snapshot.val();
            this.isConnected = connected;
            
            if (connected && this.currentUser) {
                this.onUserConnected();
            } else if (!connected) {
                this.onUserDisconnected();
            }
            
            // Notify connection listeners
            this.connectionListeners.forEach(listener => {
                try {
                    listener(connected);
                } catch (error) {
                    console.error('Error in connection listener:', error);
                }
            });
        });
    }

    /**
     * Initialize presence for a specific user
     * @param {string} userId - The user's unique ID
     * @param {Object} userData - Additional user data to store
     */
    initializeUserPresence(userId, userData = {}) {
        if (!userId) {
            throw new Error('User ID is required for presence initialization');
        }

        this.currentUser = {
            id: userId,
            ...userData
        };

        // Set up user status reference
        this.userStatusRef = this.database.ref(`${this.config.userStatusPath}/${userId}`);
        
        // Set up presence reference
        this.presenceRef = this.database.ref(`${this.config.presencePath}/${userId}`);
        
        // Set up onDisconnect handlers
        this.setupDisconnectHandlers();
        
        // Start heartbeat if connected
        if (this.isConnected) {
            this.onUserConnected();
        }

        console.log(`Presence system initialized for user: ${userId}`);
    }

    /**
     * Set up Firebase onDisconnect handlers for automatic offline status
     */
    setupDisconnectHandlers() {
        if (!this.userStatusRef || !this.presenceRef) {
            console.warn('User references not initialized. Cannot set up disconnect handlers.');
            return;
        }

        // Set up onDisconnect for user status
        this.userStatusRef.onDisconnect().update({
            status: 'offline',
            last_changed: firebase.database.ServerValue.TIMESTAMP,
            last_seen: firebase.database.ServerValue.TIMESTAMP
        });

        // Set up onDisconnect for presence
        this.presenceRef.onDisconnect().remove();

        console.log('Disconnect handlers configured for automatic offline status');
    }

    /**
     * Handle user connection
     */
    onUserConnected() {
        if (!this.currentUser || !this.userStatusRef || !this.presenceRef) {
            return;
        }

        const timestamp = firebase.database.ServerValue.TIMESTAMP;
        
        // Update user status to online
        this.userStatusRef.update({
            status: 'online',
            last_changed: timestamp,
            last_seen: timestamp,
            user_id: this.currentUser.id,
            display_name: this.currentUser.displayName || this.currentUser.username || 'Unknown User',
            avatar_url: this.currentUser.avatarUrl || null,
            platform: this.currentUser.platform || 'web'
        });

        // Set presence data
        this.presenceRef.set({
            user_id: this.currentUser.id,
            connected_at: timestamp,
            last_activity: timestamp,
            platform: this.currentUser.platform || 'web'
        });

        // Start heartbeat
        this.startHeartbeat();

        console.log(`User ${this.currentUser.id} is now online`);
    }

    /**
     * Handle user disconnection
     */
    onUserDisconnected() {
        if (!this.currentUser) {
            return;
        }

        // Stop heartbeat
        this.stopHeartbeat();

        console.log(`User ${this.currentUser.id} disconnected`);
    }

    /**
     * Start heartbeat to keep presence alive
     */
    startHeartbeat() {
        if (this.heartbeatInterval) {
            this.stopHeartbeat();
        }

        this.heartbeatInterval = setInterval(() => {
            if (this.isConnected && this.presenceRef) {
                this.presenceRef.update({
                    last_activity: firebase.database.ServerValue.TIMESTAMP
                });
            }
        }, this.config.heartbeatInterval);

        console.log('Heartbeat started');
    }

    /**
     * Stop heartbeat
     */
    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
            console.log('Heartbeat stopped');
        }
    }

    /**
     * Update user's current activity
     * @param {string} activity - Current activity (e.g., 'typing', 'viewing_profile', etc.)
     */
    updateActivity(activity) {
        if (!this.isConnected || !this.presenceRef) {
            return;
        }

        this.presenceRef.update({
            current_activity: activity,
            last_activity: firebase.database.ServerValue.TIMESTAMP
        });
    }

    /**
     * Manually set user status
     * @param {string} status - Status to set ('online', 'offline', 'away', 'busy')
     */
    setUserStatus(status) {
        if (!this.isConnected || !this.userStatusRef) {
            return;
        }

        const validStatuses = ['online', 'offline', 'away', 'busy'];
        if (!validStatuses.includes(status)) {
            console.warn(`Invalid status: ${status}. Must be one of: ${validStatuses.join(', ')}`);
            return;
        }

        this.userStatusRef.update({
            status: status,
            last_changed: firebase.database.ServerValue.TIMESTAMP,
            last_seen: firebase.database.ServerValue.TIMESTAMP
        });

        console.log(`User status set to: ${status}`);
    }

    /**
     * Listen to another user's status changes
     * @param {string} userId - User ID to monitor
     * @param {Function} callback - Callback function(status, userData)
     * @returns {Function} - Function to remove the listener
     */
    listenToUserStatus(userId, callback) {
        if (!userId || typeof callback !== 'function') {
            console.error('Invalid parameters for listenToUserStatus');
            return null;
        }

        const statusRef = this.database.ref(`${this.config.userStatusPath}/${userId}`);
        
        const listener = statusRef.on('value', (snapshot) => {
            const data = snapshot.val();
            if (data) {
                callback(data.status, data);
            } else {
                callback('offline', null);
            }
        });

        // Store listener for cleanup
        this.statusListeners.set(userId, { ref: statusRef, listener: listener });

        return () => this.removeUserStatusListener(userId);
    }

    /**
     * Remove listener for a specific user
     * @param {string} userId - User ID to stop monitoring
     */
    removeUserStatusListener(userId) {
        const listenerData = this.statusListeners.get(userId);
        if (listenerData) {
            listenerData.ref.off('value', listenerData.listener);
            this.statusListeners.delete(userId);
            console.log(`Removed status listener for user: ${userId}`);
        }
    }

    /**
     * Listen to multiple users' status changes
     * @param {Array} userIds - Array of user IDs to monitor
     * @param {Function} callback - Callback function(userId, status, userData)
     * @returns {Function} - Function to remove all listeners
     */
    listenToMultipleUsers(userIds, callback) {
        if (!Array.isArray(userIds) || typeof callback !== 'function') {
            console.error('Invalid parameters for listenToMultipleUsers');
            return null;
        }

        const removers = userIds.map(userId => 
            this.listenToUserStatus(userId, (status, userData) => {
                callback(userId, status, userData);
            })
        );

        return () => {
            removers.forEach(remover => {
                if (remover) remover();
            });
        };
    }

    /**
     * Get current online users
     * @param {Function} callback - Callback function(onlineUsers)
     */
    getOnlineUsers(callback) {
        if (typeof callback !== 'function') {
            console.error('Callback function is required for getOnlineUsers');
            return;
        }

        const statusRef = this.database.ref(this.config.userStatusPath);
        statusRef.orderByChild('status').equalTo('online').once('value')
            .then((snapshot) => {
                const onlineUsers = [];
                snapshot.forEach((childSnapshot) => {
                    onlineUsers.push({
                        userId: childSnapshot.key,
                        ...childSnapshot.val()
                    });
                });
                callback(onlineUsers);
            })
            .catch((error) => {
                console.error('Error fetching online users:', error);
                callback([]);
            });
    }

    /**
     * Add connection status listener
     * @param {Function} callback - Callback function(isConnected)
     */
    onConnectionChange(callback) {
        if (typeof callback === 'function') {
            this.connectionListeners.push(callback);
        }
    }

    /**
     * Remove connection status listener
     * @param {Function} callback - Callback function to remove
     */
    removeConnectionListener(callback) {
        const index = this.connectionListeners.indexOf(callback);
        if (index > -1) {
            this.connectionListeners.splice(index, 1);
        }
    }

    /**
     * Get current connection status
     * @returns {boolean} - True if connected, false otherwise
     */
    isUserConnected() {
        return this.isConnected;
    }

    /**
     * Get current user data
     * @returns {Object|null} - Current user data or null
     */
    getCurrentUser() {
        return this.currentUser;
    }

    /**
     * Clean up all listeners and references
     */
    cleanup() {
        // Stop heartbeat
        this.stopHeartbeat();

        // Remove all status listeners
        this.statusListeners.forEach((listenerData, userId) => {
            this.removeUserStatusListener(userId);
        });

        // Clear connection listeners
        this.connectionListeners = [];

        // Remove connection monitoring
        if (this.connectionRef) {
            this.connectionRef.off();
        }

        // Clear references
        this.currentUser = null;
        this.userStatusRef = null;
        this.presenceRef = null;
        this.connectionRef = null;

        console.log('Presence system cleaned up');
    }
}

// Export for different module systems
if (typeof module !== 'undefined' && module.exports) {
    // Node.js/CommonJS
    module.exports = FirebasePresenceSystem;
} else if (typeof define === 'function' && define.amd) {
    // AMD
    define([], function() {
        return FirebasePresenceSystem;
    });
} else if (typeof window !== 'undefined') {
    // Browser global
    window.FirebasePresenceSystem = FirebasePresenceSystem;
}

// Example usage and integration guide
if (typeof window !== 'undefined') {
    // Browser usage example
    window.SynapsePresenceExample = {
        /**
         * Example: Initialize presence system
         */
        initialize: function() {
            // Firebase config (replace with your actual config)
            const firebaseConfig = {
                databaseURL: 'https://your-project.firebaseio.com',
                presencePath: 'presence',
                userStatusPath: 'user_status',
                connectionTimeout: 30000,
                heartbeatInterval: 25000
            };

            // Initialize presence system
            const presenceSystem = new FirebasePresenceSystem(firebaseConfig);

            // Initialize for current user
            const currentUser = {
                id: 'user123',
                displayName: 'John Doe',
                username: 'johndoe',
                avatarUrl: 'https://example.com/avatar.jpg',
                platform: 'web'
            };

            presenceSystem.initializeUserPresence(currentUser.id, currentUser);

            // Listen to connection changes
            presenceSystem.onConnectionChange((isConnected) => {
                console.log('Connection status:', isConnected ? 'Connected' : 'Disconnected');
            });

            // Listen to another user's status
            const removeListener = presenceSystem.listenToUserStatus('user456', (status, userData) => {
                console.log('User 456 status:', status, userData);
            });

            // Update activity
            presenceSystem.updateActivity('typing');

            // Cleanup when done
            // presenceSystem.cleanup();
        }
    };
}