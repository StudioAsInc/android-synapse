/**
 * @fileoverview Authentication service for managing user authentication and sessions.
 * Handles sign up, login, logout, and session management with Supabase.
 */

import { Injectable, signal, inject } from '@angular/core';
import { User, Session } from '@supabase/supabase-js';
import { SupabaseService } from './supabase.service';
import { Router } from '@angular/router';

/**
 * Service for managing user authentication and session state.
 * Provides methods for sign up, login, logout, and tracks current user and session.
 * 
 * @injectable
 * @providedIn 'root'
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  /** Supabase client instance */
  private supabase = inject(SupabaseService).client;
  
  /** Angular router for navigation */
  private router = inject(Router);
  
  /** Signal containing the currently authenticated user or null */
  currentUser = signal<User | null>(null);
  
  /** Signal containing the current session or null */
  session = signal<Session | null>(null);
  
  /** Signal indicating if authentication is being initialized */
  isLoading = signal(true);

  constructor() {
    this.initializeAuth();
  }

  /**
   * Initialize authentication by retrieving the current session from Supabase.
   * Sets up auth state change listener for real-time updates.
   * 
   * @private
   * @returns {Promise<void>}
   */
  private async initializeAuth() {
    try {
      // Get current session from Supabase (checks localStorage automatically)
      const { data: { session }, error } = await this.supabase.auth.getSession();
      
      if (error) {
        console.error('Error getting session:', error);
      }
      
      this.setSession(session);
      this.isLoading.set(false);

      // Listen for auth state changes
      this.supabase.auth.onAuthStateChange((_event, session) => {
        this.setSession(session);
      });
    } catch (error) {
      console.error('Error initializing auth:', error);
      this.isLoading.set(false);
    }
  }

  /**
   * Update session and current user signals.
   * 
   * @private
   * @param {Session | null} session - The session object or null if not authenticated
   */
  private setSession(session: Session | null) {
    this.session.set(session);
    this.currentUser.set(session?.user ?? null);
  }

  /**
   * Register a new user with email and password.
   * Automatically creates a public user profile after successful sign up.
   * 
   * @param {string} email - User's email address
   * @param {string} pass - User's password
   * @returns {Promise<any>} Sign up response data
   * @throws {Error} If sign up fails
   */
  async signUp(email: string, pass: string) {
    const { data, error } = await this.supabase.auth.signUp({
      email,
      password: pass
    });
    
    if (error) throw error;
    
    // Try to create a public user profile
    if (data.user) {
      await this.createPublicUserProfile(data.user);
    }

    return data;
  }

  /**
   * Authenticate user with email and password.
   * 
   * @param {string} email - User's email address
   * @param {string} pass - User's password
   * @returns {Promise<any>} Login response data
   * @throws {Error} If login fails
   */
  async login(email: string, pass: string) {
    const { data, error } = await this.supabase.auth.signInWithPassword({
      email,
      password: pass
    });
    if (error) throw error;
    return data;
  }

  /**
   * Sign out the current user and redirect to login page.
   * 
   * @returns {Promise<void>}
   * @throws {Error} If logout fails
   */
  async logout() {
    const { error } = await this.supabase.auth.signOut();
    if (error) throw error;
    this.router.navigate(['/login']);
  }

  /**
   * Create or update a public user profile in the users table.
   * Generates a default avatar using DiceBear API.
   * 
   * @private
   * @param {User} user - The authenticated user object
   * @returns {Promise<void>}
   */
  private async createPublicUserProfile(user: User) {
    try {
      const { error } = await this.supabase
        .from('users')
        .upsert({
          uid: user.id,
          email: user.email,
          username: user.email?.split('@')[0],
          display_name: user.email?.split('@')[0],
          avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${user.id}`,
          created_at: new Date().toISOString()
        });
        
      if (error) console.error('Error creating profile:', error);
    } catch (e) {
      console.error('Error creating profile:', e);
    }
  }
}