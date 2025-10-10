package com.synapse.social.studioasinc.backend

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.Assert.*
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.user.User

class AuthenticationServiceTest {

    private val mockAuth = mockk<Auth>()
    private val service = AuthenticationService().apply {
        // Inject mock Supabase Auth client
        supabase = mockk { every { auth } returns mockAuth }
    }

    @Test
    fun `getCurrentUser returns SupabaseUser when user is authenticated`() {
        // Arrange
        val mockSupabaseUser = User(id = "user_123", email = "test@example.com")
        every { mockAuth.currentUserOrNull() } returns mockSupabaseUser

        // Act
        val result = service.getCurrentUser()

        // Assert
        assertNotNull(result)
        assertEquals("user_123", result?.uid)
        assertEquals("test@example.com", result?.email)
        verify { mockAuth.currentUserOrNull() }
    }

    @Test
    fun `getCurrentUser returns null when user is not authenticated`() {
        // Arrange
        every { mockAuth.currentUserOrNull() } returns null

        // Act
        val result = service.getCurrentUser()

        // Assert
        assertNull(result)
        verify { mockAuth.currentUserOrNull() }
    }
}