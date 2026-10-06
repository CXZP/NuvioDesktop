package com.nuvio.app.features.player

import androidx.compose.ui.text.font.FontFamily

/**
 * Font families the subtitle renderer can pick from on this device, sorted by name.
 * Empty on platforms where the subtitle font is not user-selectable.
 */
expect fun installedSubtitleFontFamilies(): List<String>

/** Compose font used to preview [family] in settings, or null when it can't be resolved. */
expect fun subtitleFontPreviewFamily(family: String): FontFamily?
