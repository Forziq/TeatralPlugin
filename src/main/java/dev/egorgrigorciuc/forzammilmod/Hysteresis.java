package dev.egorgrigorciuc.forzammilmod;

/**
 * Turns a noisy number into a steady on/off: switches on at once when the value reaches {@code on}, and only
 * switches off after the value has stayed below {@code off} for {@code holdTicks} ticks in a row. Pure.
 */
final class Hysteresis {

    private final double on;
    private final double off;
    private final int holdTicks;
    private boolean active;
    private int quiet;

    Hysteresis(double on, double off, int holdTicks) {
        this.on = on;
        this.off = off;
        this.holdTicks = holdTicks;
    }

    boolean update(double value) {
        double v = Math.abs(value);
        if (v >= on) {
            active = true;
            quiet = 0;
        } else if (active) {
            if (v < off) {
                if (++quiet >= holdTicks) active = false;
            } else {
                quiet = 0;
            }
        }
        return active;
    }
}
