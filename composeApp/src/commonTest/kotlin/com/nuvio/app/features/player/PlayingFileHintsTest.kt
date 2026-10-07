package com.nuvio.app.features.player

import com.nuvio.app.features.streams.StreamBehaviorHints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayingFileHintsTest {

    @Test
    fun `behavior hints map to playing file hints`() {
        val hints = StreamBehaviorHints(
            filename = "Project.Hail.Mary.2026.2160p.mkv",
            videoSize = 25_055_439_307,
            videoHash = "6000c989522d12d1",
        ).toPlayingFileHints()

        assertEquals(PlayingFileHints("Project.Hail.Mary.2026.2160p.mkv", 25_055_439_307, "6000c989522d12d1"), hints)
    }

    @Test
    fun `behavior hints without file identity give no hints`() {
        assertNull(StreamBehaviorHints(bingeGroup = "group", filename = "  ", videoSize = 0).toPlayingFileHints())
    }

    @Test
    fun `extra segment follows the stremio subtitles extra format`() {
        val segment = PlayingFileHints(
            filename = "Movie (2026) [4K] & More.mkv",
            videoSize = 1234,
            videoHash = "abcdef0123456789",
        ).toSubtitleExtraPathSegment()

        assertEquals(
            "videoHash=abcdef0123456789&videoSize=1234&filename=Movie%20%282026%29%20%5B4K%5D%20%26%20More.mkv",
            segment,
        )
    }

    @Test
    fun `extra segment omits missing fields`() {
        assertEquals("videoSize=42", PlayingFileHints(videoSize = 42).toSubtitleExtraPathSegment())
        assertNull(PlayingFileHints().toSubtitleExtraPathSegment())
    }
}
