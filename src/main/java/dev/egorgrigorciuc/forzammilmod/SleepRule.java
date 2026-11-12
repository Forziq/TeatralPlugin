package dev.egorgrigorciuc.forzammilmod;

/**
 * Parked, empty vehicles do not need physics, sounds or the HUD every tick. A vehicle falls asleep after it has
 * been quiet for {@link #QUIET_TICKS} ticks (the sounds get time to stop) and gets a full check every
 * {@link #WAKE_EVERY} ticks anyway, in case the ground under it changed. Anyone getting in wakes it at once.
 */
final class SleepRule {

    static final int QUIET_TICKS = 40;
    static final int WAKE_EVERY = 20;

    private int quiet;

    /**
     * @param quiet true when nobody is aboard, it is not moving or falling or flying, and it is not smoking
     * @return true if this tick can be skipped
     */
    boolean sleepsNow(boolean quiet, long tick, int phase) {
        if (!quiet) {
            this.quiet = 0;
            return false;
        }
        if (this.quiet < QUIET_TICKS) {
            this.quiet++;
            return false;
        }
        return (tick + phase) % WAKE_EVERY != 0;
    }

    boolean asleep() {
        return quiet >= QUIET_TICKS;
    }
}
