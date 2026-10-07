package com.nuvio.app.features.player.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class AddonSubtitleFontCacheTest {

    @Test
    fun `font files are recognised by their signature`() {
        assertEquals("otf", fontFileExtension("OTTO....".toByteArray()))
        assertEquals("ttf", fontFileExtension(byteArrayOf(0, 1, 0, 0, 0, 0x10)))
        assertEquals("ttc", fontFileExtension("ttcf....".toByteArray()))
        assertEquals("woff2", fontFileExtension("wOF2....".toByteArray()))
    }

    @Test
    fun `error pages and truncated files are not fonts`() {
        assertNull(fontFileExtension("<!DOCTYPE html>".toByteArray()))
        assertNull(fontFileExtension("{\"error\":1}".toByteArray()))
        assertNull(fontFileExtension(byteArrayOf(0, 1)))
    }

    @Test
    fun `a new api key reuses the same font folder`() {
        val oldKey = listOf("https://jf/Attachments/7?ApiKey=old", "https://jf/Attachments/8?ApiKey=old")
        val newKey = listOf("https://jf/Attachments/7?ApiKey=new", "https://jf/Attachments/8?ApiKey=new")
        assertEquals(fontSetKey(oldKey), fontSetKey(newKey))
        assertNotEquals(fontSetKey(oldKey), fontSetKey(oldKey.take(1)))
    }
}
