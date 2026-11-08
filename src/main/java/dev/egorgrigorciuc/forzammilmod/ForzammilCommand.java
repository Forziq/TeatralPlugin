package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class ForzammilCommand implements TabExecutor {

    @FunctionalInterface
    private interface Sub {
        void run(CommandSender sender, String[] args);
    }

    private final Map<String, Sub> subs = new TreeMap<>();

    private final ForzammilMod plugin;

    public ForzammilCommand(ForzammilMod plugin) {
        this.plugin = plugin;
        subs.put("version", (sender, args) ->
                sender.sendMessage(plugin.lang().msg(sender, "version", "version", plugin.getDescription().getVersion())));
        subs.put("update", (sender, args) -> {
            if (!Access.isOwner(sender)) {
                sender.sendMessage(plugin.lang().msg(sender, "no-permission"));
                return;
            }
            plugin.updates().checkFor(sender);
        });
        subs.put("pack", (sender, args) -> checkPack(sender));
        subs.put("music", this::music);
        subs.put("vehicle", this::vehicle);
    }

    static final List<String> VEHICLE_SUBS = List.of("spawn", "remove", "list");
    static final double SPAWN_REACH = 30;
    static final double REMOVE_RADIUS = 10;

    private void vehicle(CommandSender sender, String[] args) {
        Lang lang = plugin.lang();
        VehicleService vehicles = plugin.vehicles();
        String sub = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        if (vehicles == null || !VEHICLE_SUBS.contains(sub)) {
            sender.sendMessage(lang.msg(sender, "vehicle.usage", "label", "fmod"));
            return;
        }
        if (sub.equals("list")) {
            listVehicles(sender, vehicles);
            return;
        }
        if (!Access.isOwner(sender)) {
            sender.sendMessage(lang.msg(sender, "no-permission"));
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang.msg(sender, "players-only"));
            return;
        }
        if (sub.equals("remove")) {
            Vehicle v = vehicles.nearest(p.getLocation(), REMOVE_RADIUS);
            if (v == null) {
                p.sendMessage(lang.msg(p, "vehicle.none-nearby"));
                return;
            }
            vehicles.remove(v);
            p.sendMessage(lang.msg(p, "vehicle.removed", "type", typeName(p, v.type)));
            return;
        }
        VehicleType type = args.length < 3 ? null : VehicleType.byId(args[2]);
        if (type == null) {
            p.sendMessage(lang.msg(p, "vehicle.bad-type"));
            return;
        }
        if (!vehicles.available()) {
            p.sendMessage(lang.msg(p, "vehicle.unavailable"));
            return;
        }
        RayTraceResult hit = p.rayTraceBlocks(SPAWN_REACH, FluidCollisionMode.NEVER);
        Block block = hit == null ? null : hit.getHitBlock();
        if (block == null) {
            p.sendMessage(lang.msg(p, "vehicle.no-target"));
            return;
        }
        double top = block.getY() + 1;
        for (BoundingBox b : block.getCollisionShape().getBoundingBoxes()) top = Math.max(top, block.getY() + b.getMaxY());
        Location at = new Location(block.getWorld(), block.getX() + 0.5, top, block.getZ() + 0.5);
        // Same yaw as the owner's look, so the front points away from them.
        switch (vehicles.spawn(type, at, p.getLocation().getYaw())) {
            case SPAWNED -> p.sendMessage(lang.msg(p, "vehicle.spawned", "type", typeName(p, type)));
            case MODEL_MISSING -> p.sendMessage(lang.msg(p, "vehicle.model-missing", "type", typeName(p, type)));
            case UNAVAILABLE -> p.sendMessage(lang.msg(p, "vehicle.unavailable"));
        }
    }

    private void listVehicles(CommandSender sender, VehicleService vehicles) {
        Lang lang = plugin.lang();
        List<Vehicle> all = vehicles.vehicles();
        if (all.isEmpty()) {
            sender.sendMessage(lang.msg(sender, "vehicle.list-empty"));
            return;
        }
        sender.sendMessage(lang.plural(sender, "vehicle.list-header", all.size()));
        for (Vehicle v : all) {
            var s = v.state();
            sender.sendMessage(lang.msg(sender, "vehicle.list-line", "type", typeName(sender, v.type),
                    "world", v.world.getName(), "x", (long) Math.floor(s.x()), "y", (long) Math.floor(s.y()),
                    "z", (long) Math.floor(s.z())));
        }
    }

    private String typeName(CommandSender sender, VehicleType type) {
        return plugin.lang().msg(sender, "vehicle.name." + type.id);
    }

    /** Tells the player what their client reported for our pack, then plays one of our sounds as a hands-on test. */
    private void checkPack(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.lang().msg(sender, "players-only"));
            return;
        }
        PackService pack = plugin.pack();
        if (pack == null) {
            p.sendMessage(plugin.lang().msg(p, "pack.check-off"));
            return;
        }
        var status = pack.status(p);
        String key;
        if (status == null) {
            key = "pack.check-unknown";
        } else {
            key = switch (status) {
                case SUCCESSFULLY_LOADED -> "pack.check-loaded";
                case ACCEPTED, DOWNLOADED -> "pack.check-loading";
                default -> "pack.check-failed";
            };
        }
        p.sendMessage(plugin.lang().msg(p, key));
        p.sendMessage(plugin.lang().msg(p, "pack.check-sound"));
        p.playSound(p, TEST_SOUND, 1f, 1f);
    }

    static final List<String> MUSIC_SUBS = List.of("play", "stop", "shuffle", "loop", "list");

    private void music(CommandSender sender, String[] args) {
        RadioService radio = plugin.radio();
        String sub = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        if (radio == null || !MUSIC_SUBS.contains(sub)) {
            sender.sendMessage(plugin.lang().msg(sender, "music.usage", "label", "fmod"));
            return;
        }
        switch (sub) {
            case "play" -> {
                Track track = args.length < 3 ? null : radio.find(args[2]);
                if (track == null) {
                    if (args.length >= 3) sender.sendMessage(plugin.lang().msg(sender, "music.no-such-track"));
                    else sender.sendMessage(plugin.lang().msg(sender, "music.usage", "label", "fmod"));
                    radio.sendList(sender);
                    return;
                }
                radio.play(sender, track);
            }
            case "stop" -> radio.stop(sender);
            case "shuffle" -> radio.shuffle(sender);
            case "loop" -> radio.toggleLoop(sender);
            default -> radio.sendList(sender);
        }
    }

    static final String TEST_SOUND = "forzammilmod:emote_cheer";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Sub sub = args.length == 0 ? null : subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            sender.sendMessage(plugin.lang().msg(sender, "usage", "label", label, "subs", String.join("|", subs.keySet())));
            return true;
        }
        sub.run(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String name : subs.keySet()) {
                if (name.startsWith(prefix)) {
                    out.add(name);
                }
            }
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("music")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (args.length == 2) {
                for (String name : MUSIC_SUBS) if (name.startsWith(prefix)) out.add(name);
            } else if (args.length == 3 && args[1].equalsIgnoreCase("play") && plugin != null && plugin.radio() != null) {
                String p3 = args[2].toLowerCase(Locale.ROOT);
                for (Track t : plugin.radio().tracks()) if (t.id().startsWith(p3)) out.add(t.id());
            }
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("vehicle")) {
            String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
            if (args.length == 2) {
                for (String name : VEHICLE_SUBS) if (name.startsWith(prefix)) out.add(name);
            } else if (args.length == 3 && args[1].equalsIgnoreCase("spawn")) {
                for (VehicleType t : VehicleType.values()) if (t.id.startsWith(prefix)) out.add(t.id);
            }
        }
        return out;
    }
}
