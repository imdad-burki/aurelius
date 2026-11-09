package com.imdadburki.aurelius.feature.youtube

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.imdadburki.aurelius.R
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.GoldButton
import com.imdadburki.aurelius.core.design.OutlineGoldButton
import com.imdadburki.aurelius.core.design.TopBar
import com.imdadburki.aurelius.core.network.YouTubeResult
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlin.math.abs

/** Plain-language player error text (unit-tested).
 * 101/150 = the uploader disabled embedding. 100 = not found/private.
 * 0 = the player never started (API script did not load within 120s, or
 * the player did not become ready within 60s after the API loaded) —
 * the watchdog reports it instead of spinning forever. 152 = YouTube
 * refusing an embed whose page claims to be youtube.com — fixed in
 * v2.0.2 by loading the page from youtube-nocookie with a real origin;
 * kept here as a safety net. */
fun playerErrorMessage(code: Int): String = when (code) {
    0 -> "The video is taking too long to load. Check your connection, or open it in YouTube to watch."
    101, 150 -> "This video can't play inside Aurelius — the uploader turned off embedding. Open it in YouTube to watch."
    152 -> "YouTube refused to play this video inside the app. Open it in YouTube to watch."
    100 -> "This video was not found or is private."
    else -> "The video could not play (error $code)."
}

/** Shown when the player became ready but the video never started playing
 * within 30s (unit-tested). On a slow connection the video may simply need
 * more time — Retry rebuilds the player; Open in YouTube always works. */
fun playbackStalledMessage(): String =
    "The video isn't starting. Your connection may be too slow — try Retry, or open it in YouTube to watch."

/** Player clock: 0:00, 1:05, 1:02:03 (unit-tested). */
fun formatPlayerTime(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "$m:${s.toString().padStart(2, '0')}"
    }
}

/** IFrame quality code -> display label (unit-tested). */
fun qualityLabel(code: String): String = when (code) {
    "tiny" -> "144p"
    "small" -> "240p"
    "medium" -> "360p"
    "large" -> "480p"
    "hd720" -> "720p"
    "hd1080" -> "1080p"
    "hd1440" -> "1440p"
    "hd2160" -> "2160p"
    "auto" -> "Auto"
    else -> code
}

/** Clamp a seek target into [0, duration] (unit-tested). */
fun clampSeek(target: Float, duration: Float): Float =
    target.coerceIn(0f, duration.coerceAtLeast(0f))

/** Speed button label: 1x, 0.5x, 1.25x (unit-tested). */
fun rateLabel(rate: Float): String =
    if (rate == rate.toInt().toFloat()) "${rate.toInt()}x" else "${rate}x"

/** Stall rule (unit-tested, v2.0.7): the video counts as stalled when the
 * player claims to be playing (1) or buffering (3) but the clock has not
 * advanced by at least 0.5s inside `elapsedInWindowMs`. Paused (2),
 * ended (0), unstarted (-1) and cued (5) freeze the clock legitimately
 * and are never stalls. v2.0.6 compared exact clock values, so a stalled
 * player whose clock jitters by tiny amounts (measured on Appetize:
 * state 1, displayed time 0:00 forever, yet no stall ever reported)
 * kept resetting the timer; the 0.5s progress bar is immune to that
 * jitter. The same 1s bar gates the poster and the JS watchdog: under
 * 1s of real playback the video has not started. */
fun shouldReportStall(
    playerState: Int,
    windowStartTime: Float,
    currentTime: Float,
    elapsedInWindowMs: Long,
    thresholdMs: Long = 30_000L
): Boolean =
    (playerState == 1 || playerState == 3) &&
        (currentTime - windowStartTime) < 0.5f &&
        elapsedInWindowMs > thresholdMs

/** Speeds offered in the speed menu (unit-tested via rateLabel). */
val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/**
 * The exact player page, as a pure function so unit tests pin the
 * load-bearing combination (script URL, host, origin) that broke in
 * v2.0.1 (error 152) and v2.0.2 (script 404).
 *
 * Measured 2026-11-08 in headless Chromium (real browser engine, same
 * HTML, same video): the page loads, YT is defined, onReady fires — and
 * the Alfaaz video (cQq6iLF66Gc) then fails with IFrame error 150 while
 * a control video (Big Buck Bunny) buffers and paints normally. The plain
 * nocookie embed of Alfaaz, with no API at all, renders YouTube's own
 * "This video is unavailable". So that video's owner/licensor blocks
 * embedding — the Data API `status.embeddable=true` does NOT reflect it,
 * and no app code can play that video in-app. The app must surface the
 * 101/150 explanation fast, with Open in YouTube — never a silent black
 * screen.
 *
 * The phone showed pure black (0.0% non-black pixels in the player area):
 * no spinner, no error, no YouTube UI. Three causes are addressed here:
 * (1) the WebView had no hardware layer and the manifest had no
 * hardware-acceleration flag — the classic black-video-surface failure,
 * fixed with setLayerType(HARDWARE) + manifest flags; (2) the black
 * WebView surface could cover the Compose loading/error overlays, so the
 * WebView is now removed from the composition entirely whenever an error
 * is showing; (3) nothing watched the real playback state, so a stall
 * was invisible — onStateChange is now reported and a 30s stall watchdog
 * raises a visible message with Retry instead of spinning forever.
 * onReady also calls playVideo() explicitly instead of trusting the
 * autoplay parameter alone (the documented IFrame pattern).
 *
 * v2.0.4 (2026-11-08): host is now nocookie too (host, base URL and
 * origin all match) with widget_referrer set, per the documented fix
 * for errors 152/153 — v2.0.3 split host youtube.com + origin nocookie
 * with no widget_referrer. The ready watchdog is now gated on the API
 * script actually loading: 120s outer guard if the script never loads,
 * 60s after the API is ready for the player to become ready. The old
 * 25s timer fired error 0 while the script was still downloading on a
 * slow connection (measured 1.5 KB/s on the Y85A) — the player was
 * killed before it had a chance.
 *
 * v2.0.5 (2026-11-08): `window.__aureliusControl` — Aurelius's own
 * MX Player-style control bridge. The YouTube IFrame player's own
 * on-screen controls did not appear on the Y85A, so the screen no
 * longer depends on them: the native Compose control bar drives the
 * player through this object (play/pause, seek, speed, quality, live
 * state). The video's original YouTube thumbnail is shown as the
 * poster until playback actually starts.
 */
fun buildYouTubePlayerHtml(videoId: String): String = """
    <html><head><style>
    html, body { height: 100%; margin: 0; padding: 0; background: #000; }
    #player { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
    </style></head><body>
    <div id="player"></div>
    <script src="https://www.youtube.com/iframe_api"></script>
    <script>
      window.__aureliusReady = false;
      window.__aureliusApiReady = false;
      // Outer guard: if the API script itself never loads (offline, or a
      // connection so slow the fetch cannot finish), report error 0 after
      // 120s instead of spinning forever. This must NOT fire while the
      // script is still downloading — v2.0.3's 25s timer did exactly that
      // on a slow connection and killed the player before it had a chance.
      setTimeout(function() {
        if (!window.__aureliusApiReady && !window.__aureliusReady) AureliusBridge.onPlayerError(0);
      }, 120000);
      var player;
      var stallTimer = null;
      // Native control bridge (v2.0.5): Aurelius's own MX Player-style
      // controls drive the player through this object, so the screen
      // never depends on YouTube's own on-screen controls appearing.
      window.__aureliusControl = {
        play: function() { if (player) player.playVideo(); },
        pause: function() { if (player) player.pauseVideo(); },
        seekTo: function(s) { if (player) player.seekTo(s, true); },
        seekBy: function(d) { if (player) player.seekTo(Math.max(0, player.getCurrentTime() + d), true); },
        setRate: function(r) { if (player) player.setPlaybackRate(r); },
        setQuality: function(q) { if (player) player.setPlaybackQuality(q); },
        getState: function() {
          if (!player) return null;
          try {
            return JSON.stringify({ state: player.getPlayerState(), time: player.getCurrentTime(), duration: player.getDuration(), rate: player.getPlaybackRate(), quality: player.getPlaybackQuality(), qualities: player.getAvailableQualityLevels() });
          } catch (e) { return null; }
        }
      };
      function armStallWatchdog() {
        if (stallTimer) clearTimeout(stallTimer);
        stallTimer = setTimeout(function() {
          try {
            // v2.0.7: fire unless the video has reached 1s of real
            // playback. v2.0.6 tested === 0, but a stalled player can
            // report a tiny positive clock (measured on Appetize:
            // state 1, displayed time 0:00 forever, no stall ever
            // reported) — under 1s the video has not started.
            if (player && player.getCurrentTime() < 1) {
              AureliusBridge.onPlaybackStalled();
            }
          } catch (e) { AureliusBridge.onPlaybackStalled(); }
        }, 30000);
      }
      function onYouTubeIframeAPIReady() {
        window.__aureliusApiReady = true;
        AureliusBridge.onApiReady();
        // Ready guard: once the API has loaded, the player has 60s to
        // become ready (player JS download + init).
        setTimeout(function() {
          if (!window.__aureliusReady) AureliusBridge.onPlayerError(0);
        }, 60000);
        player = new YT.Player('player', {
          host: 'https://www.youtube-nocookie.com',
          height: '100%', width: '100%',
          videoId: '$videoId',
          playerVars: { 'autoplay': 1, 'playsinline': 1, 'controls': 0, 'rel': 0, 'origin': 'https://www.youtube-nocookie.com', 'widget_referrer': 'https://www.youtube-nocookie.com' },
          events: {
            'onReady': function(e) { window.__aureliusReady = true; e.target.playVideo(); armStallWatchdog(); AureliusBridge.onPlayerReady(); },
            'onStateChange': function(e) { AureliusBridge.onPlayerStateChange(e.data); },
            'onError': function(e) { if (stallTimer) clearTimeout(stallTimer); AureliusBridge.onPlayerError(e.data); }
          }
        });
      }
    </script>
    </body></html>
    """.trimIndent()

/** WebChromeClient with fullscreen video support. The IFrame player's
 * fullscreen request goes through onShowCustomView — without these
 * overrides the request fails silently and the player can go black.
 * The custom view is attached to the activity's decor view and removed
 * again on hide (or when the WebView is destroyed — see onRelease). */
private class FullscreenChromeClient(
    private val activity: Activity?
) : WebChromeClient() {
    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null) {
            callback?.onCustomViewHidden()
            return
        }
        customView = view
        customViewCallback = callback
        val decor = activity?.window?.decorView as? ViewGroup ?: return
        decor.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onHideCustomView() {
        val view = customView ?: return
        (activity?.window?.decorView as? ViewGroup)?.removeView(view)
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
    }

    fun cleanup() {
        if (customView != null) onHideCustomView()
    }
}

/**
 * In-app YouTube playback via the official YouTube IFrame Player API embedded in
 * a WebView. This is the supported embed route: ads play, attribution and
 * playback controls are YouTube's own. Nothing is extracted or downloaded.
 *
 * Origin setup (measured 2026-11-08 — do not "simplify" this again):
 * - The page loads with base URL https://www.youtube-nocookie.com, so its
 *   origin is nocookie, NOT youtube.com. v2.0.1 used base URL youtube.com
 *   and YouTube refused every video with error 152.
 * - The API script MUST come from https://www.youtube.com/iframe_api
 *   (HTTP 200). v2.0.2 loaded it from youtube-nocookie.com/iframe_api,
 *   which returns HTTP 404 — YT was never defined, the player was never
 *   created, and the screen spun on "Loading video…" forever.
 * - Player host is https://www.youtube-nocookie.com (v2.0.4) with
 *   widget_referrer set, so host, base URL and origin all match; the
 *   origin playerVar matches the page origin (nocookie).
 *
 * Safety nets, so the screen can never sit on silent black again:
 * - JavaScript watchdogs report error 0 if the player never becomes
 *   ready: 120s if the API script never loads, 60s after the API has
 *   loaded. The old 25s timer fired while the script was still
 *   downloading on a slow connection.
 * - 30s stall watchdog reports a visible message if the player is ready
 *   but the video never starts playing.
 * - The WebView is removed from the composition whenever an error is
 *   showing, so no black surface can cover the error UI.
 * - The error state has a Retry button that rebuilds the WebView.
 *
 * v2.0.5 controls (MX Player-style, Imdad's order 2026-11-08): the
 * YouTube player's own on-screen controls did not appear on his phone,
 * so Aurelius now draws its OWN control bar over the video and drives
 * the player through `window.__aureliusControl` — play/pause, -10s/+10s,
 * seek bar with current/total time, speed menu (0.5x–2x), quality menu
 * (Auto + the levels YouTube reports), fullscreen, screen lock. Gestures
 * on the video area: tap toggles the bar, double-tap left/right seeks
 * -10s/+10s, hold plays at 2x until released, vertical swipe on the left
 * half changes brightness and on the right half changes volume — the
 * MX Player gesture set. The video's ORIGINAL YouTube thumbnail shows
 * as the poster until playback actually starts, so the screen is never
 * a bare black rectangle while loading. The error screen also names the
 * device's WebView version, so the next debug round can tell at a
 * glance whether the WebView itself is too old for YouTube's player.
 *
 * Videos whose uploader disabled embedding never reach the WebView: the
 * search result carries embeddable=false (Data API status) and the screen
 * goes straight to the explanation + "Open in YouTube". Note the Data API
 * flag is not perfect — a video can report embeddable=true and still be
 * refused by YouTube at play time (error 101/150, measured 2026-11-08
 * for Alfaaz cQq6iLF66Gc). That path also lands on the same explanation.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerScreen(result: YouTubeResult, onBack: () -> Unit, onOpenInYouTubeApp: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    var loading by remember { mutableStateOf(true) }
    var apiReady by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var stalled by remember { mutableStateOf(false) }
    var playerState by remember { mutableIntStateOf(-1) }
    var reloadToken by remember { mutableIntStateOf(0) }
    // v2.0.5 native-control state.
    var currentTime by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var rate by remember { mutableFloatStateOf(1f) }
    var quality by remember { mutableStateOf("auto") }
    var qualities by remember { mutableStateOf<List<String>>(emptyList()) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionToken by remember { mutableIntStateOf(0) }
    // v2.0.7: latched once the clock passes 1s of real playback. The
    // poster shows and the WebView stays invisible until then — a
    // stalled player never shows a bare black box, and seeking back to
    // 0:00 mid-video does not hide the video again.
    var hasStartedPlayback by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var seeking by remember { mutableStateOf(false) }
    var seekPreview by remember { mutableFloatStateOf(0f) }
    var hold2x by remember { mutableStateOf(false) }
    var prevRate by remember { mutableFloatStateOf(1f) }
    var flash by remember { mutableStateOf<String?>(null) }
    var gestureLabel by remember { mutableStateOf<String?>(null) }
    var indicatorToken by remember { mutableIntStateOf(0) }
    // Plain holder (not Compose state): the WebView reference is set in
    // the AndroidView factory, which runs during composition — writing
    // Compose state there is not allowed.
    val webViewHolder = remember { object { var webView: WebView? = null } }
    // Diagnostic for the next debug round: the device's WebView version
    // (an old WebView is a prime suspect when YouTube's player JS will
    // not render on the phone while it renders in modern Chromium).
    val webViewVersion = remember {
        WebView.getCurrentWebViewPackage()?.versionName ?: "unknown"
    }
    val blockedMessage = if (!result.embeddable) {
        "This video can't play inside Aurelius — the uploader turned off embedding. Open it in YouTube to watch."
    } else null
    val shownError = blockedMessage ?: error ?: if (stalled) playbackStalledMessage() else null
    val isPlaying = playerState == 1
    val posterUrl = result.thumbnailUrlLarge.ifEmpty { result.thumbnailUrl }

    fun js(cmd: String) {
        webViewHolder.webView?.evaluateJavascript(cmd, null)
    }

    fun poke() {
        interactionToken++
        controlsVisible = true
    }

    fun showFlash(text: String) {
        flash = text
        gestureLabel = null
        indicatorToken++
    }

    fun showGesture(text: String) {
        gestureLabel = text
        flash = null
        indicatorToken++
    }

    fun doSeekBy(seconds: Float) {
        js("window.__aureliusControl.seekBy($seconds)")
        showFlash(if (seconds > 0) "+10s" else "−10s")
        poke()
    }

    fun doPlayPause() {
        js(if (isPlaying) "window.__aureliusControl.pause()" else "window.__aureliusControl.play()")
        poke()
    }

    fun doSetRate(r: Float) {
        rate = r
        js("window.__aureliusControl.setRate($r)")
        poke()
    }

    fun doSetQuality(q: String) {
        quality = q
        js("window.__aureliusControl.setQuality('$q')")
        poke()
    }

    fun adjustBrightness(dir: Int) {
        val window = activity?.window ?: return
        val attrs = window.attributes
        val cur = if (attrs.screenBrightness < 0f) 0.5f else attrs.screenBrightness
        val next = (cur + dir * 0.05f).coerceIn(0.05f, 1f)
        attrs.screenBrightness = next
        window.attributes = attrs
        showGesture("Brightness ${(next * 100).toInt()}%")
        poke()
    }

    fun adjustVolume(dir: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (dir > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            0
        )
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val pct = am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max
        showGesture("Volume $pct%")
        poke()
    }

    fun setFullscreen(full: Boolean) {
        val window = activity?.window ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController ?: return
            if (full) {
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsets.Type.systemBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (full) {
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            } else {
                View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    fun resetForRetry() {
        loading = true
        apiReady = false
        error = null
        stalled = false
        playerState = -1
        currentTime = 0f
        duration = 0f
        rate = 1f
        quality = "auto"
        qualities = emptyList()
        hold2x = false
        locked = false
        controlsVisible = true
        hasStartedPlayback = false
        reloadToken++
    }

    // Auto-hide the control bar 3.5s after the last interaction.
    LaunchedEffect(controlsVisible, interactionToken) {
        if (controlsVisible && !locked) {
            delay(3500)
            controlsVisible = false
        }
    }
    // Clear the centre flash / gesture pill shortly after it shows.
    LaunchedEffect(indicatorToken) {
        if (flash != null || gestureLabel != null) {
            delay(1200)
            flash = null
            gestureLabel = null
        }
    }
    // Show the bar again whenever playback (re)starts.
    LaunchedEffect(playerState) {
        if (playerState == 1) poke()
    }
    // Poll the player's live state once a second while it is ready and
    // error-free, so the seek bar, time and speed/quality menus stay
    // true. Skips the time update while the user is dragging the seek
    // bar, so the thumb does not fight their finger.
    // v2.0.7: the poll also enforces the stall rule and latches real
    // playback. The stall window resets only on >= 0.5s of genuine
    // progress — v2.0.6 reset on ANY clock change, so a stalled player
    // whose clock jitters by tiny amounts never reported a stall.
    LaunchedEffect(loading, shownError, reloadToken) {
        var windowStartTime = -1f
        var windowStartMs = 0L
        while (!loading && shownError == null) {
            webViewHolder.webView?.evaluateJavascript(
                "window.__aureliusControl.getState()"
            ) { raw ->
                if (raw == null || raw == "null") return@evaluateJavascript
                try {
                    val cleaned = raw.removeSurrounding("\"").replace("\\\"", "\"")
                    val obj = JSONObject(cleaned)
                    val t = obj.optDouble("time", -1.0)
                    val d = obj.optDouble("duration", -1.0)
                    val r = obj.optDouble("rate", -1.0)
                    val q = obj.optString("quality", "")
                    if (!seeking && t >= 0) {
                        val now = SystemClock.uptimeMillis()
                        val tf = t.toFloat()
                        if (windowStartTime < 0f) {
                            windowStartTime = tf
                            windowStartMs = now
                        } else if (tf - windowStartTime >= 0.5f) {
                            windowStartTime = tf
                            windowStartMs = now
                        }
                        currentTime = tf
                        if (tf >= 1f) hasStartedPlayback = true
                        if (shouldReportStall(playerState, windowStartTime, tf, now - windowStartMs)) {
                            stalled = true
                        }
                    }
                    if (d > 0) duration = d.toFloat()
                    if (r > 0) rate = r.toFloat()
                    if (q.isNotEmpty()) quality = q
                    val arr = obj.optJSONArray("qualities")
                    if (arr != null) {
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) list.add(arr.optString(i))
                        if (list.isNotEmpty()) qualities = list
                    }
                } catch (_: Exception) {
                    // Next poll retries; a transient bad read is not an error.
                }
            }
            delay(1000)
        }
    }
    // Fullscreen: hide/show the system bars with the TopBar.
    LaunchedEffect(isFullscreen) { setFullscreen(isFullscreen) }
    DisposableEffect(Unit) { onDispose { setFullscreen(false) } }
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
        poke()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AureliusColors.Background)
    ) {
        if (!isFullscreen) TopBar(title = result.title, onBack = onBack)
        Box(modifier = Modifier.fillMaxSize()) {
            if (shownError == null && result.embeddable) {
                key(reloadToken) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.BLACK)
                                // v2.0.8: NO explicit layer type. v2.0.4
                                // forced a hardware layer on the theory
                                // that the video surface needs it — but on
                                // the Appetize device the player PLAYS
                                // (clock advances at 1x, state 1, bridge
                                // polling works) yet renders zero frames,
                                // and a forced hardware layer is a known
                                // suspect for breaking video compositing
                                // into a View that lives inside Compose.
                                // The manifest still enables hardware
                                // acceleration for the app, which is the
                                // flag that actually matters. If this does
                                // not change the emulator picture, the
                                // emulator's GPU-less rendering simply
                                // cannot composite WebView video at all.
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                webChromeClient = FullscreenChromeClient(ctx as? Activity)
                                webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        err: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            error = "The video could not load. Check your connection."
                                            loading = false
                                        }
                                    }
                                }
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onApiReady() {
                                        post { apiReady = true }
                                    }

                                    @JavascriptInterface
                                    fun onPlayerReady() {
                                        post { loading = false }
                                    }

                                    @JavascriptInterface
                                    fun onPlayerStateChange(state: Int) {
                                        post { playerState = state }
                                    }

                                    @JavascriptInterface
                                    fun onPlaybackStalled() {
                                        post {
                                            loading = false
                                            stalled = true
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onPlayerError(code: Int) {
                                        post {
                                            loading = false
                                            error = playerErrorMessage(code)
                                        }
                                    }
                                }, "AureliusBridge")
                                // v2.0.6: MX Player-style gestures live on
                                // the WebView itself. The Compose
                                // pointerInput Box that v2.0.5 used NEVER
                                // fired — an AndroidView child consumes
                                // touches before Compose's input system
                                // sees them (measured on Appetize: taps
                                // on the video did nothing, so the bar
                                // could never be recalled after auto-hide).
                                // The WebView needs no touches of its own
                                // (YouTube controls are off, controls=0),
                                // so this listener owns every touch: tap
                                // toggles the bar, double-tap left/right
                                // seeks -10s/+10s, hold plays 2x until
                                // release, vertical drag left = brightness,
                                // right = volume.
                                val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                                    private var dragStartX = 0f
                                    private var dragAcc = 0f

                                    override fun onDown(e: MotionEvent): Boolean {
                                        dragStartX = e.x
                                        dragAcc = 0f
                                        return true
                                    }

                                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                        post {
                                            // True toggle: poke() shows the
                                            // bar (and restarts the
                                            // auto-hide timer); hiding is a
                                            // plain flip. v2.0.6 did poke()
                                            // THEN flipped, so a hidden bar
                                            // stayed hidden — the tap could
                                            // only ever hide (measured on
                                            // Appetize: top bar returned,
                                            // bottom bar never did).
                                            if (controlsVisible) controlsVisible = false else poke()
                                        }
                                        return true
                                    }

                                    override fun onDoubleTap(e: MotionEvent): Boolean {
                                        post {
                                            if (e.x < width / 2f) doSeekBy(-10f)
                                            else doSeekBy(10f)
                                        }
                                        return true
                                    }

                                    override fun onLongPress(e: MotionEvent) {
                                        post {
                                            if (!hold2x) {
                                                prevRate = rate
                                                doSetRate(2f)
                                                hold2x = true
                                                showFlash("2x speed")
                                            }
                                        }
                                    }

                                    override fun onScroll(
                                        e1: MotionEvent?,
                                        e2: MotionEvent,
                                        distanceX: Float,
                                        distanceY: Float
                                    ): Boolean {
                                        if (e1 == null) return false
                                        // Vertical drags only — horizontal
                                        // drags are not a gesture here.
                                        if (abs(e2.y - e1.y) <= abs(e2.x - e1.x)) return false
                                        dragAcc += distanceY
                                        val step = 40f
                                        while (dragAcc <= -step || dragAcc >= step) {
                                            val dir = if (dragAcc < 0) 1 else -1
                                            dragAcc += if (dragAcc < 0) step else -step
                                            post {
                                                if (dragStartX < width / 2f) adjustBrightness(dir)
                                                else adjustVolume(dir)
                                            }
                                        }
                                        return true
                                    }
                                })
                                setOnTouchListener { _, event ->
                                    // Locked or error screen: let the
                                    // touch fall through to the WebView
                                    // (harmless — its controls are off).
                                    if (locked || blockedMessage != null || error != null || stalled) {
                                        return@setOnTouchListener false
                                    }
                                    gestureDetector.onTouchEvent(event)
                                    // Accessibility: a tap handled here
                                    // must still announce a click, or
                                    // TalkBack never hears it (lint
                                    // ClickableViewAccessibility).
                                    if (event.action == MotionEvent.ACTION_UP) performClick()
                                    if ((event.action == MotionEvent.ACTION_UP ||
                                            event.action == MotionEvent.ACTION_CANCEL) && hold2x
                                    ) {
                                        post {
                                            doSetRate(prevRate)
                                            hold2x = false
                                            flash = null
                                        }
                                    }
                                    true
                                }
                                loadDataWithBaseURL(
                                    "https://www.youtube-nocookie.com",
                                    buildYouTubePlayerHtml(result.videoId),
                                    "text/html", "utf-8", null
                                )
                                webViewHolder.webView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                            // v2.0.7: the WebView stays invisible until
                            // 1s of real playback has latched — before
                            // that it is a bare black box, and on a
                            // stalled device it would hide the poster
                            // forever (measured on Appetize: poster never
                            // showed once the player was ready). The
                            // player still loads and runs while hidden;
                            // the touch listener still receives taps.
                            .alpha(if (hasStartedPlayback) 1f else 0f),
                        onRelease = {
                            webViewHolder.webView = null
                            (it.webChromeClient as? FullscreenChromeClient)?.cleanup()
                            it.destroy()
                        }
                    )
                }
            }
            // Original YouTube thumbnail as the poster until 1s of real
            // playback has latched — never a bare black rectangle while
            // loading or stalled. (v2.0.6 hid it on any clock movement;
            // a stalled player can report a tiny positive clock, which
            // hid the poster and left black.)
            if (!hasStartedPlayback && shownError == null && posterUrl.isNotEmpty()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = result.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.aurelius_logo),
                    error = painterResource(R.drawable.aurelius_logo),
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (loading && shownError == null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = AureliusColors.GoldPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (apiReady) "Still loading — slow connection, please wait…"
                        else "Loading video…",
                        style = AureliusType.Body
                    )
                }
            }
            // v2.0.6: the gesture Box that used to live here is gone —
            // its Compose pointerInput never fired (the AndroidView
            // WebView consumes touches first). Gestures now live on the
            // WebView's own OnTouchListener above.
            // Native control bar (MX Player-style), auto-hiding.
            if (controlsVisible && !locked && shownError == null) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatPlayerTime(if (seeking) seekPreview else currentTime),
                            style = AureliusType.Body,
                            color = AureliusColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(52.dp)
                        )
                        Slider(
                            value = (if (seeking) seekPreview else currentTime)
                                .coerceIn(0f, duration.coerceAtLeast(1f)),
                            onValueChange = {
                                seeking = true
                                seekPreview = it
                            },
                            onValueChangeFinished = {
                                js("window.__aureliusControl.seekTo($seekPreview)")
                                currentTime = seekPreview
                                seeking = false
                                poke()
                            },
                            valueRange = 0f..duration.coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = AureliusColors.GoldPrimary,
                                activeTrackColor = AureliusColors.GoldPrimary,
                                inactiveTrackColor = AureliusColors.Border
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatPlayerTime(duration),
                            style = AureliusType.Body,
                            color = AureliusColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(52.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { doSeekBy(-10f) }) {
                            Icon(
                                Icons.Default.Replay10,
                                "Back 10 seconds",
                                tint = AureliusColors.TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = { doPlayPause() }) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isPlaying) "Pause" else "Play",
                                tint = AureliusColors.GoldPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        IconButton(onClick = { doSeekBy(10f) }) {
                            Icon(
                                Icons.Default.Forward10,
                                "Forward 10 seconds",
                                tint = AureliusColors.TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Box {
                            TextButton(onClick = { showSpeedMenu = true }) {
                                Text(
                                    rateLabel(rate),
                                    style = AureliusType.Title,
                                    color = AureliusColors.TextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                SPEED_OPTIONS.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(rateLabel(option), style = AureliusType.Body) },
                                        onClick = {
                                            showSpeedMenu = false
                                            doSetRate(option)
                                        }
                                    )
                                }
                            }
                        }
                        Box {
                            TextButton(onClick = { showQualityMenu = true }) {
                                Text(
                                    qualityLabel(quality),
                                    style = AureliusType.Title,
                                    color = AureliusColors.TextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showQualityMenu,
                                onDismissRequest = { showQualityMenu = false }
                            ) {
                                val options =
                                    if (qualities.isEmpty()) {
                                        listOf("auto", "tiny", "small", "medium", "large", "hd720", "hd1080")
                                    } else {
                                        (listOf("auto") + qualities).distinct()
                                    }
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(qualityLabel(option), style = AureliusType.Body) },
                                        onClick = {
                                            showQualityMenu = false
                                            doSetQuality(option)
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { isFullscreen = !isFullscreen }) {
                            Icon(
                                if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                                tint = AureliusColors.TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = { locked = true; poke() }) {
                            Icon(
                                Icons.Default.Lock,
                                "Lock screen",
                                tint = AureliusColors.TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            // Centre flash pill: +10s / −10s / 2x speed / brightness / volume.
            val pillText = flash ?: gestureLabel
            if (pillText != null && shownError == null) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(pillText, style = AureliusType.Title, color = AureliusColors.GoldPrimary)
                }
            }
            // Locked: only the unlock button stays tappable.
            if (locked && shownError == null) {
                IconButton(
                    onClick = { locked = false; poke() },
                    modifier = Modifier.align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Default.Lock, "Unlock", tint = AureliusColors.GoldPrimary)
                }
            }
            if (shownError != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(AureliusColors.Background)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(shownError, style = AureliusType.Body, color = AureliusColors.Error, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        GoldButton("Open in YouTube", onOpenInYouTubeApp, Modifier.fillMaxWidth(0.8f))
                        Spacer(Modifier.height(12.dp))
                        if (blockedMessage == null) {
                            OutlineGoldButton("Retry", { resetForRetry() }, Modifier.fillMaxWidth(0.8f))
                            Spacer(Modifier.height(12.dp))
                        }
                        OutlineGoldButton("Go back", onBack, Modifier.fillMaxWidth(0.8f))
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "WebView $webViewVersion",
                            style = AureliusType.Body,
                            color = AureliusColors.TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
