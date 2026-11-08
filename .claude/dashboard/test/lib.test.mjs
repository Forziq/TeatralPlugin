import test from 'node:test';
import assert from 'node:assert/strict';
import { attribute, summarize, parseProgress, plain } from '../lib.mjs';

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

test('summary: five agents with labels, status, active window, current action in plain words', () => {
  const s = summarize([
    ev(1000, { ev: 'status', agent: 'haiku-scout', text: 'Looking up how emotes work' }),
    ev(1000, { ev: 'PreToolUse', agent: 'sonnet-builder', tool: 'Edit', file: 'src/main/java/dev/x/EmoteCommand.java' }),
  ], 50_000);
  assert.equal(s.agents.length, 5);
  const h = s.agents.find(a => a.name === 'haiku-scout');
  assert.equal(h.status, 'Looking up how emotes work');
  assert.equal(h.label, 'Helper (Haiku)');
  assert.equal(h.active, true);
  assert.equal(s.agents.find(a => a.name === 'sonnet-builder').tool, 'Changing plugin code (EmoteCommand.java)');
  assert.equal(summarize([ev(1000, { ev: 'status', agent: 'haiku-scout', text: 'x' })], 200_000).agents[2].active, false);
});

test('plain: Bash commands become simple words, old and new event shapes', () => {
  const b = o => plain({ ev: 'PreToolUse', tool: 'Bash', ...o });
  assert.equal(b({ cmd: './gradlew test -q', desc: 'Run tests' }), 'Running the automatic tests');
  assert.equal(b({ cmd: './gradlew build -q' }), 'Building the plugin');
  assert.equal(b({ cmd: 'git add assets/ && git commit -q -m "T04: x"' }), 'Saving a snapshot of the changes');
  assert.equal(b({ cmd: 'git push origin main' }), 'Uploading the changes to GitHub');
  assert.equal(b({ cmd: 'node scripts/task.mjs done T04' }), 'Ticking the task off the list');
  assert.equal(b({ cmd: 'brew install openjdk@21' }), 'Installing openjdk@21');
  assert.equal(b({ cmd: 'ffmpeg -i a.mp3 a.wav' }), 'Converting sound files');
  assert.equal(b({ cmd: 'node .claude/dashboard/status.mjs main "x"' }), null);
  assert.equal(b({ text: 'Verify commits, task status and update dashboard (git log --oneline -3; git status --short)' }), 'Checking what has changed');
  assert.equal(b({ cmd: 'python3 tidy.py', desc: 'Normalise sound volumes' }), 'Normalise sound volumes');
});

test('plain: noise is hidden, hand-backs and slash commands read naturally', () => {
  assert.equal(plain({ ev: 'PreToolUse', tool: 'mcp__ccd_turn__wrap_up' }), null);
  assert.equal(plain({ ev: 'PreToolUse', tool: 'SubagentHandback' }), null);
  assert.equal(plain({ ev: 'UserPromptSubmit', text: 'Egor: <agent-message from="a01"> [Subagent hand-back] The text…' }), null);
  assert.equal(plain({ ev: 'UserPromptSubmit', text: 'Egor: /next' }), 'You asked for the next task');
  assert.equal(plain({ ev: 'PreToolUse', tool: 'mcp__b28e__creative_add_flow_node' }), 'Making a sound with ElevenLabs');
  assert.equal(plain({ ev: 'PreToolUse', tool: 'Read', text: 'Reading progress.md' }), 'Reading the task list');
  assert.equal(plain({ ev: 'PreToolUse', tool: 'Agent', sub: 'haiku-scout', subDesc: 'Summarise progress.md' }), 'Giving the helper a job: summarise progress.md');
  assert.equal(plain({ ev: 'PreToolUse', tool: 'Bash', text: 'Report T01 done to dashboard (node .claude/dashboard/status.mjs haiku-scout "T0…' }), null);
  assert.equal(plain({ ev: 'PreToolUse', tool: 'Bash', cmd: 'node .claude/dashboard/status.mjs x "y"; git log -1' }), 'Checking what has changed');
  assert.equal(plain({ ev: 'PreToolUse', tool: 'Bash', cmd: 'P=src/main/java/dev/x && mkdir -p $P && cat > settings.gradle.kts <<EOF' }), 'Tidying up files');
  assert.equal(plain({ ev: 'PreToolUse', tool: 'Bash', cmd: 'cd /x/assets/sounds/raw && B=https://e.com/a && curl -sO $B/1.mp3' }), 'Downloading a file');
  assert.equal(plain({ ev: 'PreToolUse', tool: 'Bash', desc: 'Report status, check git state and progress.md', cmd: 'node .claude/dashboard/status.mjs a "b"; cat progress.md' }), 'Check git state and progress.md');
  assert.equal(plain({ ev: 'PreToolUse', tool: 'Skill', skill: 'uk-writing' }), 'Using the British English writing guide');
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
