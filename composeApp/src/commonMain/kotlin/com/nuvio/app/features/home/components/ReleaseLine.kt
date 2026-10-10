package com.nuvio.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.features.details.MetaDetailsRepository
import kotlinx.coroutines.delay

/**
 * Library items keep the release line they had when they were saved, so a series saved while it was
 * airing says "2026 – Present" even after it ended. Where this is provided (the library), a still-
 * running series is looked up again and shown with its end year once the details say it ended.
 */
internal val LocalRefreshStaleReleaseInfo = staticCompositionLocalOf { false }

/** The poster's release line, corrected from the item's details when those are known. */
@Composable
internal fun rememberReleaseLine(type: String, id: String, releaseInfo: String?): String? {
    val raw = releaseInfo ?: return null
    val stillRunning = type == "series" && openRangeRegex.matches(raw.trim())
    var fresh by remember(id, raw) { mutableStateOf(if (stillRunning) freshReleaseInfo(type, id, raw) else null) }
    val refresh = LocalRefreshStaleReleaseInfo.current
    if (stillRunning && fresh == null && refresh) {
        LaunchedEffect(id, raw) {
            MetaDetailsRepository.prefetch(type, id)
            repeat(40) {
                delay(250)
                freshReleaseInfo(type, id, raw)?.let {
                    fresh = it
                    return@LaunchedEffect
                }
            }
        }
    }
    return formatReleaseDateForDisplay(fresh ?: raw)
}

private fun freshReleaseInfo(type: String, id: String, saved: String): String? {
    val meta = MetaDetailsRepository.peek(type, id) ?: return null
    val start = yearRegex.find(saved)?.value
    val ended = meta.status?.let { status ->
        status.contains("ended", ignoreCase = true) || status.contains("cancel", ignoreCase = true)
    } == true
    val end = meta.lastAirDate?.let { yearRegex.find(it)?.value }
    if (ended && start != null && end != null) return "$start-$end"
    return meta.releaseInfo?.takeIf { it.isNotBlank() } ?: saved
}

private val openRangeRegex = Regex("""\d{4}\s*[-–]\s*""")
private val yearRegex = Regex("""\d{4}""")
