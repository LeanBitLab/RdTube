package com.lean.reddittube.utils

import com.lean.reddittube.data.RedditPost
import org.json.JSONObject

// ponytail: single source of truth for RedditPost <-> JSONObject (was duplicated in VM + HomeScreen)
fun RedditPost.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("subreddit", subreddit)
    put("author", author)
    put("score", score)
    put("permalink", permalink)
    put("videoUrl", videoUrl)
    put("fallbackUrl", fallbackUrl)
    put("dashUrl", dashUrl)
    put("hlsUrl", hlsUrl)
    put("thumbnailUrl", thumbnailUrl)
    put("numComments", numComments)
}

fun JSONObject.toRedditPost(): RedditPost = RedditPost(
    id = optString("id"),
    title = optString("title"),
    subreddit = optString("subreddit"),
    author = optString("author"),
    score = optInt("score"),
    permalink = optString("permalink"),
    videoUrl = optString("videoUrl"),
    fallbackUrl = optString("fallbackUrl"),
    dashUrl = optString("dashUrl"),
    hlsUrl = optString("hlsUrl"),
    thumbnailUrl = optString("thumbnailUrl"),
    numComments = optInt("numComments")
)
