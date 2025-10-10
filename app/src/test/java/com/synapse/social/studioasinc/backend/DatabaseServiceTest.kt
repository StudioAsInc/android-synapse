package com.synapse.social.studioasinc.backend

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PostgrestResult
import io.github.jan.supabase.postgrest.query.PostgrestFilterBuilder
import kotlinx.coroutines.runBlocking

class DatabaseServiceTest {

    private val mockPostgrest = mockk<Postgrest>()
    private lateinit var databaseService: DatabaseService

    @Before
    fun setup() {
        databaseService = DatabaseService().apply {
            // Inject mock Supabase Postgrest client
            supabase = mockk { every { postgrest } returns mockPostgrest }
        }
    }

    @Test
    fun `getReference returns SupabaseDatabaseReference`() {
        val ref = databaseService.getReference("test_table")
        assertNotNull(ref)
        assertTrue(ref is SupabaseDatabaseReference)
    }

    @Test
    fun `getData calls Postgrest select and returns data`() = runBlocking {
        val mockResult = mockk<PostgrestResult>()
        val mockFilterBuilder = mockk<PostgrestFilterBuilder>()
        val expectedJson = "[{\"id\":1,\"name\":\"test\"}]"

        every { mockPostgrest["test_table"].select(any(), any()) } returns mockFilterBuilder
        every { mockFilterBuilder.decodeList<Map<String, Any>>() } returns listOf(mapOf("id" to 1, "name" to "test"))
        every { mockFilterBuilder.body } returns expectedJson

        val ref = databaseService.getReference("test_table")
        var dataSnapshot: IDataSnapshot? = null
        ref.getData(object : IDataListener {
            override fun onDataChange(snapshot: IDataSnapshot) {
                dataSnapshot = snapshot
            }

            override fun onError(error: IDatabaseError) {
                fail("Error should not be called")
            }
        })

        assertNotNull(dataSnapshot)
        assertTrue(dataSnapshot!!.exists())
        assertEquals(listOf(mapOf("id" to 1, "name" to "test")), dataSnapshot!!.value)
        verify { mockPostgrest["test_table"].select(any(), any()) }
    }

    @Test
    fun `setValue calls Postgrest upsert`() = runBlocking {
        val mockFilterBuilder = mockk<PostgrestFilterBuilder>()
        val data = mapOf("id" to 1, "name" to "new_test")

        every { mockPostgrest["test_table"].upsert(any(), any()) } returns mockFilterBuilder
        every { mockFilterBuilder.execute() } returns mockk<PostgrestResult>()

        val ref = databaseService.getReference("test_table")
        var completed = false
        ref.setValue(data, object : IDatabaseReference.CompletionListener {
            override fun onComplete(error: IDatabaseError?, ref: IDatabaseReference) {
                assertNull(error)
                completed = true
            }
        })

        assertTrue(completed)
        verify { mockPostgrest["test_table"].upsert(any(), any()) }
    }

    @Test
    fun `updateChildren calls Postgrest update`() = runBlocking {
        val mockFilterBuilder = mockk<PostgrestFilterBuilder>()
        val updates = mapOf("name" to "updated_name")

        every { mockPostgrest["test_table"].update(any(), any()) } returns mockFilterBuilder
        every { mockFilterBuilder.execute() } returns mockk<PostgrestResult>()

        val ref = databaseService.getReference("test_table").child("some_id")
        var completed = false
        ref.updateChildren(updates, object : IDatabaseReference.CompletionListener {
            override fun onComplete(error: IDatabaseError?, ref: IDatabaseReference) {
                assertNull(error)
                completed = true
            }
        })

        assertTrue(completed)
        verify { mockPostgrest["test_table"].update(any(), any()) }
    }
}