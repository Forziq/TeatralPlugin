package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.entity.Player;

/** The rendered model and its driver seat. Implemented in {@link BetterModelHook} so BetterModel types stay there. */
interface VehicleRig {

    /** Puts the player in the driver seat. False if the seat isn't there (yet) or mounting failed. */
    boolean seat(Player player);

    /** True while the player sits in this rig's driver seat. */
    boolean isSeated(Player player);

    void unseat(Player player);

    /** True if the model has a turret bone (the jeep does not). */
    boolean hasTurret();

    /** Sets the drawn turret angle (relative to the hull) and barrel angle (up positive), in degrees. No-op without a turret. */
    void turret(float yawRel, float pitchUp);

    /** Removes the model, its seat entities and anyone sitting in them. */
    void close();
}
