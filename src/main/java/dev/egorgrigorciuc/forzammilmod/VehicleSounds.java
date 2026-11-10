package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.SoundLoop.Action;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Plays our own vehicle sounds. Loops are replayed to every player within range at the vehicle's current
 * position (Minecraft sounds do not follow moving things); crew hear them at their own position. Main thread only.
 */
final class VehicleSounds {

    static final String PREFIX = "forzammilmod:";
    static final String TANK_IDLE = PREFIX + "tank_engine_idle";
    static final String TANK_REV = PREFIX + "tank_engine_rev";
    static final String TANK_TRACKS = PREFIX + "tank_tracks_loop";
    static final String TURRET_LOOP = PREFIX + "turret_traverse_loop";
    static final String TURRET_STOP = PREFIX + "turret_traverse_stop";
    static final String CANNON = PREFIX + "cannon_fire_tank";
    static final String SHELL_WHISTLE = PREFIX + "shell_whistle";
    static final String EXPLOSION_SMALL = PREFIX + "explosion_small";
    static final String EXPLOSION_BIG = PREFIX + "explosion_big";
    static final String RELOAD = PREFIX + "reload_clank";
    static final String ARMOUR_HIT = PREFIX + "armour_hit";
    static final String JEEP_ENGINE = PREFIX + "jeep_engine_loop";
    static final String JEEP_HORN = PREFIX + "jeep_horn";

    /** Volume 2 carries 32 blocks; loops are only sent to players inside that. */
    static final float LOOP_VOLUME = 2.0f;
    static final double RANGE = 32.0;
    /** Louder one-shots carry twice as far. */
    static final float LOUD = 4.0f;

    private static final class Live {
        final VehicleSoundState logic;
        final Set<UUID> heard = new HashSet<>();
        float lastYaw;

        Live(Vehicle v) {
            logic = new VehicleSoundState(v.type.isTank(), v.type.soundPitch());
            lastYaw = v.state().yaw();
        }
    }

    private final JavaPlugin plugin;
    private final Map<Vehicle, Live> live = new IdentityHashMap<>();
    private long now;

    VehicleSounds(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Call once per server tick before any {@link #tick}. */
    void advance() {
        now++;
    }

    void tick(Vehicle v, DriveInput input) {
        Live l = live.computeIfAbsent(v, Live::new);
        float yaw = v.state().yaw();
        double metric = VehicleSoundState.moveMetric(v.state().speed(), TurretModel.wrap(yaw - l.lastYaw));
        l.lastYaw = yaw;
        VehicleSoundState.Out out = l.logic.update(now, v.hasCrew(), input.throttle() != 0, metric, v.turretYawDelta);
        loop(v, l, v.type.isTank() ? TANK_IDLE : JEEP_ENGINE, out.engine());
        loop(v, l, TANK_TRACKS, out.tracks());
        loop(v, l, TURRET_LOOP, out.turret());
        if (out.rev()) oneShot(v, l, TANK_REV, LOOP_VOLUME);
        if (out.turretStop()) oneShot(v, l, TURRET_STOP, LOOP_VOLUME);
    }

    /** Jeep horn; ignored inside the cooldown and for tanks. */
    void horn(Vehicle v) {
        if (v.type.isTank() || !v.hasCrew()) return;
        Live l = live.computeIfAbsent(v, Live::new);
        if (l.logic.horn(now)) oneShot(v, l, JEEP_HORN, LOOP_VOLUME);
    }

    /** Stops every loop of this vehicle for everybody who heard it. Call before the vehicle goes away. */
    void stop(Vehicle v) {
        Live l = live.remove(v);
        if (l == null) return;
        for (UUID id : l.heard) {
            Player p = plugin.getServer().getPlayer(id);
            if (p == null) continue;
            for (String key : new String[] {TANK_IDLE, JEEP_ENGINE, TANK_TRACKS, TURRET_LOOP}) {
                p.stopSound(key, SoundCategory.NEUTRAL);
            }
        }
    }

    private void loop(Vehicle v, Live l, String key, Action a) {
        if (a == Action.PLAY) {
            sendToNearby(v, l, key, LOOP_VOLUME);
        } else if (a == Action.STOP) {
            for (UUID id : l.heard) {
                Player p = plugin.getServer().getPlayer(id);
                if (p != null) p.stopSound(key, SoundCategory.NEUTRAL);
            }
        }
    }

    private void oneShot(Vehicle v, Live l, String key, float volume) {
        sendToNearby(v, l, key, volume);
    }

    private void sendToNearby(Vehicle v, Live l, String key, float volume) {
        Location at = v.location().add(0, v.type.height / 2, 0);
        double rangeSq = RANGE * RANGE;
        for (Player p : v.world.getPlayers()) {
            boolean crew = v.seatOf(p.getUniqueId()) != null;
            if (!crew && p.getLocation().distanceSquared(at) > rangeSq) continue;
            // Crew hear it at their own position, so it does not swing around as the vehicle turns.
            p.playSound(crew ? p.getLocation() : at, key, SoundCategory.NEUTRAL, volume, (float) v.type.soundPitch());
            l.heard.add(p.getUniqueId());
        }
    }

    /** A loud one-shot at a place, heard by everyone who can hear it (about 64 blocks at volume 4). */
    static void boom(World w, Location at, String key, float volume) {
        w.playSound(at, key, SoundCategory.NEUTRAL, volume, 1f);
    }
}
