package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EmoteCommand implements TabExecutor {

    private final Lang lang;
    private final EmoteService emotes;

    public EmoteCommand(Lang lang, EmoteService emotes) {
        this.lang = lang;
        this.emotes = emotes;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.msg(sender, "players-only"));
            return true;
        }
        if (args.length == 0) {
            GreetingSettings g = emotes.greeting();
            player.sendMessage(lang.plural(player, "emote.count", emotes.emoteIds().size())
                    + " " + EmoteService.WAVE + ", " + g.id() + " (" + g.nameFor(lang.languageOf(player)) + ")");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (name.equals("stop")) {
            if (emotes.stop(player, EmoteService.StopReason.COMMAND)) {
                player.sendMessage(lang.msg(player, "emote.stopped"));
            } else {
                player.sendMessage(lang.msg(player, "emote.not-playing"));
            }
            return true;
        }
        if (!emotes.emoteIds().contains(name)) {
            player.sendMessage(lang.msg(player, "emote.unknown"));
            return true;
        }
        switch (emotes.play(player, name)) {
            case STARTED -> player.sendMessage(lang.msg(player, "emote.started"));
            case ALREADY_PLAYING -> player.sendMessage(lang.msg(player, "emote.already-playing"));
            case UNAVAILABLE -> player.sendMessage(lang.msg(player, "emote.unavailable"));
            case COOLDOWN -> player.sendMessage(lang.plural(player, "emote.cooldown", emotes.cooldownSeconds(player)));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>(emotes.emoteIds());
        out.add("stop");
        out.removeIf(s -> !s.startsWith(prefix));
        return out;
    }
}
