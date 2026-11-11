package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.Collection;

/** {@link Terrain} backed by a live world. Main thread only. Never loads chunks. */
final class BukkitTerrain implements Terrain {

    private final World world;

    BukkitTerrain(World world) {
        this.world = world;
    }

    @Override
    public double[] span(int x, int y, int z) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) return null;
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return new double[] {y, y + 1}; // treat as a wall
        // Collision shape boxes are relative to the block corner.
        Collection<BoundingBox> boxes = world.getBlockAt(x, y, z).getCollisionShape().getBoundingBoxes();
        if (boxes.isEmpty()) return null;
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (BoundingBox b : boxes) {
            min = Math.min(min, b.getMinY());
            max = Math.max(max, b.getMaxY());
        }
        if (max <= min) return null;
        return new double[] {y + min, y + max};
    }

    @Override
    public boolean loaded(int x, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4);
    }
}
