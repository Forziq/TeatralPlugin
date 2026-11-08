package dev.egorgrigorciuc.forzammilmod;

/** Wheel roll angle from vehicle speed. Pure maths, no Bukkit or BetterModel types. */
final class WheelSpin {

    /** Jeep wheel radius in blocks (10 px tall wheels in the model, 16 px per block). */
    static final double JEEP_WHEEL_RADIUS = 5.0 / 16.0;

    private WheelSpin() {}

    /** New roll angle in [0, 360): forward speed adds, reverse subtracts, zero speed leaves it. */
    static float advance(float angleDeg, double speedBlocksPerTick, double radiusBlocks) {
        double a = angleDeg + Math.toDegrees(speedBlocksPerTick / radiusBlocks);
        a %= 360.0;
        if (a < 0) a += 360.0;
        float f = (float) a;
        return f >= 360f ? 0f : f;
    }
}
