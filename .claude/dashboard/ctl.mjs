#!/usr/bin/env node
import { spawn, execFile } from 'node:child_process';
import { existsSync, readFileSync, writeFileSync, rmSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import http from 'node:http';

const here = dirname(fileURLToPath(import.meta.url));
const pidFile = join(here, 'server.pid');
const URL_ = 'http://localhost:4321';
const sleep = ms => new Promise(r => setTimeout(r, ms));

const alive = pid => { try { process.kill(pid, 0); return true; } catch { return false; } };
const readPid = () => { try { return parseInt(readFileSync(pidFile, 'utf8'), 10) || 0; } catch { return 0; } };
const ping = () => new Promise(r => {
  const q = http.get({ host: '127.0.0.1', port: 4321, path: '/api/state', timeout: 800 }, res => { res.resume(); r(true); });
  q.on('error', () => r(false)); q.on('timeout', () => { q.destroy(); r(false); });
});

const cmd = process.argv[2] || 'status';
const pid = readPid();
const running = (pid && alive(pid)) || await ping();

if (cmd === 'on') {
  if (running) { console.log(`Dashboard already running at ${URL_}`); process.exit(0); }
  const child = spawn(process.execPath, [join(here, 'server.mjs')], { detached: true, stdio: 'ignore' });
  child.unref();
  writeFileSync(pidFile, String(child.pid));
  let ok = false;
  for (let i = 0; i < 30 && !ok; i++) { await sleep(150); ok = await ping(); }
  if (!ok) { console.log('Dashboard failed to start'); process.exit(1); }
  execFile('open', [URL_], () => {});
  console.log(`Dashboard running at ${URL_}`);
} else if (cmd === 'off') {
  if (pid && alive(pid)) { try { process.kill(pid); } catch {} }
  rmSync(pidFile, { force: true });
  console.log('Dashboard stopped');
} else {
  console.log(running ? `Dashboard running at ${URL_}` : 'Dashboard is off');
}
