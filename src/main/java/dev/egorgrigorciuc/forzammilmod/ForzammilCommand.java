package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

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
    }

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
        return out;
    }
}
