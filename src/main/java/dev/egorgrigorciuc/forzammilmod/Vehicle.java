package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * One spawned vehicle: an invisible marker armour stand we move by hand (the anchor BetterModel draws on),
 * an Interaction box to right-click, and the model with its driver seat. Main thread only, except
 * {@link #renderYaw()}, which BetterModel reads from its own thread.
 */
final class Vehicle {

    /** Turn the model if it turns out to drive backwards in game (the T15 models point -Z). */
    static final float MODEL_YAW_OFFSET = 0f;

    final VehicleType type;
    final ArmorStand anchor;
    final Interaction clickBox;
    final World world;
    private VehicleRig rig;
    private VehicleMover.State state;
    private volatile float renderYaw;
    private UUID driver;

    Vehicle(VehicleType type, ArmorStand anchor, Interaction clickBox, VehicleMover.State state) {
        this.type = type;
        this.anchor = anchor;
        this.clickBox = clickBox;
        this.world = anchor.getWorld();
        this.state = state;
        this.renderYaw = state.yaw() + MODEL_YAW_OFFSET;
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

    UUID driver() {
        return driver;
    }

    void driver(UUID id) {
        this.driver = id;
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

    void remove() {
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
