package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ForzammilMod extends JavaPlugin {

    private Lang lang;
    private UpdateService updates;

    public Lang lang() {
        return lang;
    }

    public String jarFileName() {
        return getFile().getName();
    }

    public UpdateService updates() {
        return updates;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = Lang.load(this);
        updates = new UpdateService(this);
        PackService pack = PackService.create(this);
        if (pack != null) {
            getServer().getPluginManager().registerEvents(pack, this);
        }
        updates.checkOnStartup();
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
