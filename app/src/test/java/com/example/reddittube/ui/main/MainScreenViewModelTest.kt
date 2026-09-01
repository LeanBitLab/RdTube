package com.lean.reddittube.ui.main

import android.content.Context
import android.content.SharedPreferences
import com.lean.reddittube.data.DataRepository
import com.lean.reddittube.data.FetchMoreResult
import com.lean.reddittube.data.RedditError
import com.lean.reddittube.data.RedditPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class FakeRepository(
    private val fetchResult: () -> Flow<List<RedditPost>> = { flowOf(emptyList()) },
    private val searchResult: () -> Flow<List<String>> = { flowOf(emptyList()) },
    private val fetchMoreResult: () -> Flow<FetchMoreResult> = { flowOf(FetchMoreResult(emptyList(), emptyMap())) },
    private val afterMap: () -> Map<String, String?> = { emptyMap() },
    private val onSaveAfterMap: (Map<String, String?>) -> Unit = {},
) : DataRepository {
    val mockPrefs: SharedPreferences = Mockito.mock(SharedPreferences::class.java).also { prefs ->
        Mockito.`when`(prefs.getStringSet(Mockito.any(), Mockito.any())).thenReturn(emptySet())
        Mockito.`when`(prefs.getString(Mockito.any(), Mockito.any())).thenReturn(null)
        Mockito.`when`(prefs.getLong(Mockito.any(), Mockito.anyLong())).thenReturn(0L)
        val editor = Mockito.mock(SharedPreferences.Editor::class.java)
        Mockito.`when`(editor.putStringSet(Mockito.any(), Mockito.any())).thenReturn(editor)
        Mockito.`when`(editor.putString(Mockito.any(), Mockito.any())).thenReturn(editor)
        Mockito.`when`(editor.putLong(Mockito.any(), Mockito.anyLong())).thenReturn(editor)
        Mockito.`when`(editor.remove(Mockito.any())).thenReturn(editor)
        Mockito.`when`(editor.commit()).thenReturn(true)
        Mockito.`when`(prefs.edit()).thenReturn(editor)
    }
    private val mockContext: Context = Mockito.mock(Context::class.java).also { ctx ->
        Mockito.`when`(ctx.getSharedPreferences(Mockito.any(), Mockito.anyInt())).thenReturn(mockPrefs)
        Mockito.`when`(ctx.applicationContext).thenReturn(ctx)
    }
    override fun getContext(): Context = mockContext
    override fun fetchRedditVideos(subreddits: String, sort: String, feed: String) = fetchResult()
    override fun searchSubreddits(query: String) = searchResult()
    override fun fetchMoreVideos(subreddits: String, afterMap: Map<String, String?>, sort: String, feed: String) = fetchMoreResult()
    override fun fetchPostComments(subreddit: String, postId: String): Flow<List<com.lean.reddittube.data.RedditComment>> = flowOf(emptyList())
    override fun getAfterMap(feed: String): Map<String, String?> = afterMap()
    override fun saveAfterMap(map: Map<String, String?>, feed: String) = onSaveAfterMap(map)
}

class MainScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest(testDispatcher) {
        val repo = FakeRepository()
        val viewModel = MainScreenViewModel(repo)
        assert(viewModel.exploreState.value is MainScreenUiState.Loading)
        assert(viewModel.subscribedState.value is MainScreenUiState.Loading)
    }

    @Test
    fun `refreshExplore emits Success when data is fetched`() = runTest(testDispatcher) {
        val posts = listOf(
            RedditPost("1", "Test Video", "test", "author", 100, "/r/test/1", "url", "url", "", "")
        )
        val repo = FakeRepository(fetchResult = { flowOf(posts) })
        val viewModel = MainScreenViewModel(repo)
        viewModel.refreshExplore()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.exploreState.value
        assert(state is MainScreenUiState.Success) { "Expected Success, got $state" }
        assert((state as MainScreenUiState.Success).data.size == 1)
    }

    @Test
    fun `refreshExplore emits Error when repository throws`() = runTest(testDispatcher) {
        val repo = FakeRepository(fetchResult = { flow { throw RedditError.NetworkError("Timeout") } })
        val viewModel = MainScreenViewModel(repo)
        viewModel.refreshExplore()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.exploreState.value
        assert(state is MainScreenUiState.Error) { "Expected Error, got $state" }
        assert((state as MainScreenUiState.Error).throwable is RedditError.NetworkError)
    }

    @Test
    fun `refreshSubscribed with empty query shows error`() = runTest(testDispatcher) {
        val repo = FakeRepository()
        val viewModel = MainScreenViewModel(repo)
        viewModel.refreshSubscribed("")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.subscribedState.value
        assert(state is MainScreenUiState.Error) { "Expected Error for empty query, got $state" }
    }

    @Test
    fun `refreshExplore filters out watched videos`() = runTest(testDispatcher) {
        val repo = FakeRepository(
            fetchResult = {
                flowOf(listOf(
                    RedditPost("existing_id", "Old Video", "test", "author", 100, "/r/test/1", "url", "url", "", ""),
                    RedditPost("new_id", "New Video", "test", "author2", 50, "/r/test/2", "url", "url", "", "")
                ))
            }
        )
        val viewModel = MainScreenViewModel(repo)
        val existing = RedditPost("existing_id", "Old Video", "test", "author", 100, "/r/test/1", "url", "url", "", "")
        viewModel.markAsWatched(existing)
        viewModel.refreshExplore()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.exploreState.value
        assert(state is MainScreenUiState.Success)
        assert((state as MainScreenUiState.Success).data.size == 1)
        assert((state).data[0].id == "new_id")
    }

    @Test
    fun `loadMore does nothing when no more pages`() = runTest(testDispatcher) {
        val repo = FakeRepository(
            fetchResult = { flowOf(emptyList()) },
            afterMap = { mapOf("shorts" to null, "TikTokCringe" to null) }
        )
        val viewModel = MainScreenViewModel(repo)
        viewModel.refreshExplore()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore(true)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `searchSubreddits emits empty list for short queries`() = runTest(testDispatcher) {
        val repo = FakeRepository()
        val viewModel = MainScreenViewModel(repo)
        viewModel.searchSubreddits("a")
        assert(viewModel.searchResults.value.isEmpty())
    }

    @Test
    fun `refreshExplore deduplicates duplicate post IDs from repository`() = runTest(testDispatcher) {
        val posts = listOf(
            RedditPost("dup_1", "Dup 1", "test", "author", 100, "/r/test/1", "url", "url", "", ""),
            RedditPost("dup_1", "Dup 1 Copy", "test", "author", 100, "/r/test/1", "url", "url", "", ""),
            RedditPost("unique_2", "Unique 2", "test", "author2", 50, "/r/test/2", "url", "url", "", "")
        )
        val repo = FakeRepository(fetchResult = { flowOf(posts) })
        val viewModel = MainScreenViewModel(repo)
        viewModel.refreshExplore()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.exploreState.value
        assert(state is MainScreenUiState.Success)
        val data = (state as MainScreenUiState.Success).data
        assert(data.size == 2)
        assert(data.map { it.id } == listOf("dup_1", "unique_2"))
    }

    @Test
    fun `loadMore deduplicates duplicate post IDs in fetchMore results`() = runTest(testDispatcher) {
        val initialPosts = listOf(
            RedditPost("id_1", "Initial 1", "test", "author", 100, "/r/test/1", "url", "url", "", "")
        )
        val morePosts = listOf(
            RedditPost("id_2", "More 2", "test", "author", 80, "/r/test/2", "url", "url", "", ""),
            RedditPost("id_2", "More 2 Dup", "test", "author", 80, "/r/test/2", "url", "url", "", ""),
            RedditPost("id_3", "More 3", "test", "author", 60, "/r/test/3", "url", "url", "", "")
        )
        val repo = FakeRepository(
            fetchResult = { flowOf(initialPosts) },
            fetchMoreResult = { flowOf(FetchMoreResult(morePosts, mapOf("test" to "t3_after"))) },
            afterMap = { mapOf("test" to "t3_init") }
        )
        val viewModel = MainScreenViewModel(repo)
        viewModel.refreshExplore()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.exploreState.value
        assert(state is MainScreenUiState.Success)
        val data = (state as MainScreenUiState.Success).data
        assert(data.size == 3)
        assert(data.map { it.id } == listOf("id_1", "id_2", "id_3"))
    }
}
