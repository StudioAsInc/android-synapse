# Supabase BaaS Migration Plan

## 1. Introduction

This document outlines the plan for migrating the project's Backend-as-a-Service (BaaS) from its current provider to Supabase. The goal of this migration is to leverage Supabase's features, improve performance, and streamline the backend architecture.

## 2. Existing Backend Architecture Analysis

The current backend is a mix of Firebase and other services. The following components are in use:

*   **Authentication**: Firebase Authentication
*   **Database**: Firebase Realtime Database
*   **Storage**: A custom solution using `imgbb.com`
*   **Push Notifications**: OneSignal

## 3. Challenges and Dependencies

The migration process will present several challenges and dependencies that need to be addressed:

*   **Data Transfer**: Transferring data from Firebase Realtime Database to Supabase's PostgreSQL database will require a custom script.
*   **Authentication**: Migrating users from Firebase Authentication to Supabase GoTrue will require a custom solution to avoid forcing users to reset their passwords.
*   **Storage**: The existing storage solution is not integrated with the BaaS. Migrating to Supabase Storage will require a significant amount of work.
*   **Push Notifications**: The existing push notification solution is not integrated with the BaaS. Migrating to Supabase's push notification solution will require a significant amount of work.

## 4. Migration Process

The migration process will be divided into the following phases:

1.  **Phase 1: Setup and Configuration**:
    *   Set up a new Supabase project.
    *   Configure the Supabase client in the Android app.
    *   Create the database schema in Supabase.

2.  **Phase 2: Data Transfer**:
    *   Develop a script to transfer data from Firebase Realtime Database to Supabase.
    *   Run the script to transfer the data.
    *   Verify the data transfer.

3.  **Phase 3: Authentication**:
    *   Implement a custom solution to migrate users from Firebase Authentication to Supabase GoTrue.
    *   Test the user migration.

4.  **Phase 4: Testing**:
    *   Thoroughly test the migrated backend.
    *   Fix any bugs or issues.

## 5. Implementation Steps

The implementation steps for the migration are as follows:

*   **Configuration**:
    *   Add the Supabase client library to the Android app.
    *   Initialize the Supabase client in the `SynapseApp` class.
    *   Create a new `DatabaseService` to interact with the Supabase database.

*   **Data Transfer**:
    *   Write a script to export data from Firebase Realtime Database to a JSON file.
    *   Write a script to import the JSON file into Supabase.

*   **Authentication**:
    *   Implement a custom authentication solution that uses Supabase GoTrue.
    *   Implement a custom solution to migrate users from Firebase Authentication to Supabase GoTrue.

*   **Testing**:
    *   Write unit tests for the `DatabaseService`.
    *   Write integration tests for the migrated backend.
    *   Perform manual testing to ensure that the app works as expected.
