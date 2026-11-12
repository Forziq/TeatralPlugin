package dev.egorgrigorciuc.forzammilmod;

/**
 * The plane's machine guns: one burst per click, then a pause. Pure timing and numbers, no Bukkit types; the
 * shooting itself is in {@link PlaneGuns}. Times are server ticks.
 */
final class MachineGun {

    /** Bullets in a burst, spread evenly over {@link #BURST_TICKS} (10 ticks = half a second). */
    static final int BURST_SHOTS = 5;
    static final int BURST_TICKS = 10;
    /** Quiet time after a burst ends before the next click fires (30 ticks = 1.5 seconds). */
    static final int COOLDOWN_TICKS = 30;
    /** Ticks between two bullets of one burst. */
    static final int SHOT_GAP = BURST_TICKS / BURST_SHOTS;
    /** How far a bullet flies, in blocks, and how wide the ray is for catching mobs and players. */
    static final double RANGE = 60.0;
    static final double RAY_SIZE = 0.3;
    /** Damage per bullet: 3.0 is a bit over one heart and a half; vehicles lose 2 health. */
    static final double LIVING_DAMAGE = 3.0;
    static final int VEHICLE_DAMAGE = 2;
    /** The muzzle sits this far ahead of the plane's centre along the nose, and this high above the wheels. */
    static final double MUZZLE_FORWARD = 2.6;
    static final double MUZZLE_UP = 0.85;

    private long startedAt = Long.MIN_VALUE / 2;
    private boolean bursting;
    private int fired;

    /** Starts a burst at {@code now} if none is running and the cooldown is over. Returns true if it started. */
    boolean tryStart(long now) {
        if (bursting || now < startedAt + BURST_TICKS + COOLDOWN_TICKS) return false;
        bursting = true;
        startedAt = now;
        fired = 0;
        return true;
    }

    /** How many bullets leave the gun this tick (the first one the tick the burst starts). Ends the burst after the last. */
    int shotsDue(long now) {
        if (!bursting) return 0;
        int due = 0;
        while (fired < BURST_SHOTS && startedAt + (long) fired * SHOT_GAP <= now) {
            fired++;
            due++;
        }
        if (fired >= BURST_SHOTS) bursting = false;
        return due;
    }

    /** Drops a running burst (pilot left, plane gone). The cooldown still counts from the start. */
    void cancel() {
        bursting = false;
    }

    boolean bursting() {
        return bursting;
    }

    /** True if a click at {@code now} would start a burst. */
    boolean ready(long now) {
        return !bursting && now >= startedAt + BURST_TICKS + COOLDOWN_TICKS;
    }

    /** Unit vector along the nose: yaw 0 faces +Z (as {@link DriveModel}), pitch is nose up positive. */
    static double[] direction(float yawDeg, float pitchUpDeg) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchUpDeg);
        double h = Math.cos(pitch);
        return new double[] {-Math.sin(yaw) * h, Math.sin(pitch), Math.cos(yaw) * h};
    }
}
