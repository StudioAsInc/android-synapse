package com.synapse.social.studioasinc.backend

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SupabaseChatService
 * Tests the chat creation logic including duplicate key handling and validation
 */
class SupabaseChatServiceTest {
    
    private lateinit var chatService: SupabaseChatService
    
    @Before
    fun setup() {
        chatService = SupabaseChatService()
    }
    
    /**
     * Test: Successful new chat creation with valid user IDs
     * Requirement: 1.1, 1.4
     */
    @Test
    fun testSuccessfulChatCreation() = runBlocking {
        // Given: Two valid user IDs
        val userId1 = "user123"
        val userId2 = "user456"
        
        // When: getOrCreateDirectChat is called
        val result = chatService.getOrCreateDirectChat(userId1, userId2)
        
        // Then: Chat is created and chat_id is returned
        assertTrue("Chat creation should succeed", result.isSuccess)
        val chatId = result.getOrNull()
        assertNotNull("Chat ID should not be null", chatId)
        assertTrue("Chat ID should follow dm_ format", chatId?.startsWith("dm_") == true)
    }
    
    /**
     * Test: Retrieval of existing chat when duplicate key error occurs
     * Requirement: 1.2, 1.3
     */
    @Test
    fun testRetrieveExistingChat() = runBlocking {
        // Given: Two valid user IDs and a chat that already exists
        val userId1 = "user123"
        val userId2 = "user456"
        
        // When: getOrCreateDirectChat is called twice
        val firstResult = chatService.getOrCreateDirectChat(userId1, userId2)
        val secondResult = chatService.getOrCreateDirectChat(userId1, userId2)
        
        // Then: Both calls should succeed and return the same chat_id
        assertTrue("First call should succeed", firstResult.isSuccess)
        assertTrue("Second call should succeed", secondResult.isSuccess)
        assertEquals("Both calls should return same chat ID", 
            firstResult.getOrNull(), 
            secondResult.getOrNull())
    }
    
    /**
     * Test: Self-messaging prevention
     * Requirement: 3.1
     */
    @Test
    fun testSelfMessagingPrevention() = runBlocking {
        // Given: Same user ID for both parameters
        val userId = "user123"
        
        // When: getOrCreateDirectChat is called with same user ID
        val result = chatService.getOrCreateDirectChat(userId, userId)
        
        // Then: Error is returned with appropriate message
        assertTrue("Self-messaging should fail", result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull("Error should not be null", error)
        assertTrue("Error message should mention self-messaging", 
            error?.message?.contains("yourself", ignoreCase = true) == true)
    }
    
    /**
     * Test: Invalid/empty user ID validation
     * Requirement: 3.3
     */
    @Test
    fun testEmptyUserIdValidation() = runBlocking {
        // Given: Empty user IDs
        val emptyUserId = ""
        val validUserId = "user123"
        
        // When: getOrCreateDirectChat is called with empty user ID
        val result1 = chatService.getOrCreateDirectChat(emptyUserId, validUserId)
        val result2 = chatService.getOrCreateDirectChat(validUserId, emptyUserId)
        val result3 = chatService.getOrCreateDirectChat(emptyUserId, emptyUserId)
        
        // Then: All calls should fail with validation error
        assertTrue("Empty userId1 should fail", result1.isFailure)
        assertTrue("Empty userId2 should fail", result2.isFailure)
        assertTrue("Both empty should fail", result3.isFailure)
        
        assertTrue("Error should mention invalid IDs", 
            result1.exceptionOrNull()?.message?.contains("Invalid", ignoreCase = true) == true)
    }
    
    /**
     * Test: Chat ID generation consistency
     * Requirement: 1.5
     */
    @Test
    fun testChatIdConsistency() = runBlocking {
        // Given: Two user IDs in different orders
        val userId1 = "user123"
        val userId2 = "user456"
        
        // When: getOrCreateDirectChat is called with reversed user IDs
        val result1 = chatService.getOrCreateDirectChat(userId1, userId2)
        val result2 = chatService.getOrCreateDirectChat(userId2, userId1)
        
        // Then: Both should generate the same chat_id
        assertTrue("First call should succeed", result1.isSuccess)
        assertTrue("Second call should succeed", result2.isSuccess)
        assertEquals("Chat IDs should be identical regardless of order", 
            result1.getOrNull(), 
            result2.getOrNull())
    }
    
    /**
     * Test: Concurrent chat creation scenarios
     * Requirement: 1.5
     * 
     * Note: This is a simplified test. Real concurrent testing would require
     * more sophisticated setup with actual database and multiple threads.
     */
    @Test
    fun testConcurrentChatCreation() = runBlocking {
        // Given: Two valid user IDs
        val userId1 = "user789"
        val userId2 = "user012"
        
        // When: Multiple calls are made (simulating concurrent requests)
        val results = List(5) {
            chatService.getOrCreateDirectChat(userId1, userId2)
        }
        
        // Then: All calls should succeed and return the same chat_id
        results.forEach { result ->
            assertTrue("All concurrent calls should succeed", result.isSuccess)
        }
        
        val chatIds = results.mapNotNull { it.getOrNull() }
        assertTrue("All chat IDs should be identical", 
            chatIds.all { it == chatIds.first() })
    }
}
