import { test, expect } from '@playwright/test';

test.describe('Repost Functionality', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should show repost button on post card', async ({ page }) => {
    // Navigate to feed (assuming user is logged in or using mock data)
    await page.goto('/app/feed');
    
    // Wait for posts to load
    await page.waitForSelector('[title="Repost"]', { timeout: 10000 });
    
    // Check if repost button exists
    const repostButton = page.locator('[title="Repost"]').first();
    await expect(repostButton).toBeVisible();
  });

  test('should toggle repost state when clicked', async ({ page }) => {
    await page.goto('/app/feed');
    
    // Wait for posts to load
    await page.waitForSelector('[title="Repost"]', { timeout: 10000 });
    
    const repostButton = page.locator('[title="Repost"]').first();
    const repostCount = repostButton.locator('span').first();
    
    // Get initial count
    const initialCount = await repostCount.textContent();
    
    // Click repost button
    await repostButton.click();
    
    // Handle the confirm dialog (Cancel for simple repost)
    page.on('dialog', dialog => dialog.dismiss());
    
    // Wait for state update
    await page.waitForTimeout(1000);
    
    // Check if button has active state (green color)
    await expect(repostButton).toHaveClass(/text-green-500/);
  });

  test('should show quote option when reposting', async ({ page }) => {
    await page.goto('/app/feed');
    
    await page.waitForSelector('[title="Repost"]', { timeout: 10000 });
    
    const repostButton = page.locator('[title="Repost"]').first();
    
    // Set up dialog handler
    let dialogShown = false;
    page.on('dialog', dialog => {
      dialogShown = true;
      expect(dialog.message()).toContain('Quote');
      dialog.dismiss();
    });
    
    await repostButton.click();
    
    // Verify dialog was shown
    await page.waitForTimeout(500);
    expect(dialogShown).toBe(true);
  });

  test('should unrepost when clicking repost button again', async ({ page }) => {
    await page.goto('/app/feed');
    
    await page.waitForSelector('[title="Repost"]', { timeout: 10000 });
    
    const repostButton = page.locator('[title="Repost"]').first();
    
    // First repost
    page.on('dialog', dialog => dialog.dismiss());
    await repostButton.click();
    await page.waitForTimeout(1000);
    
    // Verify it's reposted
    await expect(repostButton).toHaveClass(/text-green-500/);
    
    // Click again to unrepost
    await repostButton.click();
    await page.waitForTimeout(1000);
    
    // Verify it's unreposted
    await expect(repostButton).not.toHaveClass(/text-green-500/);
  });
});
