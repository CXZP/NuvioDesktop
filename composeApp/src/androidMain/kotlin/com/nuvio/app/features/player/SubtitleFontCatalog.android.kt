package com.nuvio.app.features.player

import androidx.compose.ui.text.font.FontFamily

actual fun installedSubtitleFontFamilies(): List<String> = emptyList()

actual fun subtitleFontPreviewFamily(family: String): FontFamily? = null

actual fun subtitleFontNativeScript(family: String, preferred: SubtitlePreviewScript?): SubtitlePreviewScript? = preferred
