# Brief (condensed from Egor's "TeatralPlugin roadmap" doc, 7 Nov 2026)

Where this disagrees with CLAUDE.md, CLAUDE.md wins (it holds the later decisions from the grilling session).

## Ground rules
Private plugin for our own server: emotes first, WWII-style vehicles second. Each MVP answers one question as cheaply as possible before anything polished gets built. Free tools, placeholder art, no GUI, no config, values hard-coded.

## MVP v1: one emote
Question: do emotes look right on our offline server, with everyone's own skin?
Build: local Spigot 1.21.4 test server (offline, BetterModel, SkinsRestorer); a 2-second wave where only the arm moves; `/emote wave` and `/emote stop`; the emote stops on move or damage; resource pack sent on join, decliners kicked.
Left out: GUI, more emotes, cooldowns, permissions, config, sounds.
Gate 1 checklist (real server, at least two friends):
- SkinsRestorer skin shows on the emote model, not Steve
- Changing skin with `/skin` updates the model
- Others see it play smoothly; note flicker at start and end
- How it looks in first person and F5
- What happens to held items and armour
- Russian text reads naturally
Decision: good, go to MVP v2. Skins don't load, fix that first or try another engine. Looks bad, work out whether the animation or the engine is at fault.

## MVP v2: one tank, maybe a jeep
Question: does a server-run vehicle feel good enough to drive and fight with?
Optional first: an hour trying an existing free plugin's built-in vehicles to feel plugin vehicles at our ping.
Build: rough tank (hull, turret, barrel as separate bones, plain colours, no track animation). W/S forward/back, A/D turn the hull, climbs one block. Turret follows where the driver looks, barrel pitch within limits. Click to fire a shell in a straight line: small explosion that breaks blocks and hurts players, fixed reload of a few seconds, vanilla explosion sound. Optional jeep on the same driving code, faster, no gun.
Left out: health, armour, ammo, HUD, passengers, track animation, custom sounds. Vehicles spawn by command (owner only).
Gate 2 checklist (real server, friends, far from spawn):
- Delay between key press and the tank moving
- Jitter when watching someone else drive
- Climbing blocks and slopes, getting stuck
- Whether turret aim feels natural
- Whether explosions and block damage size feel right
- Server TPS with two tanks firing
- Whether the jeep is playable at speed
- Russian text reads naturally
Decision: good, carry on. Jittery, tune smoothing first. Jeep bad at speed, planes wait for their own test.

## Later phases (planned only after both gates pass)
- Phase 3, emotes properly: 6 to 12 polished emotes, emote GUI, cooldowns, sounds, cancel rules, held items and armour; later two-player emotes.
- Phase 4, tanks properly: textured models, track animation, health and armour, reload and ammo, crew seats, HUD, repairs, spawn item (placed like a boat), how players get vehicles, protected zones and block regeneration.
- Phase 5, more vehicles: jeeps and trucks on the same code, then artillery; planes as their own test, dropped if not fun.
- Phase 6, polish: version checks between plugin and pack, performance.

## Risks
- Skins don't reach the emote model: fix SkinsRestorer/BetterModel skin loading.
- Animation quality: AI keyframes look stiff; hand-animate in Blockbench later.
- Vehicle input lag: keep vehicles slow and heavy, tune smoothing.
- Planes: hardest for plugins; test separately.
- Engine updates: pin BetterModel and update on purpose.
- Offline mode: anyone can use any name. Egor accepts this; no AuthMe.
- Pack/model mismatch: the pack on Pages must be built from the same models and BetterModel version as the server.
