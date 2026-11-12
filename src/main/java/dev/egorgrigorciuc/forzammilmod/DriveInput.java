package dev.egorgrigorciuc.forzammilmod;

/** The driver's W/A/S/D, Space and Sprint keys, free of Bukkit types. Space and Sprint only matter to the plane. */
public record DriveInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sprint) {

    /** Just the four driving keys; Space and Sprint released. */
    public DriveInput(boolean forward, boolean backward, boolean left, boolean right) {
        this(forward, backward, left, right, false, false);
    }

    public static final DriveInput NONE = new DriveInput(false, false, false, false, false, false);

    /** +1 forward, -1 back, 0 for none or both. */
    public int throttle() {
        return (forward ? 1 : 0) - (backward ? 1 : 0);
    }

    /** +1 right (yaw goes up, clockwise from above), -1 left, 0 for none or both. */
    public int steer() {
        return (right ? 1 : 0) - (left ? 1 : 0);
    }

    /** Plane nose: +1 climb (Space), -1 dive (Sprint), 0 for none or both. */
    public int pitch() {
        return (jump ? 1 : 0) - (sprint ? 1 : 0);
    }

    public boolean any() {
        return forward || backward || left || right;
    }
}
