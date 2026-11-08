package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.entity.Player;

/** The rendered model and its driver seat. Implemented in {@link BetterModelHook} so BetterModel types stay there. */
interface VehicleRig {

    /** Puts the player in the driver seat. False if the seat isn't there (yet) or mounting failed. */
    boolean seat(Player player);

    /** True while the player sits in this rig's driver seat. */
    boolean isSeated(Player player);

    void unseat(Player player);

    /** Removes the model, its seat entities and anyone sitting in them. */
    void close();
}
