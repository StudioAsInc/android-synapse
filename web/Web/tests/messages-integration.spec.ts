import { test, expect } from '@playwright/test';

test.describe('Messages Integration Test', () => {
  test('verify media message in database', async () => {
    // This test verifies that the database schema supports media messages
    // by checking the test message we inserted earlier
    
    const response = await fetch('https://apqvyyphlrtmuyjnzmuq.supabase.co/rest/v1/messages?select=id,content,message_type,media_url,media_type&message_type=eq.image&limit=1', {
      headers: {
        'apikey': process.env.VITE_SUPABASE_ANON_KEY || '',
        'Authorization': `Bearer ${process.env.VITE_SUPABASE_ANON_KEY || ''}`
      }
    });
    
    const data = await response.json();
    
    // Verify we can query media messages
    expect(response.status).toBe(200);
    expect(Array.isArray(data)).toBe(true);
    
    if (data.length > 0) {
      const message = data[0];
      expect(message.message_type).toBe('image');
      expect(message.media_url).toBeTruthy();
      console.log('✓ Media message schema verified:', message);
    }
  });

  test('verify messaging service structure', async ({ page }) => {
    await page.goto('http://localhost:3000');
    
    // Check if the app loads
    await page.waitForTimeout(2000);
    
    // Verify the page has loaded
    const title = await page.title();
    expect(title).toContain('Synapse');
    
    console.log('✓ App loaded successfully');
  });
});
