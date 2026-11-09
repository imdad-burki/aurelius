// CDP driver for the v2.0.5 native-control bridge: load a player test
// page, wait for PLAYING, then exercise window.__aureliusControl
// (pause / play / seekTo / setRate / setQuality / getState) and print
// each observed state. Usage: node control_test.mjs <page-url> <out-prefix>
import { spawn } from 'node:child_process';
import fs from 'node:fs';

const page = process.argv[2];
const outPrefix = process.argv[3] || 'ctrl';
const WAIT_MS = 35000;

const CHROME = '/home/hatch/workspace/tools/chrome-headless-shell/chrome-headless-shell';
const args = ['--headless=new', '--no-sandbox', '--disable-gpu-sandbox',
  '--remote-debugging-port=9224', '--user-data-dir=/tmp/ytprof_' + outPrefix,
  '--autoplay-policy=no-user-gesture-required', '--window-size=540,960',
  '--proxy-server=http://127.0.0.1:8898', '--proxy-bypass-list=127.0.0.1,localhost', '--ignore-certificate-errors',
  '--mute-audio', 'about:blank'];
const chrome = spawn(CHROME, args, { stdio: 'ignore' });
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function getWsUrl() {
  for (let i = 0; i < 40; i++) {
    try {
      const r = await fetch('http://127.0.0.1:9224/json/version');
      const j = await r.json();
      return j.webSocketDebuggerUrl;
    } catch (e) { await sleep(500); }
  }
  throw new Error('chrome did not start');
}

let id = 0;
const pending = new Map();
let ws;
function send(method, params = {}) {
  return new Promise((resolve, reject) => {
    const mid = ++id;
    pending.set(mid, { resolve, reject });
    ws.send(JSON.stringify({ id: mid, method, params }));
  });
}

const browserWs = await getWsUrl();
ws = new WebSocket(browserWs);
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
ws.onmessage = ev => {
  const m = JSON.parse(ev.data);
  if (m.id && pending.has(m.id)) { pending.get(m.id).resolve(m.result); pending.delete(m.id); }
};
const { targetId } = await send('Target.createTarget', { url: 'about:blank' });
const { sessionId } = await send('Target.attachToTarget', { targetId, flatten: true });
async function sess(method, params = {}) {
  return new Promise((resolve, reject) => {
    const mid = ++id;
    pending.set(mid, { resolve, reject });
    ws.send(JSON.stringify({ sessionId, id: mid, method, params }));
  });
}
async function evalJs(expr) {
  const r = await sess('Runtime.evaluate', { expression: expr, returnByValue: true });
  return r.result ? r.result.value : undefined;
}
await sess('Page.enable');
await sess('Runtime.enable');
await sess('Page.navigate', { url: page });
await sleep(WAIT_MS);
const probe = await evalJs(`document.getElementById('probe') ? document.getElementById('probe').textContent : 'NO-PROBE'`);
console.log('PROBE[' + outPrefix + ']:', probe);

async function state(label) {
  const s = await evalJs(`window.__aureliusControl ? window.__aureliusControl.getState() : 'NO-BRIDGE'`);
  console.log('CTRL[' + outPrefix + '][' + label + ']:', s);
  return s;
}
function parse(s) { try { return JSON.parse(s); } catch (e) { return null; } }

const s0 = parse(await state('initial'));
const playing = s0 && s0.state === 1;
console.log('CTRL[' + outPrefix + '][playing-at-start]:', playing);

if (playing) {
  await evalJs(`window.__aureliusControl.pause()`);
  await sleep(2000);
  const s1 = parse(await state('after-pause'));
  console.log('CTRL[' + outPrefix + '][pause-ok]:', s1 && s1.state === 2);
  await evalJs(`window.__aureliusControl.play()`);
  await sleep(2000);
  const s2 = parse(await state('after-play'));
  console.log('CTRL[' + outPrefix + '][play-ok]:', s2 && s2.state === 1);
  await evalJs(`window.__aureliusControl.seekTo(120)`);
  await sleep(2500);
  const s3 = parse(await state('after-seek-120'));
  console.log('CTRL[' + outPrefix + '][seek-ok]:', s3 && Math.abs(s3.time - 120) < 5, '(time=' + (s3 ? s3.time : '?') + ')');
  await evalJs(`window.__aureliusControl.setRate(2)`);
  await sleep(1500);
  const s4 = parse(await state('after-rate-2'));
  console.log('CTRL[' + outPrefix + '][rate-ok]:', s4 && s4.rate === 2);
  await evalJs(`window.__aureliusControl.setRate(1)`);
  await evalJs(`window.__aureliusControl.setQuality('small')`);
  await sleep(1500);
  const s5 = parse(await state('after-quality-small'));
  console.log('CTRL[' + outPrefix + '][qualities]:', s5 ? JSON.stringify(s5.qualities) : '?', 'reported-quality=' + (s5 ? s5.quality : '?'));
  await evalJs(`window.__aureliusControl.seekBy(-10)`);
  await sleep(1500);
  const s6 = parse(await state('after-seekby-minus10'));
  console.log('CTRL[' + outPrefix + '][seekby-ok]:', s6 && s6.time < (s5 ? s5.time : 9999));
} else {
  console.log('CTRL[' + outPrefix + '][SKIPPED]: player never reached PLAYING, bridge commands not exercised');
}
const shot = await sess('Page.captureScreenshot', { format: 'png' });
fs.writeFileSync(`/tmp/ytplayer_test/${outPrefix}.png`, Buffer.from(shot.data, 'base64'));
chrome.kill('SIGKILL');
process.exit(0);
