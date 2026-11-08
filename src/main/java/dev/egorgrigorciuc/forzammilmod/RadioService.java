package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** The shared server radio. Main thread only. */
public final class RadioService {

    private final ForzammilMod plugin;
    private final List<Track> tracks;
    private final RadioPlan plan;
    private BukkitTask timer;

    public RadioService(ForzammilMod plugin) {
        this.plugin = plugin;
        this.tracks = load(plugin);
        plugin.getLogger().info("Radio loaded " + tracks.size() + " tracks");
        List<String> ids = new ArrayList<>();
        for (Track t : tracks) ids.add(t.id());
        this.plan = new RadioPlan(new ShuffleQueue(ids, new Random()));
    }

    private static List<Track> load(ForzammilMod plugin) {
        try (InputStream in = plugin.getResource("music/tracks.yml")) {
            if (in == null) {
                plugin.getLogger().warning("music/tracks.yml is missing from the jar; the radio has no tracks");
                return List.of();
            }
            return List.copyOf(Track.parse(new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not read music/tracks.yml: " + e.getMessage());
            return List.of();
        }
    }

    public List<Track> tracks() {
        return tracks;
    }

    public Track find(String id) {
        for (Track t : tracks) if (t.id().equalsIgnoreCase(id)) return t;
        return null;
    }

    public Track current() {
        return plan.current() == null ? null : find(plan.current());
    }

    // ---- commands ----

    public void play(CommandSender actor, Track track) {
        plan.play(track.id());
        start(track);
        broadcastNamed("music.started", track, actor);
    }

    public void shuffle(CommandSender actor) {
        String first = plan.startShuffle();
        Track track = first == null ? null : find(first);
        if (track == null) return;
        start(track);
        broadcastActor("music.shuffle-on", actor);
        broadcastNamed("music.now-playing", track, null);
    }

    public void stop(CommandSender actor) {
        Track was = current();
        plan.stop();
        cancelTimer();
        for (Player p : Bukkit.getOnlinePlayers()) stopFor(p, was);
        broadcastActor("music.stopped", actor);
    }

    public void toggleLoop(CommandSender actor) {
        if (plan.current() == null) {
            actor.sendMessage(plugin.lang().msg(actor, "music.loop-nothing"));
            return;
        }
        boolean on = plan.toggleLoop();
        broadcastActor(on ? "music.loop-on" : "music.loop-off", actor);
    }

    public void sendList(CommandSender to) {
        String lang = plugin.lang().languageOf(to);
        to.sendMessage(plugin.lang().plural(to, "music.list-header", tracks.size()));
        Track now = current();
        for (Track t : tracks) {
            to.sendMessage(plugin.lang().text(lang, t == now ? "music.list-playing" : "music.list-line",
                    Map.of("id", t.id(), "name", t.name(lang))));
        }
    }

    public void sendJoinInfo(Player p) {
        Track now = current();
        if (now == null) return;
        p.sendMessage(plugin.lang().msg(p, "music.join", "name", now.name(plugin.lang().languageOf(p))));
    }

    public void shutdown() {
        Track was = current();
        plan.stop();
        cancelTimer();
        for (Player p : Bukkit.getOnlinePlayers()) stopFor(p, was);
    }

    // ---- internals ----

    private void start(Track track) {
        cancelTimer();
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Track t : tracks) p.stopSound(t.sound(), SoundCategory.RECORDS);
            p.playSound(p, track.sound(), SoundCategory.RECORDS, 1f, 1f);
        }
        long ticks = track.lengthMs() / 50 + 20;
        timer = Bukkit.getScheduler().runTaskLater(plugin, this::trackEnded, ticks);
    }

    private void trackEnded() {
        timer = null;
        RadioPlan.NextStep step = plan.onTrackEnd();
        if (step instanceof RadioPlan.Replay r && find(r.id()) != null) {
            start(find(r.id()));
        } else if (step instanceof RadioPlan.Next n && find(n.id()) != null) {
            Track t = find(n.id());
            start(t);
            broadcastNamed("music.now-playing", t, null);
        }
    }

    private void stopFor(Player p, Track was) {
        if (was != null) p.stopSound(was.sound(), SoundCategory.RECORDS);
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private String actorName(CommandSender actor, String lang) {
        return actor instanceof Player ? actor.getName() : plugin.lang().text(lang, "music.console", Map.of());
    }

    private void broadcastNamed(String key, Track track, CommandSender actor) {
        for (Player p : Bukkit.getOnlinePlayers()) sendNamed(p, key, track, actor);
        if (actor != null && !(actor instanceof Player)) sendNamed(actor, key, track, actor);
    }

    private void sendNamed(CommandSender to, String key, Track track, CommandSender actor) {
        String lang = plugin.lang().languageOf(to);
        to.sendMessage(plugin.lang().text(lang, key, Map.of(
                "name", track.name(lang), "actor", actor == null ? "" : actorName(actor, lang))));
    }

    private void broadcastActor(String key, CommandSender actor) {
        for (Player p : Bukkit.getOnlinePlayers()) sendActor(p, key, actor);
        if (!(actor instanceof Player)) sendActor(actor, key, actor);
    }

    private void sendActor(CommandSender to, String key, CommandSender actor) {
        String lang = plugin.lang().languageOf(to);
        to.sendMessage(plugin.lang().text(lang, key, Map.of("actor", actorName(actor, lang))));
    }
}
