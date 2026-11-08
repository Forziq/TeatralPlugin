package dev.egorgrigorciuc.forzammilmod;

/**
 * A vehicle's hull as a box turned by its yaw: centre (cx, cz) with the floor at y, half sizes and height.
 * Pure maths for shell hits and splash range. No Bukkit types.
 */
record HullBox(double cx, double y, double cz, double yaw, double halfWidth, double halfLength, double height) {

    /** Local coordinates: x across the hull, z along its facing. */
    private double localX(double px, double pz) {
        double rad = Math.toRadians(yaw);
        return (px - cx) * -Math.cos(rad) + (pz - cz) * -Math.sin(rad);
    }

    private double localZ(double px, double pz) {
        double rad = Math.toRadians(yaw);
        return (px - cx) * -Math.sin(rad) + (pz - cz) * Math.cos(rad);
    }

    /** Distance along a unit-direction ray to the first contact with the box, 0 if it starts inside, or -1 for a miss within maxLen. */
    double rayHit(double ox, double oy, double oz, double dx, double dy, double dz, double maxLen) {
        double sx = localX(ox, oz), sz = localZ(ox, oz);
        // direction in local axes (same rotation, no translation)
        double rad = Math.toRadians(yaw);
        double ldx = dx * -Math.cos(rad) + dz * -Math.sin(rad);
        double ldz = dx * -Math.sin(rad) + dz * Math.cos(rad);
        double tMin = 0, tMax = maxLen;
        double[] s = {sx, oy - y, sz};
        double[] d = {ldx, dy, ldz};
        double[] lo = {-halfWidth, 0, -halfLength};
        double[] hi = {halfWidth, height, halfLength};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-12) {
                if (s[i] < lo[i] || s[i] > hi[i]) return -1;
            } else {
                double t1 = (lo[i] - s[i]) / d[i], t2 = (hi[i] - s[i]) / d[i];
                if (t1 > t2) { double t = t1; t1 = t2; t2 = t; }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) return -1;
            }
        }
        return tMin;
    }

    /** Shortest distance from a point to the box (0 inside). */
    double distanceTo(double px, double py, double pz) {
        double lx = localX(px, pz), lz = localZ(px, pz);
        double ex = Math.max(0, Math.abs(lx) - halfWidth);
        double ez = Math.max(0, Math.abs(lz) - halfLength);
        double ey = py < y ? y - py : Math.max(0, py - (y + height));
        return Math.sqrt(ex * ex + ey * ey + ez * ez);
    }
}
