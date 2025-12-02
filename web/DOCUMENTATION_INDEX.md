# Synapse Documentation Index

Complete index of all documentation for the Synapse project.

## 📚 Main Documentation Files

### 1. README.md
**Purpose**: Project overview and quick start guide  
**Audience**: Everyone (especially new users)  
**Contents**:
- Project description and features
- Tech stack overview
- Quick start instructions
- Project structure
- Application routes
- Authentication & security overview
- Database schema overview
- PWA capabilities
- Contributing guidelines
- Deployment instructions

**When to Read**: First time learning about the project

---

### 2. DEVELOPER_GUIDE.md
**Purpose**: Comprehensive guide for developers  
**Audience**: Developers working on the codebase  
**Contents**:
- Project overview and architecture
- High-level system design
- Getting started (installation, setup)
- Detailed project structure
- Core services documentation with examples
- Component documentation
- Authentication & security details
- Complete database schema
- Development workflow
- Testing guidelines
- Deployment procedures
- Troubleshooting guide

**When to Read**: Before starting development work

---

### 3. DOCUMENTATION_SUMMARY.md
**Purpose**: Overview of documentation coverage  
**Audience**: Developers and maintainers  
**Contents**:
- Documentation coverage statistics
- Documentation standards used
- Key services overview
- Component documentation
- Route guards documentation
- Database schema overview
- Best practices for new code
- Documentation maintenance guidelines

**When to Read**: To understand what's documented and standards

---

### 4. DOCUMENTATION_INDEX.md (This File)
**Purpose**: Navigation guide for all documentation  
**Audience**: Everyone  
**Contents**:
- Index of all documentation files
- Quick reference guide
- File-by-file documentation overview

**When to Read**: To find specific documentation

---

## 🔍 Quick Reference Guide

### I need to...

#### Understand the Project
→ Read: **README.md** → **DEVELOPER_GUIDE.md** (Architecture section)

#### Set Up Development Environment
→ Read: **DEVELOPER_GUIDE.md** (Getting Started section)

#### Create a New Feature
→ Read: **DEVELOPER_GUIDE.md** (Development Workflow section)

#### Understand Authentication
→ Read: **DEVELOPER_GUIDE.md** (Authentication & Security section)

#### Work with the Database
→ Read: **DEVELOPER_GUIDE.md** (Database Schema section)

#### Deploy the Application
→ Read: **DEVELOPER_GUIDE.md** (Deployment section)

#### Fix a Bug
→ Read: **DEVELOPER_GUIDE.md** (Troubleshooting section)

#### Understand a Service
→ Read: JSDoc comments in `src/services/[service].ts`

#### Understand a Component
→ Read: JSDoc comments in `src/components/[component].ts`

#### Learn About Routes
→ Read: JSDoc comments in `src/app.routes.ts`

---

## 📁 Source Code Documentation

### Core Application Files

#### src/main.ts
- **Purpose**: Application entry point
- **Documentation**: File-level JSDoc with bootstrap explanation
- **Key Content**: Angular bootstrap configuration

#### src/app.component.ts
- **Purpose**: Root component
- **Documentation**: Class and method JSDoc
- **Key Methods**: `installPwa()`, `updatePwa()`, `dismissInstall()`

#### src/app.routes.ts
- **Purpose**: Route configuration
- **Documentation**: Route structure documentation
- **Key Content**: Public routes, protected routes, standalone routes

---

### Services (src/services/)

#### Authentication
- **auth.service.ts** - User authentication and session management
  - Methods: `signUp()`, `login()`, `logout()`
  - Signals: `currentUser`, `session`, `isLoading`

- **supabase.service.ts** - Supabase client initialization
  - Getter: `client` - Returns Supabase client instance

#### Content Management
- **post.service.ts** - Post CRUD operations
  - Methods: `fetchPosts()`, `createPost()`, `editPost()`, `deletePost()`, `bookmarkPost()`
  - Features: Hashtag extraction, media upload

- **comment.service.ts** - Comment management
- **story.service.ts** - Story creation and management
- **profile.service.ts** - User profile operations

#### Real-time & Messaging
- **messaging.service.ts** - Direct messaging
- **realtime.service.ts** - WebSocket subscriptions
- **notification.service.ts** - Notification management

#### Features
- **pwa.service.ts** - Progressive Web App features
  - Methods: `install()`, `update()`, `requestNotificationPermission()`
  - Signals: `isInstallable`, `isInstalled`, `isOnline`, `updateAvailable`

- **theme.service.ts** - Light/dark mode management
  - Methods: `toggle()`, `toggleImmediate()`
  - Signals: `darkMode`

- **search.service.ts** - Search functionality
- **social.service.ts** - Social features (follow, like, etc.)

#### Utilities
- **image-upload.service.ts** - File upload handling
- **error-handling.service.ts** - Error management
- **performance.service.ts** - Performance monitoring
- **text-parser.service.ts** - Text parsing and formatting
- **mention.service.ts** - Mention handling
- **hashtag.service.ts** - Hashtag management

---

### Guards (src/guards/)

#### auth.guard.ts
- **Purpose**: Route protection
- **Behavior**: Ensures user is authenticated before accessing protected routes
- **Usage**: `canActivate: [authGuard]`

---

### Components (src/components/)

#### Icon Component
- **icon.component.ts** - Lucide icon wrapper
  - Inputs: `name`, `size`, `class`, `strokeWidth`
  - Supports: 100+ Lucide icons

#### Page Components (src/pages/)
- **feed.component.ts** - Main social feed
- **profile.component.ts** - User profile page
- **messages.component.ts** - Direct messaging
- **compose.component.ts** - Post creation
- **explore.component.ts** - Discovery page
- **bookmarks.component.ts** - Saved posts
- **notifications.component.ts** - Activity feed
- **settings.component.ts** - User settings
- **auth.component.ts** - Login/signup

#### Reusable Components (src/components/)
- **post-card.component.ts** - Post display
- **story-viewer.component.ts** - Story viewing
- **comment-section.component.ts** - Comments
- **navbar.component.ts** - Navigation bar
- **footer.component.ts** - Footer

---

## 🗄️ Database Documentation

### Core Tables

#### users
- User profiles and metadata
- Relationships: One-to-many with posts, messages, stories

#### posts
- User-generated content
- Relationships: Many-to-one with users, one-to-many with comments

#### comments
- Post comments and discussions
- Relationships: Many-to-one with posts and users

#### messages
- Direct messaging
- Relationships: Many-to-one with users (sender and recipient)

#### stories
- Temporary 24-hour content
- Relationships: Many-to-one with users

#### notifications
- Activity feed events
- Relationships: Many-to-one with users

#### hashtags
- Hashtag tracking
- Relationships: Many-to-many with posts

#### favorites
- Bookmarked posts
- Relationships: Many-to-one with users and posts

#### follows
- User relationships
- Relationships: Many-to-one with users

---

## 🔐 Security Documentation

### Authentication Flow
1. User signs up/logs in
2. Supabase validates credentials
3. JWT token returned and stored
4. Session restored on page reload
5. Auth guard protects routes

### Row-Level Security (RLS)
- Database-level access control
- Policies defined per table
- Enforced by Supabase

### Best Practices
- Use environment variables for secrets
- Validate input on client and server
- Use HTTPS in production
- Implement rate limiting
- Sanitize user input

---

## 🚀 Development Workflow

### Creating a Feature
1. Create service in `src/services/`
2. Create component in `src/pages/` or `src/components/`
3. Add route in `src/app.routes.ts`
4. Add documentation (JSDoc comments)
5. Test functionality
6. Commit with conventional message

### Code Style
- Use camelCase for variables/methods
- Use PascalCase for classes
- Add JSDoc comments to public methods
- Use TypeScript types (avoid `any`)
- Use Angular signals for reactive state

### Git Workflow
```bash
git checkout -b feature/my-feature
# Make changes
git commit -m "feat: add my feature"
git push origin feature/my-feature
# Create pull request
```

---

## 📊 Documentation Statistics

| Category | Count |
|----------|-------|
| TypeScript Files | 121 |
| Services | 36+ |
| Components | 30+ |
| Pages | 20+ |
| Routes | 20+ |
| Database Tables | 9 |
| Documentation Files | 4 |

---

## 🔗 External Resources

### Official Documentation
- [Angular Documentation](https://angular.dev)
- [Supabase Documentation](https://supabase.com/docs)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [PWA Documentation](https://web.dev/progressive-web-apps/)

### Tools & Libraries
- [Lucide Icons](https://lucide.dev)
- [RxJS Documentation](https://rxjs.dev)
- [Vite Documentation](https://vitejs.dev)

---

## 📝 Documentation Conventions

### JSDoc Format
```typescript
/**
 * Brief description of the function.
 * 
 * Longer description if needed.
 * 
 * @param {Type} paramName - Description of parameter
 * @returns {ReturnType} Description of return value
 * @throws {ErrorType} Description of error conditions
 * @example
 * // Usage example
 * const result = myFunction(param);
 */
```

### File Header
```typescript
/**
 * @fileoverview Brief description of file purpose.
 * Additional context about the module.
 */
```

### Class Documentation
```typescript
/**
 * Description of the class.
 * 
 * @injectable (for services)
 * @providedIn 'root' (for services)
 * @component (for components)
 * @selector app-name (for components)
 */
```

---

## 🆘 Getting Help

### Documentation Issues
- Check the relevant documentation file
- Search for keywords in JSDoc comments
- Review examples in DEVELOPER_GUIDE.md

### Code Questions
- Read JSDoc comments in source files
- Check DEVELOPER_GUIDE.md for patterns
- Review similar implementations

### Bug Reports
- Open an issue on GitHub
- Include error message and steps to reproduce
- Reference relevant documentation

### Feature Requests
- Open a discussion on GitHub
- Describe the feature and use case
- Reference related documentation

---

## 📅 Documentation Maintenance

### Update Schedule
- Update documentation when code changes
- Review documentation during code reviews
- Update examples when APIs change
- Add new sections for major features

### Version History
- **v1.0.0** (November 2024) - Initial comprehensive documentation

---

## 🎯 Next Steps

1. **Read README.md** - Get project overview
2. **Read DEVELOPER_GUIDE.md** - Learn development setup
3. **Explore source code** - Read JSDoc comments
4. **Start developing** - Follow development workflow
5. **Contribute** - Submit pull requests with documentation

---

**Last Updated**: November 2024  
**Documentation Version**: 1.0.0  
**Maintained By**: Synapse Development Team
