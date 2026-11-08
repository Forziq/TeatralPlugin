---
description: Where the project is - current, next and recent tasks (read by Haiku)
model: haiku
---

Ask the haiku-scout subagent to read `progress.md` (only that file, nothing else) and return this summary, then show it to Egor exactly as returned:

```
Now:      <id> <title> (<status>)
Next:     <id> <size> <who> <title>   (up to 3; mark manual/gate steps "for you")
Recent:   <last 3 log lines, shortened>
Progress: <done>/<total> tasks, G1 <state>, G2 <state>
Blockers: <none, or one line each>
```

British English, no extra commentary, under 15 lines.
