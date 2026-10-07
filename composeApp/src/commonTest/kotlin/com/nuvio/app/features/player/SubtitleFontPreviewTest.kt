package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubtitleFontPreviewTest {

    @Test
    fun `language codes and names map to their script`() {
        assertEquals(SubtitlePreviewScript.Thai, subtitlePreviewScriptForLanguage("th"))
        assertEquals(SubtitlePreviewScript.Thai, subtitlePreviewScriptForLanguage("tha"))
        assertEquals(SubtitlePreviewScript.Japanese, subtitlePreviewScriptForLanguage("ja"))
        assertEquals(SubtitlePreviewScript.Chinese, subtitlePreviewScriptForLanguage("zh-TW"))
        assertEquals(SubtitlePreviewScript.Cyrillic, subtitlePreviewScriptForLanguage("rus"))
    }

    @Test
    fun `latin languages and unknown values have no native preview line`() {
        assertNull(subtitlePreviewScriptForLanguage("en"))
        assertNull(subtitlePreviewScriptForLanguage("fr"))
        assertNull(subtitlePreviewScriptForLanguage(null))
        assertNull(subtitlePreviewScriptForLanguage(""))
    }

    @Test
    fun `player default previews the preferred language above english`() {
        assertEquals(
            "${SubtitlePreviewScript.Thai.sample}\n$SubtitlePreviewLatinSample",
            subtitleFontPreviewText(family = "", preferredLanguage = "th"),
        )
        assertEquals(SubtitlePreviewLatinSample, subtitleFontPreviewText(family = "", preferredLanguage = "en"))
    }
}
