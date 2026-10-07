package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals

class AddonSubtitleTitleTest {

    @Test
    fun `track label is appended to the language`() {
        assertEquals(
            "English · SDH - Hearing Impaired - SUBRIP",
            addonSubtitleTitle("English", "SDH - Hearing Impaired - SUBRIP"),
        )
    }

    @Test
    fun `missing blank or repeated labels leave the language alone`() {
        assertEquals("Thai", addonSubtitleTitle("Thai", null))
        assertEquals("Thai", addonSubtitleTitle("Thai", "  "))
        assertEquals("Thai", addonSubtitleTitle("Thai", "thai"))
    }
}
