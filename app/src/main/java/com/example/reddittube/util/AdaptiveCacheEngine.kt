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

    operator fun get(key: K): V? {
        val node = store[key] ?: return null
        node.frequency += 1
        node.lastAccessedMs = SystemClock.elapsedRealtime()
        return node.value
    }

    operator fun set(key: K, value: V) = put(key, value)

    fun put(key: K, value: V) {
        val now = SystemClock.elapsedRealtime()
        val weight = sizeEstimator(value)
        store[key] = CacheNode(value = value, frequency = 1, lastAccessedMs = now, sizeBytes = weight)
        trimToCapacity()
    }

    /**
     * Compute eviction score S(i):
     * S(i) = (f_i * exp(-lambda * delta_t)) / (1 + ln(1 + weight))
     */
    private fun computeScore(node: CacheNode<V>, nowMs: Long): Double {
        val deltaSec = ((nowMs - node.lastAccessedMs) / 1000.0).coerceAtLeast(0.0)
        val decay = kotlin.math.exp(-lambda * deltaSec)
        val weightFactor = 1.0 + ln(1.0 + node.sizeBytes)
        return (node.frequency * decay) / weightFactor
    }

    private fun trimToCapacity() {
        val target = dynamicCapacity
        val now = SystemClock.elapsedRealtime()
        while (store.size > target) {
            val lowest = store.minByOrNull { computeScore(it.value, now) } ?: break
            store.remove(lowest.key)
        }
    }

    fun containsKey(key: K): Boolean = store.containsKey(key)

    fun clear() = store.clear()

    fun size(): Int = store.size
}
