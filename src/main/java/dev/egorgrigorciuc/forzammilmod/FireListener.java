package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;

/** Left-click while driving a tank fires the gun. Three paths are listened to; debounce is in the service. */
public final class FireListener implements Listener {

    private final ShellService shells;

    public FireListener(ShellService shells) {
        this.shells = shells;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (!shells.isTankDriver(e.getPlayer())) return;
        e.setCancelled(true);
        shells.fireClick(e.getPlayer(), "interact");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwing(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        shells.fireClick(e.getPlayer(), "swing");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !shells.isTankDriver(p)) return;
        e.setCancelled(true);
        shells.fireClick(p, "hit");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent e) {
        if (shells.isTankDriver(e.getPlayer())) e.setCancelled(true);
    }
}
