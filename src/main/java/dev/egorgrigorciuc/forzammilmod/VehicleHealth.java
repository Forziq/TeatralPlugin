package dev.egorgrigorciuc.forzammilmod;

/** Hit points of one vehicle. Pure, no Bukkit types. */
final class VehicleHealth {

    private final int max;
    private int current;

    VehicleHealth(int max) {
        this.max = Math.max(1, max);
        this.current = this.max;
    }

    int current() { return current; }

    int max() { return max; }

    /** Takes off hit points, never below 0. */
    void damage(int amount) {
        if (amount > 0) current = Math.max(0, current - amount);
    }

    /** Gives hit points back, never above max. */
    void repair(int amount) {
        if (amount > 0) current = Math.min(max, current + amount);
    }

    boolean isDestroyed() { return current <= 0; }

    boolean isFull() { return current >= max; }

    double fraction() { return (double) current / max; }

    int percent() { return (int) Math.round(fraction() * 100); }

    /** One iron ingot restores 10% of max health, rounded up (tank 10, jeep 4). */
    static int repairAmount(int max) {
        return (int) Math.ceil(max * 0.10);
    }
}
