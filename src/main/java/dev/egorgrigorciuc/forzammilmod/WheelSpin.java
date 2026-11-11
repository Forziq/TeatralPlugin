package dev.egorgrigorciuc.forzammilmod;

import java.util.List;

/** Wheel roll angle from vehicle speed, and which model bones roll. Pure, no Bukkit or BetterModel types. */
final class WheelSpin {

    /** Jeep wheel radius in blocks (10 px tall wheels in the model, 16 px per block). */
    static final double JEEP_WHEEL_RADIUS = 5.0 / 16.0;
    /** Tank road wheel radius in blocks (10.5 px tall road wheels). */
    static final double TANK_WHEEL_RADIUS = 5.25 / 16.0;

    /** Road wheel radius of the heavy tank and tyre radius of the armoured jeep (12 px tall in both models). */
    static final double HEAVY_WHEEL_RADIUS = 6.0 / 16.0;

    /** Jeep wheel bones. */
    static final List<String> JEEP_WHEELS = List.of("wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr");
    /** Truck wheel bones: front axle, then two rear axles (a 6x4 look). */
    static final List<String> TRUCK_WHEELS = List.of("wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr", "wheel_rl2", "wheel_rr2");
    /** Tank road wheel bones, five a side, numbered front to back; l is the tank's left (-X). */
    static final List<String> TANK_WHEELS = List.of(
            "wheel_l1", "wheel_l2", "wheel_l3", "wheel_l4", "wheel_l5",
            "wheel_r1", "wheel_r2", "wheel_r3", "wheel_r4", "wheel_r5");

    private WheelSpin() {}

    /** Bones that roll with speed on this vehicle type's model. */
    static List<String> bones(VehicleType type) {
        return switch (type) {
            case TANK, TANK_HEAVY -> TANK_WHEELS;
            case JEEP, JEEP_HEAVY -> JEEP_WHEELS;
            case TRUCK -> TRUCK_WHEELS;
            case ARTILLERY -> List.of(); // the wheels never roll: the gun is only turned on the spot
            case PLANE -> List.of(); // rough model: the landing gear doesn't turn
        };
    }

    /** Radius of the rolling wheels in blocks. */
    static double radius(VehicleType type) {
        return switch (type) {
            case TANK -> TANK_WHEEL_RADIUS;
            case JEEP -> JEEP_WHEEL_RADIUS;
            case TANK_HEAVY, JEEP_HEAVY, TRUCK, ARTILLERY, PLANE -> HEAVY_WHEEL_RADIUS;
        };
    }

    /** New roll angle in [0, 360): forward speed adds, reverse subtracts, zero speed leaves it. */
    static float advance(float angleDeg, double speedBlocksPerTick, double radiusBlocks) {
        double a = angleDeg + Math.toDegrees(speedBlocksPerTick / radiusBlocks);
        a %= 360.0;
        if (a < 0) a += 360.0;
        float f = (float) a;
        return f >= 360f ? 0f : f;
    }
}
