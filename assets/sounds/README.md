# Sound library

Generated with ElevenLabs sound effects (model `eleven_text_to_sound_v2`, prompt influence 0.5), flow `6EXEYeied2tVleSPsw1f`. Two takes of each sound: take 1 is the default, take 2 is the spare.

To swap a take, copy `alt/<name>.ogg` over `<name>.ogg`.

The raw downloads (mp3, two per sound) are in `raw/` and are not committed. The pack build (task T07) copies `assets/sounds/*.ogg` into `assets/pack/assets/forzammilmod/sounds/`, so the ogg files are not duplicated in the pack folder yet.

| Name | Prompt used | Length | Loop |
|---|---|---|---|
| tank_engine_idle | Heavy WWII tank diesel engine idling, low rumbling chug, steady and constant, close to the hull, no music, no voices | 4 s | yes |
| tank_engine_rev | Heavy WWII tank diesel engine revving up from idle, rising roar, close-mic, no music, no voices | 2 s | no |
| tank_tracks_loop | Metal tank tracks clanking and squeaking while rolling slowly over hard ground, steady rhythm, no music, no voices | 4 s | yes |
| turret_traverse | Mechanical tank turret rotating, heavy gears whirring and grinding, inside a steel hull, no music, no voices | 2 s | no |
| cannon_fire | Large tank cannon firing once, deep powerful boom with long echo across an open field, sharp transient | 2.5 s | no |
| cannon_fire_tank | WWII tank main gun firing, heard from beside the tank: a huge deep muzzle blast with a sharp crack and a long rolling echo over open fields, plus the tank itself: the steel breech slamming back in recoil, the hull shuddering and rattling, hatches and fittings clattering, the suspension creaking as it settles. Powerful, metallic, cinematic. No music, no voices. | 3 s | no |
| shell_whistle | A single large artillery shell screaming past very close overhead at supersonic speed: one quick, heavy, tearing whoosh with a descending metallic shriek and a thick low rush of air, like ripping the sky open, over in under a second. Powerful and frightening, cinematic war film. Not a human whistle, not a cartoon bomb drop, no music, no voices. | 0.9 s | no |
| explosion_big | Large explosion with falling debris, deep rumbling blast, rocks and metal raining down, outdoors | 3 s | no |
| explosion_small | A real high-explosive shell detonating on hard ground about thirty metres away: a sharp violent crack and a punchy low concussive thump that hits the chest, followed by a short rolling boom, earth and gravel thrown up and raining down, a brief echo across open ground. Compact but destructive and dangerous, cinematic war film. No music, no voices, no fireworks. | 2 s | no |
| reload_clank | Loading a WWII tank cannon from inside the cramped steel turret: a heavy brass shell casing dragged out of a metal rack with a scrape, a loader grunting with effort, the shell rammed into the breech with a sliding metal-on-metal rasp, then the breech block slamming shut with a heavy, satisfying steel clang that rings briefly inside the hull. Close, enclosed, mechanical. No music. | 2.5 s | no |
| armour_hit | Armour-piercing tank shell slamming into thick steel tank armour: a massive, heavy, deep metallic impact like a wrecking ball hitting a steel vault door, with a violent crunch of tearing and buckling steel, a short low boom, sparks and shrapnel scattering, then a brief deep ringing of the hull. Powerful, close, cinematic war film. No music, no voices, not a kitchen pan, not a cymbal. | 1.8 s | no |
| armour_ricochet | A tank shell striking sloped steel armour and deflecting: a brutal sharp metallic clang with a deep punch behind it, immediately followed by a fast, descending, spinning ricochet whine as the shell zings away into the distance, and a short ring of the steel plate. Heavy, close, cinematic war film. No music, no voices, not a kitchen pan. | 1.5 s | no |
| jeep_engine_loop | 1940s military jeep petrol engine running steadily, rough four-cylinder idle, constant, no music, no voices | 3 s | yes |
| jeep_horn | Old 1940s jeep horn, two short nasal beeps, outdoors | 1 s | no |
| plane_engine_loop | WWII propeller fighter plane engine running steadily, droning radial engine with spinning propeller, constant, no music, no voices | 4 s | yes |
| plane_flyby | WWII propeller plane flying past overhead, engine drone rising then fading away with Doppler effect, outdoors | 3 s | no |
| mg_burst | Short burst of WWII machine gun fire, rapid heavy shots, outdoors with slight echo, no music, no voices | 1.5 s | no |
| emote_whoosh | Soft quick whoosh, light air movement, short and clean | 0.7 s | no |
| emote_clap | One person clapping hands a few times, close-mic, dry room, no voices | 1.5 s | no |
| emote_cheer | Small group of people cheering briefly, happy shouts and whoops, outdoors | 2 s | no |
| ui_click | Soft subtle UI click, short dry tick, clean | 0.5 s | no |
| ui_error | Short low error buzz, dull electronic buzzer, one brief pulse | 0.5 s | no |
| turret_traverse_loop | WWII tank turret slowly rotating, continuous and steady with no start and no stop: a heavy electric traverse motor humming and straining, slow grinding of large steel gears, a rhythmic metallic creak of the turret ring, heard from inside the steel hull. Constant, loopable, same from first moment to last. No music, no voices. | 3 s | yes |
| turret_traverse_stop | A heavy WWII tank turret coming to a stop: the traverse motor cutting out with a short descending whine and a solid steel clunk as the gears lock, with a brief rattle of the turret ring settling, heard inside the steel hull. Short, mechanical. No music, no voices. | 1 s | no |

Conversion: mono, 44.1 kHz Vorbis. One-shots have leading silence trimmed; loops are not trimmed.

The sounds `armour_hit`, `explosion_small`, `reload_clank` and `shell_whistle` were redone with longer, more detailed prompts (prompt influence 0.65) and normalised to a peak of -1 dBFS, so they are much louder. The first versions are in `old/` (`<name>.ogg` and `<name>_alt.ogg`) so you can compare them.

`cannon_fire_tank` is the louder alternative to `cannon_fire`. The old `cannon_fire` is kept as it was.

`armour_ricochet` is the deflection variant of `armour_hit`.

## Turret sound options (Nov 2026)

Five candidate replacements for `turret_traverse_loop` and `turret_traverse_stop`, in `options/turret/` (not wired in; Egor picks one). Same model and flow as above (`eleven_text_to_sound_v2`, prompt influence 0.6, loops asked for 4 s with the loop flag, stops 1.5 s). Two takes of each, the one with the least energy above 4 kHz was kept. Loops are cut to 3 s with a 0.5 s crossfade into the start; stops are trimmed to 1 s with a short fade-out; everything is low-passed at 7 kHz, mono, peak -1 dBFS. `preview_*.m4a` = loop three times then the stop; `preview_all.m4a` = all five in order. Raw takes are in `raw/turret/` (not committed).

Every prompt ends with: "Low-pitched and heavy. No screeching, no squealing, no scraping, no high-pitched grinding, no music, no voices." Loop prompts also end with: "Continuous and steady, no start and no stop, same from first moment to last."

| Option | Loop prompt | Stop prompt | Notes |
|---|---|---|---|
| 1_electric | WWII tank electric turret traverse motor running: a deep, smooth, powerful electric hum and low whirring drone of a big motor under load, soft rhythmic thrum of heavy gears, heard from inside a steel tank hull. | WWII tank turret stopping: a deep electric motor winding down with a falling low hum, ending in one solid heavy thunk as the turret locks. | Mostly a low hum (almost all energy under 300 Hz), with a faint whine ~16 dB lower above 4 kHz. |
| 2_hydraulic | WWII tank hydraulic turret traverse: a hydraulic pump throbbing and pulsing steadily, low pressurised fluid rush, deep mechanical chugging, inside a steel tank hull. | WWII tank turret stopping: hydraulic valve closing with a short low hiss of released pressure and a heavy dull clunk as the turret halts. | Mid-range pulsing pump, not much deep bass; the stop has a hiss. |
| 3_geared | Heavy WWII tank turret turning on a big geared ring: slow steady rhythmic clack-clack of massive steel gear teeth meshing, deep heavy ratcheting thuds, hand-cranked traverse, inside a steel hull. | WWII tank turret stopping: large steel gears clacking a final time and locking with a heavy, deep metallic clunk. | Clacky and metallic, mid-range heavy with little deep bass; the most likely to feel scrapey. |
| 4_engine | WWII heavy tank turret turning powered by the tank's engine: deep diesel engine rumble straining under load, low gearbox drone and heavy mechanical vibration through the hull. | WWII tank turret stopping: tank engine load dropping off with a falling low rumble and a heavy gearbox clunk as the turret drive disengages. | Mostly low end, diesel-like. |
| 5_rumble | A massive steel tank turret rotating on its huge ring bearing: a deep, heavy, rolling rumble and low resonant thrumming of tons of steel turning, the whole armoured hull vibrating, like distant thunder inside a steel vault. | Tons of steel coming to rest: a deep heavy thud and a short low resonant boom of the hull settling. | Almost pure low end, the least screech risk, but may feel less mechanical. |

**Chosen (Nov 2026): `h2_pump_engine`** is now `turret_traverse_loop.ogg` and `turret_traverse_stop.ogg`. The old screechy versions are in `old/turret_traverse_loop.ogg` and `old/turret_traverse_stop.ogg`. The prompts in the main table above are for those old versions.

### Heavy hydraulic variants

Five heavier takes on option 2 (Egor's pick), in `options/turret/` as `h1_*` to `h5_*` with `preview_h*.m4a` and `preview_hydraulic_all.m4a`. Same settings and processing as above (two takes each, least energy above 4 kHz kept; raw takes in `raw/turret/`).

Every prompt ends with: "Massive heavy WWII breakthrough tank, deep and powerful, lots of low-end weight. Low-pitched and heavy. No screeching, no squealing, no scraping, no high-pitched whine, no music, no voices." Loop prompts also end with: "Continuous and steady, no start and no stop, same from first moment to last."

| Option | Loop prompt | Stop prompt | Notes |
|---|---|---|---|
| h1_heavypump | A big high-pressure hydraulic pump pounding in a deep, slow, heavy rhythm, low thick fluid surge through steel pipes, the armoured hull vibrating with each stroke. | The hydraulic pump thudding to a halt with a deep, heavy hydraulic lock clunk. | Slow pounding pump; the brightest of the five. |
| h2_pump_engine | A hydraulic traverse pump driven off the tank's huge diesel engine: deep engine rumble under load mixed with a strong low pulsing pump throb. | The load drops off, the engine rumble eases and a heavy valve slams shut with a deep clunk. | Engine rumble plus pump throb. |
| h3_hydraulic_motor | A large hydraulic motor turning tons of steel: a smooth, deep, powerful pressurised drone, low-frequency hum of pressurised oil, steady heavy mechanical force. | Pressure releasing in one short, low, heavy whoosh, then a solid steel thud as the turret locks. | Smooth drone, little rhythm. |
| h4_rams | Huge hydraulic rams and pistons working under great strain, deep groaning pressure, slow heavy surging pushes, a feeling of enormous force moving a massive weight. | The rams locking with a huge deep metallic clunk and a short low resonant boom of the hull. | Groaning surges, cleanest top end. |
| h5_pump_ring | A heavy hydraulic pump throbbing combined with the deep low rumble of a massive turret ring rolling, tons of steel turning smoothly, the whole hull humming. | The turret settling: a deep heavy thud, a short low rumble and a brief low hydraulic exhale. | Pump throb over a rolling-ring rumble. |
