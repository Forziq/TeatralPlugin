package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Right-click to get in, protection for vehicle entities, and taking drivers out when they leave. */
public final class VehicleListener implements Listener {

    private final VehicleService vehicles;
    private final Lang lang;

    public VehicleListener(VehicleService vehicles, Lang lang) {
        this.vehicles = vehicles;
        this.lang = lang;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        handleClick(e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractAt(PlayerInteractAtEntityEvent e) {
        handleClick(e);
    }

    /** Both events can fire for one click; the second finds the player already driving and stays quiet. */
    private void handleClick(PlayerInteractEntityEvent e) {
        Vehicle v = vehicles.find(e.getRightClicked());
        if (v == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        switch (vehicles.mount(p, v)) {
            case MOUNTED -> p.sendMessage(lang.msg(p, "vehicle.entered"));
            case HAS_DRIVER -> p.sendMessage(lang.msg(p, "vehicle.has-driver"));
            case NOT_READY -> p.sendMessage(lang.msg(p, "vehicle.not-ready"));
            case ALREADY_DRIVING -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (vehicles.find(e.getEntity()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onManipulate(PlayerArmorStandManipulateEvent e) {
        if (vehicles.find(e.getRightClicked()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        vehicles.driverGone(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        vehicles.driverGone(e.getEntity());
    }

    /** Getting out by sneaking also teleports (cause DISMOUNT); the tick loop handles that one. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.DISMOUNT) return;
        vehicles.driverGone(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInput(PlayerInputEvent e) {
        vehicles.noteInputEvent(e.getPlayer(), e.getInput());
    }
}
