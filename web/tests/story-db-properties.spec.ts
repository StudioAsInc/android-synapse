import { test, expect } from '@playwright/test';
import * as fc from 'fast-check';

test.describe('Story Database Function Properties', () => {
  
  test('Property 16: Privacy check ordering - blocks override allowed lists', async ({ page }) => {
    await fc.assert(
      fc.asyncProperty(
        fc.record({
          creatorId: fc.uuid(),
          viewerId: fc.uuid(),
          privacy: fc.constantFrom('public', 'followers', 'close_friends'),
          isBlocked: fc.boolean(),
          isInAllowedList: fc.boolean()
        }),
        async ({ creatorId, viewerId, privacy, isBlocked, isInAllowedList }) => {
          const result = await page.evaluate(async ({ creatorId, viewerId, privacy, isBlocked, isInAllowedList }) => {
            const { createClient } = await import('@supabase/supabase-js');
            const supabase = createClient(
              import.meta.env.VITE_SUPABASE_URL,
              import.meta.env.VITE_SUPABASE_ANON_KEY
            );

            // Setup: Create users and story
            const storyId = crypto.randomUUID();
            
            if (isBlocked) {
              await supabase.from('blocks').insert({ blocker_id: creatorId, blocked_id: viewerId });
            }
            
            if (isInAllowedList && privacy === 'close_friends') {
              await supabase.from('close_friends').insert({ user_id: creatorId, friend_id: viewerId });
            }

            await supabase.from('stories').insert({
              id: storyId,
              user_id: creatorId,
              privacy_setting: privacy
            });

            // Test: Check visibility
            const { data } = await supabase.rpc('can_view_story', {
              p_story_id: storyId,
              p_viewer_id: viewerId
            });

            // Cleanup
            await supabase.from('stories').delete().eq('id', storyId);
            if (isBlocked) await supabase.from('blocks').delete().eq('blocker_id', creatorId);
            if (isInAllowedList) await supabase.from('close_friends').delete().eq('user_id', creatorId);

            return data;
          }, { creatorId, viewerId, privacy, isBlocked, isInAllowedList });

          // Invariant: If blocked, NEVER visible regardless of other settings
          if (isBlocked) {
            expect(result).toBe(false);
          }
        }
      ),
      { numRuns: 20 }
    );
  });

  test('Property 19: Story feed ordering - chronological within user groups', async ({ page }) => {
    await fc.assert(
      fc.asyncProperty(
        fc.array(
          fc.record({
            userId: fc.uuid(),
            storyId: fc.uuid(),
            createdAt: fc.date({ min: new Date('2024-01-01'), max: new Date() })
          }),
          { minLength: 3, maxLength: 10 }
        ),
        async (stories) => {
          const viewerId = crypto.randomUUID();

          const result = await page.evaluate(async ({ stories, viewerId }) => {
            const { createClient } = await import('@supabase/supabase-js');
            const supabase = createClient(
              import.meta.env.VITE_SUPABASE_URL,
              import.meta.env.VITE_SUPABASE_ANON_KEY
            );

            // Setup: Insert stories
            for (const story of stories) {
              await supabase.from('stories').insert({
                id: story.storyId,
                user_id: story.userId,
                created_at: story.createdAt.toISOString(),
                privacy_setting: 'public'
              });
            }

            // Test: Fetch feed
            const { data } = await supabase.rpc('get_user_story_feed', {
              p_viewer_id: viewerId
            });

            // Cleanup
            for (const story of stories) {
              await supabase.from('stories').delete().eq('id', story.storyId);
            }

            return data || [];
          }, { stories, viewerId });

          // Invariant: Stories from same user must be chronologically ordered
          const userGroups = new Map<string, any[]>();
          result.forEach((story: any) => {
            if (!userGroups.has(story.user_id)) {
              userGroups.set(story.user_id, []);
            }
            userGroups.get(story.user_id)!.push(story);
          });

          userGroups.forEach((userStories) => {
            for (let i = 1; i < userStories.length; i++) {
              const prev = new Date(userStories[i - 1].created_at);
              const curr = new Date(userStories[i].created_at);
              expect(prev.getTime()).toBeLessThanOrEqual(curr.getTime());
            }
          });
        }
      ),
      { numRuns: 15 }
    );
  });
});
