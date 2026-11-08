package dev.egorgrigorciuc.forzammilmod;

/** Pure rules for putting a new vehicle down: not on top of another one, and not too many in total. */
public final class PadRules {

    public enum Verdict { OK, OCCUPIED, FULL }

    /** A vehicle closer than this to the spawn point blocks the spawn. */
    public static final double OCCUPIED_RADIUS = 3;
    public static final int MAX_VEHICLES = 10;
    /** Players this close to the spawn point get the pad messages. */
    public static final double NOTIFY_RADIUS = 6;

    private PadRules() {
    }

    /** @param distances distance from each existing vehicle (same world) to the spawn point */
    public static Verdict check(double[] distances, int totalVehicles) {
        for (double d : distances) if (d < OCCUPIED_RADIUS) return Verdict.OCCUPIED;
        if (totalVehicles >= MAX_VEHICLES) return Verdict.FULL;
        return Verdict.OK;
    }
}
