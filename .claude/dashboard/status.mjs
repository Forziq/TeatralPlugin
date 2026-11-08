#!/usr/bin/env node
// Agents report what they're doing in plain English for the dashboard.
// Usage: node .claude/dashboard/status.mjs <agent> "<status text>"
import { appendFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const [agent, ...rest] = process.argv.slice(2);
const text = rest.join(' ').replace(/\s+/g, ' ').trim().slice(0, 160);
if (!agent || !text) {
  console.error('Usage: status.mjs <agent> "<status text>"');
  process.exit(0);
}
mkdirSync(here, { recursive: true });
appendFileSync(join(here, 'events.jsonl'), JSON.stringify({ t: Date.now(), ev: 'status', agent, text }) + '\n');
console.log('ok');
