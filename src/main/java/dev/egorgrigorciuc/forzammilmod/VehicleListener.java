package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
        if (tryRepair(p, v)) return;
        switch (vehicles.mount(p, v)) {
            case MOUNTED -> p.sendMessage(lang.msg(p, "vehicle.entered"));
            case HAS_DRIVER -> p.sendMessage(lang.msg(p, "vehicle.has-driver"));
            case NOT_READY -> p.sendMessage(lang.msg(p, "vehicle.not-ready"));
            case ALREADY_DRIVING -> { }
        }
    }

    /** Iron ingot in the main hand beats mounting. Both click events fire for one click, so repeats within 150 ms are ignored. */
    private boolean tryRepair(Player p, Vehicle v) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() != Material.IRON_INGOT || p.getUniqueId().equals(v.driver())) return false;
        long now = System.currentTimeMillis();
        Long last = lastRepair.put(p.getUniqueId(), now);
        if (last != null && now - last < 150) return true;
        if (vehicles.repair(v)) {
            if (hand.getAmount() <= 1) p.getInventory().setItemInMainHand(null);
            else hand.setAmount(hand.getAmount() - 1);
            p.sendMessage(lang.msg(p, "vehicle.repaired", "percent", v.health.percent()));
        } else {
            p.sendMessage(lang.msg(p, "vehicle.already-full"));
        }
        return true;
    }

    private final Map<UUID, Long> lastRepair = new HashMap<>();

    /** Vanilla damage never lands on vehicle entities; players and arrows do our own small damage. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        Vehicle v = vehicles.find(e.getEntity());
        if (v == null) return;
        e.setCancelled(true);
        if (!(e instanceof EntityDamageByEntityEvent by)) return;
        if (by.getDamager() instanceof Player p) {
            if (p.getUniqueId().equals(v.driver())) return;
            vehicles.damage(v, DamageRules.melee(v.type));
        } else if (by.getDamager() instanceof Projectile) {
            vehicles.damage(v, DamageRules.arrow(v.type));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onManipulate(PlayerArmorStandManipulateEvent e) {
        if (vehicles.find(e.getRightClicked()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastRepair.remove(e.getPlayer().getUniqueId());
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
