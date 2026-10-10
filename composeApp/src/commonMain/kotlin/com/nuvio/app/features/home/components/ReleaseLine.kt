package com.nuvio.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.features.details.MetaDetailsRepository

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
    if (!stillRunning) return formatReleaseDateForDisplay(raw)
    if (LocalRefreshStaleReleaseInfo.current) {
        // Only the addon's own meta: enough for the status and last air date, without the
        // TMDB/MDBList calls a full details load makes.
        LaunchedEffect(type, id) { MetaDetailsRepository.prefetch(type, id, enrich = false) }
    }
    // Look again whenever a prefetch lands, instead of polling.
    val prefetchVersion by MetaDetailsRepository.prefetchVersion.collectAsState()
    val fresh = remember(type, id, raw, prefetchVersion) { freshReleaseInfo(type, id, raw) }
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
