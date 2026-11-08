#!/usr/bin/env node
import { createServer } from 'node:http';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { buildState, trimEvents } from './lib.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..', '..');
const events = join(here, 'events.jsonl');
const PORT = 4321;

const server = createServer((req, res) => {
  const url = (req.url || '/').split('?')[0];
  try {
    if (url === '/api/state') {
      res.writeHead(200, { 'content-type': 'application/json', 'cache-control': 'no-store' });
      res.end(JSON.stringify(buildState(root)));
    } else if (url === '/' || url === '/index.html') {
      res.writeHead(200, { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store' });
      res.end(readFileSync(join(here, 'index.html')));
    } else { res.writeHead(404); res.end('not found'); }
  } catch (err) { res.writeHead(500); res.end(String(err)); }
});
server.on('error', err => { console.error(err.message); process.exit(1); });
server.listen(PORT, '127.0.0.1');
setInterval(() => trimEvents(events), 60_000).unref();
