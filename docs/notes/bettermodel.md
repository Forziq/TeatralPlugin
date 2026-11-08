# BetterModel 2.2.0: player models and animations

Pinned version: 2.2.0 (docs/versions.md). I read the source at the `2.2.0` git tag, commit `8d35ff5805bfeb87d13e01d2a950636aaf85563e` (https://github.com/toxicity188/BetterModel/tree/2.2.0). Context7 (`/toxicity188/bettermodel`) and the wiki often show main or 3.x code, so I checked the tag wherever they differ. Anything I could not confirm is marked **unverified**.

Note: the tag's `gradle.properties` builds against Minecraft 1.21.11, not 1.21.4. The Spigot 1.21.4 jar from Modrinth (versions.md) is not checked (unverified).

## 1. Dependency

Maven Central has the 2.2.0 artifacts (https://repo1.maven.org/maven2/io/github/toxicity188/bettermodel-bukkit-api/2.2.0/, HTTP 200). README: https://github.com/toxicity188/BetterModel/blob/2.2.0/README.md

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:2.2.0")
}
```

- The README also lists two extra repositories (blamejared, nucleoid). It says they are only for the Fabric build, so we can leave them out. Whether the Bukkit pom resolves fully from Central is unverified, but the pom lists only semver4j, dynamicuv, caffeine, bettermodel-api and kotlin-stdlib.
- `compileOnly` means the BetterModel plugin jar must also be on the server. Adding `depend: [BetterModel]` to our `plugin.yml` is a suggestion, not checked.

## 2. How player models work

- **Folder:** `plugins/BetterModel/players/`. Source: `core/src/main/kotlin/kr/toxicity/model/manager/ModelManagerImpl.kt` (line 104, `getOrCreateDirectory("players")`). The wiki says this folder is "Only for animation without textures", and general models go in `models/`: https://github.com/toxicity188/bettermodel/wiki/Create-your-own-model-using-BlockBench
- **Switch:** `module.player-animation: true` in `config.yml`. If false, no limb models load and no skin work runs (`SkinManagerImpl.kt` line 833).
- **First run:** if `players/` is empty, BetterModel copies its bundled `steve.bbmodel` into it (`ModelManagerImpl.kt` line 105).
- **Bone names:** a group name is `<tag>_<label>`, for example `pra_right_arm`. The tag is matched from `api/src/main/java/kr/toxicity/model/api/bone/BoneTags.java`. Player tags (wiki, Create-player-animation: https://github.com/toxicity188/bettermodel/wiki/Create-player-animation):

| Limb | Tag | Limb | Tag |
|---|---|---|---|
| head | `ph` | right leg | `prl` |
| right arm | `pra` | right foreleg | `prfl` |
| right forearm | `prfa` | left leg | `pll` |
| left arm | `pla` | left foreleg | `plfl` |
| left forearm | `plfa` | hip | `phip` |
| waist | `pw` | chest | `pc` |
| left item (offhand) | `pli` | right item (mainhand) | `pri` |
| cape | `cape` | name tag | `ptag` |

- **Limb enum:** `api/src/main/java/kr/toxicity/model/api/player/PlayerLimb.java` has the twelve body limbs: HEAD, RIGHT_ARM, RIGHT_FOREARM, LEFT_ARM, LEFT_FOREARM, HIP, WAIST, CHEST, RIGHT_LEG, RIGHT_FORELEG, LEFT_LEG, LEFT_FORELEG.
- **Example file:** `core/src/main/resources/steve.bbmodel` at the tag. Groups: `player_root`, `shadow`, `phip_hip`, `pw_waist`, `pc_chest`, `h_ph_head`, `pra_right_arm`, `prfa_right_forearm`, `pri_right_item`, `pla_left_arm`, `plfa_left_forearm`, `pli_left_item`, `prl_right_leg`, `prfl_right_foreleg`, `pll_left_leg`, `plfl_left_foreleg`, `tag_name`, `cape_cape`. The `h_ph_head` prefix is not traced (unverified).
- **Skin:** BetterModel builds the limb textures from the player's own skin, not from the `.bbmodel`. `core/src/main/kotlin/kr/toxicity/model/manager/SkinManagerImpl.kt` (around lines 780 to 800) maps each limb from the skin image with DynamicUV. It picks slim or classic arms from `profile.skin().slim` and adds the cape from the cape image. So our plugin does not ship a skin. Whether the example's own texture (`-steve_template.png`) is used is unverified.
- **Tracker for a real player:** the README pattern is `BetterModel.limb("steve").map(r -> r.getOrCreate(BukkitAdapter.adapt(player)))`. `RenderSource.java` (lines 278 to 283) creates a `PlayerTracker` for player entities. I did not run this call (unverified).

## 3. Playing and stopping an animation

- **Class:** `kr.toxicity.model.api.tracker.Tracker` (the base of the entity and dummy trackers).
- **Play:** `boolean animate(String animation, AnimationModifier modifier, Runnable removeTask)` (`Tracker.java`, lines 503 to 506). Also `animate(String)` and `animate(String, AnimationModifier)`. Returns false if the name is unknown.
- **Modifier:** `AnimationModifier.DEFAULT` and `AnimationModifier.DEFAULT_WITH_PLAY_ONCE` (`api/src/main/java/kr/toxicity/model/api/animation/AnimationModifier.java`, lines 42 and 47). Type enum in `AnimationIterator`: `PLAY_ONCE`, `LOOP`, `HOLD_ON_LAST`.
- **Stop:** `boolean stopAnimation(String animation)` (`Tracker.java` line 552). There are also overloads with a `Predicate<RenderedBone>` filter and a `PlatformPlayer`.
- **Example** (from the wiki API page, https://github.com/toxicity188/bettermodel/wiki/API-example, matches the 2.2.0 signatures):

```java
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.EntityTracker;

EntityTracker tracker = BetterModel.limb("steve")
    .map(r -> r.getOrCreate(BukkitAdapter.adapt(player)))
    .orElse(null);
if (tracker != null) {
    tracker.animate("roll", AnimationModifier.DEFAULT_WITH_PLAY_ONCE, () -> {
        // runs when "roll" finishes
    });
}
// later
tracker.stopAnimation("roll");
```

- **End of animation:** three options.
  1. The `Runnable` passed to `animate`. It runs when the last keyframe finishes (`AnimationStateHandler.java`, lines 160 to 168).
  2. `PlayerPerAnimationEndEvent(Tracker tracker, PlatformPlayer player)` (`api/src/main/java/kr/toxicity/model/api/event/PlayerPerAnimationEndEvent.java`). It fires for per-player animations (`Tracker.java` line 1022). The matching start event is `PlayerPerAnimationStartEvent`.
  3. Polling: not needed.
- **Caution:** `stopAnimation` removes the entry from the state map (`AnimationStateHandler.java`, lines 218 to 226) and does not call the `Runnable`. So a stop does not trigger the end callback. This is from reading the code, not tested in game (unverified). Our plugin should treat "stopped" and "finished" as separate paths.

## 4. Skins and SkinsRestorer

- **Supplier choice:** `core/src/main/kotlin/kr/toxicity/model/manager/ProfileManagerImpl.kt` (line 66) picks `DefaultHttpModelProfileSupplier` when `isProxyOnlineMode` is true, otherwise `HttpModelProfileSupplier`.
- **Without SkinsRestorer:** `HttpModelProfileSupplier.kt` (line 78) calls `https://sessionserver.mojang.com/session/minecraft/profile/<uuid>`. On an offline-mode server the UUIDs are offline UUIDs, so Mojang has no skin for them. The likely result is the default fallback skin. This is inferred from code, not tested (unverified).
- **With SkinsRestorer:** `core/bukkit-core/src/main/kotlin/kr/toxicity/model/bukkit/compatibility/skinsrestorer/SkinsRestorerCompatibility.kt` replaces the supplier and completes with `manager.playerStorage.getSkinForPlayer(info.id, info.name, Bukkit.getOnlineMode())`. So a SkinsRestorer skin should reach the limbs. The class also clears BetterModel's cache on `SkinApplyEvent`. The gate that loads this class is unverified.
- **Version check:** `SkinManagerImpl.kt` line 679 returns `supported()` true for 1.21.4 and later, so our server qualifies.
- **Known issues:** one web search found nothing about BetterModel with offline mode or SkinsRestorer. I did not read the BetterModel issue tracker (unverified). SkinsRestorer is pinned at 15.12.6 (versions.md).
- **Check in game:** a player's skin should show on the limbs after joining, and again after changing skin with `/skin`.

## 5. Held items and armour during a player animation

- **Held items:** `BoneTags.java` (lines 58 to 75) maps `pli`/`li` to the offhand and `pri`/`ri` to the mainhand, with third-person hand transforms. So the held items sit on these bones. Whether an animation on those bones hides or moves them is unverified.
- **Armour:** `core/src/main/kotlin/kr/toxicity/model/manager/ArmorManager.kt` (lines 181 to 200) only builds armour models when the server is on modern resources (`useModernResource()`) and `player-animation` is on. On reload it downloads the Minecraft client jar to get armour textures (`downloadMinecraftClient`). So reload needs internet access. Armour during an animation is unverified.
- **Real body:** how the vanilla player model is hidden was not traced (unverified).

## 6. Reloading models at runtime

- **Command:** `/bm reload` (aliases `bm` and `model`), handled in `core/bukkit-core/src/main/kotlin/kr/toxicity/model/bukkit/command/Commands.kt` (lines 88 to 95). The wiki gives `/bettermodel reload`. The permission node is unverified.
- **API:** `BetterModel.platform().reload()`, or `reload(ReloadInfo info)` with `ReloadInfo.builder().sender(audience)`. Defined in `api/src/main/java/kr/toxicity/model/api/BetterModelPlatform.java` (lines 52 to 80). Results: `ReloadResult.Success`, `OnReload` (already reloading), `Failure`.
- **Threading:** the command runs the reload in `PLATFORM.scheduler().asyncTask` (`Commands.kt` line 262). We should do the same.
- **Install flow:** copy our `.bbmodel` into `plugins/BetterModel/players/`, then reload. The reload reads the folder each time (`ModelManagerImpl.kt` line 104). Whether we can skip the reload when we copy before BetterModel enables is unverified.
- **Pack hook:** `PluginStartReloadEvent(PackZipper zipper)` lets us add files to the resource pack before it is built (`api/src/main/java/kr/toxicity/model/api/event/PluginStartReloadEvent.java`). A reload also rebuilds the pack zip (see docs/notes/bettermodel-pack.md).

## 7. Example model and licence

- **Example:** `core/src/main/resources/steve.bbmodel` (https://github.com/toxicity188/BetterModel/blob/2.2.0/core/src/main/resources/steve.bbmodel). About 47 KB, one animation (`roll`), one texture. It is the file BetterModel copies when `players/` is empty. The general-model examples `demon_knight.bbmodel` and `blue_wizard.bbmodel` sit in `core/src/main/resources/` too.
- **Licence:** MIT, "Copyright (c) 2024–2026 toxicity188" (https://github.com/toxicity188/BetterModel/blob/2.2.0/LICENSE.md). Source files carry the same header. Keeping the copyright notice with any copied model is my reading of the licence, not legal advice (unverified). Licences of the other dependencies were not checked.

## 8. Unverified list

1. Held items and armour during a play-animation call.
2. Whether `stopAnimation` runs the end callback (source says no).
3. The exact tracker call for a real `Player` (README pattern only).
4. Whether the Modrinth 1.21.4 jar matches the 2.2.0 tag (the tag builds for 1.21.11).
5. Offline-mode skin fallback (inferred from code).
6. Known issues on the BetterModel tracker (not read).
7. The `h_ph_head` tag prefix and whether the example texture is used.
8. Whether copying files before BetterModel enables needs a reload.
