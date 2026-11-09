package com.imdadburki.aurelius.core.network

import com.imdadburki.aurelius.feature.youtube.buildYouTubePlayerHtml
import com.imdadburki.aurelius.feature.youtube.clampSeek
import com.imdadburki.aurelius.feature.youtube.formatPlayerTime
import com.imdadburki.aurelius.feature.youtube.playbackStalledMessage
import com.imdadburki.aurelius.feature.youtube.playerErrorMessage
import com.imdadburki.aurelius.feature.youtube.qualityLabel
import com.imdadburki.aurelius.feature.youtube.rateLabel
import com.imdadburki.aurelius.feature.youtube.shouldReportStall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.0.2: embeddability parsing (the Data API `status.embeddable` flag that
 * gates the player screen) and the plain-language player error messages.
 */
class YouTubeEmbedTest {

    private val videosJson = """
        {
          "items": [
            { "id": "lsQNP-VVBis",
              "contentDetails": { "duration": "PT2H33M13S" },
              "status": { "embeddable": true, "privacyStatus": "public" } },
            { "id": "blocked123",
              "contentDetails": { "duration": "PT4M28S" },
              "status": { "embeddable": false, "privacyStatus": "public" } },
            { "id": "nostatus1",
              "contentDetails": { "duration": "PT1M0S" } }
          ]
        }
    """.trimIndent()

    @Test
    fun embeddableFlagParsed() {
        val resp = parseYtVideosResponse(videosJson)
        assertEquals(3, resp.items.size)
        assertTrue(resp.items[0].status.embeddable)
        assertFalse(resp.items[1].status.embeddable)
    }

    @Test
    fun missingStatusDefaultsToEmbeddable() {
        val resp = parseYtVideosResponse(videosJson)
        // A video with no status block must not be blocked from playing.
        assertTrue(resp.items[2].status.embeddable)
    }

    @Test
    fun durationStillParsedAlongsideStatus() {
        val resp = parseYtVideosResponse(videosJson)
        assertEquals("PT2H33M13S", resp.items[0].contentDetails.duration)
        assertEquals(2 * 3600_000L + 33 * 60_000L + 13_000L, parseIso8601Duration(resp.items[0].contentDetails.duration))
    }

    @Test
    fun error101And150SayUploaderDisabledEmbedding() {
        assertTrue(playerErrorMessage(101).contains("uploader"))
        assertTrue(playerErrorMessage(150).contains("uploader"))
    }

    @Test
    fun error152MentionsYouTubeRefusal() {
        assertTrue(playerErrorMessage(152).contains("YouTube"))
    }

    @Test
    fun error100SaysNotFound() {
        assertTrue(playerErrorMessage(100).contains("not found"))
    }

    @Test
    fun unknownErrorKeepsTheCode() {
        assertTrue(playerErrorMessage(999).contains("999"))
    }

    @Test
    fun errorZeroIsTheLoadingTimeout() {
        assertTrue(playerErrorMessage(0).contains("too long"))
    }

    // ---- v2.0.4: the player page itself is pinned by tests, because the
    // script/host/origin combination broke in v2.0.1 (152) and v2.0.2 (404).

    @Test
    fun playerHtmlLoadsApiScriptFromYoutubeCom() {
        val html = buildYouTubePlayerHtml("cQq6iLF66Gc")
        assertTrue(html.contains("https://www.youtube.com/iframe_api"))
        assertFalse(html.contains("youtube-nocookie.com/iframe_api"))
    }

    @Test
    fun playerHtmlUsesNocookieHostOriginAndReferrer() {
        // v2.0.4: host, base URL and origin must ALL be nocookie, with
        // widget_referrer set (documented fix for errors 152/153).
        // v2.0.3 split host youtube.com + origin nocookie, no referrer.
        val html = buildYouTubePlayerHtml("cQq6iLF66Gc")
        assertTrue(html.contains("host: 'https://www.youtube-nocookie.com'"))
        assertTrue(html.contains("'origin': 'https://www.youtube-nocookie.com'"))
        assertTrue(html.contains("'widget_referrer': 'https://www.youtube-nocookie.com'"))
        assertFalse(html.contains("host: 'https://www.youtube.com'"))
    }

    @Test
    fun playerHtmlWatchdogIsGatedOnApiLoad() {
        // v2.0.4: no bare 25s timer that fires while the script is still
        // downloading on a slow connection. The outer guard is 120s (script
        // never loads) and the ready guard is 60s after the API is ready.
        val html = buildYouTubePlayerHtml("cQq6iLF66Gc")
        assertTrue(html.contains("__aureliusApiReady"))
        assertTrue(html.contains("120000"))
        assertTrue(html.contains("60000"))
        assertTrue(html.contains("AureliusBridge.onApiReady()"))
        assertFalse(html.contains("25000"))
    }

    @Test
    fun playerHtmlEmbedsTheRequestedVideoId() {
        val html = buildYouTubePlayerHtml("abc123XYZ")
        assertTrue(html.contains("videoId: 'abc123XYZ'"))
    }

    @Test
    fun playerHtmlPlaysExplicitlyOnReady() {
        // The autoplay parameter alone is not trusted: onReady must call
        // playVideo() (the documented IFrame pattern).
        val html = buildYouTubePlayerHtml("cQq6iLF66Gc")
        assertTrue(html.contains("e.target.playVideo()"))
    }

    @Test
    fun playerHtmlReportsStateChangesAndHasBothWatchdogs() {
        val html = buildYouTubePlayerHtml("cQq6iLF66Gc")
        assertTrue(html.contains("onStateChange"))
        assertTrue(html.contains("AureliusBridge.onPlayerStateChange"))
        assertTrue(html.contains("AureliusBridge.onPlayerError(0)"))
        assertTrue(html.contains("AureliusBridge.onPlaybackStalled()"))
    }

    @Test
    fun stallMessageOffersRetryAndYouTube() {
        val msg = playbackStalledMessage()
        assertTrue(msg.contains("Retry"))
        assertTrue(msg.contains("YouTube"))
    }

    private val searchJson = """
        {
          "items": [
            { "id": { "videoId": "cQq6iLF66Gc" },
              "snippet": {
                "title": "Alfaaz",
                "channelTitle": "Hamza Malik",
                "thumbnails": {
                  "default": { "url": "https://i.ytimg.com/vi/cQq6iLF66Gc/default.jpg" },
                  "medium": { "url": "https://i.ytimg.com/vi/cQq6iLF66Gc/mqdefault.jpg" },
                  "high": { "url": "https://i.ytimg.com/vi/cQq6iLF66Gc/hqdefault.jpg" }
                }
              } },
            { "id": { "videoId": "onlymedium" },
              "snippet": {
                "title": "No Default",
                "channelTitle": "X",
                "thumbnails": {
                  "medium": { "url": "https://i.ytimg.com/vi/onlymedium/mqdefault.jpg" }
                }
              } }
          ]
        }
    """.trimIndent()

    @Test
    fun listThumbnailPrefersDefaultSmallest() {
        val resp = parseYtSearchResponse(searchJson)
        val (small, large) = pickThumbnailUrls(resp.items[0].snippet.thumbnails)
        assertTrue(small.endsWith("default.jpg"))
        assertTrue(large.endsWith("mqdefault.jpg"))
    }

    @Test
    fun thumbnailFallsBackWhenDefaultMissing() {
        val resp = parseYtSearchResponse(searchJson)
        val (small, large) = pickThumbnailUrls(resp.items[1].snippet.thumbnails)
        assertTrue(small.endsWith("mqdefault.jpg"))
        assertTrue(large.endsWith("mqdefault.jpg"))
    }

    // ---- v2.0.5: native MX Player-style control bridge. The YouTube
    // player's own on-screen controls did not appear on the Y85A, so the
    // screen drives the player through __aureliusControl instead.

    @Test
    fun playerHtmlHasNativeControlBridge() {
        val html = buildYouTubePlayerHtml("cQq6iLF66Gc")
        assertTrue(html.contains("window.__aureliusControl"))
        assertTrue(html.contains("play: function()"))
        assertTrue(html.contains("pause: function()"))
        assertTrue(html.contains("seekTo: function(s)"))
        assertTrue(html.contains("seekBy: function(d)"))
        assertTrue(html.contains("setRate: function(r)"))
        assertTrue(html.contains("setQuality: function(q)"))
        assertTrue(html.contains("getState: function()"))
        assertTrue(html.contains("player.playVideo()"))
        assertTrue(html.contains("player.pauseVideo()"))
        assertTrue(html.contains("player.setPlaybackRate(r)"))
        assertTrue(html.contains("player.setPlaybackQuality(q)"))
        assertTrue(html.contains("player.getCurrentTime()"))
        assertTrue(html.contains("player.getDuration()"))
        assertTrue(html.contains("player.getAvailableQualityLevels()"))
    }

    @Test
    fun formatPlayerTimeZeroMinutesAndHours() {
        assertEquals("0:00", formatPlayerTime(0f))
        assertEquals("1:05", formatPlayerTime(65f))
        assertEquals("12:34", formatPlayerTime(754f))
        assertEquals("1:02:03", formatPlayerTime(3723f))
        assertEquals("0:00", formatPlayerTime(-5f))
    }

    @Test
    fun qualityLabelMapsKnownCodes() {
        assertEquals("144p", qualityLabel("tiny"))
        assertEquals("240p", qualityLabel("small"))
        assertEquals("360p", qualityLabel("medium"))
        assertEquals("480p", qualityLabel("large"))
        assertEquals("720p", qualityLabel("hd720"))
        assertEquals("1080p", qualityLabel("hd1080"))
        assertEquals("Auto", qualityLabel("auto"))
    }

    @Test
    fun qualityLabelPassesUnknownCodesThrough() {
        assertEquals("hd9999", qualityLabel("hd9999"))
    }

    @Test
    fun clampSeekKeepsInsideDuration() {
        assertEquals(30f, clampSeek(30f, 100f))
        assertEquals(0f, clampSeek(-10f, 100f))
        assertEquals(100f, clampSeek(150f, 100f))
        assertEquals(0f, clampSeek(5f, 0f))
    }

    @Test
    fun rateLabelFormatsWholeAndFractional() {
        assertEquals("1x", rateLabel(1f))
        assertEquals("2x", rateLabel(2f))
        assertEquals("0.5x", rateLabel(0.5f))
        assertEquals("1.25x", rateLabel(1.25f))
    }

    // v2.0.7: the stall rule — a PLAYING/BUFFERING player whose clock
    // has not advanced by 0.5s inside 30s is stalled. Tiny clock jitter
    // does not reset the window (the v2.0.6 exact-change rule never fired
    // on Appetize: state 1, displayed time 0:00 forever). Paused/ended/
    // unstarted freeze the clock legitimately and are never stalls.
    @Test
    fun stallRuleFiresWhenPlayingButClockBarelyMoves() {
        assertTrue(shouldReportStall(1, 0f, 0.04f, 30_001L))
        assertTrue(shouldReportStall(3, 0f, 0.1f, 45_000L))
        assertTrue(shouldReportStall(1, 120f, 120.2f, 31_000L))
    }

    @Test
    fun stallRuleDoesNotFireEarlyOrOnRealProgress() {
        assertFalse(shouldReportStall(1, 0f, 0.04f, 10_000L))
        assertFalse(shouldReportStall(1, 0f, 0.04f, 30_000L))
        assertFalse(shouldReportStall(1, 0f, 0.6f, 60_000L))
        assertFalse(shouldReportStall(3, 10f, 12f, 60_000L))
    }

    @Test
    fun stallRuleIgnoresPausedEndedAndUnstarted() {
        assertFalse(shouldReportStall(2, 0f, 0f, 60_000L))
        assertFalse(shouldReportStall(0, 0f, 0f, 60_000L))
        assertFalse(shouldReportStall(-1, 0f, 0f, 60_000L))
        assertFalse(shouldReportStall(5, 0f, 0f, 60_000L))
    }

    // v2.0.6: YouTube titles carry HTML entities — the player title bar
    // showed a literal &quot; on Appetize until this was added.
    @Test
    fun unescapeHtmlEntitiesDecodesCommonEntities() {
        assertEquals("\"Big Buck Bunny\" 60fps", unescapeHtmlEntities("&quot;Big Buck Bunny&quot; 60fps"))
        assertEquals("Tom & Jerry", unescapeHtmlEntities("Tom &amp; Jerry"))
        assertEquals("a < b > c", unescapeHtmlEntities("a &lt; b &gt; c"))
        assertEquals("it's", unescapeHtmlEntities("it&#39;s"))
        assertEquals("say \"hi\"", unescapeHtmlEntities("say &#34;hi&#34;"))
        assertEquals("plain title", unescapeHtmlEntities("plain title"))
    }

    @Test
    fun unescapeHtmlEntitiesDecodesOnlyOneLevel() {
        // &amp; is replaced last: &amp;quot; becomes the literal &quot;,
        // never a bare quote.
        assertEquals("&quot;", unescapeHtmlEntities("&amp;quot;"))
    }

    // v2.0.6: YouTube's own controls are off (the native bar is the
    // control surface, and the WebView touch listener owns every touch),
    // and the JS stall watchdog fires on a frozen clock whatever the
    // player state is.
    @Test
    fun playerHtmlDisablesYoutubeOwnControls() {
        val html = buildYouTubePlayerHtml("aqz-KE-bpKQ")
        assertTrue(html.contains("'controls': 0"))
    }

    @Test
    fun playerHtmlStallWatchdogFiresOnFrozenClockAlone() {
        val html = buildYouTubePlayerHtml("aqz-KE-bpKQ")
        // v2.0.7: fires unless the video has reached 1s of real playback
        // (a stalled player can report a tiny positive clock).
        assertTrue(html.contains("player.getCurrentTime() < 1"))
        assertFalse(html.contains("getPlayerState() !== 1"))
    }
}
