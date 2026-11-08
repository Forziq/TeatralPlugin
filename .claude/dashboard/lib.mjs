// Pure logic for the dashboard: event attribution, plain-English wording and progress.md parsing.
import { readFileSync, existsSync, statSync, writeFileSync } from 'node:fs';
import { join, basename } from 'node:path';

export const AGENTS = ['main', 'opus-deep', 'sonnet-builder', 'sonnet-tester', 'haiku-scout'];
export const LABELS = {
  'main': 'Lead (Opus)',
  'opus-deep': 'Deep thinker (Opus)',
  'sonnet-builder': 'Builder (Sonnet)',
  'sonnet-tester': 'Tester (Sonnet)',
  'haiku-scout': 'Helper (Haiku)',
};
const ROLE = {
  'main': 'the lead',
  'opus-deep': 'the deep thinker',
  'sonnet-builder': 'the builder',
  'sonnet-tester': 'the tester',
  'haiku-scout': 'the helper',
};
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

// ---------- plain-English wording ----------

const cap = s => (s ? s[0].toUpperCase() + s.slice(1) : s);
const short = (s, n = 90) => { s = String(s || '').replace(/\s+/g, ' ').trim(); return s.length > n ? s.slice(0, n - 1) + '…' : s; };

// Older events stored Bash as "description (command)", sometimes cut short; split that back up.
function bashParts(e) {
  if (e.cmd || e.desc) return { cmd: e.cmd || '', desc: e.desc || '' };
  const t = e.text || '';
  const i = t.indexOf(' (');
  if (i > 0 && /^[A-Z]/.test(t) && !/^[A-Z]+=/.test(t)) return { desc: t.slice(0, i), cmd: t.slice(i + 2).replace(/\)$/, '') };
  return { desc: '', cmd: t };
}

// Break a shell line into the parts that matter: drop cd, echo, variable assignments and dashboard status calls.
function segments(cmd) {
  return String(cmd).split(/&&|\|\||;|\n/).map(s => s.trim()).filter(s => s &&
    !/^cd\s/.test(s) && !/^echo\b/.test(s) && !/^(export\s+)?[A-Za-z_][A-Za-z0-9_]*=\S*$/.test(s) &&
    !/dashboard\/status\.mjs/.test(s) && !/^(true|false|:)$/.test(s));
}

// Specific jobs: these win wherever they appear in the command.
const STRONG = [
  [/task\.mjs\s+start/, 'Marking the task as started'],
  [/task\.mjs\s+done/, 'Ticking the task off the list'],
  [/task\.mjs\s+block/, 'Marking the task as stuck'],
  [/task\.mjs\s+gate/, 'Recording the test result'],
  [/task\.mjs/, 'Checking which task is next'],
  [/dashboard\/ctl\.mjs/, 'Switching the dashboard on or off'],
  [/node\s+--test/, 'Running the dashboard checks'],
  [/smoke-test/, 'Starting a test server to check the plugin loads'],
  [/server-start/, 'Starting the local Minecraft server'],
  [/setup-server|BuildTools/, 'Setting up the local Minecraft server'],
  [/release\.sh/, 'Publishing a new version'],
  [/build-pack/, 'Building the resource pack'],
  [/install-harness/, 'Installing the Claude Code setup'],
  [/gradlew[^|;&]*\btest\b/, 'Running the automatic tests'],
  [/gradlew[^|;&]*\bwrapper\b/, 'Setting up the build tool'],
  [/gradlew[^|;&]*\b(build|jar|assemble)\b/, 'Building the plugin'],
  [/gradlew/, 'Running the build tool'],
  [/git\s+push/, 'Uploading the changes to GitHub'],
  [/git\s+commit/, 'Saving a snapshot of the changes'],
  [/git\s+(status|diff|log|show|add\s+-n)/, 'Checking what has changed'],
  [/git\s+add/, 'Getting the changes ready to save'],
  [/\bgit\b/, 'Working with the project history'],
  [/gh\s+release/, 'Working on a GitHub release'],
  [/gh\s+auth/, 'Checking the GitHub login'],
  [/gh\s+api[^|;&]*pages/, 'Setting up the GitHub Pages site'],
  [/gh\s+repo/, 'Setting up the GitHub repo'],
  [/\bgh\b/, 'Talking to GitHub'],
  [/brew\s+install\s+([\w@.-]+)/, m => `Installing ${m[1]}`],
  [/\bbrew\b/, 'Checking what is installed'],
  [/\b(ffmpeg|oggenc|sox)\b/, 'Converting sound files'],
  [/curl[^|;&]*\s-s?I\b/, 'Checking a link works'],
  [/\b(curl|wget)\b/, 'Downloading a file'],
  [/claude\s+mcp/, 'Setting up a Claude Code add-on'],
  [/(^|\s)op\s+(read|item|signin|whoami)/, 'Getting a password from 1Password'],
  [/(^|\s)java\s+-version|java_home/, 'Checking the Java version'],
];

// General shell chores: matched against the start of the first meaningful part.
const WEAK = [
  [/^cat\s*>+\s*(\S+)/, m => fileWords(m[1], 'Creating')],
  [/^sed\s+-i/, 'Changing a file'],
  [/^(ba|z)?sh\s+\S+\.sh|^\.\/\S+\.sh|^python3?\s/, 'Running a helper script'],
  [/^java\s/, 'Running Java'],
  [/^(npm|npx)\s/, 'Running a Node tool'],
  [/^(ls|find|tree)\b/, 'Looking through the files'],
  [/^(grep|rg|tail|head|cat|wc|sed\s+-n|less)\b/, 'Reading part of a file'],
  [/^(mkdir|cp|mv|rm|touch|chmod|ln)\b/, 'Tidying up files'],
  [/^(sleep|kill|pkill|lsof|ps|wait)\b/, 'Waiting on a background job'],
  [/^(which|command -v|type)\b/, 'Checking what is installed'],
];

// Drops "Report status / tell the dashboard" clauses from Claude's own command descriptions.
function cleanDesc(d) {
  return String(d || '')
    .replace(/(^|,\s*|\s+and\s+)report(ing)?\s+[^,]*?(status|dashboard)[^,]*?(?=,|\s+and\s+|$)/gi, '')
    .replace(/(^|,\s*|\s+and\s+)update\s+(the\s+)?dashboard(?=,|\s+and\s+|$)/gi, '')
    .replace(/^\s*(,|and)\s*/i, '').replace(/\s*(,|and)\s*$/i, '').trim();
}

function plainBash(e) {
  const { cmd, desc } = bashParts(e);
  const segs = segments(cmd);
  if (!segs.length) return /dashboard\/status\.mjs/.test(cmd) ? null : (desc ? cap(short(cleanDesc(desc), 80)) || null : 'Running a command');
  const joined = segs.join(' ; ');
  for (const [re, out] of STRONG) {
    const m = re.exec(joined);
    if (m) return typeof out === 'function' ? out(m) : out;
  }
  const d = cleanDesc(desc);
  if (d) return cap(short(d, 80));
  for (const [re, out] of WEAK) {
    const m = re.exec(segs[0]);
    if (m) return typeof out === 'function' ? out(m) : out;
  }
  return 'Running a command';
}

function fileWords(path, verb) {
  const p = String(path || '');
  const b = basename(p);
  if (b === 'progress.md') return verb === 'Reading' ? 'Reading the task list' : 'Updating the task list';
  if (b === 'CLAUDE.md') return `${verb} the project rules`;
  if (b === 'manual.md') return `${verb} your manual steps`;
  const t = /tasks\/(\w+)\.md$/.exec(p);
  if (t) return `${verb} the instructions for ${t[1]}`;
  if (/lang\/ru\.yml$/.test(p)) return `${verb} the Russian text`;
  if (/lang\/en\.yml$/.test(p)) return `${verb} the English text`;
  if (/\.bbmodel$/.test(p)) return `${verb} the 3D model ${b.replace(/\.bbmodel$/, '')}`;
  if (/src\/test\//.test(p)) return `${verb} a test (${b})`;
  if (/\.java$/.test(p)) return `${verb} plugin code (${b})`;
  if (/\.gradle\.kts$|gradle\.properties$/.test(p)) return `${verb} the build settings`;
  if (/\.ogg$|sounds\.json$/.test(p)) return `${verb} the sounds`;
  if (/\.(sh|py)$/.test(p)) return `${verb} a helper script`;
  if (/\.gitignore$/.test(p)) return `${verb} the list of files git ignores`;
  return `${verb} ${b || 'a file'}`;
}

function domainOf(u) { try { return new URL(u).hostname.replace(/^www\./, ''); } catch { return 'the web'; } }

// Turns a raw event into one short plain-English line, or null to hide it.
export function plain(e) {
  switch (e.ev) {
    case 'status': return e.text || null;
    case 'SubagentStart': return 'Started work';
    case 'SubagentStop': return 'Finished and reported back';
    case 'Stop': return e.owner === 'main' ? 'Done for now, waiting for you' : null;
    case 'SessionStart': return 'New session started';
    case 'UserPromptSubmit': {
      const t = String(e.prompt ?? (e.text || '').replace(/^Egor:\s*/, ''));
      if (/<agent-message|Subagent hand-back|<task-notification/i.test(t)) return null;
      const c = /^\/(\w+)\s*(\w*)/.exec(t.trim());
      if (c) {
        if (c[1] === 'next') return 'You asked for the next task';
        if (c[1] === 'progress') return 'You asked for a progress summary';
        if (c[1] === 'dashboard') return `You turned the dashboard ${c[2] || 'on or off'}`;
        if (c[1] === 'clear') return 'You cleared the conversation';
        return `You ran /${c[1]}`;
      }
      return `You said: “${short(t, 80)}”`;
    }
    case 'PreToolUse': break;
    default: return null;
  }
  const tool = e.tool || '';
  const input = e;
  switch (tool) {
    case 'Bash': return plainBash(e);
    case 'Read': return fileWords(input.file || fileFromText(e.text), 'Reading');
    case 'Write': return fileWords(input.file || fileFromText(e.text), 'Creating');
    case 'Edit': case 'MultiEdit': case 'NotebookEdit': return fileWords(input.file || fileFromText(e.text), 'Changing');
    case 'Grep': return 'Searching through the code';
    case 'Glob': return 'Looking for files';
    case 'WebSearch': return input.q ? `Searching the web for “${short(input.q, 60)}”` : 'Searching the web';
    case 'WebFetch': return `Reading a web page on ${domainOf(input.url || (e.text || '').replace(/^Fetching /, ''))}`;
    case 'Agent': case 'Task': {
      const who = ROLE[e.sub] || 'a helper';
      const what = input.subDesc || (e.text || '').replace(/^Handing to [^:]+:\s*/, '');
      return what ? `Giving ${who} a job: ${lowerFirst(short(what, 70))}` : `Giving ${who} a job`;
    }
    case 'Skill': return /uk-writing/.test(input.skill || e.text || '') ? 'Using the British English writing guide' : 'Using a skill';
    case 'AskUserQuestion': return 'Asking you a question';
    case 'Monitor': return 'Keeping an eye on a running job';
    case 'TaskStop': return 'Stopping a background job';
    case 'SubagentHandback': case 'ToolSearch': case 'TodoWrite': case 'TaskCreate': case 'TaskUpdate': case 'TaskList': case 'TaskGet':
      return null;
  }
  if (tool.startsWith('mcp__')) {
    const [, server = '', name = ''] = tool.split('__');
    if (/ccd_turn/.test(server)) return null;
    if (/context7/i.test(server)) return 'Looking up documentation';
    if (/^creative_/.test(name) || /elevenlabs/i.test(server)) {
      if (/create_flow/.test(name)) return 'Setting up an ElevenLabs workspace';
      if (/run_status/.test(name)) return 'Waiting for ElevenLabs to finish a sound';
      if (/get_generations/.test(name)) return 'Collecting the finished sounds';
      if (/(add_flow_node|run_flow|generate)/.test(name)) return 'Making a sound with ElevenLabs';
      return 'Reading how ElevenLabs works';
    }
    if (/chrome|browser/i.test(server)) return 'Working in Chrome';
    if (/1password/i.test(server)) return 'Getting a password from 1Password';
    return `Using ${server.replace(/[-_]/g, ' ')}`;
  }
  return null;
}
const lowerFirst = s => (s && /^[A-Z][a-z]/.test(s) ? s[0].toLowerCase() + s.slice(1) : s);
function fileFromText(t) { return String(t || '').replace(/^(Reading|Writing|Editing)\s+/, ''); }

// ---------- summary ----------

export function summarize(events, now = Date.now()) {
  const att = attribute(events);
  const agents = {};
  for (const name of AGENTS) agents[name] = { name, label: LABELS[name], status: null, statusT: null, active: false, tool: null, actions: [], lastT: null, ago: null };
  const stopped = {};
  const pending = {};
  const feed = [];
  for (const e of att) {
    const a = agents[e.owner];
    if (!a) continue;
    a.lastT = e.t;
    const line = plain(e);
    if (e.ev === 'SubagentStop' || (e.ev === 'Stop' && e.owner === 'main')) {
      stopped[e.owner] = true; pending[e.owner] = null;
      if (line) feed.push({ t: e.t, agent: e.owner, label: LABELS[e.owner], text: line, kind: 'event' });
      continue;
    }
    stopped[e.owner] = false;
    if (e.ev === 'status') { a.status = e.text; a.statusT = e.t; }
    else if (e.ev === 'PreToolUse') pending[e.owner] = line;
    else if (e.ev === 'PostToolUse' || e.ev === 'PostToolUseFailure') pending[e.owner] = null;
    if (line && ['status', 'PreToolUse', 'UserPromptSubmit', 'SubagentStart'].includes(e.ev)) {
      const prev = a.actions[a.actions.length - 1];
      if (!prev || prev.text !== line) a.actions.push({ t: e.t, text: line, kind: e.ev === 'status' ? 'status' : 'tool' });
      const kind = e.ev === 'status' ? 'status' : e.ev === 'UserPromptSubmit' ? 'you' : 'tool';
      const last = feed[feed.length - 1];
      if (!last || last.text !== line || last.agent !== e.owner) feed.push({ t: e.t, agent: e.owner, label: LABELS[e.owner], text: line, kind });
    }
  }
  for (const a of Object.values(agents)) {
    a.actions = a.actions.slice(-8).reverse();
    a.tool = pending[a.name] || null;
    a.ago = a.lastT ? Math.max(0, Math.round((now - a.lastT) / 1000)) : null;
    a.active = !!a.lastT && now - a.lastT < ACTIVE_MS && !stopped[a.name];
  }
  return { agents: AGENTS.map(n => agents[n]), feed: feed.slice(-40).reverse() };
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
    if (p.length === 6 && p[0] !== 'id' && /^[A-Z]+\d+[a-z]?$/.test(p[0].trim())) {
      res.tasks.push({ id: p[0].trim(), size: p[1].trim(), who: p[2].trim(), deps: p[3].trim(), status: p[4].trim(), title: p[5].trim() });
    }
  }
  res.total = res.tasks.length;
  res.done = res.tasks.filter(t => t.status === 'done').length;
  const nowId = res.now.split('|')[0].trim().split(/\s+/)[0];
  res.current = res.tasks.find(t => t.status === 'doing') || res.tasks.find(t => t.id === nowId) || null;
  res.next = res.tasks.filter(t => t !== res.current && t.status !== 'done').slice(0, 3);
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
