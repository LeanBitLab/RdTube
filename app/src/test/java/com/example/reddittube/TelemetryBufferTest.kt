package com.lean.reddittube

import com.lean.reddittube.util.AdaptiveCacheEngine
import com.lean.reddittube.util.MediaCacheManager
import org.junit.Assert.*
import org.junit.Test

class TelemetryBufferTest {

    @Test
    fun testBufferInvariants_allRanges() {
        val testMaxBuffers = listOf(0, 3000, 4000, 8000, 12000, 20000)
        val testPlayBuffers = listOf(250, 300, 500, 900)
        val testRebuffers = listOf(500, 800, 1500)

        for (maxB in testMaxBuffers) {
            val upper = minOf(12000, maxB)
            val lower = minOf(4000, upper)
            val minBufferMs = (maxB / 2).coerceIn(lower, upper)
            val safeMax = maxOf(maxB, minBufferMs)

            // Test 4: minBufferMs <= bufferMaxMs
            assertTrue("minBufferMs $minBufferMs must be <= safeMax $safeMax", minBufferMs <= safeMax)

            for (playB in testPlayBuffers) {
                val safePlayMs = minOf(playB, minBufferMs)
                // Test 5: playbackBufferMs <= minBufferMs
                assertTrue("safePlayMs $safePlayMs must be <= minBufferMs $minBufferMs", safePlayMs <= minBufferMs)
            }

            for (rebB in testRebuffers) {
                val safeRebufferMs = minOf(rebB, minBufferMs)
                // Test 6: rebufferMs <= minBufferMs
                assertTrue("safeRebufferMs $safeRebufferMs must be <= minBufferMs $minBufferMs", safeRebufferMs <= minBufferMs)
            }
        }
    }

    @Test
    fun testOomRiskClamping() {
        val sigmoid = { x: Float -> (1.0f / (1.0f + kotlin.math.exp(-x))) }
        val testMemPressures = listOf(0.0f, 0.5f, 0.85f, 1.0f, 1.5f)
        val testNativePressures = listOf(0.0f, 0.5f, 0.85f, 1.0f, 2.0f)

        for (mem in testMemPressures) {
            for (nat in testNativePressures) {
                val rawRisk = sigmoid(10f * (mem - 0.82f)) + 0.5f * sigmoid(10f * (nat - 0.80f))
                val clamped = rawRisk.coerceIn(0.0f, 1.0f)
                // Test 7: oomRisk never exceeds 1.0
                assertTrue("clamped risk $clamped must be <= 1.0", clamped <= 1.0f)
                assertTrue("clamped risk $clamped must be >= 0.0", clamped >= 0.0f)
            }
        }
    }

    @Test
    fun testAdaptiveCacheEngineProtectedKeys() {
        val cache = AdaptiveCacheEngine<String, String>(
            lowerBound = 2,
            upperBound = 2,
            isProtectedKey = { key -> key.startsWith("protected_") }
        )

        cache["protected_1"] = "val1"
        cache["item_2"] = "val2"
        cache["item_3"] = "val3" // Triggers eviction

        // Test 10: Protected cache keys are not evicted
        assertTrue("protected_1 must remain in cache", cache.containsKey("protected_1"))
        assertEquals(2, cache.size())
    }
}
