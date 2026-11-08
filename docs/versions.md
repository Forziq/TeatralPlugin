# Pinned versions

Change these on purpose only, and note why in progress.md.

| Thing | Version | Notes |
|---|---|---|
| Minecraft / Spigot | 1.21.4 | Server software on exaroton and locally |
| Java | 21 | Toolchain in Gradle |
| BetterModel | 2.2.0 (patched) | Modrinth `BetterModel-2.2.0-spigot.jar`, run through `scripts/patch-bettermodel.sh` to give `BetterModel-2.2.0-spigot-fmod.jar`. Every Spigot build up to 3.6.0 reads the protected `EntityHuman.bW` field directly and crashes player models on Spigot; the patch swaps that read for reflection. Re-patch after any BetterModel update. (The old note that 3.x needs Java 25 was wrong: 3.0 to 3.6 class files are Java 21.) |
| SkinsRestorer | 15.12.6 | File `SkinsRestorer-15.12.6.jar` (Modrinth, loader spigot, 1.21.4). Pinned in scripts/setup-server.sh |
| Gradle | 9.8.2 (wrapper) | |
