package com.synapse.social.studioasinc.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.synapse.social.studioasinc.data.model.Post
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostPagingSource(
    private val postgrest: Postgrest
) : PagingSource<Int, Post>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val page = params.key ?: 1
        val pageSize = params.loadSize
        return try {
            val response = withContext(Dispatchers.IO) {
                postgrest.from("posts")
                    .select()
                    .range((page - 1) * pageSize, page * pageSize - 1)
                    .executeAs<List<Post>>()
            }

            LoadResult.Page(
                data = response,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
