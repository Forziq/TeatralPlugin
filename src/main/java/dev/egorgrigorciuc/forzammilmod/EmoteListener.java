package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Ends an emote when the player walks, gets hurt, dies, teleports or leaves. */
public final class EmoteListener implements Listener {

    private final EmoteService emotes;
    private final Lang lang;
    private final Plugin plugin;

    public EmoteListener(Plugin plugin, EmoteService emotes, Lang lang) {
        this.plugin = plugin;
        this.emotes = emotes;
        this.lang = lang;
    }

    private void cancel(Player p, EmoteService.StopReason reason) {
        if (emotes.stop(p, reason)) p.sendMessage(lang.msg(p, "emote.stopped"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!emotes.isPlaying(p)) return;
        var f = e.getFrom();
        var t = e.getTo();
        if (t == null) return;
        if (MoveCheck.isMoving(f.getX(), f.getY(), f.getZ(), t.getX(), t.getY(), t.getZ())) {
            cancel(p, EmoteService.StopReason.MOVED);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && emotes.isPlaying(p)) cancel(p, EmoteService.StopReason.DAMAGE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (emotes.isPlaying(e.getPlayer())) cancel(e.getPlayer(), EmoteService.StopReason.TELEPORT);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        emotes.stop(e.getEntity(), EmoteService.StopReason.DEATH);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        emotes.stop(e.getPlayer(), EmoteService.StopReason.QUIT);
    }

    /** The server resends real gear on these; put the empty gear back one tick later. */
    private void refreshLater(Player p) {
        if (!emotes.isPlaying(p)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> emotes.refreshHidden(p));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeld(PlayerItemHeldEvent e) {
        refreshLater(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        refreshLater(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) refreshLater(p);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) refreshLater(p);
    }

    private void showLater(Player viewer) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (viewer.isOnline()) emotes.showHiddenTo(viewer);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        showLater(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        showLater(e.getPlayer());
    }
}
