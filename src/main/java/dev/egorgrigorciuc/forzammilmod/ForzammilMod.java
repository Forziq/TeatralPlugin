package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ForzammilMod extends JavaPlugin {

    private Lang lang;

    public Lang lang() {
        return lang;
    }

    @Override
    public void onEnable() {
        lang = Lang.load(this);
        getServer().getPluginManager().registerEvents(new LangListener(lang), this);
        register("forzammilmod", new ForzammilCommand(this));
        register("emote", new EmoteCommand(lang));
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
