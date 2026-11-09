// CDP driver: load a player test page, wait, report player state + screenshot.
import { spawn } from 'node:child_process';
import fs from 'node:fs';

const page = process.argv[2];
const ua = process.argv[3] || '';
const outPrefix = process.argv[4] || 'run';
const WAIT_MS = 35000;

const CHROME = '/home/hatch/workspace/tools/chrome-headless-shell/chrome-headless-shell';
const args = ['--headless=new', '--no-sandbox', '--disable-gpu-sandbox',
  '--remote-debugging-port=9223', '--user-data-dir=/tmp/ytprof_' + outPrefix,
  '--autoplay-policy=no-user-gesture-required', '--window-size=540,960',
  '--proxy-server=http://127.0.0.1:8898', '--proxy-bypass-list=127.0.0.1,localhost', '--ignore-certificate-errors',
  '--mute-audio', 'about:blank'];
if (ua) args.push('--user-agent=' + ua);
const chrome = spawn(CHROME, args, { stdio: 'ignore' });
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function getWsUrl() {
  for (let i = 0; i < 40; i++) {
    try {
      const r = await fetch('http://127.0.0.1:9223/json/version');
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
await sess('Page.enable');
await sess('Runtime.enable');
await sess('Page.navigate', { url: page });
await sleep(WAIT_MS);
const probe = await sess('Runtime.evaluate', { expression: `document.getElementById('probe') ? document.getElementById('probe').textContent : 'NO-PROBE'`, returnByValue: true });
const shot = await sess('Page.captureScreenshot', { format: 'png' });
fs.writeFileSync(`/tmp/ytplayer_test/${outPrefix}.png`, Buffer.from(shot.data, 'base64'));
console.log('PROBE[' + outPrefix + ']:', probe.result.value);
chrome.kill('SIGKILL');
process.exit(0);
