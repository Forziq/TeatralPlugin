package dev.egorgrigorciuc.forzammilmod;

/** Pure flight bookkeeping for one tank shell: a straight ray, 3 blocks per tick, 150 blocks max. No Bukkit types. */
final class ShellModel implements ShellPath {

    static final double SPEED = 3.0;
    static final double MAX_RANGE = 150.0;

    /** The stretch of flight to test this tick: start point, unit direction and length. */
    record Segment(double x, double y, double z, double dx, double dy, double dz, double length) {
        double endX() { return x + dx * length; }
        double endY() { return y + dy * length; }
        double endZ() { return z + dz * length; }
    }

    private double x, y, z;
    private final double dx, dy, dz;
    private double travelled;

    ShellModel(double x, double y, double z, double dx, double dy, double dz) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) {
            dx = 1;
            dy = 0;
            dz = 0;
            len = 1;
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.dx = dx / len;
        this.dy = dy / len;
        this.dz = dz / len;
    }

    boolean done() {
        return travelled >= MAX_RANGE - 1e-9;
    }

    @Override
    public java.util.List<Segment> nextTick() {
        Segment s = next();
        return s == null ? java.util.List.of() : java.util.List.of(s);
    }

    @Override
    public double travelled() {
        return travelled;
    }

    /** Next segment (and moves on to its end), or null once the range is used up. The last one is shortened. */
    Segment next() {
        if (done()) return null;
        double len = Math.min(SPEED, MAX_RANGE - travelled);
        Segment s = new Segment(x, y, z, dx, dy, dz, len);
        x = s.endX();
        y = s.endY();
        z = s.endZ();
        travelled += len;
        return s;
    }
}
