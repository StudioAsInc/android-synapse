/**
 * @fileoverview Root component for the Synapse application.
 * Manages PWA installation and global application state.
 */

import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PwaService } from './services/pwa.service';
import { LiveRegionComponent } from './components/live-region.component';

/**
 * Root component of the Synapse application.
 * Handles PWA installation prompts and initializes global services.
 * 
 * @component
 * @selector app-root
 * @standalone true
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, LiveRegionComponent],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  /** Service for managing Progressive Web App functionality */
  pwaService = inject(PwaService);

  /**
   * Initialize the application.
   * PWA service is automatically initialized via dependency injection.
   */
  ngOnInit() {
    // PWA is automatically initialized via the service
  }

  /**
   * Trigger PWA installation prompt.
   * Shows the browser's native install dialog if available.
   * 
   * @returns {Promise<void>} Resolves when installation is complete or dismissed
   */
  async installPwa() {
    const installed = await this.pwaService.install();
    if (installed) {
      console.log('✅ App installed successfully');
    }
  }

  /**
   * Dismiss the PWA installation prompt.
   * Hides the install banner from the UI.
   */
  dismissInstall() {
    this.pwaService.isInstallable.set(false);
  }

  /**
   * Trigger PWA update check and installation.
   * Checks for new service worker versions and updates if available.
   * 
   * @returns {Promise<void>} Resolves when update check is complete
   */
  async updatePwa() {
    await this.pwaService.update();
  }
}
