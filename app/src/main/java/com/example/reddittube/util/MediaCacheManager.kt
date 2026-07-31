package com.example.reddittube.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import java.io.File

@OptIn(UnstableApi::class)
object MediaCacheManager {
    private const val MAX_CACHE_SIZE: Long = 100 * 1024 * 1024 // 100 MB LRU Cache

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getSimpleCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(context.cacheDir, "media3_video_cache")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
                val databaseProvider = StandaloneDatabaseProvider(context)
                SimpleCache(cacheDir, evictor, databaseProvider).also { simpleCache = it }
            }
        }
    }

    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("org.quantumbadger.redreader/1.25.1")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)

        val upstreamFactory = DefaultDataSource.Factory(context, httpFactory)
        val cache = getSimpleCache(context)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun getLowLatencyLoadControl(): LoadControl {
        return getAdaptiveLoadControl(500, 1000, 10000)
    }

    fun getAdaptiveLoadControl(
        bufferPlayMs: Int = 300,
        bufferRebufferMs: Int = 800,
        bufferMaxMs: Int = 12000
    ): LoadControl {
        val minBuffer = (bufferMaxMs / 2).coerceIn(4000, minOf(12000, bufferMaxMs))
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ minBuffer,
                /* maxBufferMs = */ bufferMaxMs,
                /* bufferForPlaybackMs = */ bufferPlayMs,
                /* bufferForPlaybackAfterRebufferMs = */ bufferRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
}
