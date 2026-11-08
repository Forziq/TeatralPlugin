package dev.egorgrigorciuc.forzammilmod;

/** Pure rules for putting a new vehicle down: not on top of another one, and not too many in total. */
public final class PadRules {

    public enum Verdict { OK, OCCUPIED, FULL }

    /** A vehicle closer than this to the spawn point blocks the spawn. */
    public static final double OCCUPIED_RADIUS = 3;
    /** Used when config.yml has no max-vehicles value. */
    public static final int DEFAULT_MAX_VEHICLES = 10;
    /** Players this close to the spawn point get the pad messages. */
    public static final double NOTIFY_RADIUS = 6;

    private PadRules() {
    }

    /** Config value to a usable cap: anything below 1 counts as 1. */
    public static int sanitiseMax(int raw) {
        return Math.max(1, raw);
    }

    public static Verdict check(double[] distances, int totalVehicles) {
        return check(distances, totalVehicles, DEFAULT_MAX_VEHICLES);
    }

    /** @param distances distance from each existing vehicle (same world) to the spawn point */
    public static Verdict check(double[] distances, int totalVehicles, int maxVehicles) {
        for (double d : distances) if (d < OCCUPIED_RADIUS) return Verdict.OCCUPIED;
        if (totalVehicles >= maxVehicles) return Verdict.FULL;
        return Verdict.OK;
    }
}
