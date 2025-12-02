import { Injectable, inject, signal } from '@angular/core';
import { SupabaseService } from './supabase.service';

export interface TagRequest {
  id: string;
  post_id: string;
  tagged_user_id: string;
  tagged_by_user_id: string;
  status: 'pending' | 'accepted' | 'rejected';
  created_at: string;
  responded_at?: string;
  post?: any;
  tagged_by?: any;
}

@Injectable({
  providedIn: 'root'
})
export class TagRequestService {
  private supabase = inject(SupabaseService).client;
  
  pendingRequests = signal<TagRequest[]>([]);

  async loadPendingRequests(userId: string) {
    const { data, error } = await this.supabase
      .from('tag_requests')
      .select(`
        *,
        post:posts(*),
        tagged_by:users!tag_requests_tagged_by_user_id_fkey(*)
      `)
      .eq('tagged_user_id', userId)
      .eq('status', 'pending')
      .order('created_at', { ascending: false });

    if (!error && data) {
      this.pendingRequests.set(data);
    }
    return data || [];
  }

  async acceptTagRequest(requestId: string) {
    const { error } = await this.supabase
      .from('tag_requests')
      .update({
        status: 'accepted',
        responded_at: new Date().toISOString()
      })
      .eq('id', requestId);

    if (!error) {
      this.pendingRequests.update(reqs => reqs.filter(r => r.id !== requestId));
    }
    return !error;
  }

  async rejectTagRequest(requestId: string) {
    const { error } = await this.supabase
      .from('tag_requests')
      .update({
        status: 'rejected',
        responded_at: new Date().toISOString()
      })
      .eq('id', requestId);

    if (!error) {
      this.pendingRequests.update(reqs => reqs.filter(r => r.id !== requestId));
    }
    return !error;
  }

  async getAcceptedTagsForPost(postId: string): Promise<string[]> {
    const { data } = await this.supabase
      .from('tag_requests')
      .select('tagged_user_id')
      .eq('post_id', postId)
      .eq('status', 'accepted');

    return data?.map(t => t.tagged_user_id) || [];
  }
}
