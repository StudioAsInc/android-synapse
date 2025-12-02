import { createClient } from 'jsr:@supabase/supabase-js@2';

export async function validateApiKey(authHeader: string | null): Promise<{ valid: boolean; userId?: string; error?: string }> {
  if (!authHeader?.startsWith('Bearer sk_live_')) {
    return { valid: false, error: 'Invalid API key format' };
  }

  const apiKey = authHeader.replace('Bearer ', '');
  const keyHash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(apiKey));
  const hashHex = Array.from(new Uint8Array(keyHash)).map(b => b.toString(16).padStart(2, '0')).join('');

  const supabase = createClient(
    Deno.env.get('SUPABASE_URL') ?? '',
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
  );

  const { data, error } = await supabase
    .from('user_api_keys')
    .select('id, user_id')
    .eq('key_hash', hashHex)
    .eq('status', 'active')
    .single();

  if (error || !data) {
    return { valid: false, error: 'Invalid or revoked API key' };
  }

  // Update last_used_at
  await supabase
    .from('user_api_keys')
    .update({ last_used_at: new Date().toISOString() })
    .eq('id', data.id);

  return { valid: true, userId: data.user_id };
}
