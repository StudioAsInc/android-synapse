
/**
 * @fileoverview Theme service for managing light/dark mode preferences.
 * Persists theme selection and applies it to the DOM.
 */

import { Injectable, signal, effect } from '@angular/core';

/**
 * Service for managing application theme (light/dark mode).
 * Persists user preference to localStorage and applies theme to the document.
 * 
 * @injectable
 * @providedIn 'root'
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  /** Signal indicating if dark mode is currently active */
  darkMode = signal<boolean>(true);
  
  /** Flag to prevent rapid theme transitions */
  private isTransitioning = false;

  /**
   * Initialize theme service.
   * Loads saved theme preference or uses system preference.
   * Sets up reactive effect to apply theme changes to DOM.
   */
  constructor() {
    const saved = localStorage.getItem('theme');
    if (saved) {
      this.darkMode.set(saved === 'dark');
    } else {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.darkMode.set(prefersDark);
      
      // Listen for system theme changes only if user hasn't set a preference
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        if (!localStorage.getItem('theme')) {
          this.darkMode.set(e.matches);
        }
      });
    }

    effect(() => {
      const isDark = this.darkMode();
      localStorage.setItem('theme', isDark ? 'dark' : 'light');
      const html = document.documentElement;
      
      if (isDark) {
        html.classList.add('dark');
        html.style.colorScheme = 'dark';
      } else {
        html.classList.remove('dark');
        html.style.colorScheme = 'light';
      }
      
      // Update meta color-scheme tag
      let meta = document.querySelector('meta[name="color-scheme"]') as HTMLMetaElement;
      if (meta) {
        meta.content = isDark ? 'dark' : 'light';
      }
      
      // Update theme-color for mobile browsers
      let themeColor = document.querySelector('meta[name="theme-color"]') as HTMLMetaElement;
      if (themeColor) {
        themeColor.content = isDark ? '#020617' : '#ffffff';
      }
    });
  }

  /**
   * Toggle between light and dark mode with smooth transition.
   * Prevents rapid successive toggles.
   * 
   * @param {MouseEvent} [event] - Optional mouse event from toggle button
   * @returns {void}
   */
  toggle(event?: MouseEvent) {
    if (this.isTransitioning) return;
    this.isTransitioning = true;
    
    this.darkMode.update(d => !d);
    
    setTimeout(() => {
      this.isTransitioning = false;
    }, 50);
  }

  /**
   * Toggle theme without animation (backup method).
   * Useful for immediate theme changes without visual transition.
   * 
   * @returns {void}
   */
  toggleImmediate() {
    this.darkMode.update(d => !d);
  }
}
