package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** One active emote per player. Main thread only (BetterModel end callbacks hop back via the scheduler). */
public final class EmoteService {

    public enum StopReason { COMMAND, MOVED, DAMAGE, DEATH, TELEPORT, QUIT, FINISHED }

    public enum PlayResult { STARTED, ALREADY_PLAYING, UNAVAILABLE }

    public static final List<String> EMOTES = List.of("wave");

    private record Active(long token, AutoCloseable handle) {}

    private final JavaPlugin plugin;
    private final Map<UUID, Active> active = new HashMap<>();
    private long nextToken;

    public EmoteService(JavaPlugin plugin) {
        this.plugin = plugin;
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
        Plugin bm = plugin.getServer().getPluginManager().getPlugin("BetterModel");
        if (bm == null || !bm.isEnabled()) return PlayResult.UNAVAILABLE;
        long token = ++nextToken;
        AutoCloseable handle;
        try {
            handle = BetterModelHook.play(player, emote, () ->
                    plugin.getServer().getScheduler().runTask(plugin, () -> finished(id, token)));
        } catch (RuntimeException | LinkageError e) {
            plugin.getLogger().log(Level.WARNING, "Could not play emote " + emote, e);
            return PlayResult.UNAVAILABLE;
        }
        if (handle == null) return PlayResult.UNAVAILABLE;
        active.put(id, new Active(token, handle));
        return PlayResult.STARTED;
    }

    /** Returns true if an emote was running. */
    public boolean stop(Player player, StopReason reason) {
        return stop(player.getUniqueId(), reason);
    }

    private boolean stop(UUID id, StopReason reason) {
        Active a = active.remove(id);
        if (a == null) return false;
        close(a);
        return true;
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
        for (Active a : active.values()) close(a);
        active.clear();
    }
}
