# BetterModel resource pack (2.2.0)

- Generated pack: `server/plugins/BetterModel/build.zip` (config `pack-type: zip`). Regenerated on server start.
- Layout: `pack.mcmeta`, `pack.png`, `assets/bettermodel/...`, plus overlay folders `bettermodel_legacy/` (formats 22-45) and `bettermodel_modern/` (46-99).
- `pack.mcmeta` declares those overlays. Our final `pack.mcmeta` must keep the `overlays` block and format fields, or models break.
- Pack format for 1.21.4 is 46.
- 2.2.0 has no option to host or send the pack, so there is nothing to disable. Our plugin sends the pack (T08).
- `scripts/build-pack.sh` unzips BetterModel's zip into `build/pack/stage/`, copies `assets/pack/*` and `assets/sounds/*.ogg` (to `assets/forzammilmod/sounds/`) on top, rewrites `pack.mcmeta` (overlays kept, description replaced) and zips to `build/pack/forzammilmod-pack-<version>.zip`.
