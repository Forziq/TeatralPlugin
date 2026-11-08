# Updater and resource pack notes (T08)
- `UpdateClient`: blocking HTTP (latest release, asset download, manual redirects, no auth on later hops). Base URI injectable. Run only off the main thread.
- `UpdateService`: async wrapper + startup check + `/fmod update` replies (main thread). `UpdateResult` = UpToDate / Downloaded / NoToken / Error.
- `Versions`, `ReleaseInfo`: pure version compare and release JSON parsing (first `.jar` asset).
- `PackProperties` (pure parser) + `PackService` (join listener, kick on decline/failure). Missing `pack.properties` = one warning, feature off.
- Owner check: `Access.isOwner(CommandSender)`; console counts as owner.
- Test: `./gradlew test` (UpdateClientTest uses a local HttpServer).
- Gotchas: jar name in update folder must equal the running jar name; Spigot only swaps it on restart. Pack UUID derives from sha1 so a new pack re-prompts.
