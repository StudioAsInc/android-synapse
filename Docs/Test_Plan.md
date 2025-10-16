# Test Plan: Firebase to Supabase Migration

This document outlines the test plan for validating the migration from Firebase to Supabase.

## 1. Authentication

*   [ ] User can sign up with a new email and password.
*   [ ] User can sign in with an existing email and password.
*   [ ] User can sign out.
*   [ ] User session is persisted across app restarts.
*   [ ] User data is correctly created in the Supabase `auth.users` table.

## 2. Database

*   [ ] User profile data is correctly created in the Supabase database upon registration.
*   [ ] User profile data is correctly read from the Supabase database upon login.
*   [ ] User can update their profile data, and the changes are reflected in the Supabase database.
*   [ ] User can delete their account, and the data is removed from the Supabase database.

## 3. Storage

*   [ ] User can upload a profile picture, and it is stored in Supabase Storage.
*   [ ] User can view their profile picture, and it is downloaded from Supabase Storage.
*   [ ] User can delete their profile picture, and it is removed from Supabase Storage.

## 4. Regression Testing

*   [ ] All existing functionality that was not part of the migration should be tested to ensure it is still working as expected.
*   [ ] The application should be tested on a variety of devices and Android versions to ensure compatibility.
