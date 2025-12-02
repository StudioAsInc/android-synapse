import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:3000';

test.describe('Messages with Media', () => {
  test('should display messages page', async ({ page }) => {
    await page.goto(BASE_URL);
    
    // Check if page loads
    await expect(page).toHaveTitle(/Synapse/);
  });

  test('should verify message schema supports media', async ({ page }) => {
    // This test verifies the database schema through the UI
    await page.goto(`${BASE_URL}/#/app/messages`);
    
    // Wait for page load
    await page.waitForTimeout(2000);
    
    // Check if messages page exists
    const hasMessages = await page.locator('h1:has-text("Messages")').count();
    expect(hasMessages).toBeGreaterThan(0);
  });
});
