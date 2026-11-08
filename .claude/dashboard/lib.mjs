// Pure logic for the dashboard: event attribution and progress.md parsing. No I/O except buildState.
import { readFileSync, existsSync, statSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

export const AGENTS = ['main', 'opus-deep', 'sonnet-builder', 'sonnet-tester', 'haiku-scout'];
export const ACTIVE_MS = 90_000;
const STACK_TTL = 30 * 60_000;
const isSpawn = e => e.tool === 'Agent' || e.tool === 'Task';

// Returns events with an `owner` field added.
export function attribute(events) {
  const stack = []; // {name, t}
  const out = [];
  for (const e of events) {
    while (stack.length && e.t - stack[0].t > STACK_TTL) stack.shift();
    const top = () => (stack.length ? stack[stack.length - 1].name : 'main');
    const popName = name => {
      for (let i = stack.length - 1; i >= 0; i--) if (stack[i].name === name) { stack.splice(i, 1); return; }
    };
    let owner;
    if (e.ev === 'PreToolUse' && isSpawn(e)) {
      owner = e.agent || top();
      if (e.sub) stack.push({ name: e.sub, t: e.t });
    } else if (e.ev === 'PostToolUse' && isSpawn(e)) {
      if (e.sub) popName(e.sub);
      owner = e.agent || top();
    } else if (e.ev === 'SubagentStop') {
      owner = e.sub || e.agent || top();
      if (e.sub) popName(e.sub);
    } else if (e.ev === 'SubagentStart') {
      owner = e.sub || e.agent || top();
    } else {
      owner = e.agent || top();
    }
    out.push({ ...e, owner });
  }
  return out;
}

export function summarize(events, now = Date.now()) {
  const att = attribute(events);
  const agents = {};
  for (const name of AGENTS) agents[name] = { name, status: null, statusT: null, active: false, tool: null, actions: [], lastT: null, ago: null };
  const stopped = {};
  const pending = {};
  for (const e of att) {
    const a = agents[e.owner];
    if (!a) continue;
    a.lastT = e.t;
    if (e.ev === 'SubagentStop' || (e.ev === 'Stop' && e.owner === 'main')) { stopped[e.owner] = true; pending[e.owner] = null; continue; }
    stopped[e.owner] = false;
    if (e.ev === 'status') { a.status = e.text; a.statusT = e.t; a.actions.push({ t: e.t, text: e.text, kind: 'status' }); }
    else if (e.ev === 'PreToolUse') { pending[e.owner] = e.text; a.actions.push({ t: e.t, text: e.text, kind: 'tool' }); }
    else if (e.ev === 'PostToolUse' || e.ev === 'PostToolUseFailure') pending[e.owner] = null;
    else if (e.ev === 'UserPromptSubmit') a.actions.push({ t: e.t, text: e.text, kind: 'prompt' });
  }
  for (const a of Object.values(agents)) {
    a.actions = a.actions.slice(-8).reverse();
    a.tool = pending[a.name] || null;
    a.ago = a.lastT ? Math.max(0, Math.round((now - a.lastT) / 1000)) : null;
    a.active = !!a.lastT && now - a.lastT < ACTIVE_MS && !stopped[a.name];
  }
  const feed = att.filter(e => ['status', 'PreToolUse', 'UserPromptSubmit', 'SubagentStart', 'SubagentStop'].includes(e.ev))
    .slice(-40).reverse().map(e => ({ t: e.t, agent: e.owner, text: e.text || e.ev, kind: e.ev === 'status' ? 'status' : 'tool' }));
  return { agents: AGENTS.map(n => agents[n]), feed };
}

export function parseProgress(md) {
  const res = { now: '', updated: '', current: null, next: [], done: 0, total: 0, gates: [], blockers: [], tasks: [] };
  const lines = md.split('\n');
  for (const l of lines) {
    if (l.startsWith('now:')) res.now = l.slice(4).trim();
    else if (l.startsWith('updated:')) res.updated = l.slice(8).trim();
  }
  for (const l of lines) {
    const p = l.split('|');
    if (p.length === 6 && p[0] !== 'id' && /^[A-Z]+\d+$/.test(p[0].trim())) {
      res.tasks.push({ id: p[0].trim(), size: p[1].trim(), who: p[2].trim(), deps: p[3].trim(), status: p[4].trim(), title: p[5].trim() });
    }
  }
  res.total = res.tasks.length;
  res.done = res.tasks.filter(t => t.status === 'done').length;
  const nowId = res.now.split('|')[0].trim().split(/\s+/)[0];
  res.current = res.tasks.find(t => t.status === 'doing') || res.tasks.find(t => t.id === nowId) || null;
  res.next = res.tasks.filter(t => t.status === 'todo').slice(0, 3);
  res.gates = res.now.split('|').slice(1).map(s => s.trim()).filter(Boolean).map(s => { const [id, ...r] = s.split(/\s+/); return { id, state: r.join(' ') }; });
  const bi = lines.findIndex(l => /^##\s+blockers/i.test(l));
  if (bi >= 0) {
    for (const l of lines.slice(bi + 1)) {
      if (l.startsWith('## ')) break;
      const s = l.replace(/^[-*]\s*/, '').trim();
      if (s && s.toLowerCase() !== 'none') res.blockers.push(s);
    }
  }
  return res;
}

export function readEvents(file, maxLines = 2000) {
  if (!existsSync(file)) return [];
  const out = [];
  for (const l of readFileSync(file, 'utf8').split('\n').slice(-maxLines)) {
    if (!l.trim()) continue;
    try { out.push(JSON.parse(l)); } catch {}
  }
  return out;
}

export function trimEvents(file, maxBytes = 5 * 1024 * 1024, keep = 3000) {
  try {
    if (statSync(file).size > maxBytes) {
      const all = readFileSync(file, 'utf8').trim().split('\n');
      writeFileSync(file, all.slice(-keep).join('\n') + '\n');
    }
  } catch {}
}

export function buildState(root, now = Date.now()) {
  const dash = join(root, '.claude', 'dashboard');
  const prog = join(root, 'progress.md');
  const progress = parseProgress(existsSync(prog) ? readFileSync(prog, 'utf8') : '');
  return { project: 'forzammilmod', now, progress, ...summarize(readEvents(join(dash, 'events.jsonl')), now) };
}
