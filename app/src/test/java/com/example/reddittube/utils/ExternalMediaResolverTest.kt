package com.lean.reddittube.utils

import com.lean.reddittube.data.RedditPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalMediaResolverTest {

    @Test
    fun testIsRedgifsUrl() {
        assertTrue(ExternalMediaResolver.isRedgifsUrl("https://www.redgifs.com/watch/clevercutecat"))
        assertTrue(ExternalMediaResolver.isRedgifsUrl("https://redgifs.com/ifr/clevercutecat"))
        assertTrue(ExternalMediaResolver.isRedgifsUrl("https://thumbs2.redgifs.com/CleverCuteCat.mp4"))
        assertTrue(ExternalMediaResolver.isRedgifsUrl("https://gfycat.com/clevercutecat"))
        assertFalse(ExternalMediaResolver.isRedgifsUrl("https://v.redd.it/abc1234/DASH_720.mp4"))
        assertFalse(ExternalMediaResolver.isRedgifsUrl("https://imgur.com/gallery/12345"))
    }

    @Test
    fun testIsImgurUrl() {
        assertTrue(ExternalMediaResolver.isImgurUrl("https://imgur.com/abc1234.gifv"))
        assertTrue(ExternalMediaResolver.isImgurUrl("https://i.imgur.com/abc1234.mp4"))
        assertFalse(ExternalMediaResolver.isImgurUrl("https://redgifs.com/watch/abc"))
    }

    @Test
    fun testResolveImgurDirectUrl() {
        assertEquals("https://i.imgur.com/abc1234.mp4", ExternalMediaResolver.resolveImgurDirectUrl("https://imgur.com/abc1234.gifv"))
        assertEquals("https://i.imgur.com/xyz987.mp4", ExternalMediaResolver.resolveImgurDirectUrl("https://i.imgur.com/xyz987.mp4"))
        assertEquals("https://i.imgur.com/test12.mp4", ExternalMediaResolver.resolveImgurDirectUrl("https://imgur.com/test12"))
        assertNull(ExternalMediaResolver.resolveImgurDirectUrl("https://imgur.com/gallery/test12"))
        assertNull(ExternalMediaResolver.resolveImgurDirectUrl("https://imgur.com/a/test12"))
    }

    @Test
    fun testExtractRedgifsId() {
        assertEquals("CleverCuteCat", ExternalMediaResolver.extractRedgifsId("https://www.redgifs.com/watch/CleverCuteCat"))
        assertEquals("CleverCuteCat", ExternalMediaResolver.extractRedgifsId("https://redgifs.com/ifr/CleverCuteCat"))
        assertEquals("clevercutecat", ExternalMediaResolver.extractRedgifsId("https://redgifs.com/watch/clevercutecat?hasAudio=true"))
        assertEquals("CleverCuteCat", ExternalMediaResolver.extractRedgifsId("https://thumbs2.redgifs.com/CleverCuteCat.mp4"))
        assertEquals("CleverCuteCat", ExternalMediaResolver.extractRedgifsId("https://media.redgifs.com/CleverCuteCat-mobile.mp4"))
        assertEquals("CleverCuteCat", ExternalMediaResolver.extractRedgifsId("https://media.redgifs.com/CleverCuteCat-silent.mp4"))
        assertEquals("Cool_Cat-123", ExternalMediaResolver.extractRedgifsId("https://www.redgifs.com/watch/Cool_Cat-123"))
        assertEquals("CleverCuteCat", ExternalMediaResolver.extractRedgifsId("https://gfycat.com/CleverCuteCat"))
        assertNull(ExternalMediaResolver.extractRedgifsId("https://redgifs.com/"))
        assertNull(ExternalMediaResolver.extractRedgifsId("https://www.redgifs.com"))
        assertNull(ExternalMediaResolver.extractRedgifsId("https://www.redgifs.com/"))
        assertNull(ExternalMediaResolver.extractRedgifsId("https://www.redgifs.com/watch/"))
    }

    @Test
    fun testResolveDirectRedgifsUrlFromPost() {
        val oembed = org.json.JSONObject().apply {
            put("thumbnail_url", "https://media.redgifs.com/AzureWateryMarmoset-poster.jpg")
        }
        val media = org.json.JSONObject().apply {
            put("type", "redgifs.com")
            put("oembed", oembed)
        }
        val targetData = org.json.JSONObject().apply {
            put("url", "https://redgifs.com/watch/azurewaterymarmoset")
            put("media", media)
        }
        val childData = org.json.JSONObject()

        val direct = ExternalMediaResolver.resolveDirectRedgifsUrlFromPost(targetData, childData)
        assertEquals("https://media.redgifs.com/AzureWateryMarmoset.mp4", direct)
        assertEquals("https://media.redgifs.com/AzureWateryMarmoset.mp4", ExternalMediaResolver.getCachedRedgifsVideo("https://redgifs.com/watch/azurewaterymarmoset"))
        assertEquals("https://media.redgifs.com/AzureWateryMarmoset.mp4", ExternalMediaResolver.getCachedRedgifsVideo("azurewaterymarmoset"))
    }

    @Test
    fun testPostDataClassWithContentUrl() {
        val post = RedditPost(
            id = "test_post_id",
            title = "Test Post Title",
            subreddit = "gifs",
            author = "author1",
            score = 100,
            permalink = "/r/gifs/comments/test_post_id/test_post_title/",
            videoUrl = "https://preview.redd.it/preview.mp4",
            fallbackUrl = "https://preview.redd.it/preview.mp4",
            dashUrl = "",
            hlsUrl = "",
            contentUrl = "https://www.redgifs.com/watch/CleverCuteCat"
        )

        assertEquals("https://www.redgifs.com/watch/CleverCuteCat", post.contentUrl)
    }
}
