package com.nuvio.app.features.player

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.SystemFont
import java.awt.GraphicsEnvironment
import java.util.Locale

// AWT's logical aliases, not real installed families that libass could resolve.
private val javaLogicalFontFamilies = setOf("Dialog", "DialogInput", "Monospaced", "SansSerif", "Serif")

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

@OptIn(ExperimentalTextApi::class)
actual fun subtitleFontPreviewFamily(family: String): FontFamily? =
    family.trim().takeIf { it.isNotEmpty() }?.let { FontFamily(SystemFont(it)) }
