/**
 * @fileoverview Main entry point for the Synapse Angular application.
 * Bootstraps the root AppComponent with routing configuration.
 */

import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app.component';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';

/**
 * Bootstrap the Angular application with the root component and routing providers.
 * Initializes the application with all configured routes.
 */
bootstrapApplication(AppComponent, {
  providers: [provideRouter(routes)]
});
