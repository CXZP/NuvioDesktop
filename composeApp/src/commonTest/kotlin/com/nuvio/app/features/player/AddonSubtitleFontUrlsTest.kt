package com.nuvio.app.features.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class AddonSubtitleFontUrlsTest {

    private fun fontUrls(json: String) = Json.parseToJsonElement(json).jsonObject.fontUrls()

    @Test
    fun `fonts sent with an ASS subtitle are read in order`() {
        assertEquals(
            listOf("https://jf.example/Attachments/7?ApiKey=k", "https://jf.example/Attachments/8?ApiKey=k"),
            fontUrls(
                """{"url":"https://jf.example/Stream.ass","lang":"zho",
                   "fonts":["https://jf.example/Attachments/7?ApiKey=k","https://jf.example/Attachments/8?ApiKey=k"]}""",
            ),
        )
    }

    @Test
    fun `non http entries and duplicates are dropped`() {
        assertEquals(
            listOf("http://jf.local/a.ttf"),
            fontUrls("""{"fonts":["http://jf.local/a.ttf"," ","file:///c/evil.ttf",42,{"url":"x"},"http://jf.local/a.ttf"]}"""),
        )
    }

    @Test
    fun `subtitles without fonts have none`() {
        assertEquals(emptyList(), fontUrls("""{"url":"https://jf.example/Stream.srt"}"""))
        assertEquals(emptyList(), fontUrls("""{"fonts":"https://jf.example/a.ttf"}"""))
    }
}
