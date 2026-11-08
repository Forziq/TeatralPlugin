package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Adds our required resource pack on join (stacked on top of any server pack) and kicks players who refuse it. */
public final class PackService implements Listener {

    private final ForzammilMod plugin;
    private final PackProperties pack;
    private final UUID packId;
    /** Last status each online player's client reported for our pack. */
    private final Map<UUID, Status> statuses = new ConcurrentHashMap<>();

    private PackService(ForzammilMod plugin, PackProperties pack) {
        this.plugin = plugin;
        this.pack = pack;
        this.packId = UUID.nameUUIDFromBytes(pack.sha1());
    }

    /** Returns null (after one warning) when pack.properties is missing or invalid. */
    public static PackService create(ForzammilMod plugin) {
        try (InputStream in = plugin.getResource("pack.properties")) {
            var parsed = PackProperties.parse(in);
            if (parsed.isEmpty()) {
                plugin.getLogger().warning("pack.properties missing or invalid; resource pack disabled (dev build).");
                return null;
            }
            return new PackService(plugin, parsed.get());
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Could not read pack.properties; resource pack disabled.");
            return null;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            plugin.lang().setLocale(p.getUniqueId(), p.getLocale());
            // add, not set: set would unload any pack the server itself sends (server.properties)
            p.addResourcePack(packId, pack.url(), pack.sha1(), plugin.lang().msg(p, "pack.prompt"), true);
        }, 1L);
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent e) {
        if (!packId.equals(e.getID())) return; // another pack (e.g. the server's own); not ours to police
        Player p = e.getPlayer();
        statuses.put(p.getUniqueId(), e.getStatus());
        String key = switch (e.getStatus()) {
            case DECLINED -> "pack.kick-declined";
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> "pack.kick-failed";
            default -> null;
        };
        if (key != null) {
            p.kickPlayer(plugin.lang().msg(p, key));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        statuses.remove(e.getPlayer().getUniqueId());
    }

    /** Last status the player's client reported for our pack, or null if it hasn't answered yet. */
    public Status status(Player p) {
        return statuses.get(p.getUniqueId());
    }
}
