package com.nuvio.app.features.player

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.SystemFont
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle

// AWT's logical aliases, not real installed families that libass could resolve.
private val javaLogicalFontFamilies = setOf("Dialog", "DialogInput", "Monospaced", "SansSerif", "Serif")

// Windows exposes non-regular weights as their own GDI family ("IBM Plex Sans Thai Medium").
// libass resolves those names, but Skia only knows the typographic family plus a weight.
private val gdiWeightSuffixes = listOf(
    "ExtraLight" to 200, "Extra Light" to 200, "UltraLight" to 200,
    "SemiLight" to 350, "Semi Light" to 350,
    "SemiBold" to 600, "Semi Bold" to 600, "DemiBold" to 600, "Demi" to 600,
    "ExtraBold" to 800, "Extra Bold" to 800, "UltraBold" to 800, "Ultra Bold" to 800,
    "Thin" to 100, "Light" to 300, "Medium" to 500, "Bold" to 700, "Black" to 900, "Heavy" to 900,
)

private val installedFamilies: List<String> by lazy {
    runCatching {
        GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ENGLISH).toList()
    }.getOrDefault(emptyList())
        .map(String::trim)
        .filter { it.isNotEmpty() && it !in javaLogicalFontFamilies && !it.startsWith("@") }
        .distinct()
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
}

actual fun installedSubtitleFontFamilies(): List<String> = installedFamilies

private class FontCoverage(val scripts: Set<SubtitlePreviewScript>, val hasLatin: Boolean)

private val coverageCache = ConcurrentHashMap<String, FontCoverage>()

private fun coverageOf(family: String): FontCoverage? {
    coverageCache[family]?.let { return it }
    // An unknown name silently becomes AWT's composite "Dialog" font, whose coverage is not the font's.
    val font = Font(family, Font.PLAIN, 12).takeIf { it.getFamily(Locale.ENGLISH).equals(family, ignoreCase = true) }
        ?: return null
    val coverage = FontCoverage(
        scripts = SubtitlePreviewScript.entries.filterTo(mutableSetOf()) { font.canDisplayUpTo(it.sample) == -1 },
        hasLatin = font.canDisplayUpTo(SubtitlePreviewLatinSample) == -1,
    )
    coverageCache[family] = coverage
    return coverage
}

// Scripts that only show up in fonts made for them, so glyph coverage alone identifies the font.
// Order matters for pan-CJK fonts: kana marks Japanese before Han-only falls through to Chinese.
private val distinctiveScripts = listOf(
    SubtitlePreviewScript.Thai,
    SubtitlePreviewScript.Korean,
    SubtitlePreviewScript.Japanese,
    SubtitlePreviewScript.Chinese,
    SubtitlePreviewScript.Devanagari,
)

// Western UI fonts (Arial, Segoe UI, ...) routinely carry these too, so coverage alone would make
// Arial preview in Russian; they need a name hint or a font without Latin glyphs.
private val bundledWithLatinScripts = listOf(
    SubtitlePreviewScript.Arabic,
    SubtitlePreviewScript.Hebrew,
    SubtitlePreviewScript.Cyrillic,
    SubtitlePreviewScript.Greek,
)

private val scriptNameHints = mapOf(
    SubtitlePreviewScript.Thai to Regex("(?i)thai|leelawadee|angsana|browallia|cordia|sarabun"),
    SubtitlePreviewScript.Japanese to Regex("(?i)\\bJP\\b|japan|mincho|meiryo|\\byu (gothic|mincho)|ud digi|biz ud|ms p?gothic"),
    SubtitlePreviewScript.Korean to Regex("(?i)\\bKR\\b|korean|malgun|gulim|dotum|batang|gungsuh"),
    SubtitlePreviewScript.Chinese to Regex("(?i)\\b(SC|TC|HK)\\b|chinese|yahei|jhenghei|simsun|simhei|kaiti|fangsong|mingliu|dengxian"),
    SubtitlePreviewScript.Devanagari to Regex("(?i)devanagari|mangal|aparajita|kokila|utsaah"),
    SubtitlePreviewScript.Arabic to Regex("(?i)arabic|naskh|kufi|sakkal|urdu|aldhabi|andalus"),
    SubtitlePreviewScript.Hebrew to Regex("(?i)hebrew|david|miriam|narkisim|gisha|levenim|aharoni|frankruehl"),
    SubtitlePreviewScript.Cyrillic to Regex("(?i)cyrillic"),
    SubtitlePreviewScript.Greek to Regex("(?i)greek"),
)

actual fun subtitleFontNativeScript(family: String, preferred: SubtitlePreviewScript?): SubtitlePreviewScript? {
    val name = family.trim()
    val coverage = runCatching { coverageOf(name) }.getOrNull() ?: return null
    val covered = coverage.scripts
    if (preferred != null && preferred in covered) return preferred
    scriptNameHints.entries.firstOrNull { (script, hint) -> script in covered && hint.containsMatchIn(name) }
        ?.let { return it.key }
    distinctiveScripts.firstOrNull { it in covered }?.let { return it }
    if (!coverage.hasLatin) return bundledWithLatinScripts.firstOrNull { it in covered }
    return null
}

private fun skiaKnowsFamily(name: String): Boolean =
    runCatching { FontMgr.default.matchFamilyStyle(name, FontStyle.NORMAL) != null }.getOrDefault(true)

@OptIn(ExperimentalTextApi::class)
actual fun subtitleFontPreviewFamily(family: String): FontFamily? {
    val name = family.trim().takeIf { it.isNotEmpty() } ?: return null
    if (skiaKnowsFamily(name)) return FontFamily(SystemFont(name))
    // GDI-style names append style words ("... Medium", "... Ultra Bold Condensed"); drop them
    // until Skia knows the family, then carry the weight they named. Width can't be expressed.
    val words = name.split(' ').filter(String::isNotEmpty)
    for (dropped in 1..minOf(3, words.size - 1)) {
        val base = words.dropLast(dropped).joinToString(" ")
        if (!skiaKnowsFamily(base)) continue
        val styleWords = words.takeLast(dropped).joinToString(" ")
        val weight = gdiWeightSuffixes.firstOrNull { (suffix, _) -> styleWords.contains(suffix, ignoreCase = true) }
            ?.second ?: FontWeight.Normal.weight
        return FontFamily(SystemFont(base, FontWeight(weight)))
    }
    return FontFamily(SystemFont(name))
}
