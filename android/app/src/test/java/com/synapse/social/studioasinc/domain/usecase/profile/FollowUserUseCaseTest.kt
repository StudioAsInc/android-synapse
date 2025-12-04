package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.ProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@RunWith(MockitoJUnitRunner::class)
class FollowUserUseCaseTest {

    @Mock
    private lateinit var repository: ProfileRepository

    private lateinit var useCase: FollowUserUseCase

    @Before
    fun setup() {
        useCase = FollowUserUseCase(repository)
    }

    @Test
    fun `execute calls repository followUser`() = runTest {
        whenever(repository.followUser("user1")).thenReturn(Result.success(Unit))

        val result = useCase.execute("user1")

        Assert.assertTrue(result.isSuccess)
        verify(repository).followUser("user1")
    }

    @Test
    fun `execute returns failure when repository fails`() = runTest {
        val exception = Exception("Follow failed")
        whenever(repository.followUser("user1")).thenReturn(Result.failure(exception))

        val result = useCase.execute("user1")

        Assert.assertTrue(result.isFailure)
    }

    @Test
    fun `execute validates userId is not empty`() = runTest {
        val result = useCase.execute("")

        Assert.assertTrue(result.isFailure)
        verify(repository, never()).followUser(any())
    }
}
