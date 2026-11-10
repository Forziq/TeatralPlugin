package dev.egorgrigorciuc.forzammilmod;

import java.util.ArrayList;
import java.util.List;

/**
 * Flight of a field-gun shell: constant horizontal speed, gravity pulling down, no drag. Time is in server ticks and
 * positions come from the exact parabola, so the path does not depend on how finely a tick is cut up. Each tick is
 * split into straight chords no longer than {@link #MAX_CHORD} blocks, so every block along the way is tested.
 * No Bukkit types.
 */
final class BallisticArc implements ShellPath {

    /** Blocks per tick squared (the same pull arrows get). */
    static final double GRAVITY = 0.05;
    /** Blocks per tick at the muzzle: with the gravity above, 45 degrees on flat ground carries about 300 blocks. */
    static final double MUZZLE_SPEED = 3.87;
    /** Cut-off: 30 seconds in the air. */
    static final int MAX_TICKS = 600;
    /** No chord is longer than this, so a ray never skips a block column. */
    static final double MAX_CHORD = 1.0;

    private final double x0, y0, z0;
    private final double vx, vy, vz;
    private final double gravity;
    private int tick;
    private double travelled;

    /** @param dx,dy,dz aim direction (any length); {@code speed} blocks per tick. */
    BallisticArc(double x, double y, double z, double dx, double dy, double dz, double speed, double gravity) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) {
            dx = 1;
            dy = 0;
            dz = 0;
            len = 1;
        }
        this.x0 = x;
        this.y0 = y;
        this.z0 = z;
        this.vx = dx / len * speed;
        this.vy = dy / len * speed;
        this.vz = dz / len * speed;
        this.gravity = gravity;
    }

    /** Standard shell from a muzzle with the given aim. */
    static BallisticArc fire(double x, double y, double z, double dx, double dy, double dz) {
        return new BallisticArc(x, y, z, dx, dy, dz, MUZZLE_SPEED, GRAVITY);
    }

    /** Position {@code t} ticks after firing: {x, y, z}. */
    double[] at(double t) {
        return new double[] {x0 + vx * t, y0 + vy * t - 0.5 * gravity * t * t, z0 + vz * t};
    }

    /** Vertical speed {@code t} ticks after firing, blocks per tick (negative is falling). */
    double verticalSpeed(double t) {
        return vy - gravity * t;
    }

    int ticks() {
        return tick;
    }

    boolean done() {
        return tick >= MAX_TICKS;
    }

    @Override
    public double travelled() {
        return travelled;
    }

    /** The chords of the next tick; empty once {@link #MAX_TICKS} have passed. */
    @Override
    public List<ShellModel.Segment> nextTick() {
        if (done()) return List.of();
        double[] a = at(tick);
        double[] b = at(tick + 1);
        double chord = Math.sqrt(sq(b[0] - a[0]) + sq(b[1] - a[1]) + sq(b[2] - a[2]));
        // The path is a parabola, so the tick is longer than its chord; cut by the longer of the chord and the
        // speed at either end so every piece stays under the limit.
        double reach = Math.max(chord, Math.max(speedAt(tick), speedAt(tick + 1)));
        int n = Math.max(1, (int) Math.ceil(reach / MAX_CHORD));
        List<ShellModel.Segment> out = new ArrayList<>(n);
        double[] from = a;
        for (int i = 1; i <= n; i++) {
            double[] to = i == n ? b : at(tick + (double) i / n);
            double dx = to[0] - from[0], dy = to[1] - from[1], dz = to[2] - from[2];
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 1e-9) {
                out.add(new ShellModel.Segment(from[0], from[1], from[2], dx / len, dy / len, dz / len, len));
                travelled += len;
            }
            from = to;
        }
        tick++;
        return out;
    }

    private double speedAt(double t) {
        double vyT = verticalSpeed(t);
        return Math.sqrt(vx * vx + vyT * vyT + vz * vz);
    }

    private static double sq(double v) {
        return v * v;
    }

    /** Textbook flat-ground range for a level launch: speed squared times sin(2 angle) over gravity. */
    static double flatRange(double speed, double gravity, double angleDeg) {
        return speed * speed * Math.sin(2 * Math.toRadians(angleDeg)) / gravity;
    }

    /** Range found by flying the arc from ground level until it comes back down, interpolating the last tick. */
    static double simulatedRange(double speed, double gravity, double angleDeg) {
        double a = Math.toRadians(angleDeg);
        BallisticArc arc = new BallisticArc(0, 0, 0, Math.cos(a), Math.sin(a), 0, speed, gravity);
        for (int t = 1; t < 100_000; t++) {
            double[] p = arc.at(t);
            if (p[1] <= 0) {
                double[] q = arc.at(t - 1);
                double f = q[1] / (q[1] - p[1]);
                return q[0] + (p[0] - q[0]) * f;
            }
        }
        return Double.NaN;
    }
}
