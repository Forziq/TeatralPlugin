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
| shell_whistle | Artillery shell whistling through the air, descending high-pitched whistle, outdoors, no music | 1.5 s | no |
| explosion_big | Large explosion with falling debris, deep rumbling blast, rocks and metal raining down, outdoors | 3 s | no |
| explosion_small | Small explosion, sharp crack with dirt and stones scattering, outdoors | 1.5 s | no |
| reload_clank | Heavy shell loaded into a tank cannon breech, metallic slide and loud clank, steel breech block closing, inside a tank | 1.5 s | no |
| armour_hit | Shell striking thick steel armour plate, loud ringing metallic impact with fading resonance | 1.5 s | no |
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

Conversion: mono, 44.1 kHz Vorbis. One-shots have leading silence trimmed; loops are not trimmed.
