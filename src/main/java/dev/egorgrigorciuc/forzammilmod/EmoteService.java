package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** One active emote per player. Main thread only (BetterModel end callbacks hop back via the scheduler). */
public final class EmoteService {

    public enum StopReason { COMMAND, MOVED, DAMAGE, DEATH, TELEPORT, QUIT, FINISHED }

    public enum PlayResult { STARTED, ALREADY_PLAYING, UNAVAILABLE, COOLDOWN }

    public static final String WAVE = "wave";
    public static final double SOUND_RADIUS = 16;

    private record Active(long token, AutoCloseable handle) {}

    private final JavaPlugin plugin;
    private final Map<UUID, Active> active = new HashMap<>();
    private static final EquipmentSlot[] GEAR_SLOTS = {EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final EmoteCooldown cooldown = new EmoteCooldown();
    private final HiddenGear hiddenGear = new HiddenGear();
    private GreetingSettings greeting = GreetingSettings.defaults();
    private long nextToken;

    public EmoteService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Re-reads the greeting section of config.yml and logs the sound key so typos show up. */
    public void loadGreeting() {
        greeting = GreetingSettings.from(plugin.getConfig().getConfigurationSection("greeting"),
                msg -> plugin.getLogger().warning(msg));
        plugin.getLogger().info("Greeting emote: /emote " + greeting.id() + ", sound \""
                + (greeting.silent() ? "(none)" : greeting.sound()) + "\"");
    }

    public GreetingSettings greeting() {
        return greeting;
    }

    /** Ids players can type, in list order. */
    public List<String> emoteIds() {
        return List.of(WAVE, greeting.id());
    }

    /** Seconds before this player may start another emote, 0 if now. */
    public int cooldownSeconds(Player player) {
        return cooldown.secondsLeft(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean isPlaying(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public boolean hasActive() {
        return !active.isEmpty();
    }

    public PlayResult play(Player player, String emote) {
        UUID id = player.getUniqueId();
        if (active.containsKey(id)) return PlayResult.ALREADY_PLAYING;
        boolean isGreeting = emote.equals(greeting.id());
        String animation = isGreeting ? GreetingSettings.ANIMATION : emote;
        if (cooldown.secondsLeft(id, System.currentTimeMillis()) > 0) return PlayResult.COOLDOWN;
        Plugin bm = plugin.getServer().getPluginManager().getPlugin("BetterModel");
        if (bm == null || !bm.isEnabled()) return PlayResult.UNAVAILABLE;
        long token = ++nextToken;
        AutoCloseable handle;
        try {
            handle = BetterModelHook.play(player, animation, () ->
                    plugin.getServer().getScheduler().runTask(plugin, () -> finished(id, token)));
        } catch (RuntimeException | LinkageError e) {
            plugin.getLogger().log(Level.WARNING, "Could not play emote " + emote, e);
            return PlayResult.UNAVAILABLE;
        }
        if (handle == null) return PlayResult.UNAVAILABLE;
        active.put(id, new Active(token, handle));
        cooldown.markStarted(id, System.currentTimeMillis());
        hideGear(player);
        if (isGreeting) playGreetingSound(player);
        return PlayResult.STARTED;
    }

    private void playGreetingSound(Player player) {
        GreetingSettings g = greeting;
        if (g.silent()) return;
        try {
            player.getWorld().playSound(player.getLocation(), g.sound(), SoundCategory.PLAYERS, g.volume(), g.pitch());
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.FINE, "Could not play greeting sound", e);
        }
    }

    /** Returns true if an emote was running. */
    public boolean stop(Player player, StopReason reason) {
        return stop(player.getUniqueId(), reason);
    }

    private boolean stop(UUID id, StopReason reason) {
        Active a = active.remove(id);
        if (a == null) return false;
        try {
            close(a);
        } finally {
            restoreGear(id);
        }
        return true;
    }

    private void hideGear(Player emoter) {
        List<UUID> viewers = new java.util.ArrayList<>();
        for (Player v : emoter.getWorld().getPlayers()) if (!v.equals(emoter)) viewers.add(v.getUniqueId());
        hiddenGear.hide(emoter.getUniqueId(), viewers);
        sendGear(emoter, viewers, false);
    }

    private void restoreGear(UUID id) {
        Set<UUID> viewers = hiddenGear.release(id);
        if (viewers.isEmpty()) return;
        Player emoter = plugin.getServer().getPlayer(id);
        if (emoter == null) return;
        sendGear(emoter, viewers, true);
    }

    private void sendGear(Player emoter, java.util.Collection<UUID> viewers, boolean real) {
        Map<EquipmentSlot, ItemStack> items = new EnumMap<>(EquipmentSlot.class);
        EntityEquipment eq = emoter.getEquipment();
        for (EquipmentSlot slot : GEAR_SLOTS) {
            ItemStack item = real && eq != null ? eq.getItem(slot) : null;
            items.put(slot, item != null ? item : new ItemStack(Material.AIR));
        }
        for (UUID vid : viewers) {
            Player v = plugin.getServer().getPlayer(vid);
            if (v == null || v.equals(emoter)) continue;
            try {
                v.sendEquipmentChange(emoter, items);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.FINE, "Could not send gear change", e);
            }
        }
    }

    /** Re-sends the empty gear to everyone who should see it empty. No-op if the player isn't emoting. */
    public void refreshHidden(Player emoter) {
        if (!hiddenGear.isHidden(emoter.getUniqueId())) return;
        sendGear(emoter, hiddenGear.viewers(emoter.getUniqueId()), false);
    }

    /** A player joined or moved into the emoter's world mid-emote: show them the empty gear. */
    public void showHiddenTo(Player viewer) {
        for (Player emoter : viewer.getWorld().getPlayers()) {
            if (emoter.equals(viewer) || !hiddenGear.isHidden(emoter.getUniqueId())) continue;
            hiddenGear.addViewer(emoter.getUniqueId(), viewer.getUniqueId());
            sendGear(emoter, List.of(viewer.getUniqueId()), false);
        }
    }

    private void finished(UUID id, long token) {
        Active a = active.get(id);
        if (a == null || a.token() != token) return; // stale callback from an older play
        stop(id, StopReason.FINISHED);
    }

    private void close(Active a) {
        try {
            a.handle().close();
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Closing emote tracker failed", e);
        }
    }

    public void stopAll() {
        for (UUID id : List.copyOf(active.keySet())) stop(id, StopReason.FINISHED);
    }
}
