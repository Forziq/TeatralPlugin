#!/usr/bin/env node
// Claude Code hook: appends one compact JSON line per event to .claude/dashboard/events.jsonl.
// Always exits 0 so it can never block or break a session.
import { appendFileSync, mkdirSync, statSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectDir = process.env.CLAUDE_PROJECT_DIR || join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const out = join(projectDir, '.claude', 'dashboard', 'events.jsonl');
const MAX_BYTES = 5 * 1024 * 1024;
const KEEP_LINES = 3000;

const clip = (s, n = 140) => {
  if (s == null) return '';
  s = String(s).replace(/\s+/g, ' ').trim();
  return s.length > n ? s.slice(0, n - 1) + '…' : s;
};
const rel = p => { try { return p ? relative(projectDir, p) || p : ''; } catch { return p || ''; } };

function describe(tool, input = {}) {
  switch (tool) {
    case 'Bash': return input.description ? `${clip(input.description, 80)} (${clip(input.command, 60)})` : clip(input.command);
    case 'Read': return `Reading ${rel(input.file_path)}`;
    case 'Write': return `Writing ${rel(input.file_path)}`;
    case 'Edit': case 'MultiEdit': return `Editing ${rel(input.file_path)}`;
    case 'NotebookEdit': return `Editing ${rel(input.notebook_path)}`;
    case 'Grep': return `Searching for "${clip(input.pattern, 50)}"${input.path ? ' in ' + rel(input.path) : ''}`;
    case 'Glob': return `Finding files ${clip(input.pattern, 60)}`;
    case 'WebFetch': return `Fetching ${clip(input.url, 90)}`;
    case 'WebSearch': return `Searching the web: ${clip(input.query, 80)}`;
    case 'Task': case 'Agent': return `Handing to ${input.subagent_type || 'subagent'}: ${clip(input.description || input.prompt, 90)}`;
    case 'Skill': return `Using skill ${input.skill || input.name || ''}`;
    case 'TodoWrite': case 'TaskCreate': case 'TaskUpdate': return 'Updating task list';
    default:
      if (tool && tool.startsWith('mcp__')) {
        const [, server, name] = tool.split('__');
        if (/context7/i.test(server)) return `Looking up docs (${name})`;
        return `${server}: ${name}`;
      }
      return tool || '';
  }
}

let raw = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', c => { raw += c; });
process.stdin.on('end', () => {
  try {
    const h = JSON.parse(raw || '{}');
    const ev = h.hook_event_name || 'unknown';
    const e = { t: Date.now(), ev, agent: h.agent_type || null, sid: (h.session_id || '').slice(0, 8) };
    if (ev === 'PreToolUse' || ev === 'PostToolUse' || ev === 'PostToolUseFailure') {
      e.tool = h.tool_name;
      e.text = describe(h.tool_name, h.tool_input);
      if (h.tool_name === 'Task' || h.tool_name === 'Agent') e.sub = h.tool_input?.subagent_type || null;
      if (ev === 'PostToolUse' && h.tool_response && typeof h.tool_response === 'object' && h.tool_response.is_error) e.err = true;
    } else if (ev === 'UserPromptSubmit') {
      e.text = 'Egor: ' + clip(h.prompt, 100);
    } else if (ev === 'SubagentStart' || ev === 'SubagentStop') {
      e.sub = h.agent_type || h.subagent_type || null;
      e.text = ev === 'SubagentStart' ? 'Started' : 'Finished';
    } else if (ev === 'Stop') {
      e.text = 'Turn finished';
    } else if (ev === 'SessionStart') {
      e.text = `Session started (${h.source || 'new'})`;
    }
    mkdirSync(dirname(out), { recursive: true });
    appendFileSync(out, JSON.stringify(e) + '\n');
    try {
      if (statSync(out).size > MAX_BYTES) {
        const all = readFileSync(out, 'utf8').trim().split('\n');
        writeFileSync(out, all.slice(-KEEP_LINES).join('\n') + '\n');
      }
    } catch {}
  } catch {}
  process.exit(0);
});
