package dev.egorgrigorciuc.forzammilmod;

import java.util.Locale;

/** What the vehicle HUD shows. Pure, no Bukkit types. */
final class HudModel {

    enum Band { GREEN, YELLOW, RED }

    /** Server ticks per second; vehicle speed is blocks per tick. */
    static final int TICKS_PER_SECOND = 20;

    private HudModel() { }

    /** Green above 60%, yellow above 30% up to 60%, red at 30% or below. */
    static Band band(double fraction) {
        if (fraction > 0.60) return Band.GREEN;
        if (fraction > 0.30) return Band.YELLOW;
        return Band.RED;
    }

    /** Boss bar progress, kept inside 0..1. */
    static double progress(double fraction) {
        if (Double.isNaN(fraction)) return 0;
        return Math.max(0, Math.min(1, fraction));
    }

    /** Blocks per second from the per-tick hull speed, always positive. */
    static double blocksPerSecond(double speedPerTick) {
        return Math.abs(speedPerTick) * TICKS_PER_SECOND;
    }

    /** One decimal with a dot, e.g. "4.2". */
    static String formatSpeed(double speedPerTick) {
        return String.format(Locale.ROOT, "%.1f", blocksPerSecond(speedPerTick));
    }

    /** Whole blocks, never below zero, e.g. "42". */
    static String formatHeight(double blocks) {
        if (Double.isNaN(blocks) || blocks < 0) return "0";
        return Long.toString(Math.round(blocks));
    }

    /** Only the driver sees speed. */
    static boolean showsSpeed(CrewSeats.Seat seat) {
        return CrewSeats.drives(seat);
    }

    /** The reload line goes to whoever fires: the seat the turret controller resolves to. */
    static boolean showsReload(CrewSeats.Seat seat, CrewSeats.Seat turretController) {
        return seat != null && seat == turretController;
    }
}
