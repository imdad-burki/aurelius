"""Generate headless-test pages from the SHIPPED Kotlin player HTML.
Only the origin is swapped to match the local serving origin (the app
uses base-URL nocookie as its origin; the test page is served from
127.0.0.1:8931, so its origin must be that to replicate the matched
condition). Everything else is byte-identical to buildYouTubePlayerHtml.
A stub AureliusBridge records every bridge call for the probe.

v2.0.5: the stub now includes onApiReady (the template calls it first
thing in onYouTubeIframeAPIReady — without it the function throws and
the player is never created), and the assertions pin the nocookie host
and the __aureliusControl native-control bridge.

v2.0.6: the template now has controls=0 and a stall watchdog that fires
on a frozen clock whatever the state is; pages are generated as v206.

v2.0.8: the watchdog fires unless the video has reached 1s of real
playback (getCurrentTime() < 1); pages are generated as v207.
ALWAYS regenerate from the current template before a headless pass —
the v2.0.4 "4x" runs used stale v2.0.3-era pages (see AGENTS.md).
"""
import re

kt = open('/home/hatch/workspace/aurelius-v2/app/src/main/java/com/imdadburki/aurelius/feature/youtube/YouTubePlayerScreen.kt').read()
m = re.search(r'fun buildYouTubePlayerHtml\(videoId: String\): String = """(.*?)"""\.trimIndent\(\)', kt, re.S)
assert m, "template not found"
tpl = m.group(1)

STUB = '''<script>
  window.__bridge = { ready: false, apiReady: false, errors: [], states: [], stalled: false };
  window.AureliusBridge = {
    onApiReady: function() { window.__bridge.apiReady = true; },
    onPlayerReady: function() { window.__bridge.ready = true; },
    onPlayerError: function(c) { window.__bridge.errors.push(c); },
    onPlayerStateChange: function(s) { window.__bridge.states.push(s); },
    onPlaybackStalled: function() { window.__bridge.stalled = true; }
  };
</script>'''

PROBE = '''<div id="probe" style="position:absolute;left:0;bottom:0;color:#0f0;font-size:10px;z-index:9;">INIT</div>
<script>
  setInterval(function() {
    var s = -9, t = -1;
    try { if (player) { s = player.getPlayerState(); t = player.getCurrentTime(); } } catch(e) {}
    var b = window.__bridge;
    document.getElementById('probe').textContent =
      'READY=' + b.ready + ' APIREADY=' + b.apiReady + ' STATE=' + s + ' TIME=' + t +
      ' ERR=' + (b.errors.length ? b.errors.join(',') : 'none') +
      ' STALLED=' + b.stalled + ' STATES=' + b.states.join(',');
  }, 1000);
</script>'''

def make(video_id, origin, out):
    html = tpl.replace('$videoId', video_id)
    html = html.replace("'origin': 'https://www.youtube-nocookie.com'", "'origin': '%s'" % origin)
    html = html.replace('<div id="player"></div>', '<div id="player"></div>' + STUB, 1)
    html = html.replace('</body>', PROBE + '</body>', 1)
    # verify the shipped load-bearing lines survived
    for needle in ['https://www.youtube.com/iframe_api', "host: 'https://www.youtube-nocookie.com'",
                   'e.target.playVideo()', 'onPlaybackStalled()', 'onPlayerError(0)',
                   'window.__aureliusControl', 'AureliusBridge.onApiReady()']:
        assert needle in html, 'missing: ' + needle
    open(out, 'w').write(html)
    print('wrote', out)

make('cQq6iLF66Gc', 'http://127.0.0.1:8931', '/tmp/ytplayer_test/v208_alfaaz.html')
make('aqz-KE-bpKQ', 'http://127.0.0.1:8931', '/tmp/ytplayer_test/v208_control.html')
