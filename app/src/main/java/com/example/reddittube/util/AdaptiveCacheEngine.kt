package com.lean.reddittube.util

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ln

/**
 * Adaptive Dynamic Cache Engine
 * Uses Heap-Aware Capacity Sizing and Time-Decayed Frequency-Recency Priority Score S(i):
 * S(i) = [ f_i * e^(-lambda * delta_t) ] / [ 1 + ln(1 + weight_bytes) ]
 */
class AdaptiveCacheEngine<K : Any, V : Any>(
    private val lowerBound: Int = 32,
    private val upperBound: Int = 500,
    private val memoryFraction: Float = 0.15f,
    private val isProtectedKey: (K) -> Boolean = { false },
    private val sizeEstimator: (V) -> Long = { 1024L }
) {
    private data class CacheNode<V>(
        val value: V,
        var frequency: Int,
        var lastAccessedMs: Long,
        val sizeBytes: Long
    )

    private val store = ConcurrentHashMap<K, CacheNode<V>>()
    private val lambda = 0.005 // Time decay parameter

    /**
     * Dynamically calculates target capacity C_max based on available JVM memory:
     * C_max = min(upperBound, max(lowerBound, floor(AvailableMemory * alpha / avgSize)))
     */
    val dynamicCapacity: Int
        get() {
            val runtime = Runtime.getRuntime()
            val availableMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
            val allocatable = (availableMemory * memoryFraction).toLong()
            val totalSize = store.values.sumOf { it.sizeBytes }
            val count = store.size.coerceAtLeast(1)
            val avgItemSize = (totalSize / count).coerceAtLeast(1024L)
            val calcCapacity = (allocatable / avgItemSize).toInt()
            return calcCapacity.coerceIn(lowerBound, upperBound)
        }

    private var hitCount = 0L
    private var missCount = 0L
    private val protectedTtlMs = 60_000L // 60s TTL for protected keys

    val cacheHitRatio: Float
        get() {
            val total = hitCount + missCount
            return if (total > 0) (hitCount.toFloat() / total.toFloat()) else 1.0f
        }

    operator fun get(key: K): V? {
        val node = store[key]
        if (node != null) {
            hitCount++
            node.frequency += 1
            node.lastAccessedMs = SystemClock.elapsedRealtime()
            return node.value
        } else {
            missCount++
            return null
        }
    }

    operator fun set(key: K, value: V) = put(key, value)

    fun put(key: K, value: V) {
        val now = SystemClock.elapsedRealtime()
        val weight = sizeEstimator(value)
        store[key] = CacheNode(value = value, frequency = 1, lastAccessedMs = now, sizeBytes = weight)
        trimToCapacity()
    }

    /**
     * Compute keep_score S(i) from Plan.md:
     * keep_score = ((1 + freq)^0.9) * exp(-lambda * age_sec) / (1 + ln(1 + size_weight))
     */
    private fun computeScore(node: CacheNode<V>, nowMs: Long): Double {
        val deltaSec = ((nowMs - node.lastAccessedMs) / 1000.0).coerceAtLeast(0.0)
        val freqFactor = Math.pow(1.0 + node.frequency, 0.9)
        val decay = kotlin.math.exp(-lambda * deltaSec)
        val sizeWeightMB = node.sizeBytes / (1024.0 * 1024.0)
        val weightFactor = 1.0 + ln(1.0 + sizeWeightMB)
        return (freqFactor * decay) / weightFactor
    }

    private fun trimToCapacity() {
        val target = dynamicCapacity
        val now = SystemClock.elapsedRealtime()
        while (store.size > target) {
            val candidate = store.filter { (k, n) ->
                !isProtectedKey(k) || (now - n.lastAccessedMs > protectedTtlMs)
            }.minByOrNull { computeScore(it.value, now) } ?: break
            store.remove(candidate.key)
        }
    }

    fun containsKey(key: K): Boolean = store.containsKey(key)

    fun clear() = store.clear()

    fun size(): Int = store.size
}
