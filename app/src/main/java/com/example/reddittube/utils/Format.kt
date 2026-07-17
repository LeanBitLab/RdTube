package com.example.reddittube.utils

// ponytail: shared score formatter (was duplicated in HomeScreen + VideoPage)
fun formatScore(score: Int): String {
    return when {
        score >= 1000000 -> String.format("%.1fM", score / 1000000f)
        score >= 1000 -> String.format("%.1fk", score / 1000f)
        else -> score.toString()
    }
}
