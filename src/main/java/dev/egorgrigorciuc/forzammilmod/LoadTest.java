package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Hidden measuring tool (owner only): spawns a ring of mixed vehicles, drives half of them in circles and
 * fires the guns on a timer, so the tick cost can be read with {@code /fmod debug perf}.
 */
final class LoadTest {

    static final int DEFAULT_COUNT = 10;
    static final int MAX_COUNT = 40;
    static final double RING_RADIUS = 14.0;
    static final long FIRE_EVERY_TICKS = 40L;
    private static final VehicleType[] MIX = {
            VehicleType.TANK, VehicleType.ARTILLERY, VehicleType.JEEP, VehicleType.TANK_HEAVY, VehicleType.TRUCK,
            VehicleType.TANK, VehicleType.JEEP_HEAVY, VehicleType.ARTILLERY, VehicleType.PLANE, VehicleType.JEEP};

    private final JavaPlugin plugin;
    private final VehicleService vehicles;
    private final ShellService shells;
    private final List<Vehicle> spawned = new ArrayList<>();
    private BukkitTask task;

    LoadTest(JavaPlugin plugin, VehicleService vehicles, ShellService shells) {
        this.plugin = plugin;
        this.vehicles = vehicles;
        this.shells = shells;
    }

    boolean running() {
        return task != null;
    }

    int vehicleCount() {
        return spawned.size();
    }

    /** Returns how many vehicles were spawned. */
    int start(CommandSender sender, int count) {
        stop();
        Location centre = sender instanceof Player p ? p.getLocation() : plugin.getServer().getWorlds().get(0).getSpawnLocation();
        World w = centre.getWorld();
        count = Math.max(1, Math.min(MAX_COUNT, count));
        int driven = 0;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            double dx = Math.cos(angle) * RING_RADIUS, dz = Math.sin(angle) * RING_RADIUS;
            double x = Math.floor(centre.getX() + dx) + 0.5, z = Math.floor(centre.getZ() + dz) + 0.5;
            double y = w.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)); // front points away from the middle
            VehicleType type = MIX[i % MIX.length];
            if (vehicles.spawn(type, new Location(w, x, y, z), yaw) != VehicleService.SpawnResult.SPAWNED) continue;
            List<Vehicle> all = vehicles.vehicles();
            Vehicle v = all.get(all.size() - 1);
            spawned.add(v);
            if (type.moves() && !type.isPlane() && driven++ % 2 == 0) {
                v.testInput = new DriveInput(true, false, true, false, false, false); // forward and left: a slow circle
            }
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::fireAll, FIRE_EVERY_TICKS, FIRE_EVERY_TICKS);
        vehicles.perf().reset();
        return spawned.size();
    }

    private void fireAll() {
        for (Vehicle v : spawned) {
            if (v.hasTurret() && !v.destroyed) shells.fireFrom(v);
        }
    }

    /** Stops firing and removes every vehicle this test spawned. Returns how many were removed. */
    int stop() {
        if (task != null) task.cancel();
        task = null;
        int n = 0;
        for (Vehicle v : spawned) {
            v.testInput = null;
            if (!v.destroyed) {
                vehicles.remove(v);
                n++;
            }
        }
        spawned.clear();
        return n;
    }
}
