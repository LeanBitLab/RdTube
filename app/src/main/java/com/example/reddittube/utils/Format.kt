package com.lean.reddittube.utils

// ponytail: shared score formatter (was duplicated in HomeScreen + VideoPage)
fun formatScore(score: Int): String {
    return when {
        score >= 1000000 -> String.format("%.1fM", score / 1000000f)
        score >= 1000 -> String.format("%.1fk", score / 1000f)
        else -> score.toString()
    }
}

fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

fun formatRelativeTime(createdUtc: Long): String {
    if (createdUtc <= 0L) return ""
    val now = System.currentTimeMillis() / 1000L
    val diff = (now - createdUtc).coerceAtLeast(0L)
    return when {
        diff < 60 -> "${diff}s ago"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        diff < 2592000 -> "${diff / 2592000}mo ago"
        diff < 31536000 -> "${diff / 2592000}mo ago"
        else -> "${diff / 31536000}y ago"
    }
}
