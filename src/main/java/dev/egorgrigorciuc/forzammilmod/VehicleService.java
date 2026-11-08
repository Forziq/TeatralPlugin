package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.CrewSeats.Seat;
import org.bukkit.Input;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

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

    public enum MountResult { MOUNTED, ALREADY_ABOARD, FULL, NOT_READY }

    static final String TAG = "forzammilmod_vehicle";

    private final JavaPlugin plugin;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final Map<World, Terrain> terrains = new HashMap<>();
    private BukkitTask task;
    private Lang lang;
    private boolean loggedSeated;
    private boolean loggedPolledInput;
    private boolean loggedInputEvent;

    public VehicleService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Needed for the damage and wreck messages; set before the first tick. */
    public void lang(Lang lang) {
        this.lang = lang;
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

    /** Vehicle where this player is the driver. */
    public Vehicle drivenBy(Player p) {
        UUID id = p.getUniqueId();
        for (Vehicle v : vehicles) if (id.equals(v.driver())) return v;
        return null;
    }

    /** Vehicle where this player sits in any seat. */
    public Vehicle boardedBy(Player p) {
        UUID id = p.getUniqueId();
        for (Vehicle v : vehicles) if (v.seatOf(id) != null) return v;
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

    /** Seat the player took on the last successful {@link #mount}. */
    public Seat seatOf(Player p, Vehicle v) {
        return v.seatOf(p.getUniqueId());
    }

    /** Puts the player in the first free seat (driver, gunner, passengers). */
    public MountResult mount(Player p, Vehicle v) {
        Seat mine = v.seatOf(p.getUniqueId());
        if (mine != null) {
            if (v.rig().isSeated(p, mine)) return MountResult.ALREADY_ABOARD;
            v.vacate(mine);
        }
        // Forget seats whose occupant is gone or climbed out since the last tick.
        for (Seat seat : v.takenSeats()) {
            Player current = plugin.getServer().getPlayer(v.occupant(seat));
            if (current == null || !v.rig().isSeated(current, seat)) v.vacate(seat);
        }
        List<Seat> free = CrewSeats.freeSeats(v.type, v.takenSeats());
        if (free.isEmpty()) return MountResult.FULL;
        Vehicle other = boardedBy(p);
        if (other != null) crewGone(p);
        if (p.isInsideVehicle()) p.leaveVehicle();
        for (Seat seat : free) {
            boolean ok;
            try {
                ok = v.rig().seat(p, seat);
            } catch (RuntimeException | LinkageError e) {
                plugin.getLogger().log(Level.WARNING, "Could not seat " + p.getName() + " in the " + v.type.id, e);
                ok = false;
            }
            if (!ok) continue;
            v.occupy(seat, p.getUniqueId());
            if (!loggedSeated) {
                loggedSeated = true;
                plugin.getLogger().info("Seat check: " + p.getName() + " is seated as " + seat + " in the " + v.type.id
                        + " (BetterModel seat). Next lines say whether W/A/S/D arrive while riding.");
            }
            return MountResult.MOUNTED;
        }
        return MountResult.NOT_READY;
    }

    /** Crew member quit, died or teleported away: take them out of their seat without moving them. */
    public void crewGone(Player p) {
        Vehicle v = boardedBy(p);
        if (v == null) return;
        Seat seat = v.seatOf(p.getUniqueId());
        v.vacate(seat);
        try {
            v.rig().unseat(p, seat);
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

    /** Applies damage; shows the driver the hull state and blows the vehicle up at 0. Returns true if it was destroyed. */
    public boolean damage(Vehicle v, int amount) {
        if (amount <= 0 || v.destroyed || !vehicles.contains(v)) return false;
        v.health.damage(amount);
        if (v.health.isDestroyed()) {
            destroy(v);
            return true;
        }
        if (lang != null) {
            for (Seat seat : v.takenSeats()) {
                Player d = plugin.getServer().getPlayer(v.occupant(seat));
                if (d == null) continue;
                d.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(lang.msg(d, "vehicle.damaged", "percent", v.health.percent())));
            }
        }
        return false;
    }

    /** Big blast (particles and sound only, no block damage), crew thrown out and hurt, vehicle removed. */
    private void destroy(Vehicle v) {
        v.destroyed = true;
        Location at = v.location().add(0, v.type.height / 2, 0);
        World w = v.world;
        w.spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
        w.spawnParticle(Particle.LARGE_SMOKE, at, 30, 1.0, 0.6, 1.0, 0.05);
        w.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
        for (Seat seat : v.takenSeats()) {
            Player d = plugin.getServer().getPlayer(v.occupant(seat));
            v.vacate(seat);
            if (d == null) continue;
            try {
                v.rig().unseat(d, seat);
            } catch (RuntimeException | LinkageError ignored) {
                // the seat is going away with the vehicle anyway
            }
            d.teleport(at.clone().add(0, 0.5, 0));
            d.setVelocity(new Vector((Math.random() - 0.5) * 0.6, 0.5, (Math.random() - 0.5) * 0.6));
            d.damage(6.0);
            if (lang != null) d.sendMessage(lang.msg(d, "vehicle.destroyed"));
        }
        remove(v);
    }

    static final long MELEE_DEBOUNCE_MILLIS = 400;
    static final double MELEE_REACH = 3.5;
    private final Map<UUID, Long> lastMelee = new HashMap<>();

    /** A player's punch on a vehicle (from the damage event or the arm swing; both can fire for one hit, so debounced). */
    public void melee(Player p, Vehicle v) {
        if (v.seatOf(p.getUniqueId()) != null) return;
        int dmg = DamageRules.melee(v.type);
        if (dmg <= 0) return;
        long now = System.currentTimeMillis();
        Long last = lastMelee.get(p.getUniqueId());
        if (last != null && now - last < MELEE_DEBOUNCE_MILLIS) return;
        lastMelee.put(p.getUniqueId(), now);
        damage(v, dmg);
    }

    /**
     * Arm swing by a player on foot: the click box is an Interaction, which may not raise a damage event for a
     * left-click, so we ray trace against the hull ourselves.
     */
    public void swing(Player p) {
        if (p.isInsideVehicle()) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        double blockDist = MELEE_REACH;
        RayTraceResult blk = p.getWorld().rayTraceBlocks(eye, dir, MELEE_REACH);
        if (blk != null) blockDist = blk.getHitPosition().distance(eye.toVector());
        Vehicle best = null;
        double bestT = blockDist;
        for (Vehicle v : vehicles) {
            if (!v.world.equals(p.getWorld()) || v.destroyed) continue;
            double t = v.hull().rayHit(eye.getX(), eye.getY(), eye.getZ(), dir.getX(), dir.getY(), dir.getZ(), blockDist);
            if (t >= 0 && t <= bestT) {
                bestT = t;
                best = v;
            }
        }
        if (best != null) melee(p, best);
    }

    public void forget(Player p) {
        lastMelee.remove(p.getUniqueId());
    }

    /** Iron ingot repair: returns true if it restored health, false if the vehicle was already full. */
    public boolean repair(Vehicle v) {
        if (v.health.isFull()) return false;
        v.health.repair(VehicleHealth.repairAmount(v.health.max()));
        v.world.playSound(v.location(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.4f);
        return true;
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
            if (!v.idle.update(v.hasCrew(), now, limit)) continue;
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
        for (Seat seat : v.takenSeats()) {
            Player d = plugin.getServer().getPlayer(v.occupant(seat));
            if (d == null || !d.isOnline() || d.isDead()) {
                v.vacate(seat);
            } else if (!v.rig().isSeated(d, seat)) { // sneaked out
                v.vacate(seat);
                placeBeside(v, d);
            } else if (CrewSeats.drives(seat)) {
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
        UUID operator = v.turretOperator();
        if (operator != null) {
            Player d = plugin.getServer().getPlayer(operator);
            if (d != null) v.aimTurret(d);
        }
        if (v.health.fraction() < DamageRules.SMOKE_BELOW && ++v.smokeTick >= 10) {
            v.smokeTick = 0;
            v.world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, v.location().add(0, v.type.height, 0), 3, 0.3, 0.1, 0.3, 0.02);
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
