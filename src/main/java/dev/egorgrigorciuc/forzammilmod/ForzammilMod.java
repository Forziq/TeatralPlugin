package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ForzammilMod extends JavaPlugin {

    @Override
    public void onEnable() {
        register("forzammilmod", new ForzammilCommand(this));
        register("emote", new EmoteCommand());
    }

    private void register(String name, org.bukkit.command.TabExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Command missing from plugin.yml: " + name);
            return;
        }
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
    }
}
