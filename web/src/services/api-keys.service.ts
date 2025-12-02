import { Injectable, inject } from '@angular/core';
import { SupabaseClient } from '@supabase/supabase-js';
import { from, Observable } from 'rxjs';

export interface ApiKey {
  id: string;
  key_prefix: string;
  name: string;
  status: 'active' | 'revoked';
  created_at: string;
  last_used_at?: string;
}

export interface ApiKeyWithSecret extends ApiKey {
  key: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiKeysService {
  private supabase = inject(SupabaseClient);
  private functionsUrl = 'https://apqvyyphlrtmuyjnzmuq.supabase.co/functions/v1';

  async generateKey(name?: string): Promise<ApiKeyWithSecret> {
    const token = (await this.supabase.auth.getSession()).data.session?.access_token;
    const res = await fetch(`${this.functionsUrl}/api-keys`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ name }),
    });
    if (!res.ok) throw new Error('Failed to generate API key');
    return res.json();
  }

  async listKeys(): Promise<ApiKey[]> {
    const token = (await this.supabase.auth.getSession()).data.session?.access_token;
    const res = await fetch(`${this.functionsUrl}/api-keys`, {
      headers: { 'Authorization': `Bearer ${token}` },
    });
    if (!res.ok) throw new Error('Failed to fetch API keys');
    return res.json();
  }

  async revokeKey(keyId: string): Promise<void> {
    const token = (await this.supabase.auth.getSession()).data.session?.access_token;
    const res = await fetch(`${this.functionsUrl}/api-keys?id=${keyId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` },
    });
    if (!res.ok) throw new Error('Failed to revoke API key');
  }
}
