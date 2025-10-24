---
inclusion: always
---

# Synapse Android Development Guidelines

## Project Overview
Synapse is an open-source social media platform built with Kotlin for Android, using Supabase as the backend. The app focuses on privacy, real-time communication, and a lightweight user experience.

## Code Style & Architecture

### Kotlin Conventions
- Use Kotlin idioms and modern language features
- Prefer data classes for model objects
- Use coroutines for asynchronous operations
- Follow Android KTX extensions where applicable
- Use sealed classes for state management

### Architecture Patterns
- Follow MVVM architecture with ViewModels and LiveData/StateFlow
- Use Repository pattern for data layer abstraction
- Implement dependency injection where appropriate
- Separate UI logic from business logic

### Android Development Standards
- Target SDK 32, Min SDK 26, Compile SDK 36
- Use ViewBinding for UI components
- Follow Material Design guidelines
- Implement proper lifecycle management
- Use Navigation Component for app navigation

## Backend Integration

### Supabase Configuration
- All Supabase credentials are managed through BuildConfig
- Use environment variables for sensitive configuration
- Implement proper error handling for network operations
- Use Kotlin serialization for JSON parsing

### Authentication
- Leverage Supabase GoTrue for user authentication
- Handle different auth states (debug vs release)
- Implement proper session management
- Follow security best practices for token handling

## Development Practices

### Build Configuration
- Use Gradle Kotlin DSL where possible
- Maintain separate debug and release configurations
- Keep dependencies up to date
- Use ProGuard rules for release builds

### File Organization
- Follow standard Android project structure
- Group related functionality in packages
- Use meaningful naming conventions
- Keep resources organized and named consistently

### Communication Guidelines
- Keep responses concise and actionable
- **NEVER create .md files without explicit user permission**
- Focus on code implementation over lengthy explanations
- Provide specific, technical guidance when needed
- Avoid creating unnecessary documentation files

## Dependencies & Libraries
- Supabase BOM for backend services
- AndroidX libraries for modern Android development
- Glide for image loading
- Material Components for UI
- Kotlin Coroutines for async operations
- Markwon for markdown rendering
- Media3 for media playback

## Security & Privacy
- Implement end-to-end encryption for sensitive data
- Follow Android security best practices
- Properly handle user permissions
- Secure storage for sensitive information
- Regular security audits and updates