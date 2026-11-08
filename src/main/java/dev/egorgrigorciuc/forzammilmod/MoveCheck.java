package dev.egorgrigorciuc.forzammilmod;

/** Decides whether a move event counts as the player walking away (pure maths, no Bukkit types). */
public final class MoveCheck {

    private static final double STEP = 0.05;

    private MoveCheck() {}

    public static boolean isMoving(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        if ((int) Math.floor(fromX) != (int) Math.floor(toX)
                || (int) Math.floor(fromY) != (int) Math.floor(toY)
                || (int) Math.floor(fromZ) != (int) Math.floor(toZ)) {
            return true;
        }
        return Math.abs(toX - fromX) > STEP || Math.abs(toZ - fromZ) > STEP;
    }
}
