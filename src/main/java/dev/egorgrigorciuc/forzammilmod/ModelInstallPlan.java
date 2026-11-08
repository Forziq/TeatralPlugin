package dev.egorgrigorciuc.forzammilmod;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Pure logic for installing our bundled models into BetterModel's folder: where each jar entry goes,
 * and which files need copying. No Bukkit, no file system.
 */
public final class ModelInstallPlan {

    /** Jar folder holding our models. */
    public static final String JAR_PREFIX = "models/";
    /** Sub-folder (inside {@link #JAR_PREFIX}) for player-animation models; goes to BetterModel's players/. */
    public static final String PLAYERS = "players/";

    public enum Reason { MISSING, CHANGED }

    /** One file bundled in our jar. */
    public record Bundled(String entry, byte[] data) {}

    /** One file to write, with its path relative to BetterModel's data folder. */
    public record Copy(String entry, String target, byte[] data, Reason reason) {}

    private ModelInstallPlan() {}

    /**
     * Maps a jar entry to a path relative to BetterModel's data folder.
     * {@code models/players/x.bbmodel -> players/x.bbmodel}, {@code models/x.bbmodel -> models/x.bbmodel}.
     * Empty for directories, entries outside {@code models/}, and anything that could escape the folder.
     */
    public static Optional<String> targetPath(String entry) {
        if (entry == null || !entry.startsWith(JAR_PREFIX) || entry.endsWith("/") || entry.contains("\\")) {
            return Optional.empty();
        }
        String rest = entry.substring(JAR_PREFIX.length());
        if (rest.isEmpty() || rest.startsWith("/")) return Optional.empty();
        for (String part : rest.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) return Optional.empty();
        }
        if (rest.startsWith(PLAYERS)) {
            return rest.length() > PLAYERS.length() ? Optional.of(rest) : Optional.empty();
        }
        return Optional.of(JAR_PREFIX + rest);
    }

    public static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 missing from the JVM", e);
        }
    }

    /** MISSING when there is no file, CHANGED when the SHA-1 differs, empty when it already matches. */
    public static Optional<Reason> reason(byte[] bundled, byte[] existing) {
        if (existing == null) return Optional.of(Reason.MISSING);
        if (!sha1(bundled).equals(sha1(existing))) return Optional.of(Reason.CHANGED);
        return Optional.empty();
    }

    /**
     * Works out which bundled files to write.
     *
     * @param existing returns the current bytes at a target path, or null if there is no file
     */
    public static List<Copy> plan(List<Bundled> bundled, Function<String, byte[]> existing) {
        List<Copy> out = new ArrayList<>();
        for (Bundled b : bundled) {
            Optional<String> target = targetPath(b.entry());
            if (target.isEmpty()) continue;
            byte[] data = Arrays.copyOf(b.data(), b.data().length);
            reason(data, existing.apply(target.get()))
                    .ifPresent(r -> out.add(new Copy(b.entry(), target.get(), data, r)));
        }
        return out;
    }
}
