package dev.egorgrigorciuc.forzammilmod;

/**
 * Turret and barrel angles for a tank. Pure maths, no Bukkit or BetterModel types.
 *
 * <p>Sign conventions:
 * <ul>
 *   <li>Yaw is Minecraft yaw in degrees: 0 faces +Z, positive turns clockwise seen from above (towards -X).
 *       {@link #yaw()} is relative to the hull: 0 means the barrel points the way the hull faces.</li>
 *   <li>Pitch here is "barrel up": positive raises the barrel, negative lowers it. Minecraft player pitch is the
 *       opposite (positive looks DOWN), so use {@link #pitchUpFromMc(float)} to convert.</li>
 * </ul>
 */
final class TurretModel {

    /** Turret traverse speed, degrees per second. */
    static final float YAW_RATE = 40f;
    /** Barrel elevation speed, degrees per second. */
    static final float PITCH_RATE = 25f;
    /** Lowest barrel angle (down), degrees. */
    static final float PITCH_MIN = -5f;
    /** Highest barrel angle (up), degrees. */
    static final float PITCH_MAX = 20f;

    // Bone pivots from tank.bbmodel, in blocks (pixels / 16), relative to the model origin (the anchor's feet).
    // "forward" is the way the hull faces (the model's -Z).
    /** Height of the turret pivot above the origin. */
    static final double TURRET_PIVOT_UP = 16.0 / 16.0;
    /** Barrel pivot: this far forward of the turret pivot and this high above the origin. */
    static final double BARREL_PIVOT_FORWARD = 12.0 / 16.0;
    static final double BARREL_PIVOT_UP = 21.5 / 16.0;
    /** Muzzle is this far along the barrel from the barrel pivot (54 - 12 px, the T35 long gun). */
    static final double BARREL_LENGTH = 42.0 / 16.0;

    /** Barrel geometry: pivot this far forward of and this high above the model origin, tube this long (blocks). */
    record Gun(double pivotForward, double pivotUp, double length) {}

    static final Gun TANK_GUN = new Gun(BARREL_PIVOT_FORWARD, BARREL_PIVOT_UP, BARREL_LENGTH);
    /** artillery.bbmodel: barrel pivot 4 px forward and 16 px up, muzzle 50 px along the barrel. */
    static final Gun FIELD_GUN = new Gun(4.0 / 16.0, 16.0 / 16.0, 50.0 / 16.0);
    /** The field gun cannot point below level, and goes up to 45 degrees. */
    static final float FIELD_GUN_PITCH_MIN = 0f;
    static final float FIELD_GUN_PITCH_MAX = 45f;

    private float yaw;
    private float pitch;
    private final float pitchMin;
    private final float pitchMax;

    TurretModel() {
        this(PITCH_MIN, PITCH_MAX);
    }

    TurretModel(float pitchMin, float pitchMax) {
        this.pitchMin = pitchMin;
        this.pitchMax = pitchMax;
    }

    float yaw() {
        return yaw;
    }

    /** Barrel angle, positive = up. */
    float pitch() {
        return pitch;
    }

    /**
     * Moves towards the target, rate limited. {@code targetRelYaw} is relative to the hull (any angle, wrapped here),
     * {@code targetPitchUp} is barrel-up degrees (clamped to the limits).
     */
    void tick(float targetRelYaw, float targetPitchUp, float dtSeconds) {
        float diff = wrap(targetRelYaw - yaw);
        float maxYaw = YAW_RATE * dtSeconds;
        yaw = wrap(yaw + Math.max(-maxYaw, Math.min(maxYaw, diff)));

        float target = Math.max(pitchMin, Math.min(pitchMax, targetPitchUp));
        float maxPitch = PITCH_RATE * dtSeconds;
        pitch += Math.max(-maxPitch, Math.min(maxPitch, target - pitch));
    }

    /** Wraps an angle into [-180, 180). */
    static float wrap(float deg) {
        float r = deg % 360f;
        if (r >= 180f) r -= 360f;
        if (r < -180f) r += 360f;
        return r;
    }

    static float clampPitch(float up) {
        return Math.max(PITCH_MIN, Math.min(PITCH_MAX, up));
    }

    /** Where the driver looks, relative to the hull heading. */
    static float relativeYaw(float driverHeadYaw, float hullYaw) {
        return wrap(driverHeadYaw - hullYaw);
    }

    /** Converts Minecraft pitch (positive = looking down) to barrel-up degrees. */
    static float pitchUpFromMc(float mcPitch) {
        return -mcPitch;
    }

    /** Muzzle position (world) and unit direction. */
    record Muzzle(double x, double y, double z, double dx, double dy, double dz) {}

    /**
     * Muzzle for a hull at ({@code ax}, {@code ay}, {@code az}) heading {@code hullYaw}, with the given turret
     * angle (relative to the hull) and barrel angle (up positive). Pure geometry from the bone pivots above.
     */
    static Muzzle muzzle(double ax, double ay, double az, float hullYaw, float turretYaw, float pitchUp) {
        return muzzle(ax, ay, az, hullYaw, turretYaw, pitchUp, TANK_GUN);
    }

    /** Same as above for any barrel geometry. */
    static Muzzle muzzle(double ax, double ay, double az, float hullYaw, float turretYaw, float pitchUp, Gun gun) {
        double yawRad = Math.toRadians(hullYaw + turretYaw);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double p = Math.toRadians(pitchUp);
        double horizontal = gun.pivotForward() + gun.length() * Math.cos(p);
        double up = gun.pivotUp() + gun.length() * Math.sin(p);
        return new Muzzle(ax + fx * horizontal, ay + up, az + fz * horizontal,
                fx * Math.cos(p), Math.sin(p), fz * Math.cos(p));
    }
}
