# Synapse Developer Guide

Complete documentation for developers working on the Synapse social media platform.

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Getting Started](#getting-started)
4. [Project Structure](#project-structure)
5. [Core Services](#core-services)
6. [Components](#components)
7. [Authentication & Security](#authentication--security)
8. [Database Schema](#database-schema)
9. [Development Workflow](#development-workflow)
10. [Testing](#testing)
11. [Deployment](#deployment)
12. [Troubleshooting](#troubleshooting)

## Project Overview

**Synapse** is a fast, offline-first, open-source social media platform built with Angular 21 and Supabase. It provides real-time social features with Progressive Web App (PWA) capabilities for seamless offline functionality.

### Key Features

- **Offline-First**: Works without internet connection using service workers
- **Real-time Updates**: Live feed, messaging, and notifications via WebSockets
- **PWA Support**: Installable on any device as a native app
- **Social Features**: Posts, stories, direct messaging, bookmarks, notifications
- **Security**: Row-level security (RLS) with Supabase authentication
- **Performance**: Optimized bundle size, lazy loading, and caching strategies

### Technology Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | Angular 21, TypeScript 5.9, Tailwind CSS, RxJS |
| **Backend** | Supabase (PostgreSQL + Auth + Realtime) |
| **Storage** | Cloudinary / Cloudflare R2 |
| **Build** | Vite, Angular CLI |
| **Testing** | Jasmine, Karma, Fast-check |

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Angular Application                   │
│  ┌──────────────────────────────────────────────────┐   │
│  │              Components & Pages                   │   │
│  │  (Feed, Messages, Profile, Stories, etc.)        │   │
│  └──────────────────────────────────────────────────┘   │
│                         ↓                                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │              Services Layer                       │   │
│  │  (Auth, Post, Profile, Messaging, etc.)          │   │
│  └──────────────────────────────────────────────────┘   │
│                         ↓                                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │         Supabase Client (SDK)                    │   │
│  │  (Database, Auth, Realtime, Storage)             │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│              Supabase Backend                            │
│  ┌──────────────────────────────────────────────────┐   │
│  │  PostgreSQL Database with RLS Policies           │   │
│  │  Authentication (JWT-based)                      │   │
│  │  Realtime Subscriptions (WebSockets)             │   │
│  │  Storage (File uploads)                          │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### Data Flow

1. **User Interaction** → Component
2. **Component** → Service (business logic)
3. **Service** → Supabase Client (API calls)
4. **Supabase** → PostgreSQL Database
5. **Response** → Service → Component → UI Update

## Getting Started

### Prerequisites

- **Node.js** 18+ and npm
- **Git** for version control
- **Supabase** account (free tier available at supabase.com)
- **Code Editor** (VS Code recommended)

### Installation Steps

#### 1. Clone the Repository

```bash
git clone https://github.com/SynapseOSS/Web.git
cd Web
```

#### 2. Install Dependencies

```bash
npm install
```

#### 3. Set Up Environment Variables

```bash
cp .env.example .env.local
```

Edit `.env.local` with your Supabase credentials:

```env
# Required: Supabase Configuration
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your_anon_key_here

# Optional: Firebase Analytics
VITE_FIREBASE_API_KEY=your_api_key
VITE_FIREBASE_PROJECT_ID=your_project_id

# Optional: AI Features
VITE_GEMINI_API_KEY=your_gemini_key
```

#### 4. Set Up Supabase Database

```bash
# Install Supabase CLI
npm install -g supabase

# Link to your project
supabase link --project-ref your-project-ref

# Run migrations
supabase db push
```

#### 5. Start Development Server

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Verify Installation

- [ ] Application loads without errors
- [ ] Can navigate to login page
- [ ] Can create a new account
- [ ] Can log in with created account
- [ ] Feed page loads with sample data

## Project Structure

```
synapse/
├── src/
│   ├── app.component.ts              # Root component
│   ├── app.routes.ts                 # Route configuration
│   ├── main.ts                       # Application entry point
│   │
│   ├── components/                   # Reusable UI components
│   │   ├── navbar.component.ts       # Navigation bar
│   │   ├── post-card.component.ts    # Post display component
│   │   ├── story-viewer.component.ts # Story viewing component
│   │   ├── comment-section.component.ts
│   │   ├── icon.component.ts         # Icon wrapper (Lucide)
│   │   └── ui/                       # UI primitives
│   │       ├── button.component.ts
│   │       ├── card.component.ts
│   │       ├── alert.component.ts
│   │       └── ...
│   │
│   ├── pages/                        # Page components (routes)
│   │   ├── feed.component.ts         # Main feed page
│   │   ├── messages.component.ts     # Direct messaging
│   │   ├── profile.component.ts      # User profile
│   │   ├── compose.component.ts      # Post creation
│   │   ├── explore.component.ts      # Discovery page
│   │   ├── bookmarks.component.ts    # Saved posts
│   │   ├── notifications.component.ts
│   │   ├── settings.component.ts
│   │   ├── auth.component.ts         # Login/signup
│   │   └── ...
│   │
│   ├── services/                     # Business logic services
│   │   ├── auth.service.ts           # Authentication
│   │   ├── supabase.service.ts       # Supabase client
│   │   ├── post.service.ts           # Post operations
│   │   ├── profile.service.ts        # User profiles
│   │   ├── messaging.service.ts      # Direct messages
│   │   ├── notification.service.ts   # Notifications
│   │   ├── story.service.ts          # Stories
│   │   ├── pwa.service.ts            # PWA features
│   │   ├── theme.service.ts          # Theme management
│   │   ├── realtime.service.ts       # WebSocket subscriptions
│   │   ├── search.service.ts         # Search functionality
│   │   ├── image-upload.service.ts   # File uploads
│   │   ├── error-handling.service.ts # Error management
│   │   ├── performance.service.ts    # Performance monitoring
│   │   └── ...
│   │
│   ├── directives/                   # Custom Angular directives
│   │   ├── animate-on-scroll.directive.ts
│   │   └── keyboard-nav.directive.ts
│   │
│   ├── guards/                       # Route guards
│   │   └── auth.guard.ts             # Authentication guard
│   │
│   ├── layouts/                      # Layout components
│   │   ├── landing-layout.component.ts
│   │   └── app-layout.component.ts
│   │
│   └── firebase.config.ts            # Firebase configuration
│
├── public/
│   ├── icons/                        # PWA icons
│   ├── manifest.json                 # PWA manifest
│   ├── sw.js                         # Service worker
│   ├── offline.html                  # Offline fallback
│   └── robots.txt                    # SEO
│
├── supabase/
│   └── migrations/                   # Database migrations
│
├── angular.json                      # Angular CLI config
├── tsconfig.json                     # TypeScript config
├── vite.config.ts                    # Vite config
├── package.json                      # Dependencies
├── .env.example                      # Environment template
└── README.md                         # Project README
```

## Core Services

### AuthService

Manages user authentication and session state.

**Key Methods:**
- `signUp(email, password)` - Register new user
- `login(email, password)` - Authenticate user
- `logout()` - Sign out user
- `currentUser` - Signal with current user
- `session` - Signal with current session
- `isLoading` - Signal indicating auth initialization

**Example Usage:**

```typescript
import { AuthService } from './services/auth.service';

export class MyComponent {
  authService = inject(AuthService);

  async handleLogin(email: string, password: string) {
    try {
      await this.authService.login(email, password);
      console.log('Logged in:', this.authService.currentUser());
    } catch (error) {
      console.error('Login failed:', error);
    }
  }
}
```

### PostService

Handles post creation, fetching, editing, and deletion.

**Key Methods:**
- `fetchPosts()` - Get all posts
- `createPost(text, mediaFiles?)` - Create new post
- `editPost(postId, newText)` - Edit post
- `deletePost(postId)` - Delete post
- `bookmarkPost(postId)` - Save post
- `unbookmarkPost(postId)` - Remove bookmark

**Example Usage:**

```typescript
export class FeedComponent {
  postService = inject(PostService);

  ngOnInit() {
    this.postService.fetchPosts();
  }

  get posts() {
    return this.postService.posts();
  }

  async createPost(text: string) {
    await this.postService.createPost(text);
  }
}
```

### ProfileService

Manages user profile data and operations.

**Key Methods:**
- `getProfile(userId)` - Fetch user profile
- `updateProfile(data)` - Update profile
- `followUser(userId)` - Follow user
- `unfollowUser(userId)` - Unfollow user
- `getFollowers(userId)` - Get followers list
- `getFollowing(userId)` - Get following list

### MessagingService

Handles direct messaging and conversations.

**Key Methods:**
- `getConversations()` - Fetch all conversations
- `getMessages(conversationId)` - Get messages in conversation
- `sendMessage(conversationId, text)` - Send message
- `deleteMessage(messageId)` - Delete message

### RealtimeService

Manages WebSocket subscriptions for real-time updates.

**Key Methods:**
- `subscribeToFeed()` - Subscribe to feed updates
- `subscribeToMessages()` - Subscribe to new messages
- `subscribeToNotifications()` - Subscribe to notifications
- `unsubscribe(channel)` - Unsubscribe from channel

### PwaService

Manages Progressive Web App features.

**Key Methods:**
- `install()` - Trigger install prompt
- `update()` - Check for updates
- `requestNotificationPermission()` - Request notification access
- `subscribeToPushNotifications()` - Subscribe to push notifications

**Signals:**
- `isInstallable` - Can app be installed
- `isInstalled` - Is app installed
- `isOnline` - Current online status
- `updateAvailable` - Is update available

### ThemeService

Manages light/dark mode theme.

**Key Methods:**
- `toggle(event?)` - Toggle theme with transition
- `toggleImmediate()` - Toggle theme without animation

**Signals:**
- `darkMode` - Current theme mode

## Components

### Page Components

#### FeedComponent (`pages/feed.component.ts`)

Main social feed displaying posts from followed users.

**Features:**
- Infinite scroll loading
- Real-time post updates
- Like/comment interactions
- Post creation

#### ProfileComponent (`pages/profile.component.ts`)

User profile page showing user information and posts.

**Features:**
- User bio and avatar
- Post history
- Follower/following lists
- Edit profile (own profile only)

#### MessagesComponent (`pages/messages.component.ts`)

Direct messaging interface.

**Features:**
- Conversation list
- Real-time message updates
- Message search
- Typing indicators

#### ComposeComponent (`pages/compose.component.ts`)

Post creation interface.

**Features:**
- Text input with formatting
- Media upload
- Hashtag/mention support
- Preview before posting

#### ExploreComponent (`pages/explore.component.ts`)

Discovery page for finding content and users.

**Features:**
- Trending posts
- Suggested users
- Hashtag search
- Content recommendations

### Reusable Components

#### PostCardComponent (`components/post-card.component.ts`)

Displays a single post with interactions.

**Inputs:**
- `post: Post` - Post data
- `showAuthor: boolean` - Show author info

**Outputs:**
- `like: EventEmitter<string>` - Like event
- `comment: EventEmitter<string>` - Comment event
- `share: EventEmitter<string>` - Share event

#### StoryViewerComponent (`components/story-viewer.component.ts`)

Displays stories with progress indicators.

**Inputs:**
- `stories: Story[]` - Array of stories
- `currentIndex: number` - Current story index

**Outputs:**
- `next: EventEmitter<void>` - Next story
- `previous: EventEmitter<void>` - Previous story
- `close: EventEmitter<void>` - Close viewer

#### IconComponent (`components/icon.component.ts`)

Wrapper for Lucide icons.

**Inputs:**
- `name: string` - Icon name (Lucide icon)
- `size: number` - Icon size in pixels
- `class: string` - CSS classes

**Example:**

```html
<app-icon name="heart" [size]="24" class="text-red-500"></app-icon>
```

## Authentication & Security

### Authentication Flow

1. **Sign Up**
   - User enters email and password
   - Supabase creates auth user
   - Public profile created automatically
   - User receives confirmation email

2. **Login**
   - User enters credentials
   - Supabase validates and returns JWT
   - JWT stored in localStorage
   - Session restored on page reload

3. **Logout**
   - User clicks logout
   - JWT removed from storage
   - Session cleared
   - Redirect to login page

### Route Protection

Protected routes use the `authGuard`:

```typescript
// In app.routes.ts
{
  path: 'app',
  component: AppLayoutComponent,
  canActivate: [authGuard],
  children: [...]
}
```

The guard:
- Waits for auth initialization
- Checks if user is authenticated
- Redirects to login if not authenticated

### Row-Level Security (RLS)

Database tables have RLS policies:

```sql
-- Example: Users can only see their own profile
CREATE POLICY "Users can view own profile"
  ON users FOR SELECT
  USING (auth.uid() = uid);

-- Example: Users can only edit their own posts
CREATE POLICY "Users can edit own posts"
  ON posts FOR UPDATE
  USING (auth.uid() = author_uid);
```

### Best Practices

- ✅ Never commit `.env.local` with real credentials
- ✅ Use environment variables for sensitive data
- ✅ Validate input on both client and server
- ✅ Use HTTPS in production
- ✅ Implement rate limiting for API calls
- ✅ Sanitize user input to prevent XSS
- ✅ Use CSRF tokens for state-changing operations

## Database Schema

### Core Tables

#### users
```sql
CREATE TABLE users (
  uid UUID PRIMARY KEY REFERENCES auth.users(id),
  email VARCHAR(255) UNIQUE,
  username VARCHAR(50) UNIQUE,
  display_name VARCHAR(100),
  bio TEXT,
  avatar VARCHAR(500),
  cover_image VARCHAR(500),
  followers_count INTEGER DEFAULT 0,
  following_count INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
```

#### posts
```sql
CREATE TABLE posts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  author_uid UUID REFERENCES users(uid) ON DELETE CASCADE,
  post_text TEXT,
  post_type VARCHAR(20) DEFAULT 'TEXT',
  media_items JSONB,
  likes_count INTEGER DEFAULT 0,
  comments_count INTEGER DEFAULT 0,
  is_edited BOOLEAN DEFAULT FALSE,
  edited_at TIMESTAMP,
  is_deleted BOOLEAN DEFAULT FALSE,
  deleted_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),
  timestamp BIGINT
);
```

#### comments
```sql
CREATE TABLE comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id UUID REFERENCES posts(id) ON DELETE CASCADE,
  author_uid UUID REFERENCES users(uid) ON DELETE CASCADE,
  comment_text TEXT NOT NULL,
  likes_count INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW()
);
```

#### messages
```sql
CREATE TABLE messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_uid UUID REFERENCES users(uid) ON DELETE CASCADE,
  recipient_uid UUID REFERENCES users(uid) ON DELETE CASCADE,
  message_text TEXT NOT NULL,
  is_read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT NOW()
);
```

#### stories
```sql
CREATE TABLE stories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  author_uid UUID REFERENCES users(uid) ON DELETE CASCADE,
  media_url VARCHAR(500) NOT NULL,
  media_type VARCHAR(20),
  caption TEXT,
  expires_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);
```

#### notifications
```sql
CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_uid UUID REFERENCES users(uid) ON DELETE CASCADE,
  actor_uid UUID REFERENCES users(uid) ON DELETE CASCADE,
  notification_type VARCHAR(50),
  related_post_id UUID REFERENCES posts(id) ON DELETE CASCADE,
  is_read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT NOW()
);
```

## Development Workflow

### Creating a New Feature

#### 1. Create a Service

```typescript
// src/services/my-feature.service.ts
import { Injectable, inject, signal } from '@angular/core';
import { SupabaseService } from './supabase.service';

/**
 * Service for managing my feature.
 * 
 * @injectable
 * @providedIn 'root'
 */
@Injectable({ providedIn: 'root' })
export class MyFeatureService {
  private supabase = inject(SupabaseService).client;
  
  data = signal<any[]>([]);
  loading = signal(false);

  /**
   * Fetch data from database.
   * 
   * @returns {Promise<void>}
   */
  async fetchData() {
    this.loading.set(true);
    try {
      const { data, error } = await this.supabase
        .from('my_table')
        .select('*');
      
      if (error) throw error;
      this.data.set(data);
    } finally {
      this.loading.set(false);
    }
  }
}
```

#### 2. Create a Component

```typescript
// src/pages/my-feature.component.ts
import { Component, inject, OnInit } from '@angular/core';
import { MyFeatureService } from '../services/my-feature.service';

/**
 * My feature page component.
 * 
 * @component
 * @selector app-my-feature
 * @standalone true
 */
@Component({
  selector: 'app-my-feature',
  standalone: true,
  template: `
    <div class="container">
      <h1>My Feature</h1>
      @if (service.loading()) {
        <p>Loading...</p>
      } @else {
        <ul>
          @for (item of service.data(); track item.id) {
            <li>{{ item.name }}</li>
          }
        </ul>
      }
    </div>
  `
})
export class MyFeatureComponent implements OnInit {
  service = inject(MyFeatureService);

  ngOnInit() {
    this.service.fetchData();
  }
}
```

#### 3. Add Route

```typescript
// src/app.routes.ts
export const routes: Routes = [
  {
    path: 'app',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'my-feature',
        component: MyFeatureComponent
      }
    ]
  }
];
```

### Code Style Guidelines

- **Naming**: Use camelCase for variables/methods, PascalCase for classes
- **Formatting**: Use Prettier (configured in project)
- **Comments**: Add JSDoc comments to public methods
- **Types**: Always use TypeScript types, avoid `any`
- **Signals**: Use Angular signals for reactive state
- **Imports**: Organize imports alphabetically

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/my-feature

# Make changes and commit
git add .
git commit -m "feat: add my feature"

# Push to remote
git push origin feature/my-feature

# Create pull request on GitHub
```

### Commit Message Format

Follow conventional commits:

```
feat: add new feature
fix: fix bug in component
docs: update documentation
style: format code
refactor: refactor service
test: add unit tests
chore: update dependencies
```

## Testing

### Running Tests

```bash
# Run all tests
npm test

# Run tests in watch mode
npm run test:watch

# Generate coverage report
npm run test:coverage
```

### Writing Tests

```typescript
// src/services/my-feature.service.spec.ts
import { TestBed } from '@angular/core/testing';
import { MyFeatureService } from './my-feature.service';

describe('MyFeatureService', () => {
  let service: MyFeatureService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MyFeatureService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch data', async () => {
    await service.fetchData();
    expect(service.data().length).toBeGreaterThan(0);
  });
});
```

## Deployment

### Build for Production

```bash
npm run build
```

Output is in `dist/` directory.

### Deploy to Vercel

```bash
npm i -g vercel
vercel
```

### Deploy to Netlify

```bash
npm run build
netlify deploy --prod --dir=dist
```

### Environment Variables

Set these in your deployment platform:
- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_ANON_KEY`
- `VITE_FIREBASE_API_KEY` (optional)
- `VITE_FIREBASE_PROJECT_ID` (optional)

## Troubleshooting

### Common Issues

#### "Cannot find module" Error

**Solution**: Run `npm install` to ensure all dependencies are installed.

#### Authentication Not Working

**Solution**: 
- Check Supabase credentials in `.env.local`
- Verify Supabase project is active
- Check browser console for errors

#### Service Worker Not Caching

**Solution**:
- Clear browser cache
- Unregister old service workers: `navigator.serviceWorker.getRegistrations()`
- Rebuild application

#### Realtime Subscriptions Not Working

**Solution**:
- Check WebSocket connection in browser DevTools
- Verify Supabase Realtime is enabled
- Check RLS policies allow read access

#### Build Fails

**Solution**:
- Clear `node_modules` and `dist` directories
- Run `npm install` again
- Check for TypeScript errors: `npx tsc --noEmit`

### Performance Optimization

- Use `OnPush` change detection strategy
- Implement virtual scrolling for long lists
- Lazy load routes and components
- Optimize images with Cloudinary transformations
- Use service worker caching effectively

### Debugging

**Browser DevTools:**
- Open DevTools (F12)
- Check Console for errors
- Use Network tab to monitor API calls
- Use Application tab to inspect service worker

**Angular DevTools:**
- Install Angular DevTools extension
- Inspect component tree
- Monitor change detection
- Profile performance

## Additional Resources

- [Angular Documentation](https://angular.dev)
- [Supabase Documentation](https://supabase.com/docs)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [PWA Documentation](https://web.dev/progressive-web-apps/)

## Support

- 📖 **Documentation**: [synapse.social/docs](https://synapse.social/docs)
- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/SynapseOSS/Web/issues)
- 💡 **Feature Requests**: [GitHub Discussions](https://github.com/SynapseOSS/Web/discussions)
- 📧 **Email**: support@synapse.social

---

**Last Updated**: November 2024
**Version**: 1.0.0
