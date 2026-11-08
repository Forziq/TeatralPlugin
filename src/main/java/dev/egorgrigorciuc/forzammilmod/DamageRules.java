package dev.egorgrigorciuc.forzammilmod;

/** Hard-coded damage numbers for vehicles. Pure, no Bukkit types. */
final class DamageRules {

    static final double SPLASH_RADIUS = 4.0;
    static final int SPLASH_MAX = 10;
    static final int MELEE_JEEP = 2;
    static final int ARROW_JEEP = 3;
    /** Below this share of max health the vehicle smokes. */
    static final double SMOKE_BELOW = 0.30;

    private DamageRules() { }

    /** Direct shell hit. Tanks: front 25, side 35, back 50. Jeeps: 40 from anywhere. */
    static int shell(VehicleType type, HitSide side) {
        if (type == VehicleType.JEEP) return 40;
        return switch (side) {
            case FRONT -> 25;
            case SIDE -> 35;
            case BACK -> 50;
        };
    }

    /** Splash from an explosion {@code distance} blocks from the hull: 10 up close, 0 at 4 blocks. */
    static int splash(double distance) {
        if (distance >= SPLASH_RADIUS) return 0;
        return (int) Math.round(SPLASH_MAX * (1.0 - Math.max(0, distance) / SPLASH_RADIUS));
    }

    static int melee(VehicleType type) {
        return type == VehicleType.JEEP ? MELEE_JEEP : 0;
    }

    static int arrow(VehicleType type) {
        return type == VehicleType.JEEP ? ARROW_JEEP : 0;
    }
}
