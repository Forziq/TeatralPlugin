package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.CrewSeats.Seat;
import org.bukkit.Input;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final VehicleSounds sounds;
    private Lang lang;
    private VehicleHud hud;
    private final PlaneGuns guns = new PlaneGuns(this);
    private boolean loggedSeated;
    private boolean loggedPolledInput;
    private boolean loggedInputEvent;

    /** Log what the plane test needs (key events, take-offs, landings, crashes). Set false once Gate 6 is done. */
    static final boolean FLIGHT_LOG = false;
    static final int FLIGHT_KEY_LOGS = 12;
    /** Crash blast: a bit smaller than a tank shell's 2.5. Breaks blocks, no fire. */
    static final float CRASH_POWER = 2.0f;
    /** Bailing out (or being shot down) this high up opens a parachute: slow falling for 10 seconds. */
    static final double PARACHUTE_HEIGHT = 4.0;
    static final int PARACHUTE_TICKS = 200;

    /** Pilots we gave flight to (so the server doesn't kick them for flying), and what they had before. */
    private final Map<UUID, Boolean> flightBefore = new HashMap<>();
    /** Crew of a plane that is blowing up right now: the blast doesn't hurt them, destroy() does its usual 6. */
    private final Set<UUID> crashShield = new HashSet<>();
    private long ticks;
    private int planeKeyLogs;
    private boolean loggedTakeoff, loggedLanding, loggedWaiting;

    public VehicleService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.sounds = new VehicleSounds(plugin);
    }

    VehicleSounds sounds() {
        return sounds;
    }

    /** Needed for the damage and wreck messages; set before the first tick. */
    public void lang(Lang lang) {
        this.lang = lang;
    }

    /** Starts the boss bar and action bar HUD; the clock is the gun reload clock. Set before the first tick. */
    void hud(Lang lang, java.util.function.LongSupplier reloadClock) {
        this.hud = new VehicleHud(lang, reloadClock);
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
            rig = BetterModelHook.attachVehicle(anchor, type.model(), WheelSpin.bones(type), v::renderYaw,
                    type.isPlane() ? v::renderPitch : null, plugin.getLogger());
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
            if (v.type.isPlane() && CrewSeats.drives(seat)) grantFlight(p);
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
        restoreFlight(p);
        if (hud != null) hud.clear(p);
        try {
            v.rig().unseat(p, seat);
        } catch (RuntimeException | LinkageError ignored) {
            // nothing more we can do
        }
    }

    /**
     * Lets the pilot "fly" while seated, so the server's anti-fly check doesn't kick them (docs/notes/planes.md
     * section 4). The previous setting comes back when they leave the seat, however they leave it.
     */
    private void grantFlight(Player p) {
        flightBefore.putIfAbsent(p.getUniqueId(), p.getAllowFlight());
        p.setAllowFlight(true);
    }

    private void restoreFlight(Player p) {
        Boolean before = flightBefore.remove(p.getUniqueId());
        if (before == null) return;
        p.setAllowFlight(before); // false also stops them flying
        p.setFallDistance(0f);
    }

    /** Safety net, once per tick: anyone we gave flight who no longer flies a plane gets their setting back. */
    private void reconcileFlight() {
        if (flightBefore.isEmpty()) return;
        for (UUID id : new ArrayList<>(flightBefore.keySet())) {
            Player p = plugin.getServer().getPlayer(id);
            if (p == null) {
                flightBefore.remove(id); // gone; the quit event already put it back
                continue;
            }
            Vehicle v = drivenBy(p);
            if (v == null || !v.type.isPlane() || v.destroyed) restoreFlight(p);
        }
    }

    /** Out of the plane in mid-air: flight goes back to normal and a parachute opens if it is a long way down. */
    private void bailOut(Player p, Vehicle v) {
        restoreFlight(p);
        if (v.heightAboveGround < PARACHUTE_HEIGHT) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, PARACHUTE_TICKS, 0, false, true, true));
        if (lang != null) p.sendMessage(lang.msg(p, "vehicle.bailed-out"));
    }

    /** True while this player's plane is exploding, so the listener can keep the blast off them. */
    public boolean shieldedFromCrash(Entity e) {
        return !crashShield.isEmpty() && crashShield.contains(e.getUniqueId());
    }

    /** Logs a kick of anyone aboard a vehicle or kicked for flying, to answer whether pilots get kicked. */
    public void noteKick(Player p, String reason) {
        Vehicle v = boardedBy(p);
        String r = reason == null ? "" : reason;
        String low = r.toLowerCase(java.util.Locale.ROOT);
        if (v == null && !low.contains("fly") && !low.contains("float")) return;
        plugin.getLogger().warning("Flight check: " + p.getName() + " was kicked (" + r + ")"
                + (v == null ? " while not in a vehicle" : " while in the " + v.type.id
                + (v.airborne() ? ", in the air" : "")) + "; allowed to fly: " + p.getAllowFlight());
    }

    /** Logs once whether the input event fires for someone who is driving. */
    public void noteInputEvent(Player p, Input in) {
        Vehicle driven = drivenBy(p);
        if (FLIGHT_LOG && planeKeyLogs < FLIGHT_KEY_LOGS && driven != null && driven.type.isPlane()) {
            planeKeyLogs++;
            plugin.getLogger().info("Flight check: key event " + planeKeyLogs + "/" + FLIGHT_KEY_LOGS + " at tick " + ticks
                    + ": forward=" + in.isForward() + " back=" + in.isBackward() + " left=" + in.isLeft()
                    + " right=" + in.isRight() + " jump=" + in.isJump() + " sneak=" + in.isSneak()
                    + " sprint=" + in.isSprint() + " (ticks far apart while a key is held = it fires on change only)");
        }
        if (loggedInputEvent || driven == null) return;
        loggedInputEvent = true;
        plugin.getLogger().info("Driver check: key-change events arrive while riding (forward=" + in.isForward()
                + ", back=" + in.isBackward() + ", left=" + in.isLeft() + ", right=" + in.isRight() + ")");
    }

    /** Vehicle ticks so far; the plane guns time bursts with it. */
    long currentTick() {
        return ticks;
    }

    /** The plane guns (click handling for the listener). */
    PlaneGuns guns() {
        return guns;
    }

    /** Applies damage; blows the vehicle up at 0. Returns true if it was destroyed. */
    public boolean damage(Vehicle v, int amount) {
        if (amount <= 0 || v.destroyed || !vehicles.contains(v)) return false;
        v.health.damage(amount);
        if (v.health.isDestroyed()) {
            destroy(v, "vehicle.destroyed", v.airborne());
            return true;
        }
        return false;
    }

    /** The plane hit the ground or a wall too hard: a real (block-breaking) blast, then the usual wreck. */
    private void crash(Vehicle v) {
        FlightModel.State f = v.flight();
        if (FLIGHT_LOG) {
            plugin.getLogger().info("Flight check: plane crashed at " + v.world.getName() + " " + (long) Math.floor(f.x()) + " "
                    + (long) Math.floor(f.y()) + " " + (long) Math.floor(f.z()) + String.format(java.util.Locale.ROOT,
                    ", speed %.1f b/s, sinking %.1f b/s, bank %.0f, pitch %.0f", HudModel.blocksPerSecond(f.speed()),
                    -f.verticalSpeed() * HudModel.TICKS_PER_SECOND, f.roll(), f.pitch()));
        }
        Location at = v.location().add(0, 0.5, 0);
        for (Seat seat : v.takenSeats()) crashShield.add(v.occupant(seat));
        try {
            v.world.createExplosion(at, CRASH_POWER, false, true);
        } finally {
            crashShield.clear();
        }
        destroy(v, "vehicle.crashed", false);
    }

    /**
     * Big blast (particles and sound only, no block damage), crew thrown out and hurt, vehicle removed.
     * {@code parachute}: a plane shot down in the air gives its crew a parachute.
     */
    private void destroy(Vehicle v, String messageKey, boolean parachute) {
        v.destroyed = true;
        if (hud != null) hud.clear(v);
        Location at = v.location().add(0, v.type.height / 2, 0);
        World w = v.world;
        w.spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
        w.spawnParticle(Particle.LARGE_SMOKE, at, 30, 1.0, 0.6, 1.0, 0.05);
        VehicleSounds.boom(w, at, VehicleSounds.EXPLOSION_BIG, VehicleSounds.LOUD);
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
            if (lang != null) d.sendMessage(lang.msg(d, messageKey));
            if (parachute) bailOut(d, v);
            else restoreFlight(d);
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
        sounds.forget(p.getUniqueId());
    }

    /** Iron ingot repair: returns true if it restored health, false if the vehicle was already full. */
    public boolean repair(Vehicle v) {
        if (v.health.isFull()) return false;
        v.health.repair(VehicleHealth.repairAmount(v.health.max()));
        v.world.playSound(v.location(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.4f);
        return true;
    }

    public void remove(Vehicle v) {
        if (v.type.isPlane()) {
            // Removed with somebody still in it (a command, idle clean-up): don't leave them hovering.
            for (Seat seat : v.takenSeats()) {
                Player p = plugin.getServer().getPlayer(v.occupant(seat));
                if (p != null) bailOut(p, v);
            }
        }
        sounds.stop(v);
        if (hud != null) hud.clear(v);
        vehicles.remove(v);
        v.remove();
    }

    public void removeAll() {
        if (task != null) task.cancel();
        for (UUID id : new ArrayList<>(flightBefore.keySet())) {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null) restoreFlight(p);
        }
        flightBefore.clear();
        for (Vehicle v : new ArrayList<>(vehicles)) {
            sounds.stop(v);
            v.remove();
        }
        vehicles.clear();
        if (hud != null) hud.clearAll();
    }

    private int idleCounter;

    private void tickAll() {
        ticks++;
        sounds.advance();
        if (++idleCounter >= 20) {
            idleCounter = 0;
            checkIdle();
        }
        for (Vehicle v : new ArrayList<>(vehicles)) {
            try {
                tick(v);
                if (hud != null) hud.update(v);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Vehicle tick failed; removing the " + v.type.id, e);
                remove(v);
            }
        }
        if (hud != null) hud.endTick();
        reconcileFlight();
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
        Player pilot = null;
        v.turretYawDelta = 0f;
        for (Seat seat : v.takenSeats()) {
            Player d = plugin.getServer().getPlayer(v.occupant(seat));
            if (d == null || !d.isOnline() || d.isDead()) {
                v.vacate(seat);
            } else if (!v.rig().isSeated(d, seat)) { // sneaked out
                v.vacate(seat);
                if (v.airborne()) bailOut(d, v);
                else placeBeside(v, d);
            } else if (CrewSeats.drives(seat)) {
                pilot = d;
                Input in = d.getCurrentInput();
                input = new DriveInput(in.isForward(), in.isBackward(), in.isLeft(), in.isRight(), in.isJump(), in.isSprint());
                if (input.any() && !loggedPolledInput) {
                    loggedPolledInput = true;
                    plugin.getLogger().info("Driver check: the current-input poll reports keys while riding ("
                            + (input.forward() ? "W" : "") + (input.backward() ? "S" : "")
                            + (input.left() ? "A" : "") + (input.right() ? "D" : "") + ")");
                }
            }
        }
        Terrain terrain = terrains.computeIfAbsent(v.world, BukkitTerrain::new);
        if (v.type.isPlane()) {
            flyTick(v, terrain, input, pilot);
            return;
        }
        v.move(terrain, input);
        v.spinWheels();
        UUID operator = v.turretOperator();
        if (operator != null) {
            Player d = plugin.getServer().getPlayer(operator);
            if (d != null) v.aimTurret(d);
        }
        if (v.type.moves()) sounds.tick(v, input);
        if (v.health.fraction() < DamageRules.SMOKE_BELOW && ++v.smokeTick >= 10) {
            v.smokeTick = 0;
            v.world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, v.location().add(0, v.type.height, 0), 3, 0.3, 0.1, 0.3, 0.02);
        }
        if (v.state().y() < v.world.getMinHeight() - 64) remove(v); // fell out of the world
    }

    /** The plane's tick: flight, read-out height, sounds; a crash ends it. */
    private void flyTick(Vehicle v, Terrain terrain, DriveInput keys, Player pilot) {
        FlightModel.Input in = pilot == null ? FlightModel.Input.NONE
                : new FlightModel.Input(keys.throttle(), keys.steer(), keys.pitch(), true);
        FlightModel.Outcome out = v.fly(terrain, in, pilot != null);
        guns.tick(v, pilot, ticks);
        v.heightAboveGround = heightAboveGround(v);
        noteFlight(v, out);
        if (out == FlightModel.Outcome.CRASHED) {
            crash(v);
            return;
        }
        sounds.tick(v, keys);
        sounds.flyby(v);
        if (v.state().y() < v.world.getMinHeight() - 64) remove(v);
    }

    /** Wheels to the highest block under the plane's centre; keeps the last value over unloaded land. */
    private static double heightAboveGround(Vehicle v) {
        VehicleMover.State s = v.state();
        int bx = (int) Math.floor(s.x()), bz = (int) Math.floor(s.z());
        if (!v.world.isChunkLoaded(bx >> 4, bz >> 4)) return v.heightAboveGround;
        return Math.max(0, s.y() - (v.world.getHighestBlockYAt(bx, bz) + 1));
    }

    private void noteFlight(Vehicle v, FlightModel.Outcome out) {
        if (!FLIGHT_LOG) return;
        FlightModel.State f = v.flight();
        if (out == FlightModel.Outcome.TAKEOFF && !loggedTakeoff) {
            loggedTakeoff = true;
            plugin.getLogger().info("Flight check: first take-off at " + HudModel.formatSpeed(f.speed()) + " b/s");
        } else if (out == FlightModel.Outcome.LANDED && !loggedLanding) {
            loggedLanding = true;
            plugin.getLogger().info("Flight check: first landing at " + HudModel.formatSpeed(f.speed()) + " b/s");
        } else if (out == FlightModel.Outcome.WAITING && !loggedWaiting) {
            loggedWaiting = true;
            plugin.getLogger().info("Flight check: the plane waited for land ahead to load (logged once)");
        }
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
