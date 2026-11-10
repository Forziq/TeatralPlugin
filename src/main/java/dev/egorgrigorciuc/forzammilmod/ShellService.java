package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;

/** Tank firing: click handling, reload, and flying shells. Main thread only. */
public final class ShellService {

    static final int RELOAD_TICKS = 80;
    static final int CLICK_DEBOUNCE_TICKS = 5;
    static final float EXPLOSION_POWER = 2.5f;
    static final double SHELL_RAY_SIZE = 0.3;
    /** A shell cannot hurt the vehicle that fired it until it has flown this far. */
    static final double ORIGIN_SAFE_BLOCKS = 10.0;

    private static final class Shell {
        final ShellPath model;
        final World world;
        final UUID driver;
        /** The firing vehicle; it cannot be hit by its own shell near the muzzle. */
        final Vehicle origin;
        /** Players who already heard this shell whistle past. */
        final Set<UUID> whistled = new HashSet<>();

        Shell(ShellPath model, World world, UUID driver, Vehicle origin) {
            this.model = model;
            this.world = world;
            this.driver = driver;
            this.origin = origin;
        }
    }

    private final JavaPlugin plugin;
    private final VehicleService vehicles;
    private final Lang lang;
    private final List<Shell> shells = new ArrayList<>();
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private final Set<String> loggedPaths = new HashSet<>();
    private long tick;
    private BukkitTask task;

    public ShellService(JavaPlugin plugin, VehicleService vehicles, Lang lang) {
        this.plugin = plugin;
        this.vehicles = vehicles;
        this.lang = lang;
    }

    /** Clicks this soon after getting in are ignored. */
    static final long MOUNT_GRACE_MILLIS = 500;

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
    }

    public void stop() {
        if (task != null) task.cancel();
        shells.clear();
        lastClick.clear();
    }

    /** True if this player sits in a tank (any seat), so their left-click never hits blocks or mobs. */
    boolean isTankCrew(Player p) {
        Vehicle v = vehicles.boardedBy(p);
        return v != null && v.type.hasGun();
    }

    /** True if this player aims and fires the tank gun: the gunner, or the driver when there is no gunner. */
    boolean isTurretOperator(Player p) {
        Vehicle v = vehicles.boardedBy(p);
        return v != null && v.type.hasGun() && p.getUniqueId().equals(v.turretOperator());
    }

    /** Handles a fire click from one of the three paths ("interact", "swing", "hit"). */
    void fireClick(Player p, String path) {
        if (!isTurretOperator(p)) return;
        if (loggedPaths.add(p.getUniqueId() + path)) {
            plugin.getLogger().info("Fire check: click seen via " + path);
        }
        Long last = lastClick.get(p.getUniqueId());
        lastClick.put(p.getUniqueId(), tick);
        if (last != null && tick - last < CLICK_DEBOUNCE_TICKS) return;
        Vehicle v = vehicles.boardedBy(p);
        // The right-click that seats a crew member is followed by an arm swing; don't treat it as a shot.
        if (System.currentTimeMillis() - v.joinedMillis(p.getUniqueId()) < MOUNT_GRACE_MILLIS) return;
        TurretModel.Muzzle m = v.muzzle();
        if (m == null) return;
        if (!v.reload.tryFire(tick)) return;
        launch(p, v, m);
    }

    private void launch(Player p, Vehicle v, TurretModel.Muzzle m) {
        World w = v.world;
        Location at = new Location(w, m.x(), m.y(), m.z());
        ShellPath path = v.type.isArtillery()
                ? BallisticArc.fire(m.x(), m.y(), m.z(), m.dx(), m.dy(), m.dz())
                : new ShellModel(m.x(), m.y(), m.z(), m.dx(), m.dy(), m.dz());
        shells.add(new Shell(path, w, p.getUniqueId(), v));
        VehicleSounds.boom(w, at, v.type.isArtillery() ? VehicleSounds.CANNON_FIELD : VehicleSounds.CANNON, VehicleSounds.LOUD);
        w.spawnParticle(Particle.LARGE_SMOKE, at, 6, 0.15, 0.15, 0.15, 0.02);
        w.spawnParticle(Particle.EXPLOSION, at, 1);
    }

    long tick() {
        return tick;
    }

    private void tickAll() {
        tick++;
        for (Iterator<Shell> it = shells.iterator(); it.hasNext(); ) {
            Shell s = it.next();
            boolean keep;
            try {
                keep = step(s);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Shell tick failed; dropping the shell", e);
                keep = false;
            }
            if (!keep) it.remove();
        }
        for (Vehicle v : vehicles.vehicles()) {
            if (v.hasTurret() && v.reload.pollFinished(tick)) {
                VehicleSounds.boom(v.world, v.location().add(0, v.type.height / 2, 0), VehicleSounds.RELOAD, 2.0f);
            }
        }
    }

    /** Moves one shell along; returns false when it is finished. */
    private boolean step(Shell s) {
        List<ShellModel.Segment> segs = s.model.nextTick();
        if (segs.isEmpty()) return false;
        for (ShellModel.Segment seg : segs) {
            if (!step(s, seg)) return false;
        }
        return true;
    }

    /** Tests one stretch of flight; returns false when the shell burst or left the loaded world. */
    private boolean step(Shell s, ShellModel.Segment seg) {
        World w = s.world;
        if (seg.endY() < w.getMinHeight()) return false; // fell into the void without hitting anything
        int ex = (int) Math.floor(seg.endX()), ez = (int) Math.floor(seg.endZ());
        if (!w.isChunkLoaded(ex >> 4, ez >> 4)) return false;
        Location start = new Location(w, seg.x(), seg.y(), seg.z());
        Vector dir = new Vector(seg.dx(), seg.dy(), seg.dz());
        Predicate<Entity> filter = e -> e instanceof LivingEntity
                && !e.getUniqueId().equals(s.driver)
                && vehicles.find(e) == null
                && !(e instanceof ArmorStand); // BetterModel hitboxes and our anchors are armour stands
        RayTraceResult hit = w.rayTrace(start, dir, seg.length(), FluidCollisionMode.NEVER, true, SHELL_RAY_SIZE, filter);
        // Closest vehicle hull along the path (the firing vehicle is skipped while the shell is still near it).
        boolean skipOrigin = s.model.travelled() < ORIGIN_SAFE_BLOCKS + seg.length();
        Vehicle hitVehicle = null;
        double vehicleDist = Double.MAX_VALUE;
        for (Vehicle v : vehicles.vehicles()) {
            if (!v.world.equals(w) || v.destroyed || (skipOrigin && v == s.origin)) continue;
            double t = v.hull().rayHit(seg.x(), seg.y(), seg.z(), seg.dx(), seg.dy(), seg.dz(), seg.length());
            if (t >= 0 && t < vehicleDist) {
                vehicleDist = t;
                hitVehicle = v;
            }
        }
        if (hitVehicle != null && hit != null && hit.getHitPosition().distance(start.toVector()) < vehicleDist) {
            hitVehicle = null; // a block or mob was in the way
        }
        if (hitVehicle != null || hit != null) {
            Location boom;
            if (hitVehicle != null) {
                boom = start.clone().add(dir.clone().multiply(vehicleDist));
            } else {
                boom = hit.getHitPosition().toLocation(w);
                if (hit.getHitBlockFace() != null) boom.add(hit.getHitBlockFace().getDirection().multiply(0.1));
            }
            Player shooter = plugin.getServer().getPlayer(s.driver);
            VehicleSounds.boom(w, boom, VehicleSounds.EXPLOSION_SMALL, 3.0f);
            if (hitVehicle != null) VehicleSounds.boom(w, boom, VehicleSounds.ARMOUR_HIT, 3.0f);
            w.createExplosion(boom, EXPLOSION_POWER, false, true, shooter);
            if (hitVehicle != null) {
                HitSide side = HitSide.classify(hitVehicle.state().yaw(), seg.dx(), seg.dz());
                vehicles.damage(hitVehicle, DamageRules.shell(hitVehicle.type, side));
            }
            for (Vehicle v : vehicles.vehicles()) {
                if (v == hitVehicle || !v.world.equals(w) || v.destroyed || (skipOrigin && v == s.origin)) continue;
                int dmg = DamageRules.splash(v.hull().distanceTo(boom.getX(), boom.getY(), boom.getZ()));
                if (dmg > 0) vehicles.damage(v, dmg);
            }
            return false;
        }
        whistle(s, seg);
        // A lobbed shell is far away and moves in small steps, so only draw a trail; the flame rides the tick's last step.
        for (double d = 0; d < seg.length(); d += 0.5) {
            w.spawnParticle(Particle.SMOKE, seg.x() + seg.dx() * d, seg.y() + seg.dy() * d, seg.z() + seg.dz() * d,
                    1, 0, 0, 0, 0);
        }
        w.spawnParticle(Particle.FLAME, seg.endX(), seg.endY(), seg.endZ(), 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.CRIT, seg.endX(), seg.endY(), seg.endZ(), 2, 0.05, 0.05, 0.05, 0);
        return true;
    }

    /** Blocks from the shell's path within which a player hears it whistle past. */
    static final double WHISTLE_RANGE = 24.0;
    /** The same for a lobbed shell, which only whistles on its way down. */
    static final double FALLING_WHISTLE_RANGE = 40.0;

    /** Plays the whistle once to each player near the shell, except the firing vehicle's crew. */
    private void whistle(Shell s, ShellModel.Segment seg) {
        boolean lobbed = s.model instanceof BallisticArc;
        if (lobbed && seg.dy() >= 0) return; // still climbing
        double range = lobbed ? FALLING_WHISTLE_RANGE : WHISTLE_RANGE;
        double rangeSq = range * range;
        for (Player p : s.world.getPlayers()) {
            UUID id = p.getUniqueId();
            if (id.equals(s.driver) || s.whistled.contains(id) || s.origin.seatOf(id) != null) continue;
            Location pl = p.getLocation();
            double dx = pl.getX() - seg.endX(), dy = pl.getY() - seg.endY(), dz = pl.getZ() - seg.endZ();
            if (dx * dx + dy * dy + dz * dz > rangeSq) continue;
            s.whistled.add(id);
            p.playSound(new Location(s.world, seg.endX(), seg.endY(), seg.endZ()), VehicleSounds.SHELL_WHISTLE,
                    SoundCategory.NEUTRAL, lobbed ? 3.0f : VehicleSounds.LOOP_VOLUME, 1f);
        }
    }
}
