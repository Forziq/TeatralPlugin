package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.plugin.Plugin;

/** Vehicle values Egor can change in config.yml. Read on every use, so a restart isn't the only way in. */
final class VehicleSettings {

    static final String MAX_KEY = "max-vehicles";
    static final String IDLE_KEY = "idle-despawn-minutes";

    private VehicleSettings() {
    }

    static int maxVehicles(Plugin plugin) {
        return PadRules.sanitiseMax(plugin.getConfig().getInt(MAX_KEY, PadRules.DEFAULT_MAX_VEHICLES));
    }

    static int idleMinutes(Plugin plugin) {
        return plugin.getConfig().getInt(IDLE_KEY, IdleTimer.DEFAULT_MINUTES);
    }
}
