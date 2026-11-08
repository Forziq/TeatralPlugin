package dev.egorgrigorciuc.forzammilmod;

/**
 * Pure driving rules, one call per server tick. Yaw follows Minecraft: 0 faces +Z (south), 90 faces -X (west),
 * so the forward vector is (-sin yaw, cos yaw).
 */
public final class DriveModel {

    /** Blocks per tick squared, the same pull vanilla uses for falling entities. */
    public static final double GRAVITY = 0.08;
    public static final double AIR_DRAG = 0.98;
    /** Fastest fall per tick; kept below the ground search depth so a vehicle can't skip through the floor. */
    public static final double TERMINAL_FALL = 2.0;
    public static final double EPS = 1e-3;

    public enum Step { MOVE, STEP_UP, BLOCKED }

    /** New speed and yaw, plus the horizontal move planned for this tick. */
    public record Result(double speed, float yaw, double dx, double dz) {}

    private DriveModel() {}

    public static Result step(VehicleType t, DriveInput in, double speed, float yaw) {
        double s = speed;
        int throttle = in.throttle();
        if (throttle > 0) {
            s = s < 0 ? Math.min(0, s + t.braking) : Math.min(t.maxSpeed, s + t.acceleration);
        } else if (throttle < 0) {
            s = s > 0 ? Math.max(0, s - t.braking) : Math.max(-t.reverseMax, s - t.acceleration);
        } else {
            s = towardZero(s, t.coastDrag);
        }
        s = Math.max(-t.reverseMax, Math.min(t.maxSpeed, s));

        float newYaw = yaw;
        int steer = in.steer();
        if (steer != 0) {
            double factor;
            if (t.turnsOnSpot) {
                factor = 1;
            } else {
                // Wheels: no turning at a standstill, full lock from half speed; reversing swings the nose the other way.
                factor = Math.min(1, Math.abs(s) / (t.maxSpeed * 0.5)) * Math.signum(s);
            }
            newYaw = wrap(yaw + (float) (steer * t.turnRate * factor));
        }
        double rad = Math.toRadians(newYaw);
        return new Result(s, newYaw, -Math.sin(rad) * s, Math.cos(rad) * s);
    }

    /** Moves {@code v} toward zero by {@code amount} without crossing it. */
    static double towardZero(double v, double amount) {
        if (v > 0) return Math.max(0, v - amount);
        if (v < 0) return Math.min(0, v + amount);
        return 0;
    }

    /** Wraps to [-180, 180). */
    public static float wrap(float yaw) {
        float y = yaw % 360f;
        if (y >= 180f) y -= 360f;
        if (y < -180f) y += 360f;
        return y;
    }

    /**
     * What to do about the highest obstacle in the way, measured from the vehicle's feet:
     * nothing above the feet moves on, up to the step height climbs, anything taller stops.
     */
    public static Step stepDecision(double obstacleHeight, double stepHeight) {
        if (obstacleHeight <= EPS) return Step.MOVE;
        if (obstacleHeight <= stepHeight + EPS) return Step.STEP_UP;
        return Step.BLOCKED;
    }

    /** Vertical speed after one tick in the air (negative is down). */
    public static double fallVelocity(double vy) {
        return Math.max(-TERMINAL_FALL, (vy - GRAVITY) * AIR_DRAG);
    }

    /** One tick of falling: returns {newFeetY, newVy}. Lands on {@code groundTop} instead of passing it. */
    public static double[] fall(double feetY, double vy, double groundTop) {
        if (groundTop >= feetY - EPS) return new double[] {Math.max(feetY, groundTop), 0};
        double nvy = fallVelocity(vy);
        double ny = feetY + nvy;
        if (ny <= groundTop) return new double[] {groundTop, 0};
        return new double[] {ny, nvy};
    }
}
