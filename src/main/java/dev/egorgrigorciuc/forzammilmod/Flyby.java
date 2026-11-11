package dev.egorgrigorciuc.forzammilmod;

/** When a passing plane plays its fly-past sound to someone. Pure, no Bukkit types. */
final class Flyby {

    /** Plays to players within this many blocks of the plane. */
    static final double RANGE = 24.0;
    /** The plane has to be in the air and going at least this fast (10 blocks a second). */
    static final double MIN_SPEED = 0.5;
    /** Each player hears it at most once in this many ticks (10 seconds), so circling overhead isn't a din. */
    static final int COOLDOWN_TICKS = 200;

    private Flyby() { }

    static boolean fastEnough(boolean airborne, double speed) {
        return airborne && speed >= MIN_SPEED;
    }

    /** {@code lastTick} is when this player last heard it, or null if never. */
    static boolean due(double distanceSq, Long lastTick, long now) {
        if (distanceSq > RANGE * RANGE) return false;
        return lastTick == null || now - lastTick >= COOLDOWN_TICKS;
    }
}
