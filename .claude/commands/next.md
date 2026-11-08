---
description: Start the next task in progress.md (one task, then stop)
---

Run exactly one task from the plan, then stop. Egor will `/clear` afterwards, so leave everything the next session needs in progress.md.

## 1. Pick the task
- Read `progress.md` only (CLAUDE.md is already loaded). Don't read other files yet.
- The next task is the first row, top to bottom, whose status is `todo`, `doing`, `blocked`, `manual` or `gate` and whose deps are all `done`.
  - `doing`: a previous session was cut off. Check `git status` and `git log -1`, then carry on from where it stopped.
  - `blocked`: show Egor the reason from the blockers section and ask whether it's been sorted. If not, stop.
  - `manual`: tell Egor which section of manual.md to do, in two or three lines. Ask whether it's done (AskUserQuestion: Done / Not yet). If done, run `node scripts/task.mjs done <id>` and continue to the next task; otherwise stop.
  - `gate`: ask Egor for the result (AskUserQuestion: Passed / Failed / Not tested yet) and, if tested, his notes on each checklist item from manual.md. Have haiku-scout record the notes under "gate notes" and run `node scripts/task.mjs gate <id> pass|fail`. If it failed, plan the fix tasks yourself (add rows and task files after the gate, ids like `T13a`), then stop.
- If nothing is left, say so and stop.

## 2. Start
- `node scripts/task.mjs start <id>`.
- `node .claude/dashboard/status.mjs main "<plain words>"`, e.g. "Reading the next job: the language files". Every status line and job description follows the plain-words rule in CLAUDE.md.
- Read `tasks/<id>.md`. Read docs/notes files only if the task names them.

## 3. Do it
- `who: haiku-scout`: send the task to haiku-scout with the task file's steps as the brief.
- `who: sonnet-builder`: write a brief (goal, files to touch, steps, done-when, constraints from CLAUDE.md that matter here) and send it to sonnet-builder. If it needs docs first, get haiku-scout to look them up and pass the answer in the brief.
- `who: opus-deep` or `hard: yes`: send the brief to opus-deep.
- `who: main`: do it yourself.
- Use background or parallel subagents only when the jobs are independent.

## 4. Check (by size)
- S: haiku-scout runs `./gradlew build -q` (skip if the task touched no code).
- M: sonnet-tester runs the tests and adds tests for new pure logic.
- L: sonnet-tester runs tests and `scripts/smoke-test.sh`; then you review `git diff --stat` and the important hunks, and read any new Russian text.
- On FAIL: send the failure to the same implementer once. Fails again: hand it to opus-deep with both failure reports. Fails a third time: `node scripts/task.mjs block <id> "<reason>"`, tell Egor in two lines, stop.

## 5. Finish
- Send haiku-scout one job: commit as `<id>: <summary>`, push if a remote exists, run `node scripts/task.mjs done <id>`, add a log line to progress.md (uk-writing skill), and note anything the next task needs. If the work revealed something Egor must do by hand, add it to manual.md too.
- `node .claude/dashboard/status.mjs main "<plain words>"`, e.g. "Finished the language files, waiting for you".
- Reply to Egor in at most five lines: what got done, anything he needs to do, and the next task's id and title. End with: "Run /clear, then /next."
