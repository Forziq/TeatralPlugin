package dev.egorgrigorciuc.forzammilmod;

/** Which side of a hull a shell struck. Pure, no Bukkit types. Yaw is the Minecraft convention (0 = +Z, 90 = -X). */
enum HitSide {
    FRONT, SIDE, BACK;

    /** Half-angle of the front and back cones, in degrees. */
    static final double CONE = 45.0;

    /** Yaw of a horizontal direction (dx, dz), Minecraft convention. */
    static double yawOf(double dx, double dz) {
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Smallest signed difference a - b, in (-180, 180]. */
    static double wrapDiff(double a, double b) {
        double d = (a - b) % 360.0;
        if (d > 180.0) d -= 360.0;
        if (d <= -180.0) d += 360.0;
        return d;
    }

    /** Side struck by a shell travelling along (dx, dz) against a hull facing hullYaw. */
    static HitSide classify(double hullYaw, double dx, double dz) {
        if (Math.abs(dx) < 1e-9 && Math.abs(dz) < 1e-9) return SIDE;
        return classifyYaw(hullYaw, yawOf(dx, dz));
    }

    /** Same, with the shell's travel direction given as a yaw. Shell flying opposite to the hull's facing hits the front. */
    static HitSide classifyYaw(double hullYaw, double shellYaw) {
        double rel = Math.abs(wrapDiff(shellYaw, hullYaw));
        if (rel >= 180.0 - CONE - 1e-9) return FRONT;
        if (rel <= CONE + 1e-9) return BACK;
        return SIDE;
    }
}
