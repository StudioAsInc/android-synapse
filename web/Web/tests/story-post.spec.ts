import { test, expect } from '@playwright/test';

test.describe('Story Posting', () => {
  test('should post a story successfully', async ({ page }) => {
    await page.goto('http://localhost:3000');
    
    // Login
    await page.fill('input[type="email"]', 'test@example.com');
    await page.fill('input[type="password"]', 'password123');
    await page.click('button:has-text("Sign In")');
    
    await page.waitForURL('**/app/feed');
    
    // Click story button
    await page.click('button:has-text("Story")');
    
    // Wait for story dialog
    await page.waitForSelector('[role="dialog"]');
    
    // Add story text
    await page.fill('textarea', 'Test story content');
    
    // Post story
    await page.click('button:has-text("Post Story")');
    
    // Verify success
    await expect(page.locator('text=Story posted')).toBeVisible({ timeout: 5000 });
  });
});
