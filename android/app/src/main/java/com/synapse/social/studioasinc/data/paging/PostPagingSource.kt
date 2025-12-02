package com.synapse.social.studioasinc.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.synapse.social.studioasinc.model.Post
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostPagingSource(
    private val queryBuilder: PostgrestQueryBuilder
) : PagingSource<Int, Post>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val position = params.key ?: 0
        val pageSize = params.loadSize
        return try {
            val response = withContext(Dispatchers.IO) {
                queryBuilder
                    .select()
                    .range(position, position + pageSize - 1)
                    .executeAs<List<Post>>()
            }

            LoadResult.Page(
                data = response,
                prevKey = if (position == 0) null else position - pageSize,
                nextKey = if (response.isEmpty()) null else position + pageSize
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
