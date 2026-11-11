package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.CrewSeats.Seat;
import org.bukkit.entity.Player;

/** The rendered model and its seats. Implemented in {@link BetterModelHook} so BetterModel types stay there. */
interface VehicleRig {

    /** Puts the player in the seat. False if the seat isn't there (yet) or mounting failed. */
    boolean seat(Player player, Seat seat);

    /** True while the player sits in this rig's given seat. */
    boolean isSeated(Player player, Seat seat);

    void unseat(Player player, Seat seat);

    /** True if the model has a turret bone (the jeep does not). */
    boolean hasTurret();

    /** Sets the drawn turret angle (relative to the hull) and barrel angle (up positive), in degrees. No-op without a turret. */
    void turret(float yawRel, float pitchUp);

    /** Sets the roll angle of the wheel bones in degrees. No-op if the model has no wheels. */
    default void wheels(float angleDeg) {}

    /** Plane only: bank angle (right wing down positive) and propeller angle, in degrees. No-op for other models. */
    default void plane(float rollDeg, float propellerDeg) {}

    /** Removes the model, its seat entities and anyone sitting in them. */
    void close();
}
