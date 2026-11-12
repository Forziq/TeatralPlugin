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
    JEEP_HEAVY("jeep_heavy", 0.42, 0.14, 0.014, 0.040, 0.012, 4.5, false, 1.0, 1.15, 1.90, 1.5, 40),
    // ZIS-5 style army truck (model 40 x 102 px): between the jeep and the tank in speed, slow to pick up speed,
    // wide turning circle, carries a squad of eight. No gun.
    TRUCK("truck", 0.30, 0.10, 0.009, 0.032, 0.012, 3.0, false, 1.0, 1.15, 2.90, 2.4, 60),
    // ZiS-3 style field gun (model about 40 x 96 px). Cannot move under its own power: A and D swing the whole gun
    // on the spot, slowly (half a degree a tick), the barrel elevates from the gunner's look pitch.
    ARTILLERY("artillery", 0.0, 0.0, 0.0, 0.0, 0.0, 0.5, true, 0.5, 1.0, 2.2, 1.5, 50),
    // Yak-1 style fighter (model 112 px across the wings, 76 px long), 40 health. It never uses the driving
    // numbers here: FlightModel moves it. They only describe a slow taxi, so the shared driving checks still hold.
    // The hit box covers the body and the inner wing; the wing tips stick out past it.
    PLANE("plane", 0.30, 0.05, 0.010, 0.030, 0.010, 2.5, false, 1.0, 1.5, 2.2, 1.9, 40);

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

    /** True for the field gun: a barrel that lobs shells in an arc, no engine, no driving. */
    public boolean isArtillery() {
        return this == ARTILLERY;
    }

    /** True if the type has an aimable gun (both tanks and the field gun). */
    public boolean hasGun() {
        return isTank() || isArtillery();
    }

    /** True for the plane: it flies with FlightModel instead of driving with VehicleMover. */
    public boolean isPlane() {
        return this == PLANE;
    }

    /** False for the field gun: W and S do nothing, no engine sounds, no speed read-out. */
    public boolean moves() {
        return !isArtillery();
    }

    /** Ticks between shots: 4 seconds for a tank, 8 for the field gun. */
    public int reloadTicks() {
        return isArtillery() ? 160 : ShellService.RELOAD_TICKS;
    }

    /** Pitch of the engine and horn sounds: the truck plays the jeep sounds lower and slower. Range 0.5 to 2. */
    public double soundPitch() {
        return this == TRUCK ? 0.75 : 1.0;
    }

    public static VehicleType byId(String s) {
        if (s == null) return null;
        String id = s.toLowerCase(Locale.ROOT);
        for (VehicleType t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
