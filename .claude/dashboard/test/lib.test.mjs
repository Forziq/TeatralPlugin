import test from 'node:test';
import assert from 'node:assert/strict';
import { attribute, summarize, parseProgress } from '../lib.mjs';

const ev = (t, o) => ({ t, agent: null, sid: 'x', ...o });

test('explicit agent wins; null goes to main', () => {
  const r = attribute([ev(1, { ev: 'PreToolUse', tool: 'Bash', text: 'a' }), ev(2, { ev: 'PreToolUse', tool: 'Bash', agent: 'haiku-scout', text: 'b' })]);
  assert.deepEqual(r.map(e => e.owner), ['main', 'haiku-scout']);
});

test('Agent call pushes subagent; null events go to it until PostToolUse', () => {
  const r = attribute([
    ev(1, { ev: 'PreToolUse', tool: 'Agent', sub: 'sonnet-builder', text: 'Handing' }),
    ev(2, { ev: 'PreToolUse', tool: 'Edit', text: 'Editing x' }),
    ev(3, { ev: 'PostToolUse', tool: 'Agent', sub: 'sonnet-builder', text: 'Handing' }),
    ev(4, { ev: 'PreToolUse', tool: 'Bash', text: 'ls' }),
  ]);
  assert.deepEqual(r.map(e => e.owner), ['main', 'sonnet-builder', 'main', 'main']);
});

test('SubagentStop pops and marks idle', () => {
  const evs = [
    ev(1000, { ev: 'PreToolUse', tool: 'Agent', sub: 'sonnet-tester' }),
    ev(2000, { ev: 'PreToolUse', tool: 'Bash', text: 'Running tests' }),
    ev(3000, { ev: 'SubagentStop', sub: 'sonnet-tester' }),
  ];
  const s = summarize(evs, 4000);
  const t = s.agents.find(a => a.name === 'sonnet-tester');
  assert.equal(t.active, false);
  assert.equal(attribute(evs)[2].owner, 'sonnet-tester');
});

test('summary: five agents, status, active window, current tool', () => {
  const s = summarize([
    ev(1000, { ev: 'status', agent: 'haiku-scout', text: 'Looking up docs' }),
    ev(1000, { ev: 'PreToolUse', agent: 'sonnet-builder', tool: 'Edit', text: 'Editing a' }),
  ], 50_000);
  assert.equal(s.agents.length, 5);
  const h = s.agents.find(a => a.name === 'haiku-scout');
  assert.equal(h.status, 'Looking up docs');
  assert.equal(h.active, true);
  assert.equal(s.agents.find(a => a.name === 'sonnet-builder').tool, 'Editing a');
  assert.equal(summarize([ev(1000, { ev: 'status', agent: 'haiku-scout', text: 'x' })], 200_000).agents[2].active, false);
});

const MD = `# progress
now: T03 doing | G1 todo | G2 todo
updated: 2026-11-08

## tasks
id|size|who|deps|status|title
T01|S|haiku-scout|-|done|One
T03|M|sonnet-builder|T02|doing|Dashboard
T04|M|sonnet-builder|T03|todo|Sounds
T05|M|sonnet-builder|T02|todo|Lang
T06|M|sonnet-builder|T02|todo|Server
T07|M|sonnet-builder|T06|todo|Pack
G1|-|egor|T13|gate|Gate 1

## log (newest first, keep 8)
- x

## blockers
none
`;

test('parseProgress', () => {
  const p = parseProgress(MD);
  assert.equal(p.current.id, 'T03');
  assert.deepEqual(p.next.map(t => t.id), ['T04', 'T05', 'T06']);
  assert.equal(p.done, 1);
  assert.equal(p.total, 7);
  assert.deepEqual(p.gates, [{ id: 'G1', state: 'todo' }, { id: 'G2', state: 'todo' }]);
  assert.deepEqual(p.blockers, []);
  assert.deepEqual(parseProgress(MD.replace('none', '- waiting on Egor')).blockers, ['waiting on Egor']);
});
