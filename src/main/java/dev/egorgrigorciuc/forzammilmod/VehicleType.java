package dev.egorgrigorciuc.forzammilmod;

import java.util.Locale;

/**
 * Hard-coded vehicle stats. Speeds are blocks per tick, rates per tick, angles in degrees, sizes in blocks.
 * Half sizes come from the T15 models (tank 48 x 84 px, jeep 32 x 52 px), trimmed a little so the
 * vehicle does not snag on blocks it only grazes.
 */
public enum VehicleType {
    //    id      max   rev   accel   brake  coast  turn  onSpot step  halfW halfL height hp
    TANK("tank", 0.18, 0.08, 0.006, 0.025, 0.010, 2.5, true, 1.0, 1.45, 2.50, 1.7, 100),
    JEEP("jeep", 0.42, 0.14, 0.014, 0.040, 0.012, 4.5, false, 1.0, 0.95, 1.55, 1.2, 40),
    // Bulkier look-alikes, kept beside the plain ones until Egor picks. Same driving and health, bigger hit boxes
    // (heavy tank model 56 x 90 px, armoured jeep 40 x 68 px).
    TANK_HEAVY("tank_heavy", 0.18, 0.08, 0.006, 0.025, 0.010, 2.5, true, 1.0, 1.65, 2.65, 1.9, 100),
    JEEP_HEAVY("jeep_heavy", 0.42, 0.14, 0.014, 0.040, 0.012, 4.5, false, 1.0, 1.15, 1.90, 1.5, 40);

    public final String id;
    public final double maxSpeed;
    public final double reverseMax;
    public final double acceleration;
    public final double braking;
    /** Slow-down per tick with no throttle. */
    public final double coastDrag;
    /** Degrees per tick at full rate. */
    public final double turnRate;
    /** Tanks turn on the spot; wheeled vehicles only turn while moving, scaled by speed. */
    public final boolean turnsOnSpot;
    public final double stepHeight;
    public final double halfWidth;
    public final double halfLength;
    public final double height;
    public final int maxHealth;

    VehicleType(String id, double maxSpeed, double reverseMax, double acceleration, double braking, double coastDrag,
                double turnRate, boolean turnsOnSpot, double stepHeight, double halfWidth, double halfLength,
                double height, int maxHealth) {
        this.id = id;
        this.maxSpeed = maxSpeed;
        this.reverseMax = reverseMax;
        this.acceleration = acceleration;
        this.braking = braking;
        this.coastDrag = coastDrag;
        this.turnRate = turnRate;
        this.turnsOnSpot = turnsOnSpot;
        this.stepHeight = stepHeight;
        this.halfWidth = halfWidth;
        this.halfLength = halfLength;
        this.height = height;
        this.maxHealth = maxHealth;
    }

    /** BetterModel model name (file name of the .bbmodel). */
    public String model() {
        return id;
    }

    /** True for both tanks: turret, gunner seat, armour that depends on the side hit. */
    public boolean isTank() {
        return this == TANK || this == TANK_HEAVY;
    }

    public static VehicleType byId(String s) {
        if (s == null) return null;
        String id = s.toLowerCase(Locale.ROOT);
        for (VehicleType t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
