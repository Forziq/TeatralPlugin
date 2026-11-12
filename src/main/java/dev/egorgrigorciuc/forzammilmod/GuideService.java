package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** The short in-game guide: shown once after a player's first join, and again on /fmod guide. */
public final class GuideService implements Listener {

    /** 6 seconds, so it lands after the resource pack prompt. */
    static final long JOIN_DELAY_TICKS = 120;

    static final List<String> LINE_KEYS = List.of("guide.emotes", "guide.pads", "guide.car", "guide.tank",
            "guide.gun", "guide.plane", "guide.repair", "guide.radio", "guide.footer");

    private final ForzammilMod plugin;
    private final SeenGuide seen;

    GuideService(ForzammilMod plugin) {
        this.plugin = plugin;
        this.seen = new SeenGuide(plugin.getDataFolder().toPath().resolve("seen-guide.yml"));
        try {
            seen.load();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read seen-guide.yml: " + e.getMessage());
        }
    }

    /** Sends the whole guide to one sender. */
    void show(CommandSender to) {
        Lang lang = plugin.lang();
        String greeting = plugin.emotes() == null ? GreetingSettings.DEFAULT_ID : plugin.emotes().greeting().id();
        to.sendMessage(lang.msg(to, "guide.header"));
        for (String key : LINE_KEYS) to.sendMessage(lang.msg(to, key, "greeting", greeting));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (seen.hasSeen(id)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) return; // not shown, so they get it next time
            if (!seen.markSeen(id)) return;
            show(p);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    seen.save();
                } catch (IOException ex) {
                    plugin.getLogger().warning("Could not save seen-guide.yml: " + ex.getMessage());
                }
            });
        }, JOIN_DELAY_TICKS);
    }
}
