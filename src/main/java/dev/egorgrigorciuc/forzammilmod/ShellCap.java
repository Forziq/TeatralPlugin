package dev.egorgrigorciuc.forzammilmod;

/** Limits on shells and their effects, so a busy fight cannot flood the server. */
final class ShellCap {

    /** Most shells in the air at once; a new shot beyond this is refused quietly. */
    static final int MAX_LIVE = 40;
    /** Most smoke puffs drawn along one shell's path in one tick. */
    static final int MAX_TRAIL_PUFFS = 6;

    private ShellCap() {
    }

    static boolean canFire(int liveShells) {
        return liveShells < MAX_LIVE;
    }

    /** How many trail puffs to draw for a stretch of this length: one per block, at least one, capped. */
    static int trailPuffs(double length) {
        return Math.max(1, Math.min(MAX_TRAIL_PUFFS, (int) Math.ceil(length)));
    }
}
