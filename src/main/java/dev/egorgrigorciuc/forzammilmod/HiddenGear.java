package dev.egorgrigorciuc.forzammilmod;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Tracks which emoting players have their gear hidden and which viewers were sent the fake (empty) gear. No Bukkit types. */
public final class HiddenGear {

    private final Map<UUID, Set<UUID>> hidden = new HashMap<>();

    /** Marks the player hidden for these viewers (the player themself is ignored). */
    public void hide(UUID player, Collection<UUID> viewers) {
        Set<UUID> set = hidden.computeIfAbsent(player, k -> new HashSet<>());
        for (UUID v : viewers) if (!v.equals(player)) set.add(v);
    }

    /** Returns true if the viewer was newly added; false if the player isn't hidden or the viewer already known. */
    public boolean addViewer(UUID player, UUID viewer) {
        Set<UUID> set = hidden.get(player);
        if (set == null || player.equals(viewer)) return false;
        return set.add(viewer);
    }

    public boolean isHidden(UUID player) {
        return hidden.containsKey(player);
    }

    /** Viewers who currently see this player's fake gear (empty if not hidden). */
    public Set<UUID> viewers(UUID player) {
        Set<UUID> set = hidden.get(player);
        return set == null ? Set.of() : Set.copyOf(set);
    }

    /** Clears the player and returns the viewers that need the real gear back. Empty and a no-op if not hidden. */
    public Set<UUID> release(UUID player) {
        Set<UUID> set = hidden.remove(player);
        return set == null ? Set.of() : set;
    }

    /** Player to viewers, for every hidden player; clears everything. */
    public Map<UUID, Set<UUID>> releaseAll() {
        Map<UUID, Set<UUID>> all = new HashMap<>(hidden);
        hidden.clear();
        return all;
    }
}
