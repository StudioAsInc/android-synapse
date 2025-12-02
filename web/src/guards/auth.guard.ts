/**
 * @fileoverview Route guard for protecting authenticated routes.
 * Ensures user is logged in before accessing protected pages.
 */

import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Route guard that protects routes requiring authentication.
 * Waits for auth initialization and redirects to login if user is not authenticated.
 * 
 * @returns {Promise<boolean | UrlTree>} True if user is authenticated, UrlTree to redirect to login otherwise
 * 
 * @example
 * // In route configuration:
 * {
 *   path: 'app',
 *   component: AppLayoutComponent,
 *   canActivate: [authGuard]
 * }
 */
export const authGuard = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Wait for auth to finish loading
  if (authService.isLoading()) {
    // Wait for the session to be initialized
    await new Promise<void>((resolve) => {
      const checkInterval = setInterval(() => {
        if (!authService.isLoading()) {
          clearInterval(checkInterval);
          resolve();
        }
      }, 50);
      
      // Timeout after 3 seconds
      setTimeout(() => {
        clearInterval(checkInterval);
        resolve();
      }, 3000);
    });
  }

  // Now check if user is authenticated
  if (authService.currentUser()) {
    return true;
  }

  // Redirect to login if not authenticated
  return router.createUrlTree(['/login']);
};