package dev.egorgrigorciuc.forzammilmod;

/** The driver's W/A/S/D keys, free of Bukkit types. */
public record DriveInput(boolean forward, boolean backward, boolean left, boolean right) {

    public static final DriveInput NONE = new DriveInput(false, false, false, false);

    /** +1 forward, -1 back, 0 for none or both. */
    public int throttle() {
        return (forward ? 1 : 0) - (backward ? 1 : 0);
    }

    /** +1 right (yaw goes up, clockwise from above), -1 left, 0 for none or both. */
    public int steer() {
        return (right ? 1 : 0) - (left ? 1 : 0);
    }

    public boolean any() {
        return forward || backward || left || right;
    }
}
