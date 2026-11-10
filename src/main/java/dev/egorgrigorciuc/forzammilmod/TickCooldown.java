package dev.egorgrigorciuc.forzammilmod;

/** A cooldown counted in server ticks. Pure. */
final class TickCooldown {

    private final int ticks;
    private long readyAt;

    TickCooldown(int ticks) {
        this.ticks = ticks;
    }

    /** True (and starts the cooldown) if it is ready at {@code now}. */
    boolean tryUse(long now) {
        if (now < readyAt) return false;
        readyAt = now + ticks;
        return true;
    }
}
