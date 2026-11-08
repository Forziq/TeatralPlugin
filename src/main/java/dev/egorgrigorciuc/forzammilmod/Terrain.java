package dev.egorgrigorciuc.forzammilmod;

/** Block collision lookup, so the movement code can be tested without a server. */
@FunctionalInterface
public interface Terrain {
    /**
     * The block's collision box as {minY, maxY} in world coordinates, or null when nothing collides there.
     * Unloaded chunks should report a full block so vehicles never drive into them.
     */
    double[] span(int x, int y, int z);
}
