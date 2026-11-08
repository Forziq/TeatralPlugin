# Language system (T05)

- `Lang` loads `lang/en.yml` and `lang/ru.yml` from the jar. They are never copied to the data folder.
- Locale `ru*` gets Russian. Everything else, and the console, gets English.
- `LangListener` caches the language per UUID. It refreshes on locale change and clears on quit.
- API: `lang.msg(sender, key, "k", v, ...)` or a Map. `lang.plural(sender, key, n, ...)` adds `{n}` automatically.
- Plural keys end in `.one` and `.other` (en), or `.one`, `.few` and `.many` (ru).
- A missing ru key falls back to en. A key missing from both shows in red and warns once.
- Get it with `plugin.lang()`.
- Keys so far: version, usage, no-permission (unused), emote.soon, emote.count.* (demo plural, unused).
- `LangTest` checks both files have the same keys, so every new message goes into both.
- plugin.yml command usage lines are English only (Bukkit shows them, not Lang).
- Tests: 20 pass (`LangTest` 1, others 19). `./gradlew build -q` passes.
