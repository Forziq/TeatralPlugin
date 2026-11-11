package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.CrewSeats.Seat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Health boss bar and action bar for everybody seated in a vehicle. One bar per player (the title is in their
 * language). The vehicle tick calls {@link #update} for each vehicle and {@link #endTick} once; anyone not
 * updated that tick has left, so their bar goes. The only sender of the vehicle action bar. Main thread only.
 */
final class VehicleHud {

    private static final int ACTION_BAR_EVERY = 5;

    private static final class Entry {
        Vehicle vehicle;
        BossBar bar;
        String title = "";
        boolean actionShown;
    }

    private final Lang lang;
    private final LongSupplier clock;
    private final Map<UUID, Entry> entries = new HashMap<>();
    private final Set<UUID> seen = new HashSet<>();
    private long ticks;

    /** @param clock the gun reload clock (ShellService ticks), so reload seconds match the timers. */
    VehicleHud(Lang lang, LongSupplier clock) {
        this.lang = lang;
        this.clock = clock;
    }

    void update(Vehicle v) {
        Seat controller = CrewSeats.turretController(v.type, v.takenSeats());
        boolean sendActionBar = ticks % ACTION_BAR_EVERY == 0;
        for (Seat seat : v.takenSeats()) {
            UUID id = v.occupant(seat);
            Player p = id == null ? null : Bukkit.getPlayer(id);
            if (p == null || !p.isOnline() || p.isDead()) continue;
            seen.add(id);
            Entry e = entries.get(id);
            if (e != null && e.vehicle != v) {
                entries.remove(id);
                dispose(e, id);
                e = null;
            }
            if (e == null) {
                e = new Entry();
                e.vehicle = v;
                // The rough plane has no health bar: it is shot down or crashes, nothing in between to show.
                if (!v.type.isPlane()) {
                    e.bar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
                    e.bar.addPlayer(p);
                }
                entries.put(id, e);
            }
            if (e.bar == null) {
                if (sendActionBar) {
                    String line = actionLine(p, v, seat, controller);
                    if (line != null) {
                        e.actionShown = true;
                        send(p, line);
                    }
                }
                continue;
            }
            String title = lang.msg(p, "vehicle.name." + v.type.id);
            if (!title.equals(e.title)) {
                e.title = title;
                e.bar.setTitle(title);
            }
            double fraction = v.health.fraction();
            e.bar.setProgress(HudModel.progress(fraction));
            e.bar.setColor(switch (HudModel.band(fraction)) {
                case GREEN -> BarColor.GREEN;
                case YELLOW -> BarColor.YELLOW;
                case RED -> BarColor.RED;
            });
            if (sendActionBar) {
                String line = actionLine(p, v, seat, controller);
                if (line != null) {
                    e.actionShown = true;
                    send(p, line);
                }
            }
        }
    }

    /** Call once after every vehicle was updated: drops the bars of everyone who is no longer aboard. */
    void endTick() {
        ticks++;
        for (Iterator<Map.Entry<UUID, Entry>> it = entries.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Entry> me = it.next();
            if (seen.contains(me.getKey())) continue;
            it.remove();
            dispose(me.getValue(), me.getKey());
        }
        seen.clear();
    }

    /** The player left (quit, died, teleported). */
    void clear(Player p) {
        Entry e = entries.remove(p.getUniqueId());
        if (e != null) dispose(e, p.getUniqueId());
    }

    /** The vehicle is gone: everyone aboard loses the bars. */
    void clear(Vehicle v) {
        for (Iterator<Map.Entry<UUID, Entry>> it = entries.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Entry> me = it.next();
            if (me.getValue().vehicle != v) continue;
            it.remove();
            dispose(me.getValue(), me.getKey());
        }
    }

    void clearAll() {
        for (Map.Entry<UUID, Entry> me : entries.entrySet()) dispose(me.getValue(), me.getKey());
        entries.clear();
        seen.clear();
    }

    private void dispose(Entry e, UUID id) {
        if (e.bar != null) e.bar.removeAll();
        if (!e.actionShown) return;
        Player p = Bukkit.getPlayer(id);
        if (p != null && p.isOnline()) send(p, "");
    }

    private String actionLine(Player p, Vehicle v, Seat seat, Seat controller) {
        String speed = null, reload = null;
        if (v.type.isPlane()) {
            // The pilot sees speed and height above the ground, nothing else.
            if (!HudModel.showsSpeed(seat)) return null;
            return lang.msg(p, "hud.speed", "n", HudModel.formatSpeed(v.state().speed())) + ChatColor.DARK_GRAY + "  |  "
                    + lang.msg(p, "hud.height", "n", HudModel.formatHeight(v.heightAboveGround));
        }
        if (HudModel.showsSpeed(seat) && v.type.moves()) {
            speed = lang.msg(p, "hud.speed", "n", HudModel.formatSpeed(v.state().speed()));
        }
        if (HudModel.showsReload(seat, controller) && v.hasTurret()) {
            int left = v.reload.remainingSeconds(clock.getAsLong());
            reload = left > 0 ? lang.plural(p, "reload.seconds", left, "n", left) : lang.msg(p, "reload.ready");
        }
        if (speed != null && reload != null) return speed + ChatColor.DARK_GRAY + "  |  " + reload;
        return speed != null ? speed : reload;
    }

    private static void send(Player p, String text) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
    }
}
