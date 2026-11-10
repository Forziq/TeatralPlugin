package dev.egorgrigorciuc.forzammilmod;

/** Reload clock counted in server ticks. Pure, so it can be unit tested. */
final class ReloadTimer {

    static final int TICKS_PER_SECOND = 20;

    private final long durationTicks;
    private long readyAt;
    private boolean pendingReady;

    ReloadTimer(long durationTicks) {
        this.durationTicks = durationTicks;
    }

    boolean ready(long now) {
        return now >= readyAt;
    }

    /** Starts the reload if ready; returns whether the shot is allowed. */
    boolean tryFire(long now) {
        if (!ready(now)) return false;
        readyAt = now + durationTicks;
        pendingReady = true;
        return true;
    }

    /** Whole seconds left, rounded up; 0 when ready. */
    int remainingSeconds(long now) {
        if (ready(now)) return 0;
        return (int) ((readyAt - now + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
    }

    /** True once, the first time it is asked after a reload has finished. */
    boolean pollFinished(long now) {
        if (!pendingReady || !ready(now)) return false;
        pendingReady = false;
        return true;
    }
}
