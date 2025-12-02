
/**
 * @fileoverview Application routing configuration.
 * Defines all routes for the Synapse application including public and protected routes.
 */

import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home.component';
import { DownloadsComponent } from './pages/downloads.component';
import { ChangelogComponent } from './pages/changelog.component';
import { AuthComponent } from './pages/auth.component';
import { AdminComponent } from './pages/admin.component';
import { DocsComponent } from './pages/docs.component';
import { PricingComponent } from './pages/pricing.component';
import { SupportComponent } from './pages/support.component';
import { RoadmapComponent } from './pages/roadmap.component';
import { AppLayoutComponent } from './pages/app-layout.component';
import { LandingLayoutComponent } from './layouts/landing-layout.component';
import { FeedComponent } from './pages/feed.component';
import { MessagesComponent } from './pages/messages.component';
import { ProfileComponent } from './pages/profile.component';
import { EditProfileComponent } from './pages/edit-profile.component';
import { SettingsComponent } from './pages/settings.component';
import { ComposeComponent } from './pages/compose.component';
import { PostDetailComponent } from './pages/post-detail.component';
import { FollowersComponent } from './pages/followers.component';
import { AboutComponent } from './pages/about.component';
import { authGuard } from './guards/auth.guard';
import { BookmarksComponent } from './pages/bookmarks.component';
import { ExploreComponent } from './pages/explore.component';
import { NotificationsComponent } from './pages/notifications.component';
import { StoryArchiveComponent } from './components/story-archive.component';
import { StoryComposeComponent } from './pages/story-compose.component';
import { SettingsApiKeysComponent } from './pages/settings-api-keys.component';

/**
 * Application routes configuration.
 * 
 * Route Structure:
 * - Public routes: Landing page, login, docs, pricing, etc.
 * - Protected routes: Feed, messages, profile, etc. (require authentication)
 * - Standalone routes: Compose, settings, messages (no layout wrapper)
 * 
 * @type {Routes}
 */
export const routes: Routes = [
  // Settings Page (Standalone, No Layout)
  {
    path: 'app/settings',
    component: SettingsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'app/settings/api-keys',
    component: SettingsApiKeysComponent,
    canActivate: [authGuard]
  },

  // Compose Page (Standalone, No Layout)
  {
    path: 'app/compose',
    component: ComposeComponent,
    canActivate: [authGuard]
  },

  // Story Compose Page (Standalone, No Layout)
  {
    path: 'app/story/compose',
    component: StoryComposeComponent,
    canActivate: [authGuard]
  },

  // Messages Page (Standalone, No Layout)
  {
    path: 'app/messages',
    component: MessagesComponent,
    canActivate: [authGuard]
  },

  // Web App Routes (Protected, No Landing Navbar/Footer)
  {
    path: 'app',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'feed', pathMatch: 'full' },
      { path: 'feed', component: FeedComponent },
      { path: 'profile', component: ProfileComponent },
      { path: 'profile/:username', component: ProfileComponent },
      { path: 'profile/:username/:type', component: FollowersComponent },
      { path: 'edit-profile', component: EditProfileComponent },
      { path: 'post/:id', component: PostDetailComponent },
      { path: 'bookmarks', component: BookmarksComponent },
      { path: 'explore', component: ExploreComponent },
      { path: 'notifications', component: NotificationsComponent },
      { path: 'archive', component: StoryArchiveComponent }
    ]
  },

  // Landing Page Routes (Public, With Navbar/Footer)
  {
    path: '',
    component: LandingLayoutComponent,
    children: [
      { path: '', component: HomeComponent },
      { path: 'downloads', component: DownloadsComponent },
      { path: 'docs', component: DocsComponent },
      { path: 'docs/:topic', component: DocsComponent },
      { path: 'pricing', component: PricingComponent },
      { path: 'support', component: SupportComponent },
      { path: 'roadmap', component: RoadmapComponent },
      { path: 'changelog', component: ChangelogComponent },
      { path: 'about', component: AboutComponent },
      { path: 'login', component: AuthComponent },
      { path: 'admin', component: AdminComponent, canActivate: [authGuard] },
    ]
  },

  // Fallback: Redirect unknown routes to home
  { path: '**', redirectTo: '' }
];
