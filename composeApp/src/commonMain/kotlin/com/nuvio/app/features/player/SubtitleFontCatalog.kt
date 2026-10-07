package com.nuvio.app.features.player

import androidx.compose.ui.text.font.FontFamily

/**
 * Font families the subtitle renderer can pick from on this device, sorted by name.
 * Empty on platforms where the subtitle font is not user-selectable.
 */
expect fun installedSubtitleFontFamilies(): List<String>

/** Compose font used to preview [family] in settings, or null when it can't be resolved. */
expect fun subtitleFontPreviewFamily(family: String): FontFamily?

/**
 * The non-Latin script [family] is designed for, or null for Latin-only fonts. [preferred] wins
 * when the font covers it, so a pan-Unicode font previews in the viewer's own subtitle language.
 */
expect fun subtitleFontNativeScript(family: String, preferred: SubtitlePreviewScript?): SubtitlePreviewScript?

/** Scripts the font preview can show, each with a short native sample line. */
enum class SubtitlePreviewScript(val sample: String) {
    Thai("ตัวอย่างคำบรรยายภาษาไทย"),
    Japanese("日本語字幕のサンプル"),
    Korean("한국어 자막 미리보기"),
    // Characters shared by Simplified and Traditional so either kind of font can render it.
    Chinese("中文字幕示例"),
    Devanagari("उपशीर्षक पूर्वावलोकन"),
    Arabic("معاينة الترجمة"),
    Hebrew("תצוגה מקדימה של כתוביות"),
    Cyrillic("Пример субтитров"),
    Greek("Δείγμα υποτίτλων"),
}

internal const val SubtitlePreviewLatinSample = "Subtitle preview 0123"

/** Script used to write [language] (any form [normalizeLanguageCode] accepts), if previewable. */
fun subtitlePreviewScriptForLanguage(language: String?): SubtitlePreviewScript? =
    when (normalizeLanguageCode(language)?.substringBefore('-')) {
        "th" -> SubtitlePreviewScript.Thai
        "ja" -> SubtitlePreviewScript.Japanese
        "ko" -> SubtitlePreviewScript.Korean
        "zh" -> SubtitlePreviewScript.Chinese
        "hi", "mr", "ne", "sa" -> SubtitlePreviewScript.Devanagari
        "ar", "fa", "ur", "ps" -> SubtitlePreviewScript.Arabic
        "he", "yi" -> SubtitlePreviewScript.Hebrew
        "ru", "uk", "be", "bg", "sr", "mk", "kk", "mn" -> SubtitlePreviewScript.Cyrillic
        "el" -> SubtitlePreviewScript.Greek
        else -> null
    }

/**
 * Two-line preview, like a bilingual subtitle: the font's native script (if any) above English.
 * A blank [family] means the player default, which picks a font per script, so it previews the
 * viewer's preferred subtitle language.
 */
fun subtitleFontPreviewText(family: String, preferredLanguage: String?): String {
    val preferred = subtitlePreviewScriptForLanguage(preferredLanguage)
    val script = if (family.isBlank()) preferred else subtitleFontNativeScript(family, preferred)
    return listOfNotNull(script?.sample, SubtitlePreviewLatinSample).joinToString("\n")
}
