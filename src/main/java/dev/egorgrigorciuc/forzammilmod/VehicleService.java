package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** All live vehicles and the one tick loop that drives them. Main thread only. Vehicles are not saved. */
public final class VehicleService {

    public enum SpawnResult { SPAWNED, UNAVAILABLE, MODEL_MISSING }

    public enum MountResult { MOUNTED, ALREADY_DRIVING, HAS_DRIVER, NOT_READY }

    static final String TAG = "forzammilmod_vehicle";

    private final JavaPlugin plugin;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final Map<World, Terrain> terrains = new HashMap<>();
    private BukkitTask task;
    private boolean loggedSeated;
    private boolean loggedPolledInput;
    private boolean loggedInputEvent;

    public VehicleService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, (Runnable) this::tickAll, 1L, 1L);
    }

    public boolean available() {
        Plugin bm = plugin.getServer().getPluginManager().getPlugin("BetterModel");
        return bm != null && bm.isEnabled();
    }

    public List<Vehicle> vehicles() {
        return List.copyOf(vehicles);
    }

    public SpawnResult spawn(VehicleType type, Location at, float yaw) {
        if (!available()) return SpawnResult.UNAVAILABLE;
        try {
            if (!BetterModelHook.hasModel(type.model())) {
                plugin.getLogger().warning("BetterModel has no model named " + type.model() + "; check its log");
                return SpawnResult.MODEL_MISSING;
            }
        } catch (LinkageError e) {
            return SpawnResult.UNAVAILABLE;
        }
        World world = at.getWorld();
        Location base = new Location(world, at.getX(), at.getY(), at.getZ(), yaw, 0f);
        ArmorStand anchor = world.spawn(base, ArmorStand.class, a -> {
            a.setInvisible(true);
            a.setMarker(true);
            a.setGravity(false);
            a.setSilent(true);
            a.setInvulnerable(true);
            a.setPersistent(false);
            a.setBasePlate(false);
            a.setCollidable(false);
            a.addScoreboardTag(TAG);
        });
        Interaction box = world.spawn(new Location(world, at.getX(), at.getY(), at.getZ()), Interaction.class, i -> {
            // Interaction boxes are square; cover the width and a bit, the ends stay unclickable.
            i.setInteractionWidth((float) (2 * type.halfWidth + 0.4));
            i.setInteractionHeight((float) type.height);
            i.setResponsive(true);
            i.setPersistent(false);
            i.addScoreboardTag(TAG);
        });
        Vehicle v = new Vehicle(type, anchor, box, new VehicleMover.State(at.getX(), at.getY(), at.getZ(), DriveModel.wrap(yaw), 0, 0));
        VehicleRig rig;
        try {
            rig = BetterModelHook.attachVehicle(anchor, type.model(), v::renderYaw, plugin.getLogger());
        } catch (RuntimeException | LinkageError e) {
            plugin.getLogger().log(Level.WARNING, "Could not draw the " + type.id + " model", e);
            rig = null;
        }
        if (rig == null) {
            anchor.remove();
            box.remove();
            return SpawnResult.MODEL_MISSING;
        }
        v.rig(rig);
        vehicles.add(v);
        return SpawnResult.SPAWNED;
    }

    public Vehicle find(Entity e) {
        for (Vehicle v : vehicles) if (v.owns(e)) return v;
        return null;
    }

    public Vehicle drivenBy(Player p) {
        UUID id = p.getUniqueId();
        for (Vehicle v : vehicles) if (id.equals(v.driver())) return v;
        return null;
    }

    public Vehicle nearest(Location at, double radius) {
        Vehicle best = null;
        double bestSq = radius * radius;
        for (Vehicle v : vehicles) {
            if (!v.world.equals(at.getWorld())) continue;
            double d = v.location().distanceSquared(at);
            if (d <= bestSq) {
                bestSq = d;
                best = v;
            }
        }
        return best;
    }

    public MountResult mount(Player p, Vehicle v) {
        if (p.getUniqueId().equals(v.driver())) return MountResult.ALREADY_DRIVING;
        if (v.driver() != null) {
            Player current = plugin.getServer().getPlayer(v.driver());
            if (current != null && v.rig().isSeated(current)) return MountResult.HAS_DRIVER;
            v.driver(null);
        }
        Vehicle other = drivenBy(p);
        if (other != null) driverGone(p);
        if (p.isInsideVehicle()) p.leaveVehicle();
        boolean ok;
        try {
            ok = v.rig().seat(p);
        } catch (RuntimeException | LinkageError e) {
            plugin.getLogger().log(Level.WARNING, "Could not seat " + p.getName() + " in the " + v.type.id, e);
            ok = false;
        }
        if (!ok) return MountResult.NOT_READY;
        v.driver(p.getUniqueId());
        if (!loggedSeated) {
            loggedSeated = true;
            plugin.getLogger().info("Driver check: " + p.getName() + " is seated in the " + v.type.id
                    + " (BetterModel seat). Next lines say whether W/A/S/D arrive while riding.");
        }
        return MountResult.MOUNTED;
    }

    /** Driver quit, died or teleported away: take them out without moving them. */
    public void driverGone(Player p) {
        Vehicle v = drivenBy(p);
        if (v == null) return;
        v.driver(null);
        try {
            v.rig().unseat(p);
        } catch (RuntimeException | LinkageError ignored) {
            // nothing more we can do
        }
    }

    /** Logs once whether the input event fires for someone who is driving. */
    public void noteInputEvent(Player p, Input in) {
        if (loggedInputEvent || drivenBy(p) == null) return;
        loggedInputEvent = true;
        plugin.getLogger().info("Driver check: key-change events arrive while riding (forward=" + in.isForward()
                + ", back=" + in.isBackward() + ", left=" + in.isLeft() + ", right=" + in.isRight() + ")");
    }

    public void remove(Vehicle v) {
        vehicles.remove(v);
        v.remove();
    }

    public void removeAll() {
        if (task != null) task.cancel();
        for (Vehicle v : new ArrayList<>(vehicles)) v.remove();
        vehicles.clear();
    }

    private int idleCounter;

    private void tickAll() {
        if (++idleCounter >= 20) {
            idleCounter = 0;
            checkIdle();
        }
        for (Vehicle v : new ArrayList<>(vehicles)) {
            try {
                tick(v);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Vehicle tick failed; removing the " + v.type.id, e);
                remove(v);
            }
        }
    }

    private void checkIdle() {
        long now = System.currentTimeMillis();
        int minutes = VehicleSettings.idleMinutes(plugin);
        long limit = IdleTimer.limitMillis(minutes);
        for (Vehicle v : new ArrayList<>(vehicles)) {
            if (!v.idle.update(v.driver() != null, now, limit)) continue;
            VehicleMover.State s = v.state();
            plugin.getLogger().info("Idle check: removed " + v.type.id + " at " + v.world.getName() + " "
                    + (long) Math.floor(s.x()) + " " + (long) Math.floor(s.y()) + " " + (long) Math.floor(s.z())
                    + " after " + minutes + " min with no driver");
            remove(v);
        }
    }

    /** Distances from every vehicle in the same world to the point. */
    public double[] distancesTo(Location at) {
        return vehicles.stream().filter(v -> v.world.equals(at.getWorld()))
                .mapToDouble(v -> v.location().distance(at)).toArray();
    }

    private void tick(Vehicle v) {
        if (!v.isValid()) { // chunk unloaded, killed by a command, world gone
            remove(v);
            return;
        }
        DriveInput input = DriveInput.NONE;
        if (v.driver() != null) {
            Player d = plugin.getServer().getPlayer(v.driver());
            if (d == null || !d.isOnline() || d.isDead()) {
                v.driver(null);
            } else if (!v.rig().isSeated(d)) { // sneaked out
                v.driver(null);
                placeBeside(v, d);
            } else {
                Input in = d.getCurrentInput();
                input = new DriveInput(in.isForward(), in.isBackward(), in.isLeft(), in.isRight());
                if (input.any() && !loggedPolledInput) {
                    loggedPolledInput = true;
                    plugin.getLogger().info("Driver check: the current-input poll reports keys while riding ("
                            + (input.forward() ? "W" : "") + (input.backward() ? "S" : "")
                            + (input.left() ? "A" : "") + (input.right() ? "D" : "") + ")");
                }
            }
        }
        Terrain terrain = terrains.computeIfAbsent(v.world, BukkitTerrain::new);
        v.move(terrain, input);
        v.spinWheels();
        if (v.driver() != null) {
            Player d = plugin.getServer().getPlayer(v.driver());
            if (d != null) v.aimTurret(d);
        }
        if (v.state().y() < v.world.getMinHeight() - 64) remove(v); // fell out of the world
    }

    /** Puts a player who got out next to the vehicle on solid ground, trying the right, left, back and front. */
    private void placeBeside(Vehicle v, Player p) {
        VehicleMover.State s = v.state();
        double rad = Math.toRadians(s.yaw());
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double rx = -Math.cos(rad), rz = -Math.sin(rad);
        double side = v.type.halfWidth + 0.8, end = v.type.halfLength + 0.8;
        double[][] offsets = {{rx * side, rz * side}, {-rx * side, -rz * side}, {-fx * end, -fz * end}, {fx * end, fz * end}};
        for (double[] o : offsets) {
            Location spot = standable(v.world, s.x() + o[0], s.y(), s.z() + o[1]);
            if (spot != null) {
                Location to = p.getLocation();
                spot.setYaw(to.getYaw());
                spot.setPitch(to.getPitch());
                p.teleport(spot);
                return;
            }
        }
    }

    private static Location standable(World w, double x, double feetY, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        if (!w.isChunkLoaded(bx >> 4, bz >> 4)) return null;
        int base = (int) Math.floor(feetY);
        for (int by = base + 2; by >= base - 3; by--) {
            Block below = w.getBlockAt(bx, by - 1, bz);
            if (below.isPassable() || !w.getBlockAt(bx, by, bz).isPassable()
                    || !w.getBlockAt(bx, by + 1, bz).isPassable()) continue;
            Collection<BoundingBox> boxes = below.getCollisionShape().getBoundingBoxes();
            double top = by - 1;
            for (BoundingBox b : boxes) top = Math.max(top, by - 1 + b.getMaxY());
            return new Location(w, bx + 0.5, top, bz + 0.5);
        }
        return null;
    }
}
