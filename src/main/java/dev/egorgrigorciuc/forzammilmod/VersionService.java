package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.util.Properties;

import dev.egorgrigorciuc.forzammilmod.VersionCheck.Status;

/** Checks plugin / resource pack / BetterModel versions at startup and tells the owner on join. */
public final class VersionService implements Listener {

    private final ForzammilMod plugin;
    private final String pinnedBetterModel;

    public VersionService(ForzammilMod plugin) {
        this.plugin = plugin;
        this.pinnedBetterModel = readPinned(plugin);
    }

    private static String readPinned(ForzammilMod plugin) {
        try (InputStream in = plugin.getResource("versions.properties")) {
            if (in == null) return "";
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("bettermodel", "").trim();
        } catch (java.io.IOException e) {
            return "";
        }
    }

    public String pluginVersion() {
        return plugin.getDescription().getVersion();
    }

    public String packVersion() {
        return plugin.packVersion();
    }

    public String pinnedBetterModel() {
        return pinnedBetterModel;
    }

    /** Installed BetterModel version, or "" when the plugin isn't there. */
    public String installedBetterModel() {
        Plugin bm = Bukkit.getPluginManager().getPlugin("BetterModel");
        return bm == null ? "" : bm.getDescription().getVersion();
    }

    public Status packStatus() {
        return VersionCheck.pack(pluginVersion(), packVersion());
    }

    public Status betterModelStatus() {
        return VersionCheck.betterModel(pinnedBetterModel, installedBetterModel());
    }

    /** Logs the three checks. Call after plugins have loaded. */
    public void logStartup() {
        var log = plugin.getLogger();
        log.info("Plugin version " + pluginVersion());
        String pv = packVersion();
        switch (packStatus()) {
            case OK -> log.info("Resource pack version " + pv + " matches the plugin.");
            case MISMATCH -> log.warning("Resource pack version " + pv + " does not match plugin version "
                    + pluginVersion() + "; models and sounds may be wrong. Release again so both match.");
            case UNKNOWN -> log.warning("Resource pack version is unknown (dev build or no pack.properties); "
                    + "cannot check it against plugin version " + pluginVersion() + ".");
        }
        String installed = installedBetterModel();
        switch (betterModelStatus()) {
            case OK -> log.info("BetterModel " + installed + " matches the pinned version " + pinnedBetterModel + ".");
            case UNKNOWN -> log.warning("Pinned BetterModel version is unknown; cannot check the installed one.");
            case MISMATCH -> {
                log.warning("==================== BetterModel MISMATCH ====================");
                log.warning("Pinned version:    " + pinnedBetterModel);
                log.warning("Installed version: " + (installed.isEmpty() ? "NOT INSTALLED" : installed));
                log.warning("Vehicle and emote models may be broken. Install the pinned (patched) BetterModel jar.");
                log.warning("===============================================================");
            }
        }
    }

    /** Shows the three versions with an OK / mismatch marker each. */
    public void show(CommandSender to) {
        Lang lang = plugin.lang();
        to.sendMessage(lang.msg(to, "versioncheck.header"));
        String pv = packVersion();
        String installed = installedBetterModel();
        to.sendMessage(lang.msg(to, "versioncheck.plugin", "version", pluginVersion()));
        to.sendMessage(line(to, "versioncheck.pack", packStatus(), pv.isEmpty() ? "?" : pv, pluginVersion()));
        to.sendMessage(line(to, "versioncheck.bettermodel", betterModelStatus(),
                installed.isEmpty() ? lang.msg(to, "versioncheck.missing") : installed, pinnedBetterModel));
    }

    private String line(CommandSender to, String key, Status s, String found, String expected) {
        Lang lang = plugin.lang();
        String mark = lang.msg(to, switch (s) {
            case OK -> "versioncheck.ok";
            case MISMATCH -> "versioncheck.bad";
            case UNKNOWN -> "versioncheck.unknown";
        });
        return lang.msg(to, key, "found", found, "expected", expected, "mark", mark);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!Access.isOwner(p)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            Lang lang = plugin.lang();
            Status pack = packStatus();
            if (pack != Status.OK) {
                p.sendMessage(lang.msg(p, "versioncheck.warn-pack", "pack", packVersion().isEmpty() ? "?" : packVersion(),
                        "plugin", pluginVersion()));
            }
            if (betterModelStatus() == Status.MISMATCH) {
                String installed = installedBetterModel();
                p.sendMessage(lang.msg(p, "versioncheck.warn-bettermodel",
                        "found", installed.isEmpty() ? lang.msg(p, "versioncheck.missing") : installed,
                        "expected", pinnedBetterModel));
            }
        }, 40L);
    }
}
