import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { IconComponent } from '../components/icon.component';
import { PostCardComponent } from '../components/post-card.component';
import { PostShimmerComponent } from '../components/ui/shimmer.component';
import { SocialService, Post, User } from '../services/social.service';
import { SearchService, SearchFilter } from '../services/search.service';
import { HashtagService } from '../services/hashtag.service';

interface TrendingHashtag {
  tag: string;
  count: number;
  growth: number;
}

interface TrendingTopic {
  id: string;
  title: string;
  category: string;
  posts_count: number;
  icon: string;
}

@Component({
  selector: 'app-explore',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, IconComponent, PostCardComponent, PostShimmerComponent],
  template: `
    <div class="min-h-screen bg-white dark:bg-slate-950 border-x border-slate-200 dark:border-white/10">
      <!-- Header with Search -->
      <div class="sticky top-0 z-20 backdrop-blur-md bg-white/80 dark:bg-slate-950/80 border-b border-slate-200 dark:border-white/10">
        <div class="px-4 py-3">
          <h1 class="text-xl font-bold text-slate-900 dark:text-white mb-3">Explore</h1>
          
          <!-- Search Bar -->
          <div class="relative">
            <app-icon name="search" [size]="20" class="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"></app-icon>
            <input 
              [(ngModel)]="searchQuery"
              (input)="onSearch()"
              type="text" 
              placeholder="Search posts, people, photos, videos..."
              class="w-full pl-12 pr-4 py-3 bg-slate-100 dark:bg-slate-900 border border-slate-200 dark:border-white/10 rounded-full focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all">
            @if (searchQuery) {
              <button 
                (click)="clearSearch()"
                class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300">
                <app-icon name="x" [size]="20"></app-icon>
              </button>
            }
          </div>

          <!-- Search Filters -->
          @if (searchQuery) {
            <div class="flex gap-2 mt-3 overflow-x-auto scrollbar-hide">
              @for (filter of searchFilters; track filter.id) {
                <button
                  (click)="setSearchFilter(filter.id)"
                  [class.bg-indigo-600]="currentFilter() === filter.id"
                  [class.text-white]="currentFilter() === filter.id"
                  [class.bg-slate-100]="currentFilter() !== filter.id"
                  [class.dark:bg-slate-800]="currentFilter() !== filter.id"
                  [class.text-slate-600]="currentFilter() !== filter.id"
                  [class.dark:text-slate-400]="currentFilter() !== filter.id"
                  class="px-4 py-2 rounded-full text-sm font-medium transition-all whitespace-nowrap flex items-center gap-2">
                  <app-icon [name]="filter.icon" [size]="16"></app-icon>
                  <span>{{ filter.name }}</span>
                </button>
              }
            </div>
          } @else {
            <!-- Category Tabs -->
            <div class="flex gap-2 mt-3 overflow-x-auto scrollbar-hide">
              @for (category of categories; track category.id) {
                <button
                  (click)="activeCategory.set(category.id)"
                  [class.bg-indigo-600]="activeCategory() === category.id"
                  [class.text-white]="activeCategory() === category.id"
                  [class.bg-slate-100]="activeCategory() !== category.id"
                  [class.dark:bg-slate-800]="activeCategory() !== category.id"
                  [class.text-slate-600]="activeCategory() !== category.id"
                  [class.dark:text-slate-400]="activeCategory() !== category.id"
                  class="px-4 py-2 rounded-full text-sm font-medium transition-all whitespace-nowrap flex items-center gap-2">
                  <app-icon [name]="category.icon" [size]="16"></app-icon>
                  <span>{{ category.name }}</span>
                </button>
              }
            </div>
          }
        </div>
      </div>

      <!-- Content -->
      <div class="pb-20">
        @if (searchQuery) {
          <!-- Search Results -->
          <div class="p-4">
            <h2 class="text-lg font-bold text-slate-900 dark:text-white mb-4">
              {{ getResultsTitle() }}
            </h2>
            
            @if (searchLoading()) {
              <div class="p-4">
                @for (i of [1,2,3]; track i) {
                  <app-post-shimmer></app-post-shimmer>
                }
              </div>
            } @else if (searchService.searchResults().length === 0) {
              <div class="text-center py-10">
                <app-icon name="search" [size]="48" class="mx-auto mb-4 text-slate-300 dark:text-slate-700"></app-icon>
                <p class="text-slate-500">No results found for "{{ searchQuery }}"</p>
              </div>
            } @else {
              <div class="space-y-4">
                @for (result of searchService.searchResults(); track result.data.id || $index) {
                  @switch (result.type) {
                    @case ('user') {
                      <div (click)="navigateToProfile(result.data.username)" class="p-4 hover:bg-slate-50 dark:hover:bg-slate-900 rounded-xl transition-colors cursor-pointer border border-slate-200 dark:border-white/10">
                        <div class="flex items-center gap-3">
                          <img [src]="result.data.avatar" class="w-12 h-12 rounded-full object-cover">
                          <div class="flex-1">
                            <div class="font-bold text-slate-900 dark:text-white flex items-center gap-1">
                              {{ result.data.display_name }}
                              @if (result.data.verify) {
                                <app-icon name="verified" [size]="16" class="text-indigo-500"></app-icon>
                              }
                            </div>
                            <div class="text-sm text-slate-500">@{{ result.data.username }}</div>
                            @if (result.data.bio) {
                              <div class="text-sm text-slate-600 dark:text-slate-400 mt-1">{{ result.data.bio }}</div>
                            }
                          </div>
                          <button (click)="followUser(result.data.uid, $event)" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-full text-sm font-medium">
                            Follow
                          </button>
                        </div>
                      </div>
                    }
                    @case ('post') {
                      <app-post-card [post]="result.data"></app-post-card>
                    }
                    @case ('photo') {
                      <div class="relative group cursor-pointer rounded-xl overflow-hidden border border-slate-200 dark:border-white/10">
                        <img [src]="result.data.media[0]?.url" class="w-full aspect-square object-cover">
                        <div class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                          <div class="text-white text-center">
                            <div class="flex items-center gap-4">
                              <div class="flex items-center gap-1">
                                <app-icon name="heart" [size]="20"></app-icon>
                                <span>{{ result.data.likes_count }}</span>
                              </div>
                              <div class="flex items-center gap-1">
                                <app-icon name="message-circle" [size]="20"></app-icon>
                                <span>{{ result.data.comments_count }}</span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    }
                    @case ('video') {
                      <div class="relative group cursor-pointer rounded-xl overflow-hidden border border-slate-200 dark:border-white/10">
                        <video [src]="result.data.media[0]?.url" class="w-full aspect-video object-cover"></video>
                        <div class="absolute inset-0 bg-black/50 flex items-center justify-center">
                          <app-icon name="play" [size]="48" class="text-white"></app-icon>
                        </div>
                      </div>
                    }
                    @case ('location') {
                      <div class="p-4 hover:bg-slate-50 dark:hover:bg-slate-900 rounded-xl transition-colors cursor-pointer border border-slate-200 dark:border-white/10">
                        <div class="flex items-center gap-3">
                          <div class="w-12 h-12 rounded-full bg-indigo-100 dark:bg-indigo-950 flex items-center justify-center">
                            <app-icon name="map-pin" [size]="24" class="text-indigo-600 dark:text-indigo-400"></app-icon>
                          </div>
                          <div class="flex-1">
                            <div class="font-bold text-slate-900 dark:text-white">{{ result.data.name }}</div>
                            @if (result.data.address) {
                              <div class="text-sm text-slate-500">{{ result.data.address }}</div>
                            }
                            <div class="text-xs text-slate-400 mt-1">{{ result.data.post_count }} posts</div>
                          </div>
                        </div>
                      </div>
                    }
                    @case ('hashtag') {
                      <div class="p-4 hover:bg-slate-50 dark:hover:bg-slate-900 rounded-xl transition-colors cursor-pointer border border-slate-200 dark:border-white/10">
                        <div class="flex items-center gap-3">
                          <div class="w-12 h-12 rounded-full bg-indigo-100 dark:bg-indigo-950 flex items-center justify-center">
                            <app-icon name="hash" [size]="24" class="text-indigo-600 dark:text-indigo-400"></app-icon>
                          </div>
                          <div class="flex-1">
                            <div class="font-bold text-slate-900 dark:text-white">#{{ result.data.tag }}</div>
                            <div class="text-sm text-slate-500">{{ result.data.usage_count || 0 }} posts</div>
                          </div>
                        </div>
                      </div>
                    }
                  }
                }
              </div>
            }
          </div>
        } @else {
          @switch (activeCategory()) {
            @case ('trending') {
              <!-- Trending Section -->
              <div class="p-4 space-y-6">
                <!-- Trending Hashtags -->
                <div>
                  <h2 class="text-lg font-bold text-slate-900 dark:text-white mb-4">Trending Hashtags</h2>
                  <div class="grid grid-cols-1 gap-3">
                    @for (hashtag of trendingHashtags(); track hashtag.tag) {
                      <div class="p-4 bg-gradient-to-r from-slate-50 to-white dark:from-slate-900 dark:to-slate-950 border border-slate-200 dark:border-white/10 rounded-xl hover:border-indigo-500 dark:hover:border-indigo-500 transition-all cursor-pointer group">
                        <div class="flex items-center justify-between">
                          <div class="flex-1">
                            <div class="flex items-center gap-2 mb-1">
                              <span class="text-lg font-bold text-indigo-600 dark:text-indigo-400">#{{ hashtag.tag }}</span>
                              @if (hashtag.growth > 0) {
                                <span class="px-2 py-0.5 bg-green-100 dark:bg-green-950 text-green-600 dark:text-green-400 text-xs font-bold rounded-full flex items-center gap-1">
                                  <app-icon name="trending-up" [size]="12"></app-icon>
                                  {{ hashtag.growth }}%
                                </span>
                              }
                            </div>
                            <p class="text-sm text-slate-500">{{ hashtag.count }} posts</p>
                          </div>
                          <app-icon name="chevron-right" [size]="20" class="text-slate-400 group-hover:text-indigo-500 transition-colors"></app-icon>
                        </div>
                      </div>
                    }
                  </div>
                </div>

                <!-- Trending Topics -->
                <div>
                  <h2 class="text-lg font-bold text-slate-900 dark:text-white mb-4">Trending Topics</h2>
                  <div class="grid grid-cols-2 gap-3">
                    @for (topic of trendingTopics(); track topic.id) {
                      <div class="p-4 bg-gradient-to-br from-indigo-50 to-purple-50 dark:from-indigo-950/30 dark:to-purple-950/30 border border-indigo-200 dark:border-indigo-800 rounded-xl hover:shadow-lg transition-all cursor-pointer">
                        <div class="text-3xl mb-2">{{ topic.icon }}</div>
                        <h3 class="font-bold text-slate-900 dark:text-white mb-1">{{ topic.title }}</h3>
                        <p class="text-xs text-slate-500 mb-2">{{ topic.category }}</p>
                        <p class="text-xs text-indigo-600 dark:text-indigo-400 font-medium">{{ topic.posts_count }} posts</p>
                      </div>
                    }
                  </div>
                </div>

                <!-- Trending Posts -->
                <div>
                  <h2 class="text-lg font-bold text-slate-900 dark:text-white mb-4">Trending Posts</h2>
                  @for (post of trendingPosts(); track post.id) {
                    <app-post-card [post]="post"></app-post-card>
                  }
                </div>
              </div>
            }

            @case ('people') {
              <!-- Suggested People -->
              <div class="p-4">
                <h2 class="text-lg font-bold text-slate-900 dark:text-white mb-4">Suggested for You</h2>
                <div class="space-y-3">
                  @for (user of suggestedUsers(); track user.uid) {
                    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/10 rounded-xl hover:border-indigo-500 dark:hover:border-indigo-500 transition-all">
                      <div class="flex items-center gap-3">
                        <img 
                          [src]="user.avatar" 
                          (click)="navigateToProfile(user.username)"
                          class="w-12 h-12 rounded-full object-cover cursor-pointer hover:ring-2 hover:ring-indigo-500 transition-all">
                        <div class="flex-1 min-w-0 cursor-pointer" (click)="navigateToProfile(user.username)">
                          <div class="flex items-center gap-1">
                            <h3 class="font-bold text-slate-900 dark:text-white truncate">{{ user.display_name }}</h3>
                            @if (user.verify) {
                              <app-icon name="verified" [size]="16" class="text-indigo-500 flex-shrink-0"></app-icon>
                            }
                          </div>
                          <p class="text-sm text-slate-500 truncate">@{{ user.username }}</p>
                          <p class="text-xs text-slate-400 mt-1">{{ user.followers_count }} followers</p>
                        </div>
                        <button 
                          (click)="followUser(user.uid, $event)"
                          class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-bold rounded-full transition-all flex-shrink-0">
                          Follow
                        </button>
                      </div>
                    </div>
                  }
                </div>
              </div>
            }

            @case ('photos') {
              <!-- Photo Grid -->
              <div class="p-4">
                <h2 class="text-lg font-bold text-slate-900 dark:text-white mb-4">Popular Photos</h2>
                <div class="grid grid-cols-3 gap-1">
                  @for (post of photoPosts(); track post.id) {
                    @if (post.media && post.media.length > 0) {
                      <div class="aspect-square bg-slate-100 dark:bg-slate-900 rounded-lg overflow-hidden cursor-pointer hover:opacity-90 transition-opacity">
                        <img [src]="post.media[0].url" class="w-full h-full object-cover">
                      </div>
                    }
                  }
                </div>
              </div>
            }

            @case ('videos') {
              <!-- Video Feed -->
              <div>
                <div class="p-4">
                  <h2 class="text-lg font-bold text-slate-900 dark:text-white mb-4">Popular Videos</h2>
                </div>
                @for (post of videoPosts(); track post.id) {
                  <app-post-card [post]="post"></app-post-card>
                }
              </div>
            }
          }
        }
      </div>
    </div>
  `,
  styles: [`
    .scrollbar-hide::-webkit-scrollbar {
      display: none;
    }
    .scrollbar-hide {
      -ms-overflow-style: none;
      scrollbar-width: none;
    }
  `]
})
export class ExploreComponent implements OnInit {
  private socialService = inject(SocialService);
  searchService = inject(SearchService);
  private hashtagService = inject(HashtagService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  searchQuery = '';
  activeCategory = signal<string>('trending');
  searchLoading = signal(false);
  currentFilter = signal<SearchFilter>('all');

  searchFilters = [
    { id: 'all' as SearchFilter, name: 'All', icon: 'search' },
    { id: 'people' as SearchFilter, name: 'People', icon: 'users' },
    { id: 'posts' as SearchFilter, name: 'Posts', icon: 'file-text' },
    { id: 'photos' as SearchFilter, name: 'Photos', icon: 'image' },
    { id: 'videos' as SearchFilter, name: 'Videos', icon: 'video' },
    { id: 'locations' as SearchFilter, name: 'Locations', icon: 'map-pin' },
    { id: 'hashtags' as SearchFilter, name: 'Hashtags', icon: 'hash' }
  ];

  categories = [
    { id: 'trending', name: 'Trending', icon: 'zap' },
    { id: 'people', name: 'People', icon: 'users' },
    { id: 'photos', name: 'Photos', icon: 'image' },
    { id: 'videos', name: 'Videos', icon: 'video' }
  ];

  trendingHashtags = signal<TrendingHashtag[]>([
    { tag: 'Web3', count: 12500, growth: 45 },
    { tag: 'AI', count: 9800, growth: 32 },
    { tag: 'Decentralized', count: 7200, growth: 28 },
    { tag: 'Privacy', count: 5600, growth: 15 },
    { tag: 'OpenSource', count: 4300, growth: 12 }
  ]);

  trendingTopics = signal<TrendingTopic[]>([
    { id: '1', title: 'Technology', category: 'Tech', posts_count: 15000, icon: '💻' },
    { id: '2', title: 'Design', category: 'Creative', posts_count: 8500, icon: '🎨' },
    { id: '3', title: 'Gaming', category: 'Entertainment', posts_count: 12000, icon: '🎮' },
    { id: '4', title: 'Music', category: 'Arts', posts_count: 6700, icon: '🎵' }
  ]);

  trendingPosts = signal<Post[]>([]);
  suggestedUsers = signal<User[]>([]);
  photoPosts = signal<Post[]>([]);
  videoPosts = signal<Post[]>([]);

  ngOnInit() {
    this.loadTrendingContent();
    
    // Handle hashtag query param
    this.route.queryParams.subscribe(params => {
      if (params['q']) {
        this.searchQuery = params['q'];
        this.onSearch();
      }
    });
  }

  async loadTrendingContent() {
    // Load trending posts (sorted by engagement)
    const allPosts = this.socialService.getPosts();

    // Sort by engagement score (likes + comments * 2)
    const trending = [...allPosts].sort((a, b) => {
      const scoreA = a.likes_count + (a.comments_count * 2);
      const scoreB = b.likes_count + (b.comments_count * 2);
      return scoreB - scoreA;
    }).slice(0, 10);

    this.trendingPosts.set(trending);

    // Load suggested users
    this.suggestedUsers.set(this.socialService.getSuggestedUsers());

    // Filter photo and video posts
    this.photoPosts.set(allPosts.filter(p => p.post_type === 'IMAGE').slice(0, 12));
    this.videoPosts.set(allPosts.filter(p => p.post_type === 'VIDEO').slice(0, 10));

    // Fetch trending hashtags from service
    const hashtags = await this.hashtagService.getTrendingHashtags();
    this.trendingHashtags.set(hashtags.map(h => ({
      tag: h.tag,
      count: h.usage_count,
      growth: 0
    })));
  }

  async onSearch() {
    if (!this.searchQuery.trim()) {
      this.searchService.searchResults.set([]);
      return;
    }

    this.searchLoading.set(true);
    await this.searchService.search(this.searchQuery, this.currentFilter());
    this.searchLoading.set(false);
  }

  async setSearchFilter(filter: SearchFilter) {
    this.currentFilter.set(filter);
    if (this.searchQuery.trim()) {
      await this.onSearch();
    }
  }

  getResultsTitle(): string {
    const count = this.searchService.searchResults().length;
    const filterName = this.searchFilters.find(f => f.id === this.currentFilter())?.name || 'Results';
    return `${filterName} (${count})`;
  }

  clearSearch() {
    this.searchQuery = '';
    this.searchService.searchResults.set([]);
    this.currentFilter.set('all');
  }

  navigateToProfile(username: string) {
    this.router.navigate(['/app/profile', username]);
  }

  async followUser(userId: string, event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    await this.socialService.followUser(userId);
    await this.loadTrendingContent();
  }
}
