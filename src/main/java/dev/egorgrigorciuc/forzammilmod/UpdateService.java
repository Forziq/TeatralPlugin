package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.function.Consumer;

/** Runs update checks off the main thread. */
public final class UpdateService {

    private final ForzammilMod plugin;
    private final UpdateClient client = new UpdateClient(UpdateClient.GITHUB_API);

    public UpdateService(ForzammilMod plugin) {
        this.plugin = plugin;
    }

    /** Async check; the callback runs on the main thread. */
    public void check(Consumer<UpdateResult> done) {
        String token = plugin.getConfig().getString("github-token", "");
        String current = plugin.getDescription().getVersion();
        String jarName = plugin.jarFileName();
        java.nio.file.Path dir = Bukkit.getUpdateFolderFile().toPath();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UpdateResult r;
            try {
                r = client.checkAndDownload(token, current, dir, jarName);
            } catch (Throwable t) {
                r = new UpdateResult.Error(String.valueOf(t));
            }
            UpdateResult result = r;
            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> done.accept(result));
            }
        });
    }

    public void checkOnStartup() {
        check(r -> {
            if (r instanceof UpdateResult.Downloaded d) {
                plugin.getLogger().info("Downloaded forzammilmod v" + d.version() + "; restart the server to apply it.");
            } else if (r instanceof UpdateResult.Error e) {
                plugin.getLogger().warning("Update check failed: " + e.reason());
            }
        });
    }

    public void checkFor(CommandSender sender) {
        Lang lang = plugin.lang();
        sender.sendMessage(lang.msg(sender, "update.checking"));
        check(r -> {
            String current = plugin.getDescription().getVersion();
            if (r instanceof UpdateResult.UpToDate) {
                sender.sendMessage(lang.msg(sender, "update.up-to-date", "version", current));
            } else if (r instanceof UpdateResult.Downloaded d) {
                sender.sendMessage(lang.msg(sender, "update.downloaded", "version", d.version()));
                plugin.getLogger().info("Downloaded forzammilmod v" + d.version() + "; restart the server to apply it.");
            } else if (r instanceof UpdateResult.Error e) {
                sender.sendMessage(lang.msg(sender, "update.error", "reason", e.reason()));
                plugin.getLogger().warning("Update check failed: " + e.reason());
            }
        });
    }
}
