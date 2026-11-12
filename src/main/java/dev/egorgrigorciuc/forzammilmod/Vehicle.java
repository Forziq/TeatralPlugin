package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.egorgrigorciuc.forzammilmod.CrewSeats.Seat;

/**
 * One spawned vehicle: an invisible marker armour stand we move by hand (the anchor BetterModel draws on),
 * an Interaction box to right-click, and the model with its driver seat. Main thread only, except
 * {@link #renderYaw()}, which BetterModel reads from its own thread.
 */
final class Vehicle {

    /** Turn the model if it turns out to drive backwards in game (the T15 models point -Z). */
    static final float MODEL_YAW_OFFSET = 0f;

    final VehicleType type;
    /** Removes the vehicle after 10 minutes with no driver. */
    final IdleTimer idle = new IdleTimer(System.currentTimeMillis());
    final ArmorStand anchor;
    final Interaction clickBox;
    final World world;
    private VehicleRig rig;
    private VehicleMover.State state;
    private volatile float renderYaw;
    private final Map<Seat, UUID> crew = new EnumMap<>(Seat.class);
    private final Map<UUID, Long> joined = new HashMap<>();
    /** Null if the vehicle has no turret. Main thread only. */
    private final TurretModel turret;
    /** Gun reload clock (ticks of ShellService). */
    final ReloadTimer reload;
    final VehicleHealth health;
    /** Set once the vehicle has blown up, so it cannot blow up twice. */
    boolean destroyed;
    int smokeTick;
    /** Degrees the turret turned in the last tick (for the turning sound). Main thread only. */
    float turretYawDelta;
    /** Plane only (null otherwise): the flight state FlightModel works on. Main thread only. */
    private FlightModel.State flight;
    /** Plane only: the machine guns' burst timing. */
    final MachineGun gun = new MachineGun();
    /** Model pitch in Minecraft terms (nose down positive), read by BetterModel's thread. 0 for ground vehicles. */
    private volatile float renderPitch;
    /** Plane only: propeller angle in degrees. */
    private float propAngle;
    /** Plane only: blocks between the wheels and the ground below, for the pilot's read-out. */
    double heightAboveGround;

    Vehicle(VehicleType type, ArmorStand anchor, Interaction clickBox, VehicleMover.State state) {
        this.type = type;
        this.reload = new ReloadTimer(type.reloadTicks());
        this.health = new VehicleHealth(type.maxHealth);
        this.anchor = anchor;
        this.clickBox = clickBox;
        this.world = anchor.getWorld();
        this.state = state;
        this.renderYaw = state.yaw() + MODEL_YAW_OFFSET;
        this.turret = type.isArtillery() ? new TurretModel(TurretModel.FIELD_GUN_PITCH_MIN, TurretModel.FIELD_GUN_PITCH_MAX)
                : type.isTank() ? new TurretModel() : null;
        if (type.isPlane()) flight = FlightModel.State.parked(state.x(), state.y(), state.z(), state.yaw());
    }

    void rig(VehicleRig rig) {
        this.rig = rig;
    }

    VehicleRig rig() {
        return rig;
    }

    float renderYaw() {
        return renderYaw;
    }

    VehicleMover.State state() {
        return state;
    }

    float renderPitch() {
        return renderPitch;
    }

    /** Plane only; null for ground vehicles. */
    FlightModel.State flight() {
        return flight;
    }

    /** True for a plane that is off the ground. */
    boolean airborne() {
        return flight != null && !flight.onGround();
    }

    UUID driver() {
        return crew.get(Seat.DRIVER);
    }

    void driver(UUID id) {
        if (id == null) vacate(Seat.DRIVER);
        else occupy(Seat.DRIVER, id);
    }

    UUID occupant(Seat seat) {
        return crew.get(seat);
    }

    /** Seats somebody sits in right now. */
    List<Seat> takenSeats() {
        return new ArrayList<>(crew.keySet());
    }

    /** Seat this player holds, or null. */
    Seat seatOf(UUID id) {
        for (Map.Entry<Seat, UUID> e : crew.entrySet()) if (e.getValue().equals(id)) return e.getKey();
        return null;
    }

    boolean hasCrew() {
        return !crew.isEmpty();
    }

    void occupy(Seat seat, UUID id) {
        crew.put(seat, id);
        joined.put(id, System.currentTimeMillis());
    }

    void vacate(Seat seat) {
        UUID id = crew.remove(seat);
        if (id != null) joined.remove(id);
    }

    /** When this player got in; the right-click that seats them also swings their arm. 0 if not aboard. */
    long joinedMillis(UUID id) {
        return joined.getOrDefault(id, 0L);
    }

    /** Who aims the turret and fires: the gunner if there is one, else the driver. Null if nobody or no turret. */
    UUID turretOperator() {
        Seat s = CrewSeats.turretController(type, crew.keySet());
        return s == null ? null : crew.get(s);
    }

    boolean owns(Entity e) {
        UUID id = e.getUniqueId();
        return id.equals(anchor.getUniqueId()) || id.equals(clickBox.getUniqueId());
    }

    boolean isValid() {
        return anchor.isValid() && clickBox.isValid();
    }

    Location location() {
        return new Location(world, state.x(), state.y(), state.z(), state.yaw(), 0f);
    }

    /** Runs one movement tick and moves the entities if anything changed. */
    void move(Terrain terrain, DriveInput input) {
        VehicleMover.State old = state;
        state = VehicleMover.tick(type, terrain, old, input);
        renderYaw = state.yaw() + MODEL_YAW_OFFSET;
        boolean moved = Math.abs(state.x() - old.x()) > 1e-4 || Math.abs(state.y() - old.y()) > 1e-4
                || Math.abs(state.z() - old.z()) > 1e-4;
        if (moved || state.yaw() != old.yaw()) {
            anchor.teleport(location());
        }
        if (moved) {
            clickBox.teleport(new Location(world, state.x(), state.y(), state.z()));
        }
    }

    /** Pitch sign for the model: flip to -1 if the nose tilts the wrong way in game. */
    static final float PLANE_PITCH_SIGN = 1f;
    /** Propeller turn per tick: idling, plus this much more at full throttle. */
    static final float PROP_IDLE = 20f, PROP_FULL = 35f;

    /**
     * Runs one flight tick (plane only) and moves the entities. {@code engineOn} spins the propeller.
     * The state other code reads ({@link #state()}) mirrors the flight: speed is the airspeed.
     */
    FlightModel.Outcome fly(Terrain terrain, FlightModel.Input input, boolean engineOn) {
        FlightModel.Result r = FlightModel.step(flight, input, terrain);
        FlightModel.State f = r.state();
        flight = f;
        VehicleMover.State old = state;
        state = new VehicleMover.State(f.x(), f.y(), f.z(), f.yaw(), f.speed(), f.verticalSpeed());
        renderYaw = f.yaw() + MODEL_YAW_OFFSET;
        renderPitch = -f.pitch() * PLANE_PITCH_SIGN; // Minecraft pitch: nose up is negative
        boolean moved = Math.abs(state.x() - old.x()) > 1e-4 || Math.abs(state.y() - old.y()) > 1e-4
                || Math.abs(state.z() - old.z()) > 1e-4;
        if (moved || state.yaw() != old.yaw()) anchor.teleport(location());
        if (moved) clickBox.teleport(new Location(world, state.x(), state.y(), state.z()));
        if (engineOn) propAngle = (propAngle + PROP_IDLE + PROP_FULL * (float) f.throttle()) % 360f;
        if (rig != null) rig.plane(f.roll(), propAngle);
        return r.outcome();
    }

    private float wheelAngle;

    /** Rolls the wheels with the current speed. Call once per tick after {@link #move}. */
    void spinWheels() {
        if (rig == null || state.speed() == 0) return;
        wheelAngle = WheelSpin.advance(wheelAngle, state.speed(), WheelSpin.radius(type));
        rig.wheels(wheelAngle);
    }

    HullBox hull() {
        return new HullBox(state.x(), state.y(), state.z(), state.yaw(), type.halfWidth, type.halfLength, type.height);
    }

    boolean hasTurret() {
        return turret != null;
    }

    /**
     * Turns the turret and tilts the barrel towards where the turret operator (gunner, else driver) looks, within the rate limits.
     * Call once per tick after {@link #move}. Without an operator it is simply not called, so the turret holds.
     */
    void aimTurret(Player operator) {
        if (turret == null || rig == null || !rig.hasTurret()) return;
        Location look = operator.getLocation();
        // The field gun never swings its barrel sideways: the whole gun turns with A and D, only the pitch follows the look.
        float rel = type.isArtillery() ? 0f : TurretModel.relativeYaw(look.getYaw(), renderYaw);
        float before = turret.yaw();
        turret.tick(rel, TurretModel.pitchUpFromMc(look.getPitch()), 0.05f);
        turretYawDelta = TurretModel.wrap(turret.yaw() - before);
        rig.turret(turret.yaw(), turret.pitch());
    }

    /** Muzzle world position and aim direction, or null if there is no turret. For T18. */
    TurretModel.Muzzle muzzle() {
        if (turret == null) return null;
        TurretModel.Gun gun = type.isArtillery() ? TurretModel.FIELD_GUN : TurretModel.TANK_GUN;
        return TurretModel.muzzle(state.x(), state.y(), state.z(), renderYaw, turret.yaw(), turret.pitch(), gun);
    }

    void remove() {
        crew.clear();
        joined.clear();
        if (rig != null) {
            try {
                rig.close();
            } catch (RuntimeException | LinkageError ignored) {
                // BetterModel may already be gone on shutdown
            }
        }
        anchor.remove();
        clickBox.remove();
    }
}
