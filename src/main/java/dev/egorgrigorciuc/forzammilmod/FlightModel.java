package dev.egorgrigorciuc.forzammilmod;

/**
 * Rough plane flight, one call per server tick. Pure apart from the {@link Terrain} it is given, so it can be unit
 * tested. Speeds are blocks per tick, angles degrees. Yaw follows {@link DriveModel} (0 faces +Z). Pitch is
 * "nose up positive" (the Minecraft look pitch is the other way round). Roll is "right wing down positive".
 *
 * <p>On the ground the plane rolls like a wheeled vehicle and lifts off once it is at {@link #TAKEOFF_SPEED} and
 * the pilot looks up. In the air it flies where its nose points, as long as it is faster than {@link #STALL_SPEED};
 * slower than that it stalls: lift goes, the nose drops and it falls until it picks up speed again. Touching the
 * ground slowly with a low sink rate lands it; anything harder, or hitting a wall at speed, crashes it.
 */
public final class FlightModel {

    /** Level flight at full throttle settles here: 20 blocks a second. */
    public static final double MAX_SPEED = 1.0;
    /** A dive can push past {@link #MAX_SPEED}, never past this (26 blocks a second, so chunks keep up). */
    public static final double DIVE_LIMIT = 1.3;
    /** Lift-off speed on the ground (11 blocks a second). */
    public static final double TAKEOFF_SPEED = 0.55;
    /** Below this in the air the wing stops lifting (8 blocks a second). */
    public static final double STALL_SPEED = 0.40;
    /** A stalled plane flies again once it is back above this. */
    public static final double UNSTALL_SPEED = 0.48;
    /** Throttle change per tick while W or S is held: idle to full in 2 seconds. */
    public static final double THROTTLE_RATE = 0.025;
    static final double THRUST = 0.02;
    /** Share of the speed lost per tick to the air; with {@link #THRUST} this sets {@link #MAX_SPEED}. */
    static final double DRAG = THRUST / MAX_SPEED;
    /** Wheels on the ground slow the plane a little more with the engine at idle. */
    static final double GROUND_FRICTION = 0.004;
    /** Holding S on the ground with the throttle already at idle brakes. */
    static final double BRAKE = 0.01;
    /** Climbing costs speed and diving adds it: this times the sine of the pitch, per tick. */
    static final double CLIMB_COST = 0.025;

    /** Bank angle at full A or D, and how fast the bank changes. */
    static final float MAX_ROLL = 50f, ROLL_RATE = 4f;
    /** Turn rate at full bank: 60 degrees a second, a full circle in 6 s (about 19 blocks radius at top speed). */
    static final float AIR_TURN = 3f;
    /** Taxi steering, degrees per tick, reached at {@link #GROUND_TURN_FULL_SPEED} and above. */
    static final float GROUND_TURN = 2.5f;
    static final double GROUND_TURN_FULL_SPEED = 0.15;
    /** The nose follows the pilot's look between these, at this rate. */
    static final float PITCH_MIN = -45f, PITCH_MAX = 35f, PITCH_RATE = 2.5f;
    /** On the ground the nose can only come up this far, and must be at least {@link #LIFTOFF_PITCH} up to lift off. */
    static final float GROUND_PITCH_MAX = 12f, LIFTOFF_PITCH = 3f;
    /** A stalled plane's nose drops to this; a plane with nobody flying it glides down at this. */
    static final float STALL_NOSE = -30f, GLIDE_PITCH = -8f;
    /** Extra fall per tick while stalled, the fastest that fall gets, and how quickly it fades once flying again. */
    static final double SINK_GRAVITY = 0.04, MAX_SINK = 1.5, SINK_RECOVER = 0.85;

    /** A touchdown counts as a landing at or below this speed (15 blocks a second)... */
    public static final double LAND_MAX_SPEED = 0.75;
    /** ...coming down no faster than this (6 blocks a second)... */
    public static final double LAND_MAX_SINK = 0.30;
    /** ...wings near level and the nose not pointing hard at the ground. Anything else is a crash. */
    static final float LAND_MAX_ROLL = 30f, LAND_MAX_NOSE_DOWN = 20f;
    /** Hitting a wall (something taller than {@link #STEP}) faster than this crashes (6 blocks a second). */
    public static final double CRASH_WALL_SPEED = 0.30;
    /** Ground bumps up to this tall are rolled over, or touched down on; taller is a wall. */
    static final double STEP = 1.0;
    /** The move is checked in pieces no longer than this, so the plane can't skip through a thin wall. */
    static final double SUBSTEP = 0.5;
    /** Above this height the nose won't go above level. */
    static final double CEILING_Y = 320;
    /** The plane waits if the land this far past its nose isn't loaded yet. */
    static final double LOOKAHEAD = 3.0;

    private static final double EPS = DriveModel.EPS;
    private static final VehicleType T = VehicleType.PLANE;

    public enum Outcome { NONE, TAKEOFF, LANDED, CRASHED, WAITING }

    /**
     * The pilot's controls. {@code throttle} and {@code steer} are -1, 0 or +1 (W/S and D/A); {@code lookPitchUp}
     * is where the pilot looks, up positive. {@code piloted} false means nobody is at the controls.
     */
    public record Input(int throttle, int steer, float lookPitchUp, boolean piloted) {
        public static final Input NONE = new Input(0, 0, 0f, false);
    }

    /**
     * x, y (bottom of the wheels), z, yaw, pitch (up positive), roll (right positive), airspeed, throttle 0..1,
     * sink (extra vertical speed, down negative; on the ground the falling speed), on the ground, stalled.
     */
    public record State(double x, double y, double z, float yaw, float pitch, float roll, double speed,
                        double throttle, double sink, boolean onGround, boolean stalled) {

        /** Standing on the ground (or just above it, it settles), engine idle. */
        public static State parked(double x, double y, double z, float yaw) {
            return new State(x, y, z, DriveModel.wrap(yaw), 0f, 0f, 0, 0, 0, true, false);
        }

        /** Blocks per tick, up positive. */
        public double verticalSpeed() {
            return onGround ? sink : speed * Math.sin(Math.toRadians(pitch)) + sink;
        }
    }

    public record Result(State state, Outcome outcome) {}

    private FlightModel() {}

    public static Result step(State s, Input in, Terrain terrain) {
        boolean ground = s.onGround();
        double throttle = in.piloted()
                ? clamp(s.throttle() + in.throttle() * THROTTLE_RATE, 0, 1)
                : Math.max(0, s.throttle() - 2 * THROTTLE_RATE); // nobody at the controls: the engine winds down

        float roll = approach(s.roll(), ground || !in.piloted() ? 0f : in.steer() * MAX_ROLL, ROLL_RATE);
        float yaw = s.yaw();
        if (ground) {
            if (in.piloted() && in.steer() != 0) {
                yaw = DriveModel.wrap(yaw + in.steer() * GROUND_TURN * (float) Math.min(1, s.speed() / GROUND_TURN_FULL_SPEED));
            }
        } else {
            yaw = DriveModel.wrap(yaw + roll / MAX_ROLL * AIR_TURN);
        }

        float target;
        if (ground) target = in.piloted() ? clamp(in.lookPitchUp(), 0f, GROUND_PITCH_MAX) : 0f;
        else if (s.stalled()) target = STALL_NOSE;
        else if (!in.piloted()) target = GLIDE_PITCH;
        else target = clamp(in.lookPitchUp(), PITCH_MIN, PITCH_MAX);
        if (!ground && s.y() >= CEILING_Y) target = Math.min(target, 0f);
        float pitch = approach(s.pitch(), target, PITCH_RATE);

        double speed = s.speed() + throttle * THRUST - DRAG * s.speed();
        if (ground) {
            if (throttle < 0.05) speed -= GROUND_FRICTION;
            if (in.piloted() && in.throttle() < 0 && throttle == 0) speed -= BRAKE;
        } else {
            speed -= CLIMB_COST * Math.sin(Math.toRadians(pitch));
        }
        speed = clamp(speed, 0, DIVE_LIMIT);

        State next = new State(s.x(), s.y(), s.z(), yaw, pitch, roll, speed, throttle, s.sink(), ground, s.stalled());
        return ground ? taxi(next, s.yaw(), terrain) : fly(next, terrain, Outcome.NONE);
    }

    /** One tick on the wheels: roll forward, climb small bumps, settle onto the ground, or lift off. */
    private static Result taxi(State n, float oldYaw, Terrain t) {
        double x = n.x(), y = n.y(), z = n.z();
        float yaw = n.yaw();
        double speed = n.speed();
        boolean stuck = VehicleMover.obstacle(T, t, x, z, oldYaw, y) > STEP + EPS; // spawned in a wall: let it roll out
        if (yaw != oldYaw && !stuck && Double.isNaN(VehicleMover.place(T, t, x, z, yaw, y))) yaw = oldYaw;

        if (speed >= TAKEOFF_SPEED && n.pitch() >= LIFTOFF_PITCH) {
            State air = new State(x, y, z, yaw, n.pitch(), n.roll(), speed, n.throttle(), 0, false, false);
            return fly(air, t, Outcome.TAKEOFF);
        }

        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad) * speed, dz = Math.cos(rad) * speed;
        if (speed > 1e-6) {
            if (!aheadLoaded(t, x, z, yaw, speed)) {
                return new Result(new State(x, y, z, yaw, n.pitch(), n.roll(), speed, n.throttle(), n.sink(), true, false), Outcome.WAITING);
            }
            double ny = stuck ? y : VehicleMover.place(T, t, x + dx, z + dz, yaw, y);
            if (Double.isNaN(ny)) {
                if (speed > CRASH_WALL_SPEED) {
                    return new Result(new State(x, y, z, yaw, n.pitch(), n.roll(), speed, n.throttle(), 0, true, false), Outcome.CRASHED);
                }
                speed = 0;
            } else {
                x += dx;
                z += dz;
                y = ny;
            }
        }

        double g = VehicleMover.ground(T, t, x, z, yaw, y);
        if (g == Double.NEGATIVE_INFINITY) {
            // Rolled off something deeper than the ground search: it is flying now, or falling if too slow.
            return new Result(new State(x, y, z, yaw, n.pitch(), n.roll(), speed, n.throttle(), Math.min(0, n.sink()),
                    false, speed < STALL_SPEED), Outcome.NONE);
        }
        double[] fall = DriveModel.fall(y, n.sink(), g); // short drops (a pad spawn, a ledge) settle like a car
        return new Result(new State(x, fall[0], z, yaw, n.pitch(), n.roll(), speed, n.throttle(), fall[1], true, false), Outcome.NONE);
    }

    /** One tick in the air: fly along the nose (plus any stall sink), checked against the ground and walls. */
    private static Result fly(State n, Terrain t, Outcome base) {
        boolean stalled = n.stalled();
        if (n.speed() < STALL_SPEED) stalled = true;
        else if (n.speed() > UNSTALL_SPEED) stalled = false;
        double sink = n.sink();
        if (stalled) {
            sink = Math.max(-MAX_SINK, sink - SINK_GRAVITY);
        } else {
            sink *= SINK_RECOVER;
            if (sink > -0.005) sink = 0;
        }

        double rad = Math.toRadians(n.yaw()), pr = Math.toRadians(n.pitch());
        double h = n.speed() * Math.cos(pr);
        double vx = -Math.sin(rad) * h, vz = Math.cos(rad) * h;
        double vy = n.speed() * Math.sin(pr) + sink;

        if (!aheadLoaded(t, n.x(), n.z(), n.yaw(), h)) {
            return new Result(new State(n.x(), n.y(), n.z(), n.yaw(), n.pitch(), n.roll(), n.speed(), n.throttle(), sink,
                    false, stalled), Outcome.WAITING);
        }

        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(vx * vx + vy * vy + vz * vz) / SUBSTEP));
        double x = n.x(), y = n.y(), z = n.z();
        for (int i = 0; i < steps; i++) {
            double cx = x + vx / steps, cy = y + vy / steps, cz = z + vz / steps;
            double rel = VehicleMover.obstacle(T, t, cx, cz, n.yaw(), cy);
            if (rel <= EPS) {
                x = cx;
                y = cy;
                z = cz;
                continue;
            }
            if (rel <= STEP + EPS) { // touching the ground (or a low bump)
                if (gentle(n.speed(), -vy, n.roll(), n.pitch())) {
                    State landed = new State(cx, cy + rel, cz, n.yaw(), clamp(n.pitch(), 0f, GROUND_PITCH_MAX), n.roll(),
                            n.speed(), n.throttle(), 0, true, false);
                    return new Result(landed, Outcome.LANDED);
                }
                return new Result(new State(cx, cy + rel, cz, n.yaw(), n.pitch(), n.roll(), n.speed(), n.throttle(), sink,
                        false, stalled), Outcome.CRASHED);
            }
            // Something taller in the way: a wall, a hillside, a tree.
            if (n.speed() > CRASH_WALL_SPEED) {
                return new Result(new State(x, y, z, n.yaw(), n.pitch(), n.roll(), n.speed(), n.throttle(), sink,
                        false, stalled), Outcome.CRASHED);
            }
            // Bumped it slowly: lose the forward speed and drop from here.
            return new Result(new State(x, y, z, n.yaw(), n.pitch(), n.roll(), 0, n.throttle(), sink, false, true), base);
        }
        return new Result(new State(x, y, z, n.yaw(), n.pitch(), n.roll(), n.speed(), n.throttle(), sink, false, stalled), base);
    }

    /** True if a touchdown at this speed, sink rate (down positive), bank and pitch is a landing, not a crash. */
    static boolean gentle(double speed, double sinkRate, float roll, float pitch) {
        return speed <= LAND_MAX_SPEED + EPS && sinkRate <= LAND_MAX_SINK + EPS
                && Math.abs(roll) <= LAND_MAX_ROLL && pitch >= -LAND_MAX_NOSE_DOWN;
    }

    /** Checks the nose and both wing roots a little past where this tick's move ends. */
    private static boolean aheadLoaded(Terrain t, double x, double z, float yaw, double move) {
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double rx = -Math.cos(rad), rz = -Math.sin(rad);
        double reach = T.halfLength + move + LOOKAHEAD;
        for (int side = -1; side <= 1; side++) {
            double px = x + fx * reach + rx * side * T.halfWidth;
            double pz = z + fz * reach + rz * side * T.halfWidth;
            if (!t.loaded((int) Math.floor(px), (int) Math.floor(pz))) return false;
        }
        return true;
    }

    /** Moves {@code from} toward {@code to} by at most {@code rate}. */
    static float approach(float from, float to, float rate) {
        if (from < to) return Math.min(to, from + rate);
        if (from > to) return Math.max(to, from - rate);
        return to;
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
