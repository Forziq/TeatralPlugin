package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ForzammilMod extends JavaPlugin {

    private Lang lang;
    private UpdateService updates;
    private PackService pack;
    private EmoteService emotes;
    /** Set when the server-load event fires after our enable; BetterModel loads its models on that event. */
    private volatile boolean serverLoadSeen;

    public Lang lang() {
        return lang;
    }

    public String jarFileName() {
        return getFile().getName();
    }

    public UpdateService updates() {
        return updates;
    }

    /** Null in dev builds without pack.properties. */
    public PackService pack() {
        return pack;
    }

    @Override
    public void onDisable() {
        if (emotes != null) emotes.stopAll();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        installModels();
        lang = Lang.load(this);
        updates = new UpdateService(this);
        pack = PackService.create(this);
        if (pack != null) {
            getServer().getPluginManager().registerEvents(pack, this);
        }
        updates.checkOnStartup();
        getServer().getPluginManager().registerEvents(new LangListener(lang), this);
        register("forzammilmod", new ForzammilCommand(this));
        emotes = new EmoteService(this);
        getServer().getPluginManager().registerEvents(new EmoteListener(emotes, lang), this);
        register("emote", new EmoteCommand(lang, emotes));
    }

    /**
     * Copies our models into BetterModel. Runs on the main thread on purpose: this is server startup (no players,
     * no ticks yet) and the files must be in place before BetterModel's first load on the server-load event, which
     * then picks them up without an extra reload. Only if we were enabled after that (a plugin manager, /reload
     * edge cases) do we ask BetterModel for one reload, off the main thread.
     */
    private void installModels() {
        Plugin betterModel = getServer().getPluginManager().getPlugin("BetterModel");
        if (betterModel == null || !betterModel.isEnabled()) {
            getLogger().warning("BetterModel is not enabled; skipping model install (emotes will not animate)");
            return;
        }
        BetterModelHook.logReloads(this);
        var written = new ModelInstaller(getFile(), betterModel.getDataFolder().toPath(), getLogger()).install();
        if (written.isEmpty()) return;
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onServerLoad(ServerLoadEvent event) {
                serverLoadSeen = true;
            }
        }, this);
        // The scheduler first runs after startup has finished, so by then we know whether BetterModel loaded.
        getServer().getScheduler().runTask(this, () -> {
            if (serverLoadSeen) {
                getLogger().info("BetterModel picked up the new models during startup; no extra reload needed");
            } else {
                BetterModelHook.reloadAsync(this);
            }
        });
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
