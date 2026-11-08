package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Ends an emote when the player walks, gets hurt, dies, teleports or leaves. */
public final class EmoteListener implements Listener {

    private final EmoteService emotes;
    private final Lang lang;

    public EmoteListener(EmoteService emotes, Lang lang) {
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
}
