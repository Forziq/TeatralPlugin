---
description: Turn the live agent dashboard on or off
argument-hint: on | off | status
model: haiku
allowed-tools: Bash(node .claude/dashboard/ctl.mjs:*)
---

If `.claude/dashboard/ctl.mjs` doesn't exist, reply "The dashboard page is built in task T03. Activity is already being logged, so it'll have history once it's up." and stop.

Otherwise run `node .claude/dashboard/ctl.mjs $ARGUMENTS` with Bash (use `status` if no argument was given) and reply with its output in one line. Nothing else.
