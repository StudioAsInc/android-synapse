import { Injectable, signal, inject } from '@angular/core';
import { SupabaseService } from './supabase.service';

export interface SearchResult {
  type: 'user' | 'post' | 'hashtag' | 'photo' | 'video' | 'location';
  data: any;
}

export type SearchFilter = 'all' | 'people' | 'posts' | 'photos' | 'videos' | 'locations' | 'hashtags';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private supabase = inject(SupabaseService).client;

  searchResults = signal<SearchResult[]>([]);
  loading = signal(false);
  recentSearches = signal<string[]>([]);
  currentFilter = signal<SearchFilter>('all');

  async search(query: string, filter: SearchFilter = 'all') {
    if (!query.trim()) {
      this.searchResults.set([]);
      return;
    }

    this.loading.set(true);
    this.currentFilter.set(filter);
    
    try {
      const results: SearchResult[] = [];

      if (filter === 'all' || filter === 'people') {
        const users = await this.searchUsers(query);
        results.push(...users.map(user => ({ type: 'user' as const, data: user })));
      }

      if (filter === 'all' || filter === 'posts') {
        const posts = await this.searchPosts(query);
        results.push(...posts.map(post => ({ type: 'post' as const, data: post })));
      }

      if (filter === 'all' || filter === 'photos') {
        const photos = await this.searchPhotos(query);
        results.push(...photos.map(photo => ({ type: 'photo' as const, data: photo })));
      }

      if (filter === 'all' || filter === 'videos') {
        const videos = await this.searchVideos(query);
        results.push(...videos.map(video => ({ type: 'video' as const, data: video })));
      }

      if (filter === 'all' || filter === 'locations') {
        const locations = await this.searchLocations(query);
        results.push(...locations.map(loc => ({ type: 'location' as const, data: loc })));
      }

      if (filter === 'all' || filter === 'hashtags' || query.startsWith('#')) {
        const tag = query.startsWith('#') ? query.substring(1) : query;
        const hashtags = await this.searchHashtags(tag);
        results.push(...hashtags.map(hashtag => ({ type: 'hashtag' as const, data: hashtag })));
      }

      this.searchResults.set(results);
      this.addToRecentSearches(query);
    } catch (err) {
      console.error('Error searching:', err);
    } finally {
      this.loading.set(false);
    }
  }

  async searchUsers(query: string) {
    try {
      const { data, error } = await this.supabase
        .from('users')
        .select('uid, username, display_name, avatar, verify, followers_count, bio')
        .or(`username.ilike.%${query}%,display_name.ilike.%${query}%`)
        .limit(20);

      if (error) throw error;
      return data || [];
    } catch (err) {
      console.error('Error searching users:', err);
      return [];
    }
  }

  async searchPosts(query: string) {
    try {
      const { data, error } = await this.supabase
        .from('posts')
        .select(`
          *,
          users:author_uid (
            uid,
            username,
            display_name,
            avatar,
            verify
          )
        `)
        .ilike('post_text', `%${query}%`)
        .order('created_at', { ascending: false })
        .limit(20);

      if (error) throw error;

      return (data || []).map((post: any) => ({
        id: post.id,
        author_uid: post.author_uid,
        user: post.users,
        post_text: post.post_text || '',
        media: post.media_items || [],
        likes_count: post.likes_count || 0,
        comments_count: post.comments_count || 0,
        views_count: post.views_count || 0,
        created_at: post.created_at,
        post_type: post.post_type || 'TEXT'
      }));
    } catch (err) {
      console.error('Error searching posts:', err);
      return [];
    }
  }

  async searchPhotos(query: string) {
    try {
      const { data, error } = await this.supabase
        .from('posts')
        .select(`
          *,
          users:author_uid (
            uid,
            username,
            display_name,
            avatar,
            verify
          )
        `)
        .eq('post_type', 'IMAGE')
        .or(`post_text.ilike.%${query}%`)
        .order('created_at', { ascending: false })
        .limit(20);

      if (error) throw error;

      return (data || []).map((post: any) => ({
        id: post.id,
        author_uid: post.author_uid,
        user: post.users,
        post_text: post.post_text || '',
        media: post.media_items || [],
        likes_count: post.likes_count || 0,
        comments_count: post.comments_count || 0,
        created_at: post.created_at,
        post_type: post.post_type
      }));
    } catch (err) {
      console.error('Error searching photos:', err);
      return [];
    }
  }

  async searchVideos(query: string) {
    try {
      const { data, error } = await this.supabase
        .from('posts')
        .select(`
          *,
          users:author_uid (
            uid,
            username,
            display_name,
            avatar,
            verify
          )
        `)
        .eq('post_type', 'VIDEO')
        .or(`post_text.ilike.%${query}%`)
        .order('created_at', { ascending: false })
        .limit(20);

      if (error) throw error;

      return (data || []).map((post: any) => ({
        id: post.id,
        author_uid: post.author_uid,
        user: post.users,
        post_text: post.post_text || '',
        media: post.media_items || [],
        likes_count: post.likes_count || 0,
        comments_count: post.comments_count || 0,
        created_at: post.created_at,
        post_type: post.post_type
      }));
    } catch (err) {
      console.error('Error searching videos:', err);
      return [];
    }
  }

  async searchLocations(query: string) {
    try {
      const { data, error } = await this.supabase
        .from('posts')
        .select(`
          id,
          location_name,
          location_address,
          location_latitude,
          location_longitude,
          created_at
        `)
        .eq('has_location', true)
        .ilike('location_name', `%${query}%`)
        .order('created_at', { ascending: false })
        .limit(20);

      if (error) throw error;

      // Group by location name
      const locationMap = new Map();
      (data || []).forEach((post: any) => {
        const key = post.location_name;
        if (!locationMap.has(key)) {
          locationMap.set(key, {
            name: post.location_name,
            address: post.location_address,
            latitude: post.location_latitude,
            longitude: post.location_longitude,
            post_count: 0
          });
        }
        locationMap.get(key).post_count++;
      });

      return Array.from(locationMap.values());
    } catch (err) {
      console.error('Error searching locations:', err);
      return [];
    }
  }

  async searchHashtags(tag: string) {
    try {
      const { data, error } = await this.supabase
        .from('hashtags')
        .select('*')
        .ilike('tag', `%${tag}%`)
        .order('usage_count', { ascending: false })
        .limit(10);

      if (error) throw error;
      return data || [];
    } catch (err) {
      console.error('Error searching hashtags:', err);
      return [];
    }
  }

  async searchHashtag(tag: string) {
    try {
      const { data: hashtagData } = await this.supabase
        .from('hashtags')
        .select('id')
        .eq('tag', tag)
        .single();

      if (!hashtagData) return [];

      const { data: posts } = await this.supabase
        .from('post_hashtags')
        .select(`
          post_id,
          posts:post_id (
            *,
            users:author_uid (
              uid,
              username,
              display_name,
              avatar,
              verify
            )
          )
        `)
        .eq('hashtag_id', hashtagData.id);

      return (posts || []).map((p: any) => p.posts);
    } catch (err) {
      console.error('Error searching hashtag:', err);
      return [];
    }
  }

  async getTrendingHashtags(limit: number = 10) {
    try {
      const { data, error } = await this.supabase
        .from('hashtags')
        .select('*')
        .order('usage_count', { ascending: false })
        .limit(limit);

      if (error) throw error;
      return data || [];
    } catch (err) {
      console.error('Error fetching trending hashtags:', err);
      return [];
    }
  }

  private addToRecentSearches(query: string) {
    const recent = this.recentSearches();
    const updated = [query, ...recent.filter(q => q !== query)].slice(0, 10);
    this.recentSearches.set(updated);
    localStorage.setItem('recentSearches', JSON.stringify(updated));
  }

  loadRecentSearches() {
    const stored = localStorage.getItem('recentSearches');
    if (stored) {
      this.recentSearches.set(JSON.parse(stored));
    }
  }

  clearRecentSearches() {
    this.recentSearches.set([]);
    localStorage.removeItem('recentSearches');
  }

  constructor() {
    this.loadRecentSearches();
  }
}
