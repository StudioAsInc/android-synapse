/**
 * @fileoverview Progressive Web App service for managing PWA features.
 * Handles installation, updates, notifications, and offline capabilities.
 */

import { Injectable, signal } from '@angular/core';

/**
 * Service for managing Progressive Web App functionality.
 * Provides methods for app installation, updates, push notifications, and online status monitoring.
 * 
 * @injectable
 * @providedIn 'root'
 */
@Injectable({
  providedIn: 'root'
})
export class PwaService {
  /** Deferred install prompt event */
  private deferredPrompt: any = null;
  
  /** Signal indicating if app can be installed */
  isInstallable = signal(false);
  
  /** Signal indicating if app is already installed */
  isInstalled = signal(false);
  
  /** Signal indicating current online status */
  isOnline = signal(navigator.onLine);
  
  /** Signal indicating if an update is available */
  updateAvailable = signal(false);

  /**
   * Initialize PWA service and set up event listeners.
   */
  constructor() {
    this.init();
  }

  /**
   * Initialize PWA features including install prompt, app status, and service worker updates.
   * 
   * @private
   * @returns {void}
   */
  private init() {
    // Check if already installed
    if (window.matchMedia('(display-mode: standalone)').matches) {
      this.isInstalled.set(true);
    }

    // Listen for install prompt
    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault();
      this.deferredPrompt = e;
      this.isInstallable.set(true);
    });

    // Listen for successful install
    window.addEventListener('appinstalled', () => {
      this.isInstalled.set(true);
      this.isInstallable.set(false);
      this.deferredPrompt = null;
    });

    // Monitor online/offline status
    window.addEventListener('online', () => this.isOnline.set(true));
    window.addEventListener('offline', () => this.isOnline.set(false));

    // Check for service worker updates
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.ready.then(registration => {
        registration.addEventListener('updatefound', () => {
          const newWorker = registration.installing;
          if (newWorker) {
            newWorker.addEventListener('statechange', () => {
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                this.updateAvailable.set(true);
              }
            });
          }
        });
      });
    }
  }

  /**
   * Trigger the browser's native install prompt.
   * Shows the install dialog to the user.
   * 
   * @returns {Promise<boolean>} True if user accepted installation, false otherwise
   */
  async install(): Promise<boolean> {
    if (!this.deferredPrompt) {
      return false;
    }

    this.deferredPrompt.prompt();
    const { outcome } = await this.deferredPrompt.userChoice;
    
    if (outcome === 'accepted') {
      this.deferredPrompt = null;
      this.isInstallable.set(false);
      return true;
    }
    
    return false;
  }

  /**
   * Check for service worker updates and reload the page.
   * 
   * @returns {Promise<void>}
   */
  async update() {
    if ('serviceWorker' in navigator) {
      const registration = await navigator.serviceWorker.ready;
      await registration.update();
      window.location.reload();
    }
  }

  /**
   * Request permission for browser notifications.
   * 
   * @returns {Promise<NotificationPermission>} The notification permission status
   */
  async requestNotificationPermission(): Promise<NotificationPermission> {
    if (!('Notification' in window)) {
      return 'denied';
    }
    return await Notification.requestPermission();
  }

  /**
   * Subscribe to push notifications via service worker.
   * Requires a valid VAPID public key to be configured.
   * 
   * @returns {Promise<PushSubscription | null>} The push subscription or null if failed
   */
  async subscribeToPushNotifications(): Promise<PushSubscription | null> {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      return null;
    }

    const registration = await navigator.serviceWorker.ready;
    
    try {
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: this.urlBase64ToUint8Array(
          'YOUR_VAPID_PUBLIC_KEY'
        ) as BufferSource
      });
      return subscription;
    } catch (error) {
      console.error('Failed to subscribe to push notifications:', error);
      return null;
    }
  }

  /**
   * Convert a base64 URL-safe string to a Uint8Array.
   * Used for converting VAPID public keys to the format required by PushManager.
   * 
   * @private
   * @param {string} base64String - The base64 URL-safe encoded string
   * @returns {Uint8Array} The decoded byte array
   */
  private urlBase64ToUint8Array(base64String: string): Uint8Array {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
      .replace(/\-/g, '+')
      .replace(/_/g, '/');

    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }
}
