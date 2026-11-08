package dev.egorgrigorciuc.forzammilmod;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One tick of vehicle movement against the world: drive rules, then collision with blocks under and around
 * the footprint, then gravity. Pure apart from the {@link Terrain} it is given.
 */
public final class VehicleMover {

    /** x, y (feet), z, hull yaw, speed along the hull, vertical speed. */
    public record State(double x, double y, double z, float yaw, double speed, double vy) {}

    /** How far down we look for ground each tick; must exceed {@link DriveModel#TERMINAL_FALL}. */
    static final int GROUND_SEARCH = 4;
    /** Max spacing between footprint sample points; under one block so no block column is skipped. */
    static final double SAMPLE_SPACING = 0.9;
    /** Sliding along a wall keeps this share of the speed. */
    static final double SLIDE_KEEP = 0.8;

    private static final double EPS = DriveModel.EPS;
    private static final Map<VehicleType, double[][]> LOCAL = new EnumMap<>(VehicleType.class);

    static {
        for (VehicleType t : VehicleType.values()) LOCAL.put(t, localSamples(t.halfWidth, t.halfLength));
    }

    private VehicleMover() {}

    public static State tick(VehicleType t, Terrain terrain, State s, DriveInput in) {
        DriveModel.Result r = DriveModel.step(t, in, s.speed(), s.yaw());
        double x = s.x(), y = s.y(), z = s.z();
        float yaw = s.yaw();
        double speed = r.speed();

        // Already inside blocks taller than a step (spawned in a wall, a block placed into it): let it drive out.
        boolean stuck = obstacle(t, terrain, x, z, yaw, y) > t.stepHeight + EPS;

        if (r.yaw() != yaw) {
            double ny = stuck ? y : place(t, terrain, x, z, r.yaw(), y);
            if (!Double.isNaN(ny)) {
                yaw = r.yaw();
                y = ny;
            }
        }

        double dx = -Math.sin(Math.toRadians(yaw)) * speed;
        double dz = Math.cos(Math.toRadians(yaw)) * speed;
        if (Math.abs(dx) > 1e-6 || Math.abs(dz) > 1e-6) {
            if (stuck) {
                x += dx;
                z += dz;
            } else {
                double ny = place(t, terrain, x + dx, z + dz, yaw, y);
                if (!Double.isNaN(ny)) {
                    x += dx;
                    z += dz;
                    y = ny;
                } else {
                    double nyx = Math.abs(dx) > 0.01 ? place(t, terrain, x + dx, z, yaw, y) : Double.NaN;
                    double nyz = Double.isNaN(nyx) && Math.abs(dz) > 0.01 ? place(t, terrain, x, z + dz, yaw, y) : Double.NaN;
                    if (!Double.isNaN(nyx)) {
                        x += dx;
                        y = nyx;
                        speed *= SLIDE_KEEP;
                    } else if (!Double.isNaN(nyz)) {
                        z += dz;
                        y = nyz;
                        speed *= SLIDE_KEEP;
                    } else {
                        speed = 0;
                    }
                }
            }
        }

        double[] fall = DriveModel.fall(y, s.vy(), ground(t, terrain, x, z, yaw, y));
        return new State(x, fall[0], z, yaw, speed, fall[1]);
    }

    /**
     * Feet height the vehicle would have at (x, z, yaw) starting from {@code feetY}: the same height if clear,
     * one step higher if it can climb and there is room above, NaN if blocked.
     */
    static double place(VehicleType t, Terrain terrain, double x, double z, float yaw, double feetY) {
        double rel = obstacle(t, terrain, x, z, yaw, feetY);
        switch (DriveModel.stepDecision(rel, t.stepHeight)) {
            case MOVE:
                return feetY;
            case STEP_UP: {
                double up = feetY + rel;
                return obstacle(t, terrain, x, z, yaw, up) <= EPS ? up : Double.NaN;
            }
            default:
                return Double.NaN;
        }
    }

    /** Highest block top inside the vehicle's body (feet up to its height), relative to the feet; 0 if clear. */
    static double obstacle(VehicleType t, Terrain terrain, double x, double z, float yaw, double feetY) {
        double rel = 0;
        double head = feetY + t.height;
        int fromY = (int) Math.floor(feetY) - 1; // includes fences and walls poking up from below
        int toY = (int) Math.floor(head);
        for (double[] p : samples(t, x, z, yaw)) {
            int bx = (int) Math.floor(p[0]);
            int bz = (int) Math.floor(p[1]);
            for (int by = fromY; by <= toY; by++) {
                double[] span = terrain.span(bx, by, bz);
                if (span != null && span[1] > feetY + EPS && span[0] < head - EPS) {
                    rel = Math.max(rel, span[1] - feetY);
                }
            }
        }
        return rel;
    }

    /** Highest block top at or just below the feet under any footprint point; -infinity if none nearby. */
    static double ground(VehicleType t, Terrain terrain, double x, double z, float yaw, double feetY) {
        double best = Double.NEGATIVE_INFINITY;
        int top = (int) Math.floor(feetY + EPS);
        for (double[] p : samples(t, x, z, yaw)) {
            int bx = (int) Math.floor(p[0]);
            int bz = (int) Math.floor(p[1]);
            for (int by = top; by >= top - GROUND_SEARCH; by--) {
                double[] span = terrain.span(bx, by, bz);
                if (span != null && span[1] <= feetY + EPS) {
                    best = Math.max(best, span[1]);
                    break;
                }
            }
        }
        return best;
    }

    /** World positions {x, z} of the footprint sample points for this pose. */
    static List<double[]> samples(VehicleType t, double x, double z, float yaw) {
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad), fz = Math.cos(rad); // forward
        double rx = -Math.cos(rad), rz = -Math.sin(rad); // right-hand side
        double[][] local = LOCAL.get(t);
        List<double[]> out = new ArrayList<>(local.length);
        for (double[] l : local) {
            out.add(new double[] {x + rx * l[0] + fx * l[1], z + rz * l[0] + fz * l[1]});
        }
        return out;
    }

    /** Grid over the footprint {side, forward}, edges included, spacing at most {@link #SAMPLE_SPACING}. */
    static double[][] localSamples(double halfWidth, double halfLength) {
        int nw = (int) Math.ceil(2 * halfWidth / SAMPLE_SPACING) + 1;
        int nl = (int) Math.ceil(2 * halfLength / SAMPLE_SPACING) + 1;
        double[][] out = new double[nw * nl][];
        int i = 0;
        for (int a = 0; a < nw; a++) {
            double w = -halfWidth + 2 * halfWidth * a / (nw - 1);
            for (int b = 0; b < nl; b++) {
                double l = -halfLength + 2 * halfLength * b / (nl - 1);
                out[i++] = new double[] {w, l};
            }
        }
        return out;
    }
}
