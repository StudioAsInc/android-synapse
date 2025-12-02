/**
 * @fileoverview Service for managing posts and post-related operations.
 * Handles creating, fetching, editing, deleting, and bookmarking posts.
 */

import { Injectable, inject, signal } from '@angular/core';
import { SupabaseService } from './supabase.service';
import { AuthService } from './auth.service';

/**
 * Interface representing a post in the application.
 * 
 * @interface Post
 * @property {string} id - Unique identifier for the post
 * @property {string} post_text - The text content of the post
 * @property {string} author_uid - User ID of the post author
 * @property {string} created_at - ISO timestamp of post creation
 * @property {number} likes_count - Number of likes on the post
 * @property {number} comments_count - Number of comments on the post
 * @property {Object} [users] - Author user information (optional)
 * @property {string} users.display_name - Author's display name
 * @property {string} users.username - Author's username
 * @property {string} users.avatar - Author's avatar URL
 */
export interface Post {
  id: string;
  post_text: string;
  author_uid: string;
  created_at: string;
  likes_count: number;
  comments_count: number;
  users?: {
    display_name: string;
    username: string;
    avatar: string;
  };
}

/**
 * Service for managing posts and post-related operations.
 * Provides methods for CRUD operations, bookmarking, and hashtag management.
 * 
 * @injectable
 * @providedIn 'root'
 */
@Injectable({
  providedIn: 'root'
})
export class PostService {
  /** Supabase client instance */
  private supabase = inject(SupabaseService).client;
  
  /** Authentication service for user context */
  private auth = inject(AuthService);

  /** Signal containing all fetched posts */
  posts = signal<Post[]>([]);
  
  /** Signal indicating if posts are being loaded */
  loading = signal(false);

  /**
   * Fetch all posts from the database ordered by creation date.
   * Includes author information via join with users table.
   * 
   * @returns {Promise<void>}
   */
  async fetchPosts() {
    this.loading.set(true);
    try {
      const { data, error } = await this.supabase
        .from('posts')
        .select(`
          *,
          users:author_uid (
            display_name,
            username,
            avatar
          )
        `)
        .order('created_at', { ascending: false });

      if (error) throw error;
      this.posts.set(data as any[]);
    } catch (err) {
      console.error('Error fetching posts:', err);
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Create a new post with optional media attachments.
   * Automatically extracts and saves hashtags from post text.
   * 
   * @param {string} text - The post content text
   * @param {File[]} [mediaFiles] - Optional array of media files to attach
   * @returns {Promise<void>}
   * @throws {Error} If user is not authenticated or post creation fails
   */
  async createPost(text: string, mediaFiles?: File[]) {
    const user = this.auth.currentUser();
    if (!user) throw new Error('Not authenticated');

    try {
      let mediaItems: any[] = [];
      let postType = 'TEXT';

      // Upload media files if provided
      if (mediaFiles && mediaFiles.length > 0) {
        mediaItems = await Promise.all(
          mediaFiles.map(async (file) => {
            const fileExt = file.name.split('.').pop();
            const fileName = `${user.id}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}.${fileExt}`;
            const filePath = `posts/${fileName}`;

            const { error: uploadError } = await this.supabase.storage
              .from('user-media')
              .upload(filePath, file);

            if (uploadError) throw uploadError;

            const { data } = this.supabase.storage
              .from('user-media')
              .getPublicUrl(filePath);

            const mediaType = file.type.startsWith('video') ? 'VIDEO' : 'IMAGE';
            if (postType === 'TEXT') postType = mediaType;

            return {
              type: mediaType,
              url: data.publicUrl
            };
          })
        );
      }

      // Extract hashtags from text
      const hashtags = this.extractHashtags(text);

      const { data: postData, error } = await this.supabase
        .from('posts')
        .insert({
          post_text: text,
          author_uid: user.id,
          post_type: postType,
          media_items: mediaItems,
          created_at: new Date().toISOString(),
          timestamp: Date.now()
        })
        .select()
        .single();

      if (error) throw error;

      // Save hashtags
      if (hashtags.length > 0 && postData) {
        await this.saveHashtags(postData.id, hashtags);
      }

      await this.fetchPosts();
    } catch (err) {
      console.error('Error creating post:', err);
      throw err;
    }
  }

  /**
   * Extract hashtags from post text.
   * Hashtags are identified by # prefix followed by word characters.
   * 
   * @private
   * @param {string} text - The text to extract hashtags from
   * @returns {string[]} Array of hashtags without the # prefix, in lowercase
   */
  private extractHashtags(text: string): string[] {
    const hashtagRegex = /#(\w+)/g;
    const matches = text.match(hashtagRegex);
    return matches ? matches.map(tag => tag.substring(1).toLowerCase()) : [];
  }

  /**
   * Save hashtags to the database and link them to a post.
   * Creates hashtag records if they don't exist and increments usage count.
   * 
   * @private
   * @param {string} postId - The ID of the post to link hashtags to
   * @param {string[]} hashtags - Array of hashtag strings to save
   * @returns {Promise<void>}
   */
  private async saveHashtags(postId: string, hashtags: string[]) {
    for (const tag of hashtags) {
      // Upsert hashtag
      const { data: hashtagData } = await this.supabase
        .from('hashtags')
        .upsert({ tag }, { onConflict: 'tag' })
        .select()
        .single();

      if (hashtagData) {
        // Link hashtag to post
        await this.supabase
          .from('post_hashtags')
          .insert({ post_id: postId, hashtag_id: hashtagData.id });

        // Increment usage count using RPC
        await this.supabase
          .rpc('increment_hashtag_usage', { hashtag_id: hashtagData.id });
      }
    }
  }

  /**
   * Soft delete a post by marking it as deleted.
   * The post record remains in the database but is hidden from views.
   * 
   * @param {string} postId - The ID of the post to delete
   * @returns {Promise<void>}
   * @throws {Error} If deletion fails
   */
  async deletePost(postId: string) {
    try {
      const { error } = await this.supabase
        .from('posts')
        .update({ is_deleted: true, deleted_at: new Date().toISOString() })
        .eq('id', postId);

      if (error) throw error;
      await this.fetchPosts();
    } catch (err) {
      console.error('Error deleting post:', err);
      throw err;
    }
  }

  /**
   * Edit the text content of an existing post.
   * Marks the post as edited and updates the edit timestamp.
   * 
   * @param {string} postId - The ID of the post to edit
   * @param {string} newText - The new post text content
   * @returns {Promise<void>}
   * @throws {Error} If edit fails
   */
  async editPost(postId: string, newText: string) {
    try {
      const { error } = await this.supabase
        .from('posts')
        .update({
          post_text: newText,
          is_edited: true,
          edited_at: new Date().toISOString()
        })
        .eq('id', postId);

      if (error) throw error;
      await this.fetchPosts();
    } catch (err) {
      console.error('Error editing post:', err);
      throw err;
    }
  }

  /**
   * Add a post to the current user's bookmarks.
   * 
   * @param {string} postId - The ID of the post to bookmark
   * @returns {Promise<void>}
   */
  async bookmarkPost(postId: string) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    try {
      const { error } = await this.supabase
        .from('favorites')
        .insert({ user_id: userId, post_id: postId });

      if (error) throw error;
    } catch (err) {
      console.error('Error bookmarking post:', err);
    }
  }

  /**
   * Remove a post from the current user's bookmarks.
   * 
   * @param {string} postId - The ID of the post to unbookmark
   * @returns {Promise<void>}
   */
  async unbookmarkPost(postId: string) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    try {
      const { error } = await this.supabase
        .from('favorites')
        .delete()
        .eq('user_id', userId)
        .eq('post_id', postId);

      if (error) throw error;
    } catch (err) {
      console.error('Error removing bookmark:', err);
    }
  }
}