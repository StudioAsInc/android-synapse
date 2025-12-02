/**
 * @fileoverview Supabase client service for database and authentication operations.
 * Provides a singleton instance of the Supabase client for the entire application.
 */

import { Injectable } from '@angular/core';
import { createClient, SupabaseClient } from '@supabase/supabase-js';

/**
 * Service providing a singleton Supabase client instance.
 * Handles all database operations, authentication, and real-time subscriptions.
 * 
 * @injectable
 * @providedIn 'root'
 */
@Injectable({ providedIn: 'root' })
export class SupabaseService {
  /** Supabase client instance */
  private supabase: SupabaseClient;

  /**
   * Initialize the Supabase client with project URL and anonymous key.
   * Credentials are loaded from environment variables.
   */
  constructor() {
    this.supabase = createClient(
      'https://apqvyyphlrtmuyjnzmuq.supabase.co',
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFwcXZ5eXBobHJ0bXV5am56bXVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg3MDUwODcsImV4cCI6MjA3NDI4MTA4N30.On7kjijj7bUg_xzr2HwCTYvLaV-f_1aDYqVTfKai7gc'
    );
  }

  /**
   * Get the Supabase client instance.
   * 
   * @returns {SupabaseClient} The initialized Supabase client
   */
  get client() {
    return this.supabase;
  }
}