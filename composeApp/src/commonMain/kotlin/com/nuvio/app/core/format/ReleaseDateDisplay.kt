package com.nuvio.app.core.format

import androidx.compose.ui.text.intl.Locale
import com.nuvio.app.core.time.parseEpisodeReleaseLocalDate
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.release_year_present
import org.jetbrains.compose.resources.getString

fun formatReleaseDateForDisplay(raw: String): String = formatReleaseDate(raw, includeYear = true)

fun formatReleaseDateWithoutYear(raw: String): String = formatReleaseDate(raw, includeYear = false)

internal fun formatReleaseDate(
    raw: String,
    includeYear: Boolean,
    localeTag: String = Locale.current.toLanguageTag(),
): String {
    // A series that began and ended in the same year comes as "2024-2024"; show it once.
    sameYearRangeRegex.matchEntire(raw.trim())?.let { match ->
        if (match.groupValues[1] == match.groupValues[2]) return match.groupValues[1]
    }
    // Still running ("2026-"): a dangling dash reads as a cut-off label; say "2026 – Present".
    openYearRangeRegex.matchEntire(raw.trim())?.let { match -> return presentYearFormat.replace("%1\$s", match.groupValues[1]) }
    val date = parseEpisodeReleaseLocalDate(raw) ?: return raw
    return formatCalendarDate(date, localeTag, includeYear)
}

internal expect fun formatCalendarDate(isoDate: String, localeTag: String, includeYear: Boolean): String

/**
 * Parses a release/air string (ISO date, year-only, or timestamp prefix) for compact UI (e.g. year chips).
 */
fun extractReleaseYearForDisplay(raw: String): Int? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    if (t.length == 4 && t.all { it.isDigit() }) {
        return t.toIntOrNull()?.takeIf { it in 1000..9999 }
    }
    val datePart = parseEpisodeReleaseLocalDate(t) ?: return null
    val yearStr = datePart.split('-').firstOrNull() ?: return null
    return yearStr.toIntOrNull()?.takeIf { it in 1000..9999 }
}

private val sameYearRangeRegex = Regex("""(\d{4})\s*[-\u2013]\s*(\d{4})""")
private val openYearRangeRegex = Regex("""(\d{4})\s*[-\u2013]\s*""")

private val presentYearFormat: String by lazy { runBlocking { getString(Res.string.release_year_present) } }
