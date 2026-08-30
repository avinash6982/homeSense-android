package com.avinash.homesense.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import java.time.Duration
import java.time.Instant

/** Ticks every 15s so "Updated X ago" stays fresh without a full data refresh. */
@Composable
fun rememberRelativeTimeText(timestamp: Instant?): String {
    val state by produceState(initialValue = formatRelativeTime(timestamp), timestamp) {
        while (true) {
            value = formatRelativeTime(timestamp)
            kotlinx.coroutines.delay(15_000)
        }
    }
    return state
}

private fun formatRelativeTime(timestamp: Instant?): String {
    if (timestamp == null) return "Never"
    val seconds = Duration.between(timestamp, Instant.now()).seconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "Updated just now"
        seconds < 3600 -> "Updated ${seconds / 60} min${if (seconds / 60 == 1L) "" else "s"} ago"
        seconds < 86_400 -> "Updated ${seconds / 3600} hour${if (seconds / 3600 == 1L) "" else "s"} ago"
        else -> "Updated ${seconds / 86_400} day${if (seconds / 86_400 == 1L) "" else "s"} ago"
    }
}
