package com.nuvio.app.features.player

import com.nuvio.app.features.addons.encodeAddonPathSegment
import com.nuvio.app.features.streams.StreamBehaviorHints

/**
 * What the playing stream told us about its file. Stremio sends these to subtitle addons as the
 * request "extra" so they can match subtitles to the exact file (e.g. OpenSubtitles by hash, or a
 * media server picking the playing version and its sidecar tracks).
 */
data class PlayingFileHints(
    val filename: String? = null,
    val videoSize: Long? = null,
    val videoHash: String? = null,
)

internal fun StreamBehaviorHints.toPlayingFileHints(): PlayingFileHints? =
    PlayingFileHints(
        filename = filename?.trim()?.takeIf(String::isNotEmpty),
        videoSize = videoSize?.takeIf { it > 0 },
        videoHash = videoHash?.trim()?.takeIf(String::isNotEmpty),
    ).takeIf { it.filename != null || it.videoSize != null || it.videoHash != null }

/** Extra path segment for a subtitles request, e.g. `videoHash=…&videoSize=…&filename=…`. */
internal fun PlayingFileHints.toSubtitleExtraPathSegment(): String? =
    listOfNotNull(
        videoHash?.let { "videoHash=${it.encodeAddonPathSegment()}" },
        videoSize?.let { "videoSize=$it" },
        filename?.let { "filename=${it.encodeAddonPathSegment()}" },
    ).joinToString("&").takeIf(String::isNotEmpty)
