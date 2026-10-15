package com.example.reddittube.data

import org.junit.Test

data class MockRedditPost(val id: String)

class PerformanceTest {

    fun benchOld(size: Int): Long {
        val list = mutableListOf<MockRedditPost>()
        val posts = (0 until size).map { MockRedditPost(if (it % 2 == 0) it.toString() else (it-1).toString()) }
        val start = System.nanoTime()
        for (post in posts) {
            if (list.none { it.id == post.id }) {
                list.add(post)
            }
        }
        return System.nanoTime() - start
    }

    fun benchNew(size: Int): Long {
        val list = mutableListOf<MockRedditPost>()
        val seenIds = HashSet<String>()
        val posts = (0 until size).map { MockRedditPost(if (it % 2 == 0) it.toString() else (it-1).toString()) }
        val start = System.nanoTime()
        for (post in posts) {
            if (seenIds.add(post.id)) {
                list.add(post)
            }
        }
        return System.nanoTime() - start
    }

    @Test
    fun testPerformance() {
        // Warmup
        for(i in 0 until 10) { benchOld(100); benchNew(100) }

        val sizes = listOf(100, 1000, 5000, 10000)
        for (size in sizes) {
            println("Size: " + size)
            val t1 = benchOld(size)
            val t2 = benchNew(size)
            println("Old: " + (t1/1_000_000) + " ms")
            println("New: " + (t2/1_000_000) + " ms")
            val improvement = if (t2 > 0) t1 / t2 else 0
            println("Improvement: " + improvement + "x")
        }
    }
}
