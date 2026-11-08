#!/usr/bin/env node
// Updates task statuses in progress.md so no model has to rewrite the table.
// Usage:
//   node scripts/task.mjs start <id>
//   node scripts/task.mjs done <id>
//   node scripts/task.mjs block <id> "<reason>"
//   node scripts/task.mjs gate <id> pass|fail
//   node scripts/task.mjs next            (prints the next task row)
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const file = join(root, 'progress.md');
const [cmd, id, arg] = process.argv.slice(2);

const text = readFileSync(file, 'utf8');
const lines = text.split('\n');
const rowRe = /^([A-Z]\d+[a-z]?)\|([^|]*)\|([^|]*)\|([^|]*)\|([^|]*)\|(.*)$/;

const rows = [];
lines.forEach((line, i) => {
  const m = line.match(rowRe);
  if (m) rows.push({ i, id: m[1], size: m[2], who: m[3], deps: m[4], status: m[5], title: m[6] });
});
const byId = Object.fromEntries(rows.map(r => [r.id, r]));
const write = r => { lines[r.i] = [r.id, r.size, r.who, r.deps, r.status, r.title].join('|'); };

function nextRow() {
  return rows.find(r => r.status !== 'done' &&
    (r.deps === '-' || r.deps.split(',').every(d => byId[d.trim()]?.status === 'done')));
}
function gateState(g) {
  const r = byId[g];
  if (!r) return '-';
  return r.status === 'done' ? 'passed' : r.status === 'failed' ? 'failed' : 'todo';
}
function refreshHeader() {
  const n = nextRow();
  const now = n ? `${n.id} ${n.status}` : 'all done';
  const ni = lines.findIndex(l => l.startsWith('now:'));
  if (ni >= 0) lines[ni] = `now: ${now} | G1 ${gateState('G1')} | G2 ${gateState('G2')}`;
  const ui = lines.findIndex(l => l.startsWith('updated:'));
  if (ui >= 0) lines[ui] = `updated: ${new Date().toISOString().slice(0, 10)}`;
}
function setBlocker(rid, reason) {
  const bi = lines.findIndex(l => l.trim() === '## blockers');
  if (bi < 0) return;
  let end = bi + 1;
  while (end < lines.length && !lines[end].startsWith('## ')) end++;
  let items = lines.slice(bi + 1, end).filter(l => l.startsWith('- ') && !l.startsWith(`- ${rid}:`));
  if (reason) items.push(`- ${rid}: ${reason}`);
  const body = items.length ? items : ['none'];
  lines.splice(bi + 1, end - bi - 1, ...body, '');
}

function need() {
  if (!id || !byId[id]) { console.error(`Unknown task id: ${id ?? '(none)'}`); process.exit(1); }
  return byId[id];
}

switch (cmd) {
  case 'start': { const r = need(); r.status = 'doing'; write(r); setBlocker(r.id, null); break; }
  case 'done': { const r = need(); r.status = 'done'; write(r); setBlocker(r.id, null); break; }
  case 'block': { const r = need(); r.status = 'blocked'; write(r); setBlocker(r.id, arg || 'no reason given'); break; }
  case 'gate': {
    const r = need();
    if (arg !== 'pass' && arg !== 'fail') { console.error('Use: gate <id> pass|fail'); process.exit(1); }
    r.status = arg === 'pass' ? 'done' : 'failed'; write(r); break;
  }
  case 'next': {
    const n = nextRow();
    console.log(n ? [n.id, n.size, n.who, n.deps, n.status, n.title].join('|') : 'all done');
    process.exit(0);
  }
  default:
    console.error('Usage: task.mjs start|done|block|gate|next <id> [arg]'); process.exit(1);
}
refreshHeader();
writeFileSync(file, lines.join('\n'));
const n = nextRow();
console.log(`${id} -> ${byId[id].status}. Next: ${n ? `${n.id} ${n.title}` : 'nothing left'}`);
