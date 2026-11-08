# Bundled models and how they reach BetterModel

## Installer
- Our jar carries models under `models/`. On enable, `ModelInstaller` reads the jar's own entries (via `JavaPlugin#getFile()`, no build-time index) and copies each file into BetterModel's data folder:
  - `models/players/<x>` -> `plugins/BetterModel/players/<x>` (player-animation models, limbs built from the player's skin)
  - `models/<anything else>` -> `plugins/BetterModel/models/<same path>` (normal models, e.g. vehicles later)
- A file is written only when missing or when its SHA-1 differs (so hand edits on the server get overwritten on the next start). Writes go to a temp file and are moved in place. Each write is logged: `Installed model players/forzammil_player.bbmodel into BetterModel (new|updated)`, otherwise `BetterModel models are up to date`.
- Pure logic (path mapping, SHA-1, what to copy) is in `ModelInstallPlan`; tests in `ModelInstallPlanTest` and `ModelInstallerTest`.
- If BetterModel is missing or disabled we log a warning and skip. `plugin.yml` keeps `softdepend` (not `depend`) so our plugin, and with it `/fmod update`, still loads if BetterModel ever fails. All BetterModel API calls live in `BetterModelHook`, which is only touched after checking BetterModel is enabled.

## When BetterModel reloads
- BetterModel 2.2.0 does its first model load on Bukkit's `ServerLoadEvent` (checked in the 2.2.0 jar: `BetterModelPlugin$onEnable$2.load`), which fires after every plugin's `onEnable`. So the copy runs on the main thread during our `onEnable` (startup only, no players or ticks yet) and BetterModel's own first load picks the files up. No extra reload is done at startup.
- Only if our plugin is enabled after that event (plugin managers, odd `/reload` cases) and something changed do we call `BetterModel.platform().reload()` on an async task, retrying up to 3 times 5 s apart if BetterModel answers `OnReload`.
- `BetterModelHook.logReloads` registers a reload-end handler that logs, after every successful BetterModel reload, `BetterModel has loaded player model forzammil_player` (or a warning if it is missing). The smoke test log shows this line.
- A BetterModel reload also rebuilds its resource pack (`plugins/BetterModel/build.zip`). See `bettermodel-pack.md` for how that relates to our published pack.

## Player model (for T12)
- File: `src/main/resources/models/players/forzammil_player.bbmodel`, copied from BetterModel's `steve.bbmodel` (tag 2.2.0, MIT, credited in README). Groups unchanged: `player_root`, `shadow`, `phip_hip`, `pw_waist`, `pc_chest`, `cape_cape`, `h_ph_head`, `pra_right_arm`, `prfa_right_forearm`, `pri_right_item`, `pla_left_arm`, `plfa_left_forearm`, `pli_left_item`, `prl_right_leg`, `prfl_right_foreleg`, `pll_left_leg`, `plfl_left_foreleg`, `tag_name`.
- BetterModel names a model after its file name without extension (`ModelAsset.nameWithoutExtension`, checked in the jar), so the limb key is `forzammil_player`: `BetterModel.limb("forzammil_player")`. `BetterModel.limbKeys()` lists what loaded.
- One animation, `wave`: 2.0 s, loop `once`, catmullrom, 20 fps snapping. Only `pra_right_arm` and `prfa_right_forearm` have keyframes, all on the rotation Z axis:
  - right arm Z: 0 s 0, 0.4 s 150, then 140/160/140/160/140 every 0.2 s, 1.6 s 155, 2.0 s 0
  - right forearm Z: 0 at 0 and 0.4 s, then -20/20/-20/20/-20 every 0.2 s, 0 at 1.6 and 2.0 s
- Why Z and why positive: the model faces -Z (the face texture is on the head's north face) and the right arm sits at +X. In steve's own `roll`, a positive X rotation swings the legs and arms forward (otherwise the knees would bend backwards), which is the plain right-handed rule. By the same rule a positive Z turns a hanging arm's hand towards +X, so on the right arm it lifts the arm out sideways; 150 puts the hand above the head, tilted 30 degrees outward. Not yet seen in Blockbench or in game (manual.md M03 step 4).
- Play it in T12 with `tracker.animate("wave", AnimationModifier.DEFAULT_WITH_PLAY_ONCE, onEnd)` (see `bettermodel.md` §3).

## Hand-off from T11
- Rebuild the resource pack before the T13 release, and check its SHA-1.
- Wave: if the arm swings across the body, flip the Z signs.
- Not yet checked in Blockbench or in game (M03 step 4).
- T12 should call `animate("wave", AnimationModifier.DEFAULT_WITH_PLAY_ONCE, onEnd)`. Not yet checked against the 2.2.0 jar.
- iCloud makes "X 2" copies in build/ that break Gradle. If tests fail with "wrong name", run `find build -name '* 2*' -delete`.
