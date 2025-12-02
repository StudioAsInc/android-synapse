import { test, expect } from '@playwright/test';

test.describe('Comment Edit', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3000/login');
    
    // Login
    await page.fill('input[type="email"]', 'test@example.com');
    await page.fill('input[type="password"]', 'password123');
    await page.click('button[type="submit"]');
    
    // Wait for navigation to feed
    await page.waitForURL('**/app/feed');
  });

  test('should edit a comment successfully', async ({ page }) => {
    // Navigate to a post with comments or create one
    await page.goto('http://localhost:3000/app/feed');
    
    // Find first post and click to view details
    const firstPost = page.locator('app-post-card').first();
    await firstPost.click();
    
    // Wait for post detail page
    await page.waitForURL('**/app/post/**');
    
    // Add a comment
    const commentText = 'Original comment text';
    await page.fill('textarea[placeholder*="comment"]', commentText);
    await page.click('button:has-text("Comment")');
    
    // Wait for comment to appear
    await expect(page.locator(`text=${commentText}`)).toBeVisible();
    
    // Open comment menu (three dots)
    const commentItem = page.locator('app-comment-item').filter({ hasText: commentText }).first();
    await commentItem.locator('app-action-menu button').click();
    
    // Click edit option
    await page.click('text=Edit comment');
    
    // Edit the comment
    const editedText = 'Edited comment text';
    const textarea = commentItem.locator('textarea');
    await textarea.clear();
    await textarea.fill(editedText);
    
    // Save the edit
    await commentItem.locator('button:has-text("Save")').click();
    
    // Verify the comment is updated
    await expect(page.locator(`text=${editedText}`)).toBeVisible();
    await expect(page.locator('text=(edited)')).toBeVisible();
    
    // Verify original text is gone
    await expect(page.locator(`text=${commentText}`)).not.toBeVisible();
  });

  test('should cancel comment edit', async ({ page }) => {
    await page.goto('http://localhost:3000/app/feed');
    
    const firstPost = page.locator('app-post-card').first();
    await firstPost.click();
    await page.waitForURL('**/app/post/**');
    
    // Add a comment
    const commentText = 'Test comment for cancel';
    await page.fill('textarea[placeholder*="comment"]', commentText);
    await page.click('button:has-text("Comment")');
    await expect(page.locator(`text=${commentText}`)).toBeVisible();
    
    // Start editing
    const commentItem = page.locator('app-comment-item').filter({ hasText: commentText }).first();
    await commentItem.locator('app-action-menu button').click();
    await page.click('text=Edit comment');
    
    // Change text but cancel
    const textarea = commentItem.locator('textarea');
    await textarea.clear();
    await textarea.fill('This should not be saved');
    await commentItem.locator('button:has-text("Cancel")').click();
    
    // Verify original text is still there
    await expect(page.locator(`text=${commentText}`)).toBeVisible();
    await expect(page.locator('text=This should not be saved')).not.toBeVisible();
  });
});
