package com.lean.reddittube.data

import org.junit.Test

class DataRepositoryTest {

    @Test
    fun `redditPost data class handles all fields`() {
        val post = RedditPost(
            id = "abc123",
            title = "Test Video Title",
            subreddit = "test",
            author = "testuser",
            score = 1500,
            permalink = "/r/test/comments/abc123",
            videoUrl = "https://v.redd.it/abc123/DASH_720.mp4",
            fallbackUrl = "https://v.redd.it/abc123/DASH_720.mp4",
            dashUrl = "https://v.redd.it/abc123/dash.mpd",
            hlsUrl = "https://v.redd.it/abc123/hls.m3u8"
        )

        assert(post.id == "abc123")
        assert(post.title == "Test Video Title")
        assert(post.subreddit == "test")
        assert(post.author == "testuser")
        assert(post.score == 1500)
        assert(post.permalink == "/r/test/comments/abc123")
        assert(post.videoUrl == "https://v.redd.it/abc123/DASH_720.mp4")
        assert(post.fallbackUrl == "https://v.redd.it/abc123/DASH_720.mp4")
        assert(post.dashUrl == "https://v.redd.it/abc123/dash.mpd")
        assert(post.hlsUrl == "https://v.redd.it/abc123/hls.m3u8")
    }

    @Test
    fun `redditError hierarchy covers all types`() {
        val networkError = RedditError.NetworkError("Timeout", null)
        assert(networkError.message == "Timeout")

        val rateLimited = RedditError.RateLimited(60)
        assert(rateLimited.message?.contains("60") == true)

        val noVideos = RedditError.NoVideosFound("test+test2")
        assert(noVideos.message?.contains("test+test2") == true)

        val unknown = RedditError.Unknown("Something broke")
        assert(unknown.message == "Something broke")
    }

    @Test
    fun `fetchMoreResult holds posts and afterMap`() {
        val posts = listOf(
            RedditPost("1", "A", "test", "u", 1, "/r/1", "u", "u", "", "")
        )
        val afterMap = mapOf("test" to "t3_abc")
        val result = FetchMoreResult(posts, afterMap)

        assert(result.posts.size == 1)
        assert(result.afterMap["test"] == "t3_abc")
    }
}
