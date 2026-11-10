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
