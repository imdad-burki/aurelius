import { spawn } from 'node:child_process';
const CHROME = '/home/hatch/workspace/tools/chrome-headless-shell/chrome-headless-shell';
const args = ['--headless=new', '--no-sandbox', '--remote-debugging-port=9224',
  '--user-data-dir=/tmp/ytprof_net', '--proxy-server=http://127.0.0.1:8898', '--proxy-bypass-list=127.0.0.1,localhost',
  '--ignore-certificate-errors', 'about:blank'];
const chrome = spawn(CHROME, args, { stdio: 'ignore' });
const sleep = ms => new Promise(r => setTimeout(r, ms));
async function getWsUrl() {
  for (let i = 0; i < 40; i++) {
    try { const r = await fetch('http://127.0.0.1:9224/json/version'); return (await r.json()).webSocketDebuggerUrl; }
    catch (e) { await sleep(500); }
  }
  throw new Error('no chrome');
}
let id = 0; const pending = new Map(); let ws;
function send(method, params = {}, sessionId) {
  return new Promise((resolve) => {
    const mid = ++id; pending.set(mid, { resolve });
    ws.send(JSON.stringify({ ...(sessionId ? { sessionId } : {}), id: mid, method, params }));
  });
}
ws = new WebSocket(await getWsUrl());
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
const netEvents = [];
ws.onmessage = ev => {
  const m = JSON.parse(ev.data);
  if (m.id && pending.has(m.id)) { pending.get(m.id).resolve(m.result); pending.delete(m.id); }
  if (m.method === 'Network.loadingFailed') netEvents.push('FAILED: ' + JSON.stringify(m.params));
  if (m.method === 'Network.responseReceived') netEvents.push('RESP: ' + m.params.response.url + ' -> ' + m.params.response.status);
};
const { targetId } = await send('Target.createTarget', { url: 'about:blank' });
const { sessionId } = await send('Target.attachToTarget', { targetId, flatten: true });
await send('Network.enable', {}, sessionId);
await send('Page.enable', {}, sessionId);
await send('Page.navigate', { url: 'https://www.youtube.com/iframe_api' }, sessionId);
await sleep(15000);
const body = await send('Runtime.evaluate', { expression: `document.body ? document.body.innerText.slice(0,120) : 'NO-BODY'`, returnByValue: true }, sessionId);
console.log('BODY:', JSON.stringify(body.result.value));
console.log('NET:', netEvents.slice(0, 8).join('\n'));
chrome.kill('SIGKILL');
process.exit(0);
