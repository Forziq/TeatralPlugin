package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.function.Predicate;

/** Shoots the plane's machine guns: the pilot's click starts a burst, the flight tick sends the bullets. Main thread only. */
final class PlaneGuns {

    /** Clicks this soon after getting in are ignored (the right-click that seats you ends in an arm swing). */
    private static final long MOUNT_GRACE_MILLIS = 500;

    private final VehicleService vehicles;

    PlaneGuns(VehicleService vehicles) {
        this.vehicles = vehicles;
    }

    /** True if this player is a plane's pilot, so their clicks never hit blocks or mobs. */
    boolean isPilot(Player p) {
        Vehicle v = vehicles.boardedBy(p);
        return v != null && v.type.isPlane();
    }

    /** A left click by the pilot: starts a burst unless one is running or the guns are cooling down. */
    void click(Player p, long now) {
        Vehicle v = vehicles.boardedBy(p);
        if (v == null || !v.type.isPlane() || v.destroyed) return;
        if (System.currentTimeMillis() - v.joinedMillis(p.getUniqueId()) < MOUNT_GRACE_MILLIS) return;
        if (!v.gun.tryStart(now)) return;
        Location at = muzzle(v);
        VehicleSounds.boom(v.world, at, VehicleSounds.MG_BURST, VehicleSounds.LOUD);
    }

    /** Sends this tick's bullets, if a burst is running. {@code pilot} null (or the plane gone) cancels it. */
    void tick(Vehicle v, Player pilot, long now) {
        if (!v.gun.bursting()) return;
        if (pilot == null || v.destroyed) {
            v.gun.cancel();
            return;
        }
        int n = v.gun.shotsDue(now);
        for (int i = 0; i < n; i++) shoot(v, pilot);
    }

    private static Location muzzle(Vehicle v) {
        FlightModel.State f = v.flight();
        double[] d = MachineGun.direction(f.yaw(), f.pitch());
        return new Location(v.world,
                f.x() + d[0] * MachineGun.MUZZLE_FORWARD,
                f.y() + MachineGun.MUZZLE_UP + d[1] * MachineGun.MUZZLE_FORWARD,
                f.z() + d[2] * MachineGun.MUZZLE_FORWARD);
    }

    /** One bullet: a ray from the nose. Hits the nearest of a vehicle, a mob or player, or a block (which is left unharmed). */
    private void shoot(Vehicle v, Player pilot) {
        World w = v.world;
        FlightModel.State f = v.flight();
        double[] d = MachineGun.direction(f.yaw(), f.pitch());
        Location start = muzzle(v);
        Vector dir = new Vector(d[0], d[1], d[2]);
        UUID shooter = pilot.getUniqueId();
        Predicate<Entity> filter = e -> e instanceof LivingEntity
                && !e.getUniqueId().equals(shooter)
                && vehicles.find(e) == null
                && !(e instanceof ArmorStand);
        RayTraceResult hit = w.rayTrace(start, dir, MachineGun.RANGE, FluidCollisionMode.NEVER, true, MachineGun.RAY_SIZE, filter);
        double reach = hit == null ? MachineGun.RANGE : hit.getHitPosition().distance(start.toVector());

        Vehicle hitVehicle = null;
        double vehicleDist = Double.MAX_VALUE;
        for (Vehicle other : vehicles.vehicles()) {
            if (other == v || other.destroyed || !other.world.equals(w)) continue;
            double t = other.hull().rayHit(start.getX(), start.getY(), start.getZ(), d[0], d[1], d[2], MachineGun.RANGE);
            if (t >= 0 && t < vehicleDist && t <= reach) {
                vehicleDist = t;
                hitVehicle = other;
            }
        }

        w.spawnParticle(Particle.FLAME, start, 1, 0.05, 0.05, 0.05, 0.01);
        w.spawnParticle(Particle.SMOKE, start, 2, 0.1, 0.1, 0.1, 0.01);

        double end = reach;
        if (hitVehicle != null) {
            end = vehicleDist;
            vehicles.damage(hitVehicle, MachineGun.VEHICLE_DAMAGE);
            spark(w, start.clone().add(dir.clone().multiply(end)));
        } else if (hit != null) {
            if (hit.getHitEntity() instanceof LivingEntity le) {
                le.damage(MachineGun.LIVING_DAMAGE, pilot);
            }
            spark(w, hit.getHitPosition().toLocation(w));
        }
        for (double t = 3; t < end; t += 5) { // a thin tracer line
            w.spawnParticle(Particle.END_ROD, start.clone().add(dir.clone().multiply(t)), 1, 0, 0, 0, 0);
        }
    }

    private static void spark(World w, Location at) {
        w.spawnParticle(Particle.CRIT, at, 4, 0.15, 0.15, 0.15, 0.1);
        w.spawnParticle(Particle.SMOKE, at, 2, 0.1, 0.1, 0.1, 0.01);
    }
}
